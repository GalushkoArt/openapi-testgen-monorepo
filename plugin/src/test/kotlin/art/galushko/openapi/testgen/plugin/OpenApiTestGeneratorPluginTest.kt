package art.galushko.openapi.testgen.plugin

import art.galushko.openapi.testgen.model.error.ErrorMode
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OpenApiTestGeneratorPlugin")
class OpenApiTestGeneratorPluginTest {

    private lateinit var project: DefaultProject

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build() as DefaultProject
    }

    /**
     * Helper to trigger project evaluation (afterEvaluate callbacks).
     */
    private fun evaluateProject() {
        // Force evaluation of the project to trigger afterEvaluate callbacks
        project.evaluate()
    }

    @Nested
    @DisplayName("plugin application")
    inner class PluginApplicationTest {

        @Test
        @DisplayName("should create extension with correct name")
        fun shouldCreateExtension() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)

            val extension = project.extensions.findByName("openApiTestGenerator")

            assertThat(extension).isNotNull()
            assertThat(extension).isInstanceOf(TestGeneratorExtension::class.java)
        }

        @Test
        @DisplayName("should register task with correct name")
        fun shouldRegisterTask() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)

            val task = project.tasks.findByName("generateOpenApiTests")

            assertThat(task).isNotNull()
            assertThat(task).isInstanceOf(OpenApiTestGeneratorTask::class.java)
        }

        @Test
        @DisplayName("should configure task group as verification")
        fun shouldConfigureTaskGroup() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)

            val task = project.tasks.findByName("generateOpenApiTests")

            assertThat(task?.group).isEqualTo("verification")
        }

        @Test
        @DisplayName("should configure task description")
        fun shouldConfigureTaskDescription() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)

            val task = project.tasks.findByName("generateOpenApiTests")

            assertThat(task?.description).isEqualTo("Generates tests from an OpenAPI specification")
        }
    }

    @Nested
    @DisplayName("extension to task wiring")
    inner class ExtensionToTaskWiringTest {

        @Test
        @DisplayName("should wire specFile from extension to task")
        fun shouldWireSpecFile() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.specFile.set("openapi.yaml")

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.specFile.get()).isEqualTo("openapi.yaml")
        }

        @Test
        @DisplayName("should wire configFile from extension to task")
        fun shouldWireConfigFile() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.configFile.set("config.yaml")

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.configFile.get()).isEqualTo("config.yaml")
        }

        @Test
        @DisplayName("should wire outputDir from extension to task")
        fun shouldWireOutputDir() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            val dir = project.layout.projectDirectory.dir("generated")
            extension.outputDir.set(dir)

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.outputDir.get().asFile.name).isEqualTo("generated")
        }

        @Test
        @DisplayName("should wire generator from extension to task")
        fun shouldWireGenerator() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.generator.set("template")

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.generator.get()).isEqualTo("template")
        }

        @Test
        @DisplayName("should wire generatorOptions from extension to task")
        fun shouldWireGeneratorOptions() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.generatorOptions.put("templateSet", "restassured-java")

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.generatorOptions.get()).containsEntry("templateSet", "restassured-java")
        }

        @Test
        @DisplayName("should wire alwaysWriteTests from extension to task")
        fun shouldWireAlwaysWriteTests() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.alwaysWriteTests.set(true)

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.alwaysWriteTests.get()).isTrue()
        }

        @Test
        @DisplayName("should wire logLevel from extension to task")
        fun shouldWireLogLevel() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.logLevel.set("DEBUG")

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.logLevel.get()).isEqualTo("DEBUG")
        }
    }

    @Nested
    @DisplayName("testGenerationSettings wiring")
    inner class TestGenerationSettingsWiringTest {

        @Test
        @DisplayName("should wire maxSchemaDepth from extension to task")
        fun shouldWireMaxSchemaDepth() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.testGenerationSettings.maxSchemaDepth.set(25)

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.testGenerationSettings.maxSchemaDepth.get()).isEqualTo(25)
        }

        @Test
        @DisplayName("should wire includeValidCase from extension to task")
        fun shouldWireIncludeValidCase() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.testGenerationSettings.includeValidCase.set(true)

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.testGenerationSettings.includeValidCase.get()).isTrue()
        }

        @Test
        @DisplayName("should wire errorMode from extension to task")
        fun shouldWireErrorMode() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.testGenerationSettings.errorMode.set(ErrorMode.FAIL_FAST)

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.testGenerationSettings.errorMode.get()).isEqualTo(ErrorMode.FAIL_FAST)
        }

        @Test
        @DisplayName("should wire ignoreSchemaValidationRules from extension to task")
        fun shouldWireIgnoreSchemaValidationRules() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.testGenerationSettings.ignoreSchemaValidationRules.addAll(listOf("Rule1", "Rule2"))

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.testGenerationSettings.ignoreSchemaValidationRules.get())
                .containsExactlyInAnyOrder("Rule1", "Rule2")
        }

        @Test
        @DisplayName("should wire validSecurityValues from extension to task")
        fun shouldWireValidSecurityValues() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.testGenerationSettings.validSecurityValues.put("X-API-Key", "test-key")

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.testGenerationSettings.validSecurityValues.get())
                .containsEntry("X-API-Key", "test-key")
        }

        @Test
        @DisplayName("should wire ignoreTestCases from extension to task")
        fun shouldWireIgnoreTestCases() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.testGenerationSettings.ignoreTestCases.put("/users", mapOf("GET" to listOf("*")))

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.testGenerationSettings.ignoreTestCases.get()).containsKey("/users")
        }

        @Test
        @DisplayName("should wire exampleValues from extension to task")
        fun shouldWireExampleValues() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.testGenerationSettings.exampleValues.put("maxExampleDepth", 30)

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.testGenerationSettings.exampleValues.get())
                .containsEntry("maxExampleDepth", 30)
        }
    }

    @Nested
    @DisplayName("template generator wiring with Java plugin")
    inner class TemplateGeneratorJavaWiringTest {

        @Test
        @DisplayName("should configure test source set when Java plugin is applied")
        fun shouldConfigureTestSourceSet() {
            project.pluginManager.apply(JavaPlugin::class.java)
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.generator.set("template")
            extension.outputDir.set(project.layout.projectDirectory.dir("generated"))

            // Trigger afterEvaluate
            evaluateProject()

            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            val testSourceSet = sourceSets.getByName("test")
            val srcDirs = testSourceSet.java.srcDirs

            assertThat(srcDirs.map { it.name }).contains("generated")
        }

        @Test
        @DisplayName("should make compileTestJava depend on generate task when manualOnly is false")
        fun shouldMakeCompileTestJavaDependOnGenerateTask() {
            project.pluginManager.apply(JavaPlugin::class.java)
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.generator.set("template")
            extension.manualOnly.set(false)
            extension.outputDir.set(project.layout.projectDirectory.dir("generated"))

            evaluateProject()

            val compileTestJavaTask = project.tasks.getByName(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME)
            val generateTaskProvider = project.tasks.named("generateOpenApiTests")

            assertThat(compileTestJavaTask.dependsOn).contains(generateTaskProvider)
        }

        @Test
        @DisplayName("should not make compileTestJava depend on generate task when manualOnly is true")
        fun shouldNotMakeCompileTestJavaDependWhenManualOnly() {
            project.pluginManager.apply(JavaPlugin::class.java)
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.generator.set("template")
            extension.manualOnly.set(true)
            extension.outputDir.set(project.layout.projectDirectory.dir("generated"))

            evaluateProject()

            val compileTestJavaTask = project.tasks.getByName(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME)
            val generateTaskProvider = project.tasks.named("generateOpenApiTests")

            assertThat(compileTestJavaTask.dependsOn).doesNotContain(generateTaskProvider)
        }

        @Test
        @DisplayName("should configure mustRunAfter for compileTestJava")
        fun shouldConfigureMustRunAfter() {
            project.pluginManager.apply(JavaPlugin::class.java)
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.generator.set("template")
            extension.outputDir.set(project.layout.projectDirectory.dir("generated"))

            evaluateProject()

            val compileTestJavaTask = project.tasks.getByName(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME)
            val generateTask = project.tasks.getByName("generateOpenApiTests")

            assertThat(compileTestJavaTask.mustRunAfter.getDependencies(compileTestJavaTask))
                .contains(generateTask)
        }
    }

    @Nested
    @DisplayName("test-suite-writer generator wiring with Java plugin")
    inner class TestSuiteWriterGeneratorJavaWiringTest {

        @Test
        @DisplayName("should make processTestResources depend on generate task when manualOnly is false")
        fun shouldMakeProcessTestResourcesDependOnGenerateTask() {
            project.pluginManager.apply(JavaPlugin::class.java)
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.generator.set("test-suite-writer")
            extension.manualOnly.set(false)
            extension.outputDir.set(project.layout.projectDirectory.dir("generated"))

            evaluateProject()

            val processTestResourcesTask = project.tasks.getByName(JavaPlugin.PROCESS_TEST_RESOURCES_TASK_NAME)
            val generateTaskProvider = project.tasks.named("generateOpenApiTests")

            assertThat(processTestResourcesTask.dependsOn).contains(generateTaskProvider)
        }

        @Test
        @DisplayName("should not make processTestResources depend on generate task when manualOnly is true")
        fun shouldNotMakeProcessTestResourcesDependWhenManualOnly() {
            project.pluginManager.apply(JavaPlugin::class.java)
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.generator.set("test-suite-writer")
            extension.manualOnly.set(true)
            extension.outputDir.set(project.layout.projectDirectory.dir("generated"))

            evaluateProject()

            val processTestResourcesTask = project.tasks.getByName(JavaPlugin.PROCESS_TEST_RESOURCES_TASK_NAME)
            val generateTaskProvider = project.tasks.named("generateOpenApiTests")

            assertThat(processTestResourcesTask.dependsOn).doesNotContain(generateTaskProvider)
        }

        @Test
        @DisplayName("should configure mustRunAfter for processTestResources")
        fun shouldConfigureMustRunAfterForProcessTestResources() {
            project.pluginManager.apply(JavaPlugin::class.java)
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.generator.set("test-suite-writer")
            extension.outputDir.set(project.layout.projectDirectory.dir("generated"))

            evaluateProject()

            val processTestResourcesTask = project.tasks.getByName(JavaPlugin.PROCESS_TEST_RESOURCES_TASK_NAME)
            val generateTask = project.tasks.getByName("generateOpenApiTests")

            assertThat(processTestResourcesTask.mustRunAfter.getDependencies(processTestResourcesTask))
                .contains(generateTask)
        }
    }

    @Nested
    @DisplayName("no wiring when Java plugin is not applied")
    inner class NoJavaPluginTest {

        @Test
        @DisplayName("should not fail when Java plugin is not applied")
        fun shouldNotFailWithoutJavaPlugin() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.generator.set("template")
            extension.outputDir.set(project.layout.projectDirectory.dir("generated"))

            // Should not throw
            evaluateProject()

            assertThat(project.tasks.findByName("generateOpenApiTests")).isNotNull()
        }
    }
}
