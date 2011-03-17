package play.modules.spring;

import java.util.Map;

import play.Play;
import play.exceptions.UnexpectedException;

public class Spring {

	public static Object getBean(final String name) {
		if (SpringPlugin.applicationContext == null) {
			throw new SpringException() {
				@Override
				public String getErrorDescription() {
					return "Can't get beans.  Application context is not (yet) available";
				}
			};
		}
		return SpringPlugin.applicationContext.getBean(name);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getBeanOfType(final Class<T> type) {
		final Map<String, Object> beans = getBeansOfType(type);
		if (beans.isEmpty()) {
			return null;
		}
		return (T) beans.values().iterator().next();
	}

	public static Object getBeanOfType(final String type) {
		try {
			return getBeanOfType(Play.classloader.loadClass(type));
		}
		catch (final ClassNotFoundException ex) {
			throw new UnexpectedException(ex);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> Map<String, T> getBeansOfType(final Class type) {
		if (SpringPlugin.applicationContext == null) {
			throw new SpringException() {
				@Override
				public String getErrorDescription() {
					return "Can't get beans.  Application context is not (yet) available";
				}
			};
		}
		return SpringPlugin.applicationContext.getBeansOfType(type);
	}

}
