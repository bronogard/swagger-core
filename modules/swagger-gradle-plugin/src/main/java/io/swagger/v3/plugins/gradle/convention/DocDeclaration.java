package io.swagger.v3.plugins.gradle.convention;

import java.io.File;

import javax.inject.Inject;

import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public class DocDeclaration implements Named {

	private final String name;

	private final DirectoryProperty outputDir;

	private final Property<String> outputFileName;

	private final Property<String> outputFormat;

	private final Property<Boolean> skip;

	private final Property<String> encoding;

	// FIXME: with Gradle >= 4.5 should be changed to SetProperty
	private final ListProperty<String> resourcePackages;

	// FIXME: with Gradle >= 4.5 should be changed to SetProperty
	private final ListProperty<String> resourceClasses;

	private final Property<Boolean> prettyPrint;

	private final Property<File> openApiFile;

	private final Property<String> filterClass;

	private final Property<String> readerClass;

	private final Property<String> scannerClass;

	private final Property<Boolean> readAllResources;

	private final ListProperty<String> ignoredRoutes;

	private final Property<String> objectMapperProcessorClass;

	// FIXME: with Gradle >= 4.5 should be changed to SetProperty
	private final ListProperty<String> modelConverterClasses;

	private final ConfigurableFileCollection classpath;

	private final ConfigurableFileCollection buildClasspath;

	@Inject
	public DocDeclaration(String name, Project project, ObjectFactory objectFactory, ProjectLayout layout) {
		this.name = name;
		this.outputDir = layout.directoryProperty();
		this.outputFileName = objectFactory.property(String.class);
		this.outputFormat = objectFactory.property(String.class);
		this.skip = objectFactory.property(Boolean.class);
		this.encoding = objectFactory.property(String.class);
		this.resourcePackages = objectFactory.listProperty(String.class);
		this.resourceClasses = objectFactory.listProperty(String.class);
		this.prettyPrint = objectFactory.property(Boolean.class);
		this.openApiFile = objectFactory.property(File.class);
		this.filterClass = objectFactory.property(String.class);
		this.readerClass = objectFactory.property(String.class);
		this.scannerClass = objectFactory.property(String.class);
		this.readAllResources = objectFactory.property(Boolean.class);
		this.ignoredRoutes = objectFactory.listProperty(String.class);
		this.objectMapperProcessorClass = objectFactory.property(String.class);
		this.modelConverterClasses = objectFactory.listProperty(String.class);
		this.classpath = project.files();
		this.buildClasspath = project.files();
	}

	@Override
	public String getName() {
		return name;
	}

	public ConfigurableFileCollection getClasspath() {
		return classpath;
	}

	public DirectoryProperty getOutputDir() {
		return outputDir;
	}

	public Property<String> getOutputFileName() {
		return outputFileName;
	}

	public Property<String> getOutputFormat() {
		return outputFormat;
	}

	public Property<Boolean> getSkip() {
		return skip;
	}

	public Property<String> getEncoding() {
		return encoding;
	}

	public ListProperty<String> getResourcePackages() {
		return resourcePackages;
	}

	public ListProperty<String> getResourceClasses() {
		return resourceClasses;
	}

	public Property<Boolean> getPrettyPrint() {
		return prettyPrint;
	}

	public Property<File> getOpenApiFile() {
		return openApiFile;
	}

	public Property<String> getFilterClass() {
		return filterClass;
	}

	public Property<String> getReaderClass() {
		return readerClass;
	}

	public Property<String> getScannerClass() {
		return scannerClass;
	}

	public Property<Boolean> getReadAllResources() {
		return readAllResources;
	}

	public ListProperty<String> getIgnoredRoutes() {
		return ignoredRoutes;
	}

	public Property<String> getObjectMapperProcessorClass() {
		return objectMapperProcessorClass;
	}

	public ListProperty<String> getModelConverterClasses() {
		return modelConverterClasses;
	}

	public ConfigurableFileCollection getBuildClasspath() {
		return buildClasspath;
	}
}
