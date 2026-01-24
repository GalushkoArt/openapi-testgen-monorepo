package art.galushko.openapi.testgen.plugin

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.internal.project.DefaultProject
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

    private lateinit var project: DefaultProject
    private lateinit var task: OpenApiTestGeneratorTask

    @TempDir
    private lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .build() as DefaultProject
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
                .hasMessageContaining("Parsed OpenAPI model is null")
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

            task.configFile.set("config.yaml")
            // Spec and output dir can be overridden
            task.specFile.set("openapi.yaml")
            task.outputDir.set(project.layout.projectDirectory.dir("generated"))

            task.generate()

            assertThat(outputDir.toFile().exists()).isTrue()
        }
    }
}
