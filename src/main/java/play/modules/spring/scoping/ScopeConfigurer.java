package play.modules.spring.scoping;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.stereotype.Component;

@Component
public class ScopeConfigurer extends CustomScopeConfigurer {

	public ScopeConfigurer() {

		final Map<String, Object> scopes = new HashMap<String, Object>();
		scopes.put("session", new SessionScope());
		scopes.put("request", new RequestScope());
		setScopes(scopes);
	}

}
