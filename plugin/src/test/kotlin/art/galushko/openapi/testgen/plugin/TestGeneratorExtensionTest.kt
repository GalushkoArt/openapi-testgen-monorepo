package art.galushko.openapi.testgen.plugin

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.internal.project.DefaultProject
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("TestGeneratorExtension")
class TestGeneratorExtensionTest {

    private lateinit var project: DefaultProject
    private lateinit var extension: TestGeneratorExtension

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build() as DefaultProject
        extension = project.objects.newInstance(TestGeneratorExtension::class.java)
    }

    @Test
    @Suppress("DEPRECATION")
    @DisplayName("Extension should have default values for all properties except testGenerationSettings")
    fun testDefaultValues() {
        assertThat(extension.configFile.orNull).isNull()
        assertThat(extension.specFile.orNull).isNull()
        assertThat(extension.outputDir.orNull).isNull()
        assertThat(extension.generator.get()).isEqualTo("")
        assertThat(extension.generatorOptions.get()).isEmpty()
        assertThat(extension.manualOnly.get()).isFalse()
        assertThat(extension.alwaysWriteTests.orNull).isNull()
        assertThat(extension.logLevel.orNull).isNull()
        assertThat(extension.testGenerationSettings).isNotNull()
        assertThat(extension.parserSettings).isNotNull()
        assertThat(extension.parserSettings.buildParserSettingsMap()).isEmpty()
    }

    @Nested
    @DisplayName("property setting")
    inner class PropertySettingTest {

        @Test
        @DisplayName("should set configFile")
        fun shouldSetConfigFile() {
            extension.configFile.set("config.yaml")

            assertThat(extension.configFile.get()).isEqualTo("config.yaml")
        }

        @Test
        @DisplayName("should set specFile")
        fun shouldSetSpecFile() {
            extension.specFile.set("openapi.yaml")

            assertThat(extension.specFile.get()).isEqualTo("openapi.yaml")
        }

        @Test
        @DisplayName("should set outputDir")
        fun shouldSetOutputDir() {
            val dir = project.layout.projectDirectory.dir("generated")
            extension.outputDir.set(dir)

            assertThat(extension.outputDir.get().asFile.name).isEqualTo("generated")
        }

        @Test
        @DisplayName("should set generator")
        fun shouldSetGenerator() {
            extension.generator.set("template")

            assertThat(extension.generator.get()).isEqualTo("template")
        }

        @Test
        @DisplayName("should set generatorOptions")
        fun shouldSetGeneratorOptions() {
            extension.generatorOptions.put("templateSet", "restassured-java")
            extension.generatorOptions.put("package", "com.example")

            assertThat(extension.generatorOptions.get())
                .containsEntry("templateSet", "restassured-java")
                .containsEntry("package", "com.example")
        }

        @Test
        @DisplayName("should set manualOnly")
        fun shouldSetManualOnly() {
            extension.manualOnly.set(true)

            assertThat(extension.manualOnly.get()).isTrue()
        }

        @Test
        @DisplayName("should set alwaysWriteTests")
        fun shouldSetAlwaysWriteTests() {
            extension.alwaysWriteTests.set(true)

            assertThat(extension.alwaysWriteTests.get()).isTrue()
        }

        @Test
        @Suppress("DEPRECATION")
        @DisplayName("should set logLevel")
        fun shouldSetLogLevel() {
            extension.logLevel.set("DEBUG")

            assertThat(extension.logLevel.get()).isEqualTo("DEBUG")
        }
    }

    @Nested
    @DisplayName("testGenerationSettings DSL")
    inner class TestGenerationSettingsDslTest {

        @Test
        @DisplayName("should configure settings via lambda")
        fun shouldConfigureSettingsViaLambda() {
            extension.testGenerationSettings {
                maxSchemaDepth.set(25)
                includeValidCase.set(true)
            }

            assertThat(extension.testGenerationSettings.maxSchemaDepth.get()).isEqualTo(25)
            assertThat(extension.testGenerationSettings.includeValidCase.get()).isTrue()
        }

        @Test
        @DisplayName("should configure ignoreSchemaValidationRules")
        fun shouldConfigureIgnoreRules() {
            extension.testGenerationSettings {
                ignoreSchemaValidationRules.addAll(listOf("MinLengthRule", "MaxLengthRule"))
            }

            assertThat(extension.testGenerationSettings.ignoreSchemaValidationRules.get())
                .containsExactlyInAnyOrder("MinLengthRule", "MaxLengthRule")
        }

        @Test
        @DisplayName("should configure validSecurityValues")
        fun shouldConfigureSecurityValues() {
            extension.testGenerationSettings {
                validSecurityValues.put("X-API-Key", "test-key")
            }

            assertThat(extension.testGenerationSettings.validSecurityValues.get())
                .containsEntry("X-API-Key", "test-key")
        }

        @Test
        @DisplayName("should configure ignoreTestCases with nested map")
        fun shouldConfigureIgnoreTestCases() {
            extension.testGenerationSettings {
                ignoreTestCases.put("/users", mapOf("GET" to listOf("Test*")))
            }

            @Suppress("UNCHECKED_CAST")
            val ignoreMap = extension.testGenerationSettings.ignoreTestCases.get()["/users"] as Map<String, List<String>>
            assertThat(ignoreMap["GET"]).containsExactly("Test*")
        }
    }

    @Nested
    @DisplayName("parserSettings DSL")
    inner class ParserSettingsDslTest {

        @Test
        @DisplayName("should configure parser settings via lambda")
        fun shouldConfigureParserSettingsViaLambda() {
            extension.parserSettings {
                yamlCodePointLimit.set(10_000_000)
                yamlNestingDepthLimit.set(100)
            }

            assertThat(extension.parserSettings.buildParserSettingsMap())
                .containsEntry("yamlCodePointLimit", 10_000_000)
                .containsEntry("yamlNestingDepthLimit", 100)
        }

        @Test
        @DisplayName("should configure parser settings via action")
        fun shouldConfigureParserSettingsViaAction() {
            extension.parserSettings(
                org.gradle.api.Action {
                    it.yamlMaxAliasesForCollections.set(75)
                    it.yamlAllowRecursiveKeys.set(false)
                },
            )

            assertThat(extension.parserSettings.buildParserSettingsMap())
                .containsEntry("yamlMaxAliasesForCollections", 75)
                .containsEntry("yamlAllowRecursiveKeys", false)
        }
    }
}
