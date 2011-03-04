package play.modules.spring.scoping;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.Period;

import play.Logger;

public class ScopeUnit {

	private final DateTime creationTime;
	private final String conversationId;
	private final Map<String, Object> beans = new WeakHashMap<String, Object>();
	private final Map<String, Runnable> destructionCallbacks = new HashMap<String, Runnable>();

	public ScopeUnit(final String conversationId, final Period timeout) {
		Logger.debug("Creating sope unit for conversation " + conversationId);
		this.conversationId = conversationId;
		this.creationTime = new DateTime();

		final long delay = this.creationTime.withPeriodAdded(timeout, 0).getMillis();

		Logger.debug("This unit will be destroyed in " + timeout.toString());

		final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
		executor.schedule(new Runnable() {

			@Override
			public void run() {
				destroy();
			}
		}, delay, TimeUnit.MILLISECONDS);
	}

	public ScopeUnit(final String conversationId) {
		Logger.debug("Creating sope unit for conversation " + conversationId);
		this.conversationId = conversationId;
		this.creationTime = new DateTime();
	}

	public Object getBean(final String beanName) {
		return this.beans.get(beanName);
	}

	public void addBean(final String beanName, final Object bean) {
		this.beans.put(beanName, bean);
	}

	public void registerDestructionCallback(final String beanName, final Runnable callback) {
		this.destructionCallbacks.put(beanName, callback);
	}

	public void destroy() {
		Logger.debug("Destroying unit " + this.conversationId);
		for (final Runnable callback : this.destructionCallbacks.values()) {
			callback.run();
		}
	}

	public Object removeBean(final String beanName) {
		return this.beans.remove(beanName);
	}

}
