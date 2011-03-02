package play.modules.spring;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;

public class PlayClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

	private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	private final MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(this.resourcePatternResolver);

	/**
	 * The constructor, which just passed on to the parent class.
	 * 
	 * @param registry
	 */
	public PlayClassPathBeanDefinitionScanner(final BeanDefinitionRegistry registry) {
		super(registry);
	}

	/**
	 * The override, which searches through the play framework's classes, instead of using
	 * files (as Spring is trained to do).
	 */
	@Override
	public Set<BeanDefinition> findCandidateComponents(final String basePackage) {
		Logger.debug("Finding candidate components with base package: " + basePackage);

		final Set<BeanDefinition> candidates = new LinkedHashSet<BeanDefinition>();

		try {
			for (final ApplicationClass appClass : Play.classes.all()) {
				if (appClass.name.startsWith(basePackage)) {
					Logger.debug("Scanning class: " + appClass.name);
					final ByteArrayResource res = new ByteArrayResource(appClass.enhance());
					final MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(res);

					if (isCandidateComponent(metadataReader)) {
						final ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
						sbd.setSource(res);
						if (isCandidateComponent(sbd)) {
							candidates.add(sbd);
						}
					}
				}
				else {
					Logger.trace("Skipped class: " + appClass.name + " -- wrong base package");
				}
			}
		}
		catch (final IOException ex) {
			throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
		}
		return candidates;
	}
}
