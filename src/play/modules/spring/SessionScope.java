package play.modules.spring;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import play.Logger;
import play.mvc.Scope.Session;

public class SessionScope implements Scope {

	private final Map<String, Map<String, Object>> beanRepositories = new HashMap<String, Map<String, Object>>();

	public SessionScope() {
	}

	@Override
	public Object remove(final String beanName) {
		Logger.info("Removing " + beanName);
		final Map<String, Object> beanRepositoryForCurrentSite = getSessionBeans();
		synchronized (beanRepositoryForCurrentSite) {
			return beanRepositoryForCurrentSite.remove(beanName);
		}
	}

	@Override
	public String getConversationId() {

		if (Session.current() == null) {
			Logger.warn("No current session, using null");
			return "null";
		}
		return Session.current().getId();
	}

	@Override
	public void registerDestructionCallback(final String beanName, final Runnable callback) {
		// Implement this if you need it
		Logger.warn("Not implemented: registerDestructionCallback(" + beanName + ")");
	}

	@Override
	public Object get(final String beanName, final ObjectFactory<?> objectFactory) {
		Logger.debug("Getting bean " + beanName + " from session, with parent factory = " + objectFactory);
		final Map<String, Object> sessionBeans = getSessionBeans();
		synchronized (sessionBeans) {
			if (!sessionBeans.containsKey(beanName)) {
				Logger.debug("Session does not already contain bean " + beanName);
				final Object bean = objectFactory.getObject();
				Logger.debug("Created from parent factory: " + bean);
				sessionBeans.put(beanName, bean);
			}

			final Object bean = sessionBeans.get(beanName);
			Logger.debug("Returning " + bean);
			return bean;
		}
	}

	private Map<String, Object> getSessionBeans() {
		final Map<String, Object> sessionBeans = this.beanRepositories.get(getConversationId());
		return (sessionBeans == null ? new HashMap<String, Object>() : sessionBeans);
	}

	@Override
	public Object resolveContextualObject(final String arg0) {
		Logger.warn("Not implemented: resolveContextualObject(" + arg0 + ")");
		return null;
	}
}
