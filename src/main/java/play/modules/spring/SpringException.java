package play.modules.spring;

import play.exceptions.PlayException;

public abstract class SpringException extends PlayException {

	@Override
	public String getErrorTitle() {
		return "Spring Error";
	}

}