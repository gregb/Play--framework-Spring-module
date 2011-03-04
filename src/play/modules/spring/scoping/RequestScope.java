package play.modules.spring.scoping;

import play.Logger;
import play.mvc.Http;

public class RequestScope extends AbstractScope {

	@Override
	public String getConversationId() {

		if (Http.Request.current() == null) {
			Logger.warn("No current request, using null");
			return "null";
		}

		final String newId = Integer.toString(Http.Request.current().hashCode());
		Logger.debug("Registering new request " + newId);
		return newId;
	}

}
