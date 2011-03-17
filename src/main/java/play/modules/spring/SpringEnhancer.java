package play.modules.spring;

import java.util.HashMap;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;
import play.exceptions.UnexpectedException;
import play.modules.spring.SpringEnhancer.InjectionInfo.InjectionMethod;

public class SpringEnhancer extends Enhancer {

	static class InjectionInfo {

		public static enum InjectionMethod {
			BY_TYPE,
			BY_NAME
		}

		public InjectionInfo(final Class<?> beanType) {
			this.beanType = beanType;
		}

		private InjectionMethod injectionMethod;
		private final Class<?> beanType;
		private String beanName;
	}

	@Override
	public void enhanceThisClass(final ApplicationClass applicationClass) throws Exception {
		final CtClass ctClass = makeClass(applicationClass);
		if (ctClass.isInterface()) {
			return;
		}

		final Map<CtField, InjectionInfo> fieldsToInject = scanForInjections(ctClass);

		// in all methods, replace the field accesses with a call to spring
		for (final CtMethod ctMethod : ctClass.getDeclaredMethods()) {
			ctMethod.instrument(new ExprEditor() {
				@Override
				public void edit(final FieldAccess fieldAccess) {
					try {
						final InjectionInfo injectionInfo = fieldsToInject.get(fieldAccess.getField());

						if (injectionInfo != null && fieldAccess.isReader()) {

							switch (injectionInfo.injectionMethod) {
								case BY_NAME:
									fieldAccess.replace("$_ = ($r)play.utils.Java.invokeStatic(play.modules.spring.Spring.class, \"getBeanOfType\", new Object[] {$type});");
									break;
								case BY_TYPE:
									fieldAccess.replace("$_ = ($r)play.utils.Java.invokeStatic(play.modules.spring.Spring.class, \"getBean\", new Object[] {\"" + injectionInfo.beanName + "\"});");
									break;
							}
						}
					}
					catch (final Exception e) {
						Logger.error(e, "Error in SpringEnhancer. %s.%s has not been properly enhanced (fieldAccess %s).", applicationClass.name, ctMethod.getName(), fieldAccess);
						throw new UnexpectedException("Error enhancing injected field", e);
					}

				}
			});
		}

		applicationClass.enhancedByteCode = ctClass.toBytecode();
		ctClass.defrost();
	}

	private Map<CtField, InjectionInfo> scanForInjections(final CtClass ctClass) throws CannotCompileException, NotFoundException {
		final Map<CtField, InjectionInfo> fieldsToInject = new HashMap<CtField, InjectionInfo>();

		// for all fields in this class, map their injection annotations
		for (final CtField ctField : ctClass.getDeclaredFields()) {
			final Object[] availableAnnotations = ctField.getAvailableAnnotations();

			for (final Object annotation : availableAnnotations) {
				InjectionInfo injectionInfo = fieldsToInject.get(ctField);

				if (annotation instanceof Qualifier) {
					if (injectionInfo == null) {
						throw new SpringException() {
							@Override
							public String getErrorDescription() {
								return "Injected field " + ctField.getName() + " in class " + ctClass.getName() + " used @Qualifier without @Inject or @Autowire";
							}
						};
					}

					injectionInfo.injectionMethod = InjectionMethod.BY_NAME;
					injectionInfo.beanName = ((Qualifier) annotation).value();
				}

				if (injectionInfo == null) {
					injectionInfo = new InjectionInfo(ctField.getType().toClass());
				}

				if (annotation instanceof Inject || annotation instanceof Autowired) {
					// default, can be overridden by @Qualifier
					injectionInfo.injectionMethod = InjectionMethod.BY_TYPE;
				}

				if (annotation instanceof Resource) {
					injectionInfo.injectionMethod = InjectionMethod.BY_NAME;
					injectionInfo.beanName = ((Resource) annotation).name();
				}

			}
		}
		return fieldsToInject;
	}
}
