package art.galushko.openapi.testgen.plugin

import art.galushko.openapi.testgen.model.error.ErrorMode
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

@DisplayName("OpenApiTestGeneratorPlugin")
class OpenApiTestGeneratorPluginTest {

    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
    }

    private fun dependencyNames(task: Task): List<String> =
        task.taskDependencies.getDependencies(task).map { it.name }

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

            assertThat(task?.description).isEqualTo("Generates tests from an OpenAPI or Swagger specification")
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
        @DisplayName("should wire configFile from extension to task resolved against the project directory")
        fun shouldWireConfigFile() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.configFile.set("config.yaml")

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.configFile.get().asFile)
                .isEqualTo(project.layout.projectDirectory.file("config.yaml").asFile)
        }

        @Test
        @DisplayName("should track an existing local spec file for up-to-date checks")
        fun shouldTrackExistingLocalSpecFile() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            val specFile = project.layout.projectDirectory.file("openapi.yaml").asFile
            specFile.writeText("openapi: 3.0.3")
            extension.specFile.set("openapi.yaml")

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.trackedSpecFiles.files).containsExactly(specFile)
        }

        @Test
        @DisplayName("should track a local spec declared as a file URI")
        fun shouldTrackFileUriSpec() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            val specFile = project.layout.projectDirectory.file("openapi.yaml").asFile
            specFile.writeText("openapi: 3.0.3")
            extension.specFile.set(specFile.toURI().toString())

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.trackedSpecFiles.files).containsExactly(specFile)
        }

        @Test
        @DisplayName("should track a local spec declared only in the config file")
        fun shouldTrackConfigFileSpec() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            val specFile = project.layout.projectDirectory.file("openapi.yaml").asFile
            specFile.writeText("openapi: 3.0.3")
            project.layout.projectDirectory.file("config.yaml").asFile.writeText("specFile: openapi.yaml")
            extension.configFile.set("config.yaml")

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.trackedSpecFiles.files).containsExactly(specFile)
        }

        @Test
        @DisplayName("should not track a spec that is not a local file")
        fun shouldNotTrackNonLocalSpec() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.specFile.set("https://example.com/openapi.yaml")

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.trackedSpecFiles.files).isEmpty()
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
        @Suppress("DEPRECATION")
        @DisplayName("should wire logLevel from extension to task")
        fun shouldWireLogLevel() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.logLevel.set("DEBUG")

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.logLevel.get()).isEqualTo("DEBUG")
        }

        @Test
        @DisplayName("should wire parserSettings from extension to task")
        fun shouldWireParserSettings() {
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.parserSettings.yamlCodePointLimit.set(7_000_000)
            extension.parserSettings.yamlMaxAliasesForCollections.set(75)
            extension.parserSettings.yamlAllowRecursiveKeys.set(false)
            extension.parserSettings.yamlNestingDepthLimit.set(25)

            val task = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            assertThat(task.parserSettings.yamlCodePointLimit.get()).isEqualTo(7_000_000)
            assertThat(task.parserSettings.yamlMaxAliasesForCollections.get()).isEqualTo(75)
            assertThat(task.parserSettings.yamlAllowRecursiveKeys.get()).isFalse()
            assertThat(task.parserSettings.yamlNestingDepthLimit.get()).isEqualTo(25)
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
    @DisplayName("wiring completeness drift guard")
    inner class WiringDriftGuardTest {

        private fun providerPropertyNames(klass: kotlin.reflect.KClass<*>): Set<String> =
            klass.memberProperties
                .filter { Provider::class.java.isAssignableFrom(it.returnType.jvmErasure.java) }
                .map { it.name }
                .toSet()

        @Test
        @DisplayName("wireTestGenerationSettings must cover every extension property")
        fun testGenerationSettingsWiringIsComplete() {
            assertThat(providerPropertyNames(TestGenerationSettingsExtension::class))
                .withFailMessage(
                    "TestGenerationSettingsExtension properties changed. Update " +
                        "OpenApiTestGeneratorPlugin.wireTestGenerationSettings and " +
                        "TestGenerationSettingsExtension.buildTestGenerationSettingsMap, " +
                        "then adjust this guard."
                )
                .containsExactlyInAnyOrder(
                    "includeOperations",
                    "ignoreTestCases",
                    "ignoreSchemaValidationRules",
                    "ignoreAuthValidationRules",
                    "maxSchemaDepth",
                    "overrideBasicTestData",
                    "maxSchemaCombinations",
                    "maxMergedSchemaDepth",
                    "maxTestCasesPerOperation",
                    "validSecurityValues",
                    "errorMode",
                    "includeValidCase",
                    "maxErrors",
                    "exampleValues",
                    "patternGeneration",
                )
        }

        @Test
        @DisplayName("wireParserSettings must cover every extension property")
        fun parserSettingsWiringIsComplete() {
            assertThat(providerPropertyNames(ParserSettingsExtension::class))
                .withFailMessage(
                    "ParserSettingsExtension properties changed. Update " +
                        "OpenApiTestGeneratorPlugin.wireParserSettings and " +
                        "ParserSettingsExtension.buildParserSettingsMap, then adjust this guard."
                )
                .containsExactlyInAnyOrder(
                    "yamlCodePointLimit",
                    "yamlMaxAliasesForCollections",
                    "yamlAllowRecursiveKeys",
                    "yamlNestingDepthLimit",
                )
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

            val compileTestJavaTask = project.tasks.getByName(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME)

            assertThat(dependencyNames(compileTestJavaTask)).contains("generateOpenApiTests")
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

            val compileTestJavaTask = project.tasks.getByName(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME)

            assertThat(dependencyNames(compileTestJavaTask)).doesNotContain("generateOpenApiTests")
        }

        @Test
        @DisplayName("should still add the output directory as a test source dir when manualOnly is true")
        fun shouldAddSourceDirWhenManualOnly() {
            project.pluginManager.apply(JavaPlugin::class.java)
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.generator.set("template")
            extension.manualOnly.set(true)
            extension.outputDir.set(project.layout.projectDirectory.dir("generated"))

            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            val srcDirs = sourceSets.getByName("test").java.srcDirs

            assertThat(srcDirs.map { it.name }).contains("generated")
        }

        @Test
        @DisplayName("should configure mustRunAfter for compileTestJava")
        fun shouldConfigureMustRunAfter() {
            project.pluginManager.apply(JavaPlugin::class.java)
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.generator.set("template")
            extension.outputDir.set(project.layout.projectDirectory.dir("generated"))

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

            val processTestResourcesTask = project.tasks.getByName(JavaPlugin.PROCESS_TEST_RESOURCES_TASK_NAME)

            assertThat(dependencyNames(processTestResourcesTask)).contains("generateOpenApiTests")
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

            val processTestResourcesTask = project.tasks.getByName(JavaPlugin.PROCESS_TEST_RESOURCES_TASK_NAME)

            assertThat(dependencyNames(processTestResourcesTask)).doesNotContain("generateOpenApiTests")
        }

        @Test
        @DisplayName("should configure mustRunAfter for processTestResources")
        fun shouldConfigureMustRunAfterForProcessTestResources() {
            project.pluginManager.apply(JavaPlugin::class.java)
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.generator.set("test-suite-writer")
            extension.outputDir.set(project.layout.projectDirectory.dir("generated"))

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

            assertThat(project.tasks.findByName("generateOpenApiTests")).isNotNull()
        }
    }
}
