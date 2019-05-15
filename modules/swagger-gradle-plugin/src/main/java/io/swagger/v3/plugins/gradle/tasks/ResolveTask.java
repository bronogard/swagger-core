package io.swagger.v3.plugins.gradle.tasks;

import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.logging.Logging;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;

@CacheableTask
public class ResolveTask extends DefaultTask {

    private static final Logger LOGGER = Logging.getLogger(ResolveTask.class);

    public enum Format {
        JSON, YAML, JSONANDYAML;

        private static final Map<String, Format> TYPES_BY_NAME = Arrays.stream(values()).collect(toMap(Enum::name, identity()));

        public static Format fromString(String name) {
            return TYPES_BY_NAME.get(name);
        }
    }

    private final DirectoryProperty outputDir;

    private final Property<String> outputFileName;

    private final Property<Format> outputFormat;

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

    // FIXME: with Gradle >= 4.5 should be changed to SetProperty
    private final ListProperty<String> ignoredRoutes;

    private final Property<String> objectMapperProcessorClass;

    // FIXME: with Gradle >= 4.5 should be changed to SetProperty
    private final ListProperty<String> modelConverterClasses;

    private final ConfigurableFileCollection classpath;

    private final ConfigurableFileCollection buildClasspath;

    private final Property<String> contextId;

    @Inject
    public ResolveTask(ObjectFactory objectFactory, ProjectLayout layout) {
        this.outputDir = layout.directoryProperty();
        this.outputFileName = objectFactory.property(String.class);
        this.outputFormat = objectFactory.property(Format.class);
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
        this.classpath = getProject().files();
        this.buildClasspath = getProject().files();
        this.contextId = objectFactory.property(String.class);
    }

    @OutputDirectory
    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getOutputDir()} instead
     */
    @Deprecated
    @Internal
    public String getOutputPath() {
        return outputDir.get().getAsFile().getAbsolutePath();
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getOutputDir()} instead
     */
    @Deprecated
    public void setOutputPath(String outputPath) {
        this.outputDir.set(new File(outputPath));
    }

    @Input
    @Optional
    public Property<String> getOutputFileName() {
        return outputFileName;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getOutputFileName()} instead
     */
    @Deprecated
    public void setOutputFileName(String outputFileName) {
        this.outputFileName.set(outputFileName);
    }

    @Input
    @Optional
    public Property<Format> getOutputFormat() {
        return outputFormat;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getOutputFormat()} instead
     */
    @Deprecated
    public void setOutputFormat(String outputFormat) {
        this.outputFormat.set(Format.fromString(outputFormat));
    }

    @Input
    @Optional
    public Property<Boolean> getSkip() {
        return skip;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getSkip()} instead
     */
    @Deprecated
    public void setSkip(String skip) {
        this.skip.set(valueOf(skip));
    }

    @Input
    @Optional
    public Property<String> getEncoding() {
        return encoding;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getEncoding()} instead
     */
    @Deprecated
    public void setEncoding(String encoding) {
        this.encoding.set(encoding);
    }

    @Input
    @Optional
    public ListProperty<String> getResourcePackages() {
        return resourcePackages;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getResourcePackages()} instead
     */
    @Deprecated
    public void setResourcePackages(Set<String> resourcePackages) {
        this.resourcePackages.set(toFilteredList(resourcePackages));
    }

    @Input
    @Optional
    public ListProperty<String> getResourceClasses() {
        return resourceClasses;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getResourceClasses()} instead
     */
    @Deprecated
    public void setResourceClasses(Set<String> resourceClasses) {
        this.resourceClasses.set(toFilteredList(resourceClasses));
    }

    @Input
    @Optional
    public Property<Boolean> getPrettyPrint() {
        return prettyPrint;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getPrettyPrint()} instead
     */
    @Deprecated
    public void setPrettyPrint(String prettyPrint) {
        this.prettyPrint.set(valueOf(prettyPrint));
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    @Optional
    public Property<File> getOpenApiFile() {
        return openApiFile;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getOpenApiFile()} instead
     */
    @Deprecated
    public void setOpenApiFile(File openApiFile) {
        this.openApiFile.set(openApiFile);
    }

    @Input
    @Optional
    public Property<String> getFilterClass() {
        return filterClass;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getFilterClass()} instead
     */
    @Deprecated
    public void setFilterClass(String filterClass) {
        this.filterClass.set(filterClass);
    }

    @Input
    @Optional
    public Property<String> getReaderClass() {
        return readerClass;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getReaderClass()} instead
     */
    @Deprecated
    public void setReaderClass(String readerClass) {
        this.readerClass.set(readerClass);
    }

    @Input
    @Optional
    public Property<String> getScannerClass() {
        return scannerClass;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getScannerClass()} instead
     */
    @Deprecated
    public void setScannerClass(String scannerClass) {
        this.scannerClass.set(scannerClass);
    }

    @Input
    @Optional
    public Property<Boolean> getReadAllResources() {
        return readAllResources;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getReadAllResources()} instead
     */
    @Deprecated
    public void setReadAllResources(String readAllResources) {
        this.readAllResources.set(valueOf(readAllResources));
    }

    @Input
    @Optional
    public ListProperty<String> getIgnoredRoutes() {
        return ignoredRoutes;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getIgnoredRoutes()} instead
     */
    @Deprecated
    public void setIgnoredRoutes(Collection<String> ignoredRoutes) {
        this.ignoredRoutes.set(toFilteredList(ignoredRoutes));
    }

    /**
     * @since 2.0.6
     */
    @Input
    @Optional
    public Property<String> getObjectMapperProcessorClass() {
        return objectMapperProcessorClass;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getObjectMapperProcessorClass()} instead
     * @since 2.0.6
     */
    @Deprecated
    public void setObjectMapperProcessorClass(String objectMapperProcessorClass) {
        this.objectMapperProcessorClass.set(objectMapperProcessorClass);
    }

    /**
     * @since 2.0.6
     */
    @Input
    @Optional
    public ListProperty<String> getModelConverterClasses() {
        return modelConverterClasses;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getModelConverterClasses()} instead
     * @since 2.0.6
     */
    @Deprecated
    public void setModelConverterClasses(LinkedHashSet<String> modelConverterClasses) {
        this.modelConverterClasses.set(toFilteredList(modelConverterClasses));
    }

    @Classpath
    public ConfigurableFileCollection getClasspath() {
        return classpath;
    }

    /**
     * @deprecated call {@link ConfigurableFileCollection#setFrom(Iterable) setFrom} on {@link #getClasspath()} instead
     */
    @Deprecated
    public void setClasspath(Iterable<File> classpath) {
        this.classpath.setFrom(classpath);
    }

    @Classpath
    @Optional
    public ConfigurableFileCollection getBuildClasspath() {
        return buildClasspath;
    }

    /**
     * @deprecated call {@link ConfigurableFileCollection#setFrom(Iterable) setFrom} on {@link #getBuildClasspath()} instead
     */
    @Deprecated
    public void setBuildClasspath(Iterable<File> buildClasspath) {
        this.buildClasspath.setFrom(buildClasspath);
    }

    /**
     * @since 2.0.6
     */
    @Input
    @Optional
    public Property<String> getContextId() {
        return contextId;
    }

    /**
     * @deprecated call {@link Property#set(Object) set} on {@link #getContextId()} instead
     * @since 2.0.6
     */
    @Deprecated
    public void setContextId(String contextId) {
        this.contextId.set(contextId);
    }

    @TaskAction
    public void resolve() throws GradleException {
        if (skip.get()) {
            LOGGER.info("Skipped OpenAPI document resolution.");
            return;
        }
        LOGGER.info("Resolving OpenAPI document...");

        Set<URL> urls = buildClasspathFrom(getClasspath());
        Set<URL> buildUrls = buildClasspathFrom(getBuildClasspath());
        urls.addAll(buildUrls);

        try {
            ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]));
            Class swaggerLoaderClass = classLoader.loadClass("io.swagger.v3.jaxrs2.integration.SwaggerLoader");
            Object swaggerLoader = swaggerLoaderClass.newInstance();

            Method method = swaggerLoaderClass.getDeclaredMethod("setOutputFormat", String.class);
            method.invoke(swaggerLoader, outputFormat.get().name());

            if (openApiFile.getOrNull() != null) {
                if (openApiFile.get().exists() && openApiFile.get().isFile()) {
                    String openapiFileContent = new String(Files.readAllBytes(openApiFile.get().toPath()), encoding.get());
                    if (isNotBlank(openapiFileContent)) {
                        method = swaggerLoaderClass.getDeclaredMethod("setOpenapiAsString", String.class);
                        method.invoke(swaggerLoader, openapiFileContent);
                    }
                }
            }

            method = swaggerLoaderClass.getDeclaredMethod("setResourcePackages", String.class);
            method.invoke(swaggerLoader, toJoinedString(resourcePackages.getOrElse(emptyList())));

            method = swaggerLoaderClass.getDeclaredMethod("setResourceClasses", String.class);
            method.invoke(swaggerLoader, toJoinedString(resourceClasses.getOrElse(emptyList())));

            method = swaggerLoaderClass.getDeclaredMethod("setModelConverterClasses", String.class);
            method.invoke(swaggerLoader, toJoinedString(modelConverterClasses.getOrElse(emptyList())));

            method = swaggerLoaderClass.getDeclaredMethod("setIgnoredRoutes", String.class);
            method.invoke(swaggerLoader, toJoinedString(ignoredRoutes.getOrElse(emptyList())));

            if (isNotBlank(filterClass.getOrNull())) {
                method = swaggerLoaderClass.getDeclaredMethod("setFilterClass", String.class);
                method.invoke(swaggerLoader, filterClass.get());
            }

            if (isNotBlank(readerClass.getOrNull())) {
                method = swaggerLoaderClass.getDeclaredMethod("setReaderClass", String.class);
                method.invoke(swaggerLoader, readerClass.get());
            }

            if (isNotBlank(scannerClass.getOrNull())) {
                method = swaggerLoaderClass.getDeclaredMethod("setScannerClass", String.class);
                method.invoke(swaggerLoader, scannerClass.get());
            }

            if (isNotBlank(contextId.getOrNull())) {
                method = swaggerLoaderClass.getDeclaredMethod("setContextId", String.class);
                method.invoke(swaggerLoader, contextId.get());
            }

            if (isNotBlank(objectMapperProcessorClass.getOrNull())) {
                method = swaggerLoaderClass.getDeclaredMethod("setObjectMapperProcessorClass", String.class);
                method.invoke(swaggerLoader, objectMapperProcessorClass.get());
            }

            method = swaggerLoaderClass.getDeclaredMethod("setPrettyPrint", Boolean.class);
            method.invoke(swaggerLoader, prettyPrint.get());

            method = swaggerLoaderClass.getDeclaredMethod("setReadAllResources", Boolean.class);
            method.invoke(swaggerLoader, readAllResources.get());

            method = swaggerLoaderClass.getDeclaredMethod("resolve");
            writeOpenApiDoc((Map<String, String>) method.invoke(swaggerLoader));
        } catch (Exception e) {
            throw new GradleException("Failed to invoke method on SwaggerLoader.", e);
        }
    }

    private Set<URL> buildClasspathFrom(ConfigurableFileCollection classpath) {
        return StreamSupport.stream(classpath.spliterator(), false)
                .flatMap(this::toUrl)
                .collect(toSet());
    }

    private Stream<URL> toUrl(File file) {
        try {
            return Stream.of(file.toURI().toURL());
        } catch (MalformedURLException e) {
            LOGGER.warn("Skipped transforming '{}' to URL classpath for task '{}'.", file.getName(), getName());
            return Stream.empty();
        }
    }

    private void writeOpenApiDoc(Map<String, String> specs) {
        specs.entrySet().stream()
                .filter(entry -> nonNull(entry.getValue()))
                .forEach(entry -> {
                    Path path = outputDir.get().file(format("%s.%s", outputFileName.get(), entry.getKey().toLowerCase())).getAsFile().toPath();
                    byte[] bytes = entry.getValue().getBytes(Charset.forName(encoding.get()));
                    try {
                        Files.write(path, bytes);
                    } catch (IOException e) {
                        throw new GradleException(format("Failed to write OpenAPI document: '%s'", path), e);
                    }
                });
    }

    private static <T> List<T> toFilteredList(Collection<T> collection) {
        return (collection == null) ?
                emptyList() :
                collection.stream()
                        .filter(Objects::nonNull)
                        .collect(toList());
    }

    private static String toJoinedString(Collection<String> collection) {
        return (collection == null) ?
                "" :
                collection.stream()
                        .filter(Objects::nonNull)
                        .collect(joining(","));
    }
}
