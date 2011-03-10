package play.modules.spring.scoping;

import play.Logger;
import play.mvc.Scope.Session;

public class SessionScope extends AbstractScope {

	@Override
	public String getConversationId() {

		if (Session.current() == null) {
			Logger.warn("No current session, using null");
			return "null";
		}

		final String newId = Session.current().getId();
		Logger.debug("Registering new session " + newId);
		return newId;
	}

	@Override
	public Object resolveContextualObject(final String key) {
		if ("session".equals(key)) {
			return Session.current();
		}

		return null;
	}

}
