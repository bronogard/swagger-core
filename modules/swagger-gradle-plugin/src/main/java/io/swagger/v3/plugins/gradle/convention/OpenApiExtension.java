package io.swagger.v3.plugins.gradle.convention;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.provider.Property;

/**
 * Configuration options to declare different OpenAPI documents.
 * <p>
 * Example of use with a blend of most possible properties.
 *
 * <pre>
 * openApi {
 *     docs {
 *         petStoreAPI {
 *             // if you prefer different output folder than the default one '${buildDir}/generated-resources/main/openapi'
 *             outputDir.set(layout.buildDirectory.dir('muchBetterOutputDir'))
 *
 *             // if you prefer different name than the default one 'openapi'
 *             outputFileName.set('some-better-name')
 *
 *             // if you prefer different output format than the default one 'JSON'
 *             outputFormat.set('YAML')
 *
 *             // if you love a dry-run
 *             skip.set(true)
 *
 *             // if you prefer different encoding than the default one 'UTF-8'
 *             encoding.set('windows-1252')
 *
 *             resourcePackages.set(['io.test'])
 *
 *             // if you hate pretty printing
 *             prettyPrint.set(false)
 *
 *             // if you prefer different classpath than the default one 'sourceSets.main.runtimeClasspath' - only if Gradle plugin 'java' is applied
 *             classpath.setFrom(sourceSets.main.runtimeClasspath)
 *         }
 *         ...
 *     }
 * }
 * </pre>
 */
public interface OpenApiExtension {

	/**
	 * @return the version of the <a href="https://github.com/swagger-api/swagger-core/releases">Swagger Core</a> library to be used
	 */
	Property<String> getLibraryVersion();

	/**
	 * @return the declared docs
	 */
	NamedDomainObjectContainer<DocDeclaration> getDocs();

	/**
	 * Configures the declared docs.
	 *
	 * @param action the configuration action to invoke on the docs
	 */
	void docs(Action<? super NamedDomainObjectContainer<DocDeclaration>> action);
}
