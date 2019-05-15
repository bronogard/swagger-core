package io.swagger.v3.plugins.gradle;

import static java.lang.Character.toLowerCase;
import static java.lang.String.format;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SwaggerResolveTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    private File settingsFile;
    private File buildFile;
    private File openApiInputFile;

    private String outputDir;

    @Before
    public void setup() throws IOException {
        settingsFile = testProjectDir.newFile("settings.gradle");
        writeFile(settingsFile, "rootProject.name = 'gradle-test'\n");

        buildFile = testProjectDir.newFile("build.gradle");
        writeFile(buildFile, "plugins {\n" +
                "    id 'java'\n" +
                "    id 'io.swagger.core.v3.swagger-gradle-plugin'\n" +
                "}\n" +
                "sourceSets {\n" +
                "    test {\n" +
                "        java {\n" +
                "            srcDirs('" + toNormalizedPath(new File("src/test/java").getAbsolutePath()) + "')\n" +
                "            exclude('**/*Test.java')\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "repositories {\n" +
                "    jcenter()\n" +
                "    mavenLocal()\n" +
                "    mavenCentral()\n" +
                "}\n" +
                "dependencies {  \n" +
                "    compile group: 'io.swagger.core.v3', name: 'swagger-jaxrs2', version:'2.0.9'\n" +
                "    compile group: 'javax.servlet', name: 'javax.servlet-api', version:'3.1.0'\n" +
                "    compile group: 'javax.ws.rs', name: 'javax.ws.rs-api', version:'2.1.1'\n" +
                "    testCompile 'junit:junit:4.12'\n" +
                "}\n");

        openApiInputFile = testProjectDir.newFile("openapiinput.yaml");
        writeFile(openApiInputFile, "openapi: 3.0.1\n" +
                "servers:\n" +
                "- url: http://foo\n" +
                "  description: server 1\n" +
                "  variables:\n" +
                "    var1:\n" +
                "      description: var 1\n" +
                "      enum:\n" +
                "      - \"1\"\n" +
                "      - \"2\"\n" +
                "      default: \"2\"\n" +
                "    var2:\n" +
                "      description: var 2\n" +
                "      enum:\n" +
                "      - \"1\"\n" +
                "      - \"2\"\n" +
                "      default: \"2\"");

        outputDir = new File(testProjectDir.getRoot(), "build").getAbsolutePath();
    }

    @Test
    public void testSwaggerResolveTask() throws IOException {
        String outputFileName = "PetStoreAPI";
        Path outputFile = Paths.get(outputDir, format("%s.json", outputFileName));
        String resolveTask = "resolve";

        writeFile(buildFile, resolveTask + " {\n" +
                "    outputFileName = '" + outputFileName + "'\n" +
                "    outputFormat = 'JSON'\n" +
                "    prettyPrint = 'TRUE'\n" +
                "    classpath = sourceSets.test.runtimeClasspath\n" +
                "    resourcePackages = ['io.swagger.v3.plugins.gradle.petstore']\n" +
                "    outputPath = \'" + toNormalizedPath(outputDir) + "\'\n" +
                "    filterClass = 'io.swagger.v3.plugins.gradle.resources.MyFilter'\n" +
                "    openApiFile = file(\'" + toNormalizedPath(openApiInputFile.getAbsolutePath()) + "\')\n" +
                "}", true);

        BuildResult result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.getRoot())
                .withDebug(true)
                .forwardOutput()
                .withArguments(resolveTask, "--stacktrace", "--info")
                .build();

        assertThat(result.taskPaths(SUCCESS), hasItem(format(":%s", resolveTask)));
        assertThat(outputFile.toFile().exists(), is(true));
        assertThat(new String(Files.readAllBytes(outputFile), StandardCharsets.UTF_8), containsString("UPDATEDBYFILTER"));
    }

    @Test
    public void shouldGenerateOpenApiDocumentAsJsonWhenConfiguredViaCustomExtension() throws IOException {
        String apiName = "PetStoreAPI";
        Path outputFile = Paths.get(outputDir, format("%s/%s/openapi.json", SwaggerPlugin.OPEN_API_DOCS_DIR, apiName));
        String openApiTask = format("%sFor%s", SwaggerPlugin.OPEN_API_LIFECYCLE_TASK_NAME, apiName);

        writeFile(buildFile, "openApi {\n" +
                "    docs { \n" +
                "        " + toLowerCase(apiName.charAt(0)) +  apiName.substring(1) + " { \n" +
                "            resourcePackages.set(['io.swagger.v3.plugins.gradle.petstore'])\n" +
                "            filterClass.set('io.swagger.v3.plugins.gradle.resources.MyFilter')\n" +
                "            openApiFile = file(\'" + toNormalizedPath(openApiInputFile.getAbsolutePath()) + "\')\n" +
                "        }\n" +
                "    }\n" +
                "}", true);

        BuildResult result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.getRoot())
                .withDebug(true)
                .forwardOutput()
                .withArguments(openApiTask, "--stacktrace", "--info")
                .build();

        assertThat(result.taskPaths(SUCCESS), hasItem(format(":%s", openApiTask)));
        assertThat(outputFile.toFile().exists(), is(true));
        assertThat(new String(Files.readAllBytes(outputFile), StandardCharsets.UTF_8), containsString("UPDATEDBYFILTER"));
    }

    private static void writeFile(File destination, String content) throws IOException {
        writeFile(destination, content, false);
    }

    private static void writeFile(File destination, String content, boolean shouldAppend) throws IOException {
        try (BufferedWriter output = new BufferedWriter(new FileWriter(destination, shouldAppend))) {
            output.write(content);
        }
    }

    private static String toNormalizedPath(String path) {
        return path.replace("\\", "/"); // necessary on windows
    }
}
