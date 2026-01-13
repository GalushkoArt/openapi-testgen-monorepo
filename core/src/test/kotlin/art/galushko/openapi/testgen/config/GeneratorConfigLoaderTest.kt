package art.galushko.openapi.testgen.config

import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@Epic("Configuration")
@Feature("Config Loader")
@DisplayName("GeneratorConfigLoader")
class GeneratorConfigLoaderTest {

    @Nested
    @Story("Loading")
    @DisplayName("File Loading")
    inner class FileLoading {

        @Test
        @DisplayName("should load valid yaml config")
        fun shouldLoadValidConfig(@TempDir tempDir: Path) {
            val configFile = tempDir.resolve("config.yaml")
            val yamlContent = """
                specFile: "openapi.yaml"
                outputDir: "generated"
                generator: "template"
                alwaysWriteTests: true
                generatorOptions:
                  templateSet: "restassured"
                testGenerationSettings:
                  maxSchemaDepth: 5
            """.trimIndent()

            Files.writeString(configFile, yamlContent)

            val config = GeneratorConfigLoader.load(configFile)

            assertThat(config.specFile).isEqualTo("openapi.yaml")
            assertThat(config.outputDir).isEqualTo("generated")
            assertThat(config.generator).isEqualTo("template")
            assertThat(config.alwaysWriteTests).isTrue()

            assertThat(config.generatorOptions)
                .containsEntry("templateSet", "restassured")

            assertThat(config.testGenerationSettings)
                .containsEntry("maxSchemaDepth", 5)
        }

        @Test
        @DisplayName("should throw if file does not exist")
        fun shouldThrowIfMissing(@TempDir tempDir: Path) {
            val missingFile = tempDir.resolve("missing.yaml")

            assertThatThrownBy { GeneratorConfigLoader.load(missingFile) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Config file does not exist")
        }

        @Test
        @DisplayName("should fail on malformed yaml")
        fun shouldFailOnMalformedYaml(@TempDir tempDir: Path) {
            val malformedFile = tempDir.resolve("bad.yaml")
            Files.writeString(malformedFile, ": - invalid yaml :")

            // Jackson YAML parser should throw an exception
            assertThatThrownBy { GeneratorConfigLoader.load(malformedFile) }
                .isInstanceOf(Exception::class.java) // Could be JsonProcessingException or similar
        }

        @Test
        @DisplayName("should ignore unknown properties")
        fun shouldIgnoreUnknownProperties(@TempDir tempDir: Path) {
            val configFile = tempDir.resolve("config.yaml")
            val yamlContent = """
                specFile: "openapi.yaml"
                unknownProp: "should be ignored"
            """.trimIndent()

            Files.writeString(configFile, yamlContent)

            val config = GeneratorConfigLoader.load(configFile)
            assertThat(config.specFile).isEqualTo("openapi.yaml")
        }
    }
}


