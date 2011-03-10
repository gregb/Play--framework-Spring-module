package play.modules.spring;

import org.junit.Ignore;

import play.classloading.ApplicationClassloader;


@Ignore
public class SpringEnhancerTest extends ApplicationClassloader {

	public void testInjection() throws Exception {
		/*
		 * Play.init(new File("test"), "test"); Play.start();
		 * 
		 * final Class classToRun =
		 * Play.classloader.loadClass("controllers.TestController");
		 * 
		 * final ApplicationClassloader acl = new ApplicationClassloader();
		 * 
		 * final SpringEnhancer enhancer = new SpringEnhancer();
		 * 
		 * final ApplicationClass applicationClass = new
		 * ApplicationClass("controllers.TestController");
		 * 
		 * final Class<?> loadApplicationClass =
		 * loadApplicationClass("controllers.TestController");
		 * applicationClass.javaSource = ""; applicationClass.javaByteCode =
		 * applicationClass.compile(); applicationClass.enhancedByteCode =
		 * applicationClass.enhance();
		 * 
		 * enhancer.enhanceThisClass(applicationClass);
		 */
	}
}
