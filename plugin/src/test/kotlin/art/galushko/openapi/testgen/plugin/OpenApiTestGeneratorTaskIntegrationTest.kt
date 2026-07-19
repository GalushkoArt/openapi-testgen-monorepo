package art.galushko.openapi.testgen.plugin

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("OpenApiTestGeneratorTask Integration Tests")
class OpenApiTestGeneratorTaskIntegrationTest {

    private lateinit var project: Project
    private lateinit var task: OpenApiTestGeneratorTask

    @TempDir
    private lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .build()
        task = project.tasks.register("generateTests", OpenApiTestGeneratorTask::class.java).get()
    }

    @Nested
    @DisplayName("generate with valid spec")
    inner class GenerateWithValidSpecTest {

        @Test
        @DisplayName("should generate test suite files with test-suite-writer")
        fun shouldGenerateTestSuiteFiles() {
            // Copy spec to project dir
            val specContent = this::class.java.getResourceAsStream("/openapi-minimal.yaml")?.reader()?.readText()
                ?: error("Cannot read test spec file")
            val specFile = tempDir.resolve("openapi.yaml")
            Files.writeString(specFile, specContent)

            val outputDir = tempDir.resolve("generated")
            Files.createDirectories(outputDir)

            task.specFile.set("openapi.yaml")
            task.outputDir.set(project.layout.projectDirectory.dir("generated"))
            task.generator.set("test-suite-writer")
            task.generatorOptions.put("format", "json")
            task.generatorOptions.put("outputMode", "MULTIPLE_FILES")

            task.generate()

            // Verify output was created
            assertThat(outputDir.toFile().exists()).isTrue()
            val generatedFiles = outputDir.toFile().listFiles()
            assertThat(generatedFiles).isNotEmpty()
        }

        @Test
        @DisplayName("should generate test suite files from Swagger 2.0 spec")
        fun shouldGenerateTestSuiteFilesFromSwagger2Spec() {
            val specContent = this::class.java.getResourceAsStream("/swagger-20-minimal.yaml")?.reader()?.readText()
                ?: error("Cannot read Swagger 2.0 test spec file")
            val specFile = tempDir.resolve("swagger.yaml")
            Files.writeString(specFile, specContent)

            val outputDir = tempDir.resolve("generated-swagger2")
            Files.createDirectories(outputDir)

            task.specFile.set("swagger.yaml")
            task.outputDir.set(project.layout.projectDirectory.dir("generated-swagger2"))
            task.generator.set("test-suite-writer")
            task.generatorOptions.put("format", "json")
            task.generatorOptions.put("outputMode", "MULTIPLE_FILES")

            task.generate()

            assertThat(outputDir.toFile().exists()).isTrue()
            val generatedFiles = outputDir.toFile().listFiles()
            assertThat(generatedFiles).isNotEmpty()
        }

        @Test
        @DisplayName("should pass parser settings through for Swagger 2.0 spec")
        fun shouldPassParserSettingsThroughForSwagger2Spec() {
            val specContent = this::class.java.getResourceAsStream("/swagger-20-minimal.yaml")?.reader()?.readText()
                ?: error("Cannot read Swagger 2.0 test spec file")
            val specFile = tempDir.resolve("swagger.yaml")
            Files.writeString(specFile, specContent)

            val outputDir = tempDir.resolve("generated-swagger2-parser")
            Files.createDirectories(outputDir)

            task.specFile.set("swagger.yaml")
            task.outputDir.set(project.layout.projectDirectory.dir("generated-swagger2-parser"))
            task.generator.set("test-suite-writer")
            task.generatorOptions.put("format", "json")
            task.generatorOptions.put("outputMode", "MULTIPLE_FILES")
            task.parserSettings.yamlCodePointLimit.set(10_000_000)
            task.parserSettings.yamlMaxAliasesForCollections.set(100)
            task.parserSettings.yamlAllowRecursiveKeys.set(true)
            task.parserSettings.yamlNestingDepthLimit.set(100)

            task.generate()

            assertThat(outputDir.toFile().exists()).isTrue()
            val generatedFiles = outputDir.toFile().listFiles()
            assertThat(generatedFiles).isNotEmpty()
        }

        @Test
        @DisplayName("should use absolute path for spec file when file exists")
        fun shouldUseAbsolutePathForSpec() {
            val specContent = this::class.java.getResourceAsStream("/openapi-minimal.yaml")?.reader()?.readText()
                ?: error("Cannot read test spec file")
            val specFile = tempDir.resolve("openapi.yaml")
            Files.writeString(specFile, specContent)

            val outputDir = tempDir.resolve("generated")
            Files.createDirectories(outputDir)

            task.specFile.set("openapi.yaml")
            task.outputDir.set(project.layout.projectDirectory.dir("generated"))
            task.generator.set("test-suite-writer")
            task.generatorOptions.put("format", "json")
            task.generatorOptions.put("outputMode", "MULTIPLE_FILES")

            // Should not throw - spec file will be resolved to absolute path
            task.generate()
        }

        @Test
        @DisplayName("should apply testGenerationSettings")
        fun shouldApplyTestGenerationSettings() {
            val specContent = this::class.java.getResourceAsStream("/openapi-minimal.yaml")?.reader()?.readText()
                ?: error("Cannot read test spec file")
            val specFile = tempDir.resolve("openapi.yaml")
            Files.writeString(specFile, specContent)

            val outputDir = tempDir.resolve("generated")
            Files.createDirectories(outputDir)

            task.specFile.set("openapi.yaml")
            task.outputDir.set(project.layout.projectDirectory.dir("generated"))
            task.generator.set("test-suite-writer")
            task.generatorOptions.put("format", "json")
            task.generatorOptions.put("outputMode", "MULTIPLE_FILES")
            task.testGenerationSettings {
                maxSchemaDepth.set(10)
                includeValidCase.set(true)
            }

            task.generate()

            assertThat(outputDir.toFile().exists()).isTrue()
        }

        @Test
        @DisplayName("should apply logLevel setting")
        fun shouldApplyLogLevelSetting() {
            val specContent = this::class.java.getResourceAsStream("/openapi-minimal.yaml")?.reader()?.readText()
                ?: error("Cannot read test spec file")
            val specFile = tempDir.resolve("openapi.yaml")
            Files.writeString(specFile, specContent)

            val outputDir = tempDir.resolve("generated")
            Files.createDirectories(outputDir)

            task.specFile.set("openapi.yaml")
            task.outputDir.set(project.layout.projectDirectory.dir("generated"))
            task.generator.set("test-suite-writer")
            task.generatorOptions.put("format", "json")
            task.generatorOptions.put("outputMode", "MULTIPLE_FILES")
            task.logLevel.set("DEBUG")

            task.generate()

            assertThat(outputDir.toFile().exists()).isTrue()
        }
    }

    @Nested
    @DisplayName("generate with invalid input")
    inner class GenerateWithInvalidInputTest {

        @Test
        @DisplayName("should throw when spec file does not exist")
        fun shouldThrowWhenSpecFileDoesNotExist() {
            val outputDir = tempDir.resolve("generated")
            Files.createDirectories(outputDir)

            task.specFile.set("nonexistent.yaml")
            task.outputDir.set(project.layout.projectDirectory.dir("generated"))
            task.generator.set("test-suite-writer")
            task.generatorOptions.put("format", "json")
            task.generatorOptions.put("outputMode", "MULTIPLE_FILES")

            // Will throw due to missing spec file (OpenAPI parser returns null for missing file)
            assertThatThrownBy { task.generate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Parsed unknown OpenAPI/Swagger version model is null")
                .hasMessageContaining("Unable to read location")
        }

        @Test
        @DisplayName("should fail fast when parser settings are invalid")
        fun shouldFailFastForInvalidParserSettings() {
            val specContent = this::class.java.getResourceAsStream("/openapi-minimal.yaml")?.reader()?.readText()
                ?: error("Cannot read test spec file")
            val specFile = tempDir.resolve("openapi.yaml")
            Files.writeString(specFile, specContent)

            val outputDir = tempDir.resolve("generated")
            Files.createDirectories(outputDir)

            task.specFile.set("openapi.yaml")
            task.outputDir.set(project.layout.projectDirectory.dir("generated"))
            task.generator.set("test-suite-writer")
            task.generatorOptions.put("format", "json")
            task.generatorOptions.put("outputMode", "MULTIPLE_FILES")
            task.parserSettings.yamlCodePointLimit.set(0)

            assertThatThrownBy { task.generate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("yamlCodePointLimit must be positive or null, was 0")
        }
    }

    @Nested
    @DisplayName("generate with config file")
    inner class GenerateWithConfigFileTest {

        @Test
        @DisplayName("should load settings from config file")
        fun shouldLoadSettingsFromConfigFile() {
            val specContent = this::class.java.getResourceAsStream("/openapi-minimal.yaml")?.reader()?.readText()
                ?: error("Cannot read test spec file")
            val specFile = tempDir.resolve("openapi.yaml")
            Files.writeString(specFile, specContent)

            val outputDir = tempDir.resolve("generated")
            Files.createDirectories(outputDir)

            // Create config file
            val configContent = """
                specFile: openapi.yaml
                outputDir: generated
                generator: test-suite-writer
                generatorOptions:
                  format: json
                  outputMode: MULTIPLE_FILES
            """.trimIndent()
            val configFile = tempDir.resolve("config.yaml")
            Files.writeString(configFile, configContent)

            task.configFile.set(project.layout.projectDirectory.file("config.yaml"))
            // Spec and output dir can be overridden
            task.specFile.set("openapi.yaml")
            task.outputDir.set(project.layout.projectDirectory.dir("generated"))

            task.generate()

            assertThat(outputDir.toFile().exists()).isTrue()
        }

        @Test
        @DisplayName("should use generator from config file when DSL leaves it unset")
        fun shouldUseGeneratorFromConfigFileWhenDslUnset() {
            val specContent = this::class.java.getResourceAsStream("/openapi-minimal.yaml")?.reader()?.readText()
                ?: error("Cannot read test spec file")
            Files.writeString(tempDir.resolve("openapi.yaml"), specContent)

            val outputDir = tempDir.resolve("generated-config-generator")

            val configContent = """
                specFile: openapi.yaml
                outputDir: generated-config-generator
                generator: test-suite-writer
                generatorOptions:
                  format: json
                  outputMode: MULTIPLE_FILES
            """.trimIndent()
            Files.writeString(tempDir.resolve("config.yaml"), configContent)

            // Go through the plugin so the extension's generator convention ("") is wired
            // into the task, exactly as in a real build without an explicit generator.
            project.pluginManager.apply(OpenApiTestGeneratorPlugin::class.java)
            val extension = project.extensions.getByType(TestGeneratorExtension::class.java)
            extension.configFile.set("config.yaml")
            val pluginTask = project.tasks.getByName("generateOpenApiTests") as OpenApiTestGeneratorTask

            pluginTask.generate()

            assertThat(outputDir.toFile().exists()).isTrue()
            assertThat(outputDir.toFile().listFiles()).isNotEmpty()
        }

        @Test
        @DisplayName("should load Swagger 2.0 settings from config file")
        fun shouldLoadSwagger2SettingsFromConfigFile() {
            val specContent = this::class.java.getResourceAsStream("/swagger-20-minimal.yaml")?.reader()?.readText()
                ?: error("Cannot read Swagger 2.0 test spec file")
            val specFile = tempDir.resolve("swagger.yaml")
            Files.writeString(specFile, specContent)

            val outputDir = tempDir.resolve("generated-swagger2-config")
            Files.createDirectories(outputDir)

            val configContent = """
                specFile: swagger.yaml
                outputDir: generated-swagger2-config
                generator: test-suite-writer
                generatorOptions:
                  format: json
                  outputMode: MULTIPLE_FILES
            """.trimIndent()
            val configFile = tempDir.resolve("config-swagger2.yaml")
            Files.writeString(configFile, configContent)

            task.configFile.set(project.layout.projectDirectory.file("config-swagger2.yaml"))
            task.specFile.set("swagger.yaml")
            task.outputDir.set(project.layout.projectDirectory.dir("generated-swagger2-config"))

            task.generate()

            assertThat(outputDir.toFile().exists()).isTrue()
            val generatedFiles = outputDir.toFile().listFiles()
            assertThat(generatedFiles).isNotEmpty()
        }
    }
}
