package play.modules.spring;

import java.util.HashMap;
import java.util.Map;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

import javax.inject.Inject;

import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;
import play.exceptions.UnexpectedException;

public class SpringEnhancer extends Enhancer {

	private static final String INJECT_CLASS = Inject.class.getName();
	private static final String SPRING_CLASS = Spring.class.getName();

	@Override
	public void enhanceThisClass(final ApplicationClass applicationClass) throws Exception {
		final CtClass ctClass = makeClass(applicationClass);
		if (ctClass.isInterface()) {
			return;
		}

		final Map<String, CtField> fieldsToInject = new HashMap<String, CtField>();

		for (final CtField ctField : ctClass.getDeclaredFields()) {

			if (hasAnnotation(ctField, INJECT_CLASS)) {
				fieldsToInject.put(ctField.getName(), ctField);
			}
		}

		for (final CtMethod ctMethod : ctClass.getDeclaredMethods()) {
			ctMethod.instrument(new ExprEditor() {
				@Override
				public void edit(final FieldAccess fieldAccess) {

					final CtField override = fieldsToInject.get(fieldAccess.getFieldName());

					if (override != null) {
						try {
							if (fieldAccess.isReader()) {
								fieldAccess.replace("$_ = ($r)play.utils.Java.invokeStatic(\"" + SPRING_CLASS + "\", \"getBeanOfType\", $type);");
							}
						}
						catch (final Exception e) {
							Logger.error(e, "Error in SpringEnhancer. %s.%s has not been properly enhanced (fieldAccess %s).", applicationClass.name, ctMethod.getName(), fieldAccess);
							throw new UnexpectedException(e);
						}
					}

				}
			});
		}

		applicationClass.enhancedByteCode = ctClass.toBytecode();
		ctClass.defrost();
	}
}
