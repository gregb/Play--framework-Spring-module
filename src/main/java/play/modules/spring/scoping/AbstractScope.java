package play.modules.spring.scoping;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import play.Logger;

public abstract class AbstractScope implements Scope {

	private Period timeoutPeriod;
	private final Map<String, ScopeUnit> scopeUnits = new ConcurrentHashMap<String, ScopeUnit>();

	@Override
	public Object remove(final String beanName) {
		Logger.debug("Removing " + beanName);

		final ScopeUnit unit = getScopeUnit();

		synchronized (unit) {
			return unit.removeBean(beanName);
		}
	}

	@Override
	public void registerDestructionCallback(final String beanName, final Runnable callback) {
		final ScopeUnit unit = getScopeUnit();
		unit.registerDestructionCallback(beanName, callback);
	}

	@Override
	public Object get(final String beanName, final ObjectFactory<?> objectFactory) {
		Logger.debug("Getting bean " + beanName + " from scope, with parent factory = " + objectFactory);

		final ScopeUnit unit = getScopeUnit();

		synchronized (unit) {

			Object bean = unit.getBean(beanName);

			if (bean == null) {
				Logger.debug("Scope does not already contain bean " + beanName);
				bean = objectFactory.getObject();
				Logger.debug("Created from parent factory: " + bean);
				unit.addBean(beanName, bean);
			}

			Logger.debug("Returning " + bean);
			return bean;
		}
	}

	private ScopeUnit getScopeUnit() {
		ScopeUnit unit = this.scopeUnits.get(getConversationId());

		if (unit == null) {
			if (this.timeoutPeriod == null) {
				unit = new ScopeUnit(getConversationId());
			}
			else {
				unit = new ScopeUnit(getConversationId(), this.timeoutPeriod.getMillis());
			}

			this.scopeUnits.put(getConversationId(), unit);
		}
		return unit;
	}

	public void setTimeoutPeriod(final String timeoutPeriod) {
		this.timeoutPeriod = timeoutPeriod == null ? null : PeriodFormat.getDefault().parsePeriod(timeoutPeriod);
	}
}
