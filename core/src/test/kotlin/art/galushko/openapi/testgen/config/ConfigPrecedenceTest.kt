package art.galushko.openapi.testgen.config

import art.galushko.openapi.testgen.example.config.EmailProviderSettings
import art.galushko.openapi.testgen.example.config.ExampleValueSettings
import art.galushko.openapi.testgen.example.config.UuidProviderSettings
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@Epic("Configuration")
@Feature("Config Precedence")
@DisplayName("Config Precedence")
class ConfigPrecedenceTest {

    @Nested
    @Story("Config Sources")
    @DisplayName("Source Priority")
    inner class SourcePriority {

        @Test
        @DisplayName("should use YAML config values when overrides are empty")
        fun shouldUseYamlConfigWhenOverridesEmpty(@TempDir tempDir: Path) {
            val yamlOutputDir = tempDir.resolve("yaml-out")
            val configFile = tempDir.resolve("config.yaml")
            val yamlContent = """
                specFile: "config-spec.yaml"
                outputDir: '$yamlOutputDir'
                generator: "template"
                alwaysWriteTests: false
                generatorOptions:
                  packageName: "com.config"
                  keepFromConfig: "retained"
                testGenerationSettings:
                  maxSchemaDepth: 5
                  maxErrors: 100
                  validSecurityValues:
                    ApiKey: "config-secret"
            """.trimIndent()
            Files.writeString(configFile, yamlContent)

            val config = GeneratorConfigLoader.load(configFile)

            val options = TestGeneratorExecutionOptionsFactory.fromConfig(config)

            val expectedOptions = TestGeneratorExecutionOptions(
                specFile = "config-spec.yaml",
                outputDir = yamlOutputDir,
                generatorId = "template",
                generatorOptions = mapOf(
                    "packageName" to "com.config",
                    "keepFromConfig" to "retained",
                ),
                testGenerationSettings = TestGenerationSettings(
                    maxSchemaDepth = 5,
                    maxErrors = 100,
                    validSecurityValues = mapOf("ApiKey" to "config-secret"),
                ),
                alwaysWriteTests = false,
            )
            assertThat(options)
                .usingRecursiveComparison()
                .isEqualTo(expectedOptions)
        }

        @Test
        @DisplayName("should prefer overrides over YAML config with deep merge")
        fun shouldPreferOverridesOverYamlConfig(@TempDir tempDir: Path) {
            val yamlOutputDir = tempDir.resolve("yaml-out")
            val configFile = tempDir.resolve("config.yaml")
            val yamlContent = """
                specFile: "config-spec.yaml"
                outputDir: '$yamlOutputDir'
                generator: "template"
                alwaysWriteTests: false
                generatorOptions:
                  packageName: "com.config"
                  keepFromConfig: "retained"
                testGenerationSettings:
                  maxSchemaDepth: 5
                  maxErrors: 100
                  validSecurityValues:
                    ApiKey: "config-secret"
            """.trimIndent()
            Files.writeString(configFile, yamlContent)
            val config = GeneratorConfigLoader.load(configFile)

            val overrideOutputDir = tempDir.resolve("override-out")
            val overrides = TestGeneratorOverrides(
                specFile = "override-spec.yaml",
                outputDir = overrideOutputDir,
                generatorId = "custom-generator",
                generatorOptions = mapOf("packageName" to "com.override"),
                testGenerationSettings = mapOf(
                    "maxSchemaDepth" to 22,
                    "validSecurityValues" to mapOf("NewKey" to "override-secret"),
                ),
                alwaysWriteTests = true,
            )

            val options = TestGeneratorExecutionOptionsFactory.fromConfig(
                config = config,
                overrides = overrides,
            )

            val expectedOptions = TestGeneratorExecutionOptions(
                specFile = "override-spec.yaml",
                outputDir = overrideOutputDir,
                generatorId = "custom-generator",
                generatorOptions = mapOf(
                    "packageName" to "com.override",
                    "keepFromConfig" to "retained",
                ),
                testGenerationSettings = TestGenerationSettings(
                    maxSchemaDepth = 22,
                    maxErrors = 100,
                    validSecurityValues = mapOf(
                        "ApiKey" to "config-secret",
                        "NewKey" to "override-secret",
                    ),
                ),
                alwaysWriteTests = true,
            )
            assertThat(options).usingRecursiveComparison().isEqualTo(expectedOptions)
        }

        @Test
        @DisplayName("should use overrides-only when config is null")
        fun shouldUseOverridesOnlyWhenConfigNull(@TempDir tempDir: Path) {
            val outputDir = tempDir.resolve("override-out")

            val overrides = TestGeneratorOverrides(
                specFile = "override.yaml",
                outputDir = outputDir,
                generatorId = "test-suite-writer",
                generatorOptions = mapOf("format" to "json"),
                testGenerationSettings = mapOf(
                    "maxErrors" to 15,
                    "maxSchemaDepth" to 8,
                ),
                alwaysWriteTests = true,
            )

            val options = TestGeneratorExecutionOptionsFactory.fromConfig(
                config = null,
                overrides = overrides,
            )

            val expectedOptions = TestGeneratorExecutionOptions(
                specFile = "override.yaml",
                outputDir = outputDir,
                generatorId = "test-suite-writer",
                generatorOptions = mapOf("format" to "json"),
                testGenerationSettings = TestGenerationSettings(
                    maxErrors = 15,
                    maxSchemaDepth = 8,
                ),
                alwaysWriteTests = true,
            )
            assertThat(options)
                .usingRecursiveComparison()
                .isEqualTo(expectedOptions)
        }
    }

    @Nested
    @Story("Map Merging")
    @DisplayName("Nested Map Behavior")
    inner class NestedMapBehavior {

        @Test
        @DisplayName("should deep merge nested maps and replace lists")
        fun shouldDeepMergeMapsAndReplaceLists(@TempDir tempDir: Path) {
            val config = GeneratorConfig(
                specFile = "spec.yaml",
                outputDir = tempDir.toString(),
                generator = "template",
                testGenerationSettings = mapOf(
                    "exampleValues" to mapOf(
                        "providers" to listOf("enum", "const"),
                        "email" to mapOf("template" to "base%s@test.org"),
                    ),
                ),
            )

            val overrides = TestGeneratorOverrides(
                testGenerationSettings = mapOf(
                    "exampleValues" to mapOf(
                        "providers" to listOf("pattern"),
                        "uuid" to mapOf("template" to "override-%s"),
                    ),
                ),
            )

            val options = TestGeneratorExecutionOptionsFactory.fromConfig(
                config = config,
                overrides = overrides,
            )

            val expectedExampleValues = ExampleValueSettings(
                providers = listOf("pattern"),
                email = EmailProviderSettings(template = "base%s@test.org"),
                uuid = UuidProviderSettings(template = "override-%s"),
            )
            assertThat(options.testGenerationSettings.exampleValues)
                .usingRecursiveComparison()
                .isEqualTo(expectedExampleValues)
        }
    }

    @Nested
    @Story("Module Settings")
    @DisplayName("Module Settings Extraction")
    inner class ModuleSettingsExtraction {

        @Test
        @DisplayName("should extract module settings in sorted key order and preserve values")
        fun shouldExtractModuleSettingsInSortedKeyOrder(@TempDir tempDir: Path) {
            val config = GeneratorConfig(
                specFile = "spec.yaml",
                outputDir = tempDir.toString(),
                generator = "template",
                testGenerationSettings = mapOf(
                    "betaModule" to mapOf("value" to "b", "extra" to 123),
                    "alphaModule" to mapOf("value" to "a"),
                ),
            )

            val extractionOrder = mutableListOf<String>()

            val betaExtractor = object : ModuleSettingsExtractor {
                override val settingsKey: String = "betaModule"
                override fun parse(raw: Any?): Any {
                    extractionOrder.add(settingsKey)
                    return raw ?: emptyMap<String, Any>()
                }
            }

            val alphaExtractor = object : ModuleSettingsExtractor {
                override val settingsKey: String = "alphaModule"
                override fun parse(raw: Any?): Any {
                    extractionOrder.add(settingsKey)
                    return raw ?: emptyMap<String, Any>()
                }
            }

            val options = TestGeneratorExecutionOptionsFactory.fromConfig(
                config = config,
                moduleExtractors = listOf(betaExtractor, alphaExtractor),
            )

            assertThat(extractionOrder).isEqualTo(listOf("alphaModule", "betaModule"))

            assertThat(options.moduleSettings.get<Map<String, Any>>("alphaModule"))
                .isEqualTo(mapOf("value" to "a"))
            assertThat(options.moduleSettings.get<Map<String, Any>>("betaModule"))
                .isEqualTo(mapOf("value" to "b", "extra" to 123))
        }
    }
}
