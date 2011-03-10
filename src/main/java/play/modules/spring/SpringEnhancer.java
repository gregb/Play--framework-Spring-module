package play.modules.spring;

import javassist.CtClass;
import javassist.CtField;

import javax.inject.Inject;

import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;

public class SpringEnhancer extends Enhancer {

	private static final String INJECT_CLASS = Inject.class.getName();

	@Override
	public void enhanceThisClass(final ApplicationClass applicationClass) throws Exception {
		final CtClass ctClass = makeClass(applicationClass);
		if (ctClass.isInterface()) {
			return;
		}

		for (final CtField ctField : ctClass.getDeclaredFields()) {

			if (hasAnnotation(ctField, INJECT_CLASS)) {
				ctClass.removeField(ctField);
				final CtField newCtField = new CtField(ctField.getType(), ctField.getName(), ctClass);
				newCtField.setModifiers(ctField.getModifiers());
				ctClass.addField(newCtField, "play.modules.spring.Spring.getBeanOfType(\"" + newCtField.getType().getName() + "\");");
			}
		}
		applicationClass.enhancedByteCode = ctClass.toBytecode();
		ctClass.defrost();
	}
}
