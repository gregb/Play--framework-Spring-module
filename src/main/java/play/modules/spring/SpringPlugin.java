package play.modules.spring;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.xml.sax.InputSource;

import play.Logger;
import play.Play;
import play.Play.Mode;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.PlayException;
import play.inject.BeanSource;
import play.inject.Injector;
import play.modules.spring.scoping.ScopeConfigurer;
import play.vfs.VirtualFile;

public class SpringPlugin extends PlayPlugin implements BeanSource {

	/**
	 * Component scanning constants.
	 */
	private static final String PLAY_SPRING_COMPONENT_SCAN_FLAG = "play.spring.component-scan";
	private static final String PLAY_SPRING_COMPONENT_SCAN_BASE_PACKAGES = "play.spring.component-scan.base-packages";
	private static final String PLAY_SPRING_ADD_PLAY_PROPERTIES = "play.spring.add-play-properties";
	private static final String PLAY_SPRING_NAMESPACE_AWARE = "play.spring.namespace-aware";

	public static AnnotationConfigApplicationContext applicationContext;
	private long startDate = 0;

	@Override
	public void detectChange() {
		if (Play.mode == Mode.DEV) {
			final VirtualFile appRoot = VirtualFile.open(Play.applicationPath);
			final long mod = appRoot.child("conf/application-context.xml").lastModified();
			if (mod > startDate) {
				throw new RuntimeException("conf/application-context.xml has changed");
			}
		}
	}

	@Override
	public void onApplicationStop() {
		if (applicationContext != null) {
			Logger.debug("Closing Spring application context");
			applicationContext.close();
		}
	}

	@Override
	public void enhance(final ApplicationClass applicationClass) throws Exception {
		// do nothing
	}

	@Override
	public void onApplicationStart() {

		Logger.debug("Starting Spring application context");
		applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.setClassLoader(Play.classloader);

		final BeanDefinition scopeConfigurer = new RootBeanDefinition(ScopeConfigurer.class);
		applicationContext.registerBeanDefinition("scopeConfigurer", scopeConfigurer);

		if (Play.configuration.getProperty(PLAY_SPRING_ADD_PLAY_PROPERTIES, "true").equals("true")) {
			Logger.debug("Adding PropertyPlaceholderConfigurer with Play properties");
			final PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
			configurer.setProperties(Play.configuration);
			applicationContext.addBeanFactoryPostProcessor(configurer);
		}
		else {
			Logger.debug("PropertyPlaceholderConfigurer with Play properties NOT added");
		}

		// Check for component scan
		final boolean doComponentScan = Play.configuration.getProperty(PLAY_SPRING_COMPONENT_SCAN_FLAG, "false").equals("true");
		Logger.debug("Spring configuration do component scan: " + doComponentScan);
		if (doComponentScan) {
			final String scanBasePackage = Play.configuration.getProperty(PLAY_SPRING_COMPONENT_SCAN_BASE_PACKAGES, "");
			Logger.debug("Base package for scan: " + scanBasePackage);
			Logger.debug("Scanning...");
			applicationContext.scan(scanBasePackage.split(","));
			Logger.debug("... component scanning complete");
		}

		URL url = Play.classloader.getResource(Play.id + ".application-context.xml");
		if (url == null) {
			url = Play.classloader.getResource("application-context.xml");
		}
		if (url != null) {
			Logger.info("Loading beans from " + url);
			loadBeans(url);
		}

		Injector.inject(this);
	}

	private XmlBeanDefinitionReader buildBeanDefinitionReader() {
		final XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(applicationContext);
		if (Play.configuration.getProperty(PLAY_SPRING_NAMESPACE_AWARE, "false").equals("true")) {
			xmlReader.setNamespaceAware(true);
		}
		xmlReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);

		return xmlReader;
	}

	private void loadBeans(final URL url) {
		final XmlBeanDefinitionReader xmlReader = buildBeanDefinitionReader();

		InputStream is = null;
		try {
			is = url.openStream();
			xmlReader.loadBeanDefinitions(new InputSource(is));
			final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(Play.classloader);
			try {
				applicationContext.refresh();
				startDate = System.currentTimeMillis();
			}
			catch (final BeanCreationException e) {
				final Throwable ex = e.getCause();
				if (ex instanceof PlayException) {
					throw (PlayException) ex;
				}
				else {
					throw e;
				}
			}
			finally {
				Thread.currentThread().setContextClassLoader(originalClassLoader);
			}
		}
		catch (final IOException e) {
			Logger.error(e, "Can't load spring config file");
		}
		finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (final IOException e) {
					Logger.error(e, "Can't close spring config file stream");
				}
			}
		}
	}

	@Override
	public <T> T getBeanOfType(final Class<T> clazz) {
		Logger.debug("Injecting a bean from the spring context: " + clazz);

		if (applicationContext == null) {
			Logger.warn("Attempting to get a bean from a null application context");
			return null;
		}

		final Map<String, T> beans = applicationContext.getBeansOfType(clazz);
		if (beans.size() == 0) {
			Logger.warn("No beans of type " + clazz);
			return null;
		}

		final T bean = beans.values().iterator().next();
		Logger.debug("Returning: " + bean);
		return bean;
	}
}
