package art.galushko.openapi.testgen.config

import art.galushko.openapi.testgen.example.config.EmailProviderSettings
import art.galushko.openapi.testgen.example.config.ExampleValueSettings
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
@Feature("Options Factory")
@DisplayName("TestGeneratorExecutionOptionsFactory")
class TestGeneratorExecutionOptionsFactoryTest {

    @Nested
    @Story("Resolution Priority")
    @DisplayName("Priority Resolution")
    inner class PriorityResolution {

        @Test
        @DisplayName("overrides should take precedence over config")
        fun overridesShouldTakePrecedence(@TempDir tempDir: Path) {
            val config = GeneratorConfig(
                specFile = "config.yaml",
                outputDir = tempDir.resolve("config-out").toString(),
                generator = "template",
                alwaysWriteTests = false
            )

            val overrides = TestGeneratorOverrides(
                specFile = "override.yaml",
                outputDir = tempDir.resolve("override-out"),
                generatorId = "custom",
                alwaysWriteTests = true
            )

            val options = TestGeneratorExecutionOptionsFactory.fromConfig(config, overrides)

            assertThat(options.specFile).isEqualTo("override.yaml")
            assertThat(options.outputDir).isEqualTo(tempDir.resolve("override-out"))
            assertThat(options.generatorId).isEqualTo("custom")
            assertThat(options.alwaysWriteTests).isTrue()
        }

        @Test
        @DisplayName("should fallback to config when overrides are missing")
        fun shouldFallbackToConfig(@TempDir tempDir: Path) {
            val config = GeneratorConfig(
                specFile = "config.yaml",
                outputDir = tempDir.resolve("config-out").toString(),
                generator = "template",
                alwaysWriteTests = true
            )

            val overrides = TestGeneratorOverrides() // Empty overrides

            val options = TestGeneratorExecutionOptionsFactory.fromConfig(config, overrides)

            assertThat(options.specFile).isEqualTo("config.yaml")
            assertThat(options.outputDir).isEqualTo(tempDir.resolve("config-out"))
            assertThat(options.generatorId).isEqualTo("template")
            assertThat(options.alwaysWriteTests).isTrue()
        }

        @Test
        @DisplayName("should throw if required fields are missing in both")
        fun shouldThrowIfMissing() {
            val config = GeneratorConfig() // Empty config
            val overrides = TestGeneratorOverrides() // Empty overrides

            // specFile missing
            assertThatThrownBy { TestGeneratorExecutionOptionsFactory.fromConfig(config, overrides) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("specFile must be configured via overrides or config file")

            // outputDir missing
            val overridesWithSpec = TestGeneratorOverrides(specFile = "spec.yaml")
            assertThatThrownBy { TestGeneratorExecutionOptionsFactory.fromConfig(config, overridesWithSpec) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("outputDir must be configured via overrides or config file")

            // generator missing
            val overridesWithSpecAndDir = TestGeneratorOverrides(
                specFile = "spec.yaml",
                outputDir = Path.of("out")
            )
            assertThatThrownBy { TestGeneratorExecutionOptionsFactory.fromConfig(config, overridesWithSpecAndDir) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Generator must be provided either via overrides or config")
        }
    }

    @Nested
    @Story("Map Merging")
    @DisplayName("Map Merging Logic")
    inner class MapMerging {

        @Test
        @DisplayName("should merge generator options")
        fun shouldMergeGeneratorOptions(@TempDir tempDir: Path) {
            val config = GeneratorConfig(
                specFile = "spec.yaml",
                outputDir = tempDir.toString(),
                generator = "template",
                generatorOptions = mapOf(
                    "key1" to "configVal1",
                    "key2" to "configVal2"
                )
            )

            val overrides = TestGeneratorOverrides(
                generatorOptions = mapOf(
                    "key2" to "overrideVal2",
                    "key3" to "overrideVal3"
                )
            )

            val options = TestGeneratorExecutionOptionsFactory.fromConfig(config, overrides)

            assertThat(options.generatorOptions).containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    "key1" to "configVal1",
                    "key2" to "overrideVal2", // Overridden
                    "key3" to "overrideVal3"
                )
            )
        }

        @Test
        @DisplayName("should merge test generation settings")
        fun shouldMergeTestGenerationSettings(@TempDir tempDir: Path) {
            val config = GeneratorConfig(
                specFile = "spec.yaml",
                outputDir = tempDir.toString(),
                generator = "template",
                testGenerationSettings = mapOf(
                    "maxSchemaDepth" to 10,
                    "maxErrors" to 50
                )
            )

            val overrides = TestGeneratorOverrides(
                testGenerationSettings = mapOf(
                    "maxSchemaDepth" to 20,
                    "maxTestCasesPerOperation" to 5
                )
            )

            val options = TestGeneratorExecutionOptionsFactory.fromConfig(config, overrides)
            val settings = options.testGenerationSettings

            assertThat(settings.maxSchemaDepth).isEqualTo(20) // Overridden
            assertThat(settings.maxErrors).isEqualTo(50) // From config
            assertThat(settings.maxTestCasesPerOperation).isEqualTo(5) // From overrides
        }

        @Test
        @DisplayName("should deep merge nested maps from YAML config and overrides")
        fun shouldDeepMergeNestedMapsFromYaml(@TempDir tempDir: Path) {
            val configFile = tempDir.resolve("config.yaml")
            val yamlContent = """
                specFile: "spec.yaml"
                outputDir: '$tempDir'
                generator: "template"
                generatorOptions:
                  nested:
                    baseKey: "base"
                    keepKey: "keep"
                testGenerationSettings:
                  exampleValues:
                    email:
                      template: "base%s@test.org"
                    providers:
                      - "enum"
            """.trimIndent()

            Files.writeString(configFile, yamlContent)

            val config = GeneratorConfigLoader.load(configFile)
            val overrides = TestGeneratorOverrides(
                generatorOptions = mapOf(
                    "nested" to mapOf(
                        "baseKey" to "override",
                    ),
                ),
                testGenerationSettings = mapOf(
                    "exampleValues" to mapOf(
                        "uuid" to mapOf(
                            "template" to "override-%s",
                        ),
                    ),
                ),
            )

            val options = TestGeneratorExecutionOptionsFactory.fromConfig(config, overrides)

            val nested = options.generatorOptions["nested"] as Map<*, *>
            assertThat(nested["baseKey"]).isEqualTo("override")
            assertThat(nested["keepKey"]).isEqualTo("keep")

            val exampleValues = options.testGenerationSettings.exampleValues
            assertThat(exampleValues.email.template).isEqualTo("base%s@test.org")
            assertThat(exampleValues.uuid.template).isEqualTo("override-%s")
            assertThat(exampleValues.providers).containsExactly("enum")
        }

        @Test
        @DisplayName("should warn but not throw for unknown test generation settings")
        fun shouldWarnForUnknownTestGenerationSettings(@TempDir tempDir: Path) {
            val config = GeneratorConfig(
                specFile = "spec.yaml",
                outputDir = tempDir.toString(),
                generator = "template",
                testGenerationSettings = mapOf(
                    "unknownKey" to "someValue",
                ),
            )

            // Unknown keys are logged as warnings but don't throw
            val options = TestGeneratorExecutionOptionsFactory.fromConfig(config)
            assertThat(options.specFile).isEqualTo("spec.yaml")
        }

        @Test
        @DisplayName("should extract module settings when extractor is provided")
        fun shouldExtractModuleSettingsWhenExtractorProvided(@TempDir tempDir: Path) {
            val config = GeneratorConfig(
                specFile = "spec.yaml",
                outputDir = tempDir.toString(),
                generator = "template",
                testGenerationSettings = mapOf(
                    "testModuleKey" to mapOf(
                        "option1" to "value1",
                    ),
                ),
            )

            val testExtractor = object : ModuleSettingsExtractor {
                override val settingsKey: String = "testModuleKey"
                override fun parse(raw: Any?): Any = raw ?: emptyMap<String, Any>()
            }

            val options = TestGeneratorExecutionOptionsFactory.fromConfig(
                config,
                moduleExtractors = listOf(testExtractor),
            )

            assertThat(options.moduleSettings.get<Map<String, Any>>("testModuleKey"))
                .isEqualTo(mapOf("option1" to "value1"))
        }

        @Test
        @DisplayName("should warn for module settings when extractor is NOT provided")
        fun shouldWarnForModuleSettingsWithoutExtractor(@TempDir tempDir: Path) {
            val config = GeneratorConfig(
                specFile = "spec.yaml",
                outputDir = tempDir.toString(),
                generator = "template",
                testGenerationSettings = mapOf(
                    "patternGeneration" to linkedMapOf(
                        "defaultMinLength" to 10,
                    ),
                ),
            )

            // Without extractor, the key is treated as unknown and logged as warning
            val options = TestGeneratorExecutionOptionsFactory.fromConfig(config)
            assertThat(options.moduleSettings.get<Any>("patternGeneration")).isNull()
        }
    }

    @Nested
    @Story("Side Effects")
    @DisplayName("Side Effects")
    inner class SideEffects {

        @Test
        @DisplayName("should create output directory if not exists")
        fun shouldCreateOutputDir(@TempDir tempDir: Path) {
            val outputDir = tempDir.resolve("new-dir")
            assertThat(outputDir).doesNotExist()

            val config = GeneratorConfig(
                specFile = "spec.yaml",
                outputDir = outputDir.toString(),
                generator = "template"
            )

            TestGeneratorExecutionOptionsFactory.fromConfig(config)

            assertThat(outputDir).exists()
            assertThat(outputDir).isDirectory()
        }
    }

    @Nested
    @Story("Default Settings")
    @DisplayName("Default Settings Override")
    inner class DefaultSettingsOverride {

        @Test
        @DisplayName("should use defaultTestGenerationSettings when config is empty")
        fun shouldUseDefaultSettingsWhenConfigEmpty(@TempDir tempDir: Path) {
            val customDefaults = TestGenerationSettings(
                maxSchemaDepth = 99,
                maxSchemaCombinations = 77,
                exampleValues = ExampleValueSettings(
                    providers = listOf("enum", "const", "custom-provider"),
                ),
            )

            val config = GeneratorConfig(
                specFile = "spec.yaml",
                outputDir = tempDir.toString(),
                generator = "template",
                // No testGenerationSettings - should fallback to customDefaults
            )

            val options = TestGeneratorExecutionOptionsFactory.fromConfig(
                config = config,
                defaultTestGenerationSettings = customDefaults,
            )

            assertThat(options.testGenerationSettings.maxSchemaDepth).isEqualTo(99)
            assertThat(options.testGenerationSettings.maxSchemaCombinations).isEqualTo(77)
            assertThat(options.testGenerationSettings.exampleValues.providers)
                .containsExactly("enum", "const", "custom-provider")
        }

        @Test
        @DisplayName("should use config values over defaultTestGenerationSettings")
        fun shouldUseConfigOverDefaults(@TempDir tempDir: Path) {
            val customDefaults = TestGenerationSettings(
                maxSchemaDepth = 99,
                maxSchemaCombinations = 77,
            )

            val config = GeneratorConfig(
                specFile = "spec.yaml",
                outputDir = tempDir.toString(),
                generator = "template",
                testGenerationSettings = mapOf(
                    "maxSchemaDepth" to 25, // Override default
                ),
            )

            val options = TestGeneratorExecutionOptionsFactory.fromConfig(
                config = config,
                defaultTestGenerationSettings = customDefaults,
            )

            assertThat(options.testGenerationSettings.maxSchemaDepth).isEqualTo(25) // From config
            assertThat(options.testGenerationSettings.maxSchemaCombinations).isEqualTo(77) // From defaults
        }

        @Test
        @DisplayName("should use overrides over config and defaultTestGenerationSettings")
        fun shouldUseOverridesOverConfigAndDefaults(@TempDir tempDir: Path) {
            val customDefaults = TestGenerationSettings(
                maxSchemaDepth = 99,
                maxSchemaCombinations = 77,
                maxTestCasesPerOperation = 500,
            )

            val config = GeneratorConfig(
                specFile = "spec.yaml",
                outputDir = tempDir.toString(),
                generator = "template",
                testGenerationSettings = mapOf(
                    "maxSchemaDepth" to 25,
                    "maxSchemaCombinations" to 33,
                ),
            )

            val overrides = TestGeneratorOverrides(
                testGenerationSettings = mapOf(
                    "maxSchemaDepth" to 10, // Override both config and defaults
                ),
            )

            val options = TestGeneratorExecutionOptionsFactory.fromConfig(
                config = config,
                overrides = overrides,
                defaultTestGenerationSettings = customDefaults,
            )

            assertThat(options.testGenerationSettings.maxSchemaDepth).isEqualTo(10) // From overrides
            assertThat(options.testGenerationSettings.maxSchemaCombinations).isEqualTo(33) // From config
            assertThat(options.testGenerationSettings.maxTestCasesPerOperation).isEqualTo(500) // From defaults
        }

        @Test
        @DisplayName("should propagate default exampleValues through nested settings")
        fun shouldPropagateDefaultExampleValues(@TempDir tempDir: Path) {
            val customDefaults = TestGenerationSettings(
                exampleValues = ExampleValueSettings(
                    providers = listOf("pattern", "plain-string"),
                    email = EmailProviderSettings(template = "custom%s@test.org"),
                ),
            )

            val config = GeneratorConfig(
                specFile = "spec.yaml",
                outputDir = tempDir.toString(),
                generator = "template",
                testGenerationSettings = mapOf(
                    "exampleValues" to mapOf(
                        "maxExampleDepth" to 15, // Only override this
                    ),
                ),
            )

            val options = TestGeneratorExecutionOptionsFactory.fromConfig(
                config = config,
                defaultTestGenerationSettings = customDefaults,
            )

            val exampleValues = options.testGenerationSettings.exampleValues
            assertThat(exampleValues.maxExampleDepth).isEqualTo(15) // From config
            assertThat(exampleValues.providers).containsExactly("pattern", "plain-string") // From defaults
            assertThat(exampleValues.email.template).isEqualTo("custom%s@test.org") // From defaults
        }
    }

    @Nested
    @Story("Pretty Print")
    @DisplayName("Pretty Print Function")
    inner class PrettyPrintFunction {

        @Test
        @DisplayName("should format data class as YAML")
        fun shouldFormatDataClassAsYaml(@TempDir tempDir: Path) {
            val testData = TestGeneratorExecutionOptions(
                specFile = "test.yaml",
                outputDir = tempDir,
                generatorId = "template",
                generatorOptions = mapOf("key1" to "value1", "key2" to 42),
                testGenerationSettings = TestGenerationSettings(
                    maxSchemaDepth = 10,
                    validSecurityValues = mapOf("apiKey" to "testKey"),
                ),
                alwaysWriteTests = true,
            )

            val result = TestGeneratorExecutionOptionsFactory.prettyPrint(testData)

            assertThat(result).doesNotStartWith("---")
            assertThat(result).contains("specFile:")
            assertThat(result).contains("test.yaml")
            assertThat(result).contains("generatorId:")
            assertThat(result).contains("template")
            assertThat(result).contains("testGenerationSettings:")
            assertThat(result).contains("maxSchemaDepth:")
            assertThat(result).contains("validSecurityValues:")
            assertThat(result).contains("apiKey:")
            assertThat(result).contains("testKey")
            assertThat(result).contains("alwaysWriteTests:")
            assertThat(result).contains("true")
        }
    }
}


