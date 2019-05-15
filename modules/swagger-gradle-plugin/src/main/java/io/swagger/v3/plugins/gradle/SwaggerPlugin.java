package io.swagger.v3.plugins.gradle;

import static io.swagger.v3.plugins.gradle.tasks.ResolveTask.Format;
import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;
import static java.lang.String.format;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.Directory;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;

import io.swagger.v3.plugins.gradle.convention.OpenApiExtension;
import io.swagger.v3.plugins.gradle.internal.DefaultOpenApiExtension;
import io.swagger.v3.plugins.gradle.tasks.ResolveTask;

public class SwaggerPlugin implements Plugin<Project> {

    /**
     * The default version of the swagger library to be used.
     */
    public static final String DEFAULT_SWAGGER_VERSION = "2.0.10";

    /**
     * The name of the extension when installed.
     */
    public static final String OPEN_API_EXTENSION_NAME = "openApi";

    public static final String SWAGGER_DEPENDENCIES_CONFIGURATION_NAME = "swaggerDeps";

    public static final String OPEN_API_LIFECYCLE_TASK_NAME = "openApiDoc";

    private static final String GENERATED_RESOURCES_DIR = format("generated-resources/%s", SourceSet.MAIN_SOURCE_SET_NAME);
    /**
     * The relative directory path to the generated OpenAPI documents.
     */
    // VisibleForTesting
    static final String OPEN_API_DOCS_DIR = format("%s/openApiDocs", GENERATED_RESOURCES_DIR);

    private static final String GROUP_NAME = "openApi";

    public void apply(Project project) {
        OpenApiExtension extension = createExtensionWithDefaults(project);

        Configuration configuration = createConfigurationWithDefaults(project.getConfigurations(), project.getDependencies(), extension);

        configureMainSourceSet(project, project.getLayout(), project.getPlugins(), project.getConvention());

        configureTasksWithDefaults(project.getTasks(), project.getLayout(), project.getPlugins(), project.getConvention(), configuration);

        configureExtensionWithDefaults(project.getLayout(), project.getPlugins(), project.getConvention(), configuration, extension);

        createTasks(project.getTasks(), project.getLayout(), project.getProviders(), project.getPlugins(), project.getConvention(), configuration, extension);
    }

    private OpenApiExtension createExtensionWithDefaults(Project project) {
        OpenApiExtension extension = project.getExtensions().create(OpenApiExtension.class, OPEN_API_EXTENSION_NAME,
                DefaultOpenApiExtension.class, project, project.getObjects());
        extension.getLibraryVersion().set(DEFAULT_SWAGGER_VERSION);
        return extension;
    }

    private Configuration createConfigurationWithDefaults(ConfigurationContainer configurationContainer,
            DependencyHandler dependencyHandler, OpenApiExtension extension) {
        return configurationContainer.create(SWAGGER_DEPENDENCIES_CONFIGURATION_NAME)
                .setVisible(false)
                .setTransitive(true)
                .setDescription("The Swagger dependencies to be used in this project.")
                .defaultDependencies(dependencies -> {
                    dependencies.add(dependencyHandler.create(format("io.swagger.core.v3:swagger-jaxrs2:%s", extension.getLibraryVersion().get())));
                    // needed for io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder
                    dependencies.add(dependencyHandler.create("javax.servlet:javax.servlet-api:3.1.0"));
                    dependencies.add(dependencyHandler.create("javax.ws.rs:javax.ws.rs-api:2.1.1"));
                });
    }

    private void configureMainSourceSet(Project project, ProjectLayout layout, PluginContainer pluginContainer,
            Convention convention) {
        pluginContainer.withType(JavaPlugin.class, ignore -> {
            // register an additional output folder (containing generated OpenAPI documents) to be a part of the 'main' classpath and the jar
            findMainSourceSet(convention).getOutput().dir(layout.getBuildDirectory().dir(GENERATED_RESOURCES_DIR));

            File generatedResourcesDir = layout.getBuildDirectory().file(GENERATED_RESOURCES_DIR).get().getAsFile();
            if (!generatedResourcesDir.exists()) {
                // directory needs to be created before eclipse/idea checks it's existence
                project.mkdir(generatedResourcesDir);
            }
        });
    }

    private void configureTasksWithDefaults(TaskContainer tasks, ProjectLayout layout, PluginContainer pluginContainer,
            Convention convention, Configuration configuration) {
        tasks.withType(ResolveTask.class, task -> {
            task.setGroup(GROUP_NAME);

            task.getOutputDir().set(buildOutputDirProvider(layout, task.getName()));
            task.getOutputFileName().set("openapi");
            task.getOutputFormat().set(Format.JSON);
            task.getSkip().set(false);
            task.getEncoding().set(StandardCharsets.UTF_8.displayName());
            task.getPrettyPrint().set(true);
            task.getReadAllResources().set(true);
            task.getBuildClasspath().setFrom(configuration);

            pluginContainer.withType(JavaPlugin.class, ignore -> {
                task.dependsOn(JavaPlugin.COMPILE_JAVA_TASK_NAME);

                task.getClasspath().setFrom(findMainSourceSet(convention).getRuntimeClasspath());
            });
        });
    }

    private void configureExtensionWithDefaults(ProjectLayout layout, PluginContainer pluginContainer,
            Convention convention, Configuration configuration, OpenApiExtension extension) {
        extension.getDocs().all(doc -> {
            doc.getOutputDir().set(buildOutputDirProvider(layout, doc.getName()));
            doc.getOutputFileName().set("openapi");
            doc.getOutputFormat().set(Format.JSON.name());
            doc.getSkip().set(false);
            doc.getEncoding().set(StandardCharsets.UTF_8.displayName());
            doc.getPrettyPrint().set(true);
            doc.getReadAllResources().set(true);
            pluginContainer.withType(JavaPlugin.class, ignore -> doc.getClasspath().setFrom(findMainSourceSet(convention).getRuntimeClasspath()));
            doc.getBuildClasspath().setFrom(configuration);
        });
    }

    private SourceSet findMainSourceSet(Convention convention) {
        return convention.getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    }

    private void createTasks(TaskContainer tasks, ProjectLayout layout, ProviderFactory providers,
            PluginContainer pluginContainer, Convention convention, Configuration configuration, OpenApiExtension extension) {
        // FIXME: remove proactive creation of this task and let consumers create one
        tasks.create("resolve", ResolveTask.class);

        tasks.create(OPEN_API_LIFECYCLE_TASK_NAME, task -> {
            task.setDescription("Generates all OpenAPI 3.0 documents.");
            task.setGroup(GROUP_NAME);

            task.dependsOn(tasks.withType(ResolveTask.class));
        });

        extension.getDocs().all(doc -> {
            String name = doc.getName();
            String taskName = format("%sFor%s", OPEN_API_LIFECYCLE_TASK_NAME, toUpperCase(name.charAt(0)) + name.substring(1));
            tasks.create(taskName, ResolveTask.class, task -> {
                task.setDescription(format("Generates the OpenAPI 3.0 document for '%s' in either JSON, YAML or both formats.", name));

                task.getOutputDir().set(doc.getOutputDir());
                task.getOutputFileName().set(doc.getOutputFileName());
                task.getOutputFormat().set(providers.provider(() -> Format.fromString(doc.getOutputFormat().get())));
                task.getSkip().set(doc.getSkip());
                task.getEncoding().set(doc.getEncoding());
                task.getResourcePackages().set(doc.getResourcePackages());
                task.getResourceClasses().set(doc.getResourceClasses());
                task.getPrettyPrint().set(doc.getPrettyPrint());
                task.getOpenApiFile().set(doc.getOpenApiFile());
                task.getFilterClass().set(doc.getFilterClass());
                task.getReaderClass().set(doc.getReaderClass());
                task.getScannerClass().set(doc.getScannerClass());
                task.getReadAllResources().set(doc.getReadAllResources());
                task.getIgnoredRoutes().set(doc.getIgnoredRoutes());
                task.getObjectMapperProcessorClass().set(doc.getObjectMapperProcessorClass());
                task.getModelConverterClasses().set(doc.getModelConverterClasses());
                task.getClasspath().setFrom(doc.getClasspath());
                task.getBuildClasspath().setFrom(doc.getBuildClasspath());
            });
            tasks.create(format("clean%s", toUpperCase(taskName.charAt(0)) + taskName.substring(1)), Delete.class, task -> {
                task.setDescription(format("Cleans the OpenAPI 3.0 document for '%s'.", name));
                task.setGroup(GROUP_NAME);

                task.dependsOn(taskName);

                task.delete(buildOutputDirProvider(layout, doc.getName()));
            });
        });
    }

    private static Provider<Directory> buildOutputDirProvider(ProjectLayout layout, String name) {
        String path = name.replaceFirst(format("^%sFor(?<name>.*)", OPEN_API_LIFECYCLE_TASK_NAME), "${name}");
        return layout.getBuildDirectory().dir(format("%s/%s", OPEN_API_DOCS_DIR, toLowerCase(path.charAt(0)) + path.substring(1)));
    }
}
