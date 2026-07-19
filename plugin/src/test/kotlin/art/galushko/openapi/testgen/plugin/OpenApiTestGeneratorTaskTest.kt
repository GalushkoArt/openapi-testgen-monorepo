package art.galushko.openapi.testgen.plugin

import art.galushko.openapi.testgen.model.error.ErrorMode
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OpenApiTestGeneratorTask")
class OpenApiTestGeneratorTaskTest {

    private lateinit var project: Project
    private lateinit var task: OpenApiTestGeneratorTask

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
        task = project.tasks.register("testTask", OpenApiTestGeneratorTask::class.java).get()
    }

    @Test
    @DisplayName("Task should have default values for all properties except testGenerationSettings")
    fun testDefaultValues() {
        assertThat(task.configFile.orNull).isNull()
        assertThat(task.specFile.orNull).isNull()
        assertThat(task.outputDir.orNull).isNull()
        assertThat(task.generator.orNull).isNull()
        assertThat(task.generatorOptions.get()).isEmpty()
        assertThat(task.alwaysWriteTests.orNull).isNull()
        assertThat(task.logLevel.orNull).isNull()
        assertThat(task.testGenerationSettings).isNotNull()
        assertThat(task.parserSettings).isNotNull()
        assertThat(task.parserSettings.buildParserSettingsMap()).isEmpty()
    }

    @Nested
    @DisplayName("property setting")
    inner class PropertySettingTest {

        @Test
        @DisplayName("should set configFile")
        fun shouldSetConfigFile() {
            task.configFile.set(project.layout.projectDirectory.file("config.yaml"))

            assertThat(task.configFile.get().asFile.name).isEqualTo("config.yaml")
        }

        @Test
        @DisplayName("should set specFile")
        fun shouldSetSpecFile() {
            task.specFile.set("openapi.yaml")

            assertThat(task.specFile.get()).isEqualTo("openapi.yaml")
        }

        @Test
        @DisplayName("should set outputDir")
        fun shouldSetOutputDir() {
            val dir = project.layout.projectDirectory.dir("generated")
            task.outputDir.set(dir)

            assertThat(task.outputDir.get().asFile.name).isEqualTo("generated")
        }

        @Test
        @DisplayName("should set generator")
        fun shouldSetGenerator() {
            task.generator.set("template")

            assertThat(task.generator.get()).isEqualTo("template")
        }

        @Test
        @DisplayName("should set generatorOptions")
        fun shouldSetGeneratorOptions() {
            task.generatorOptions.put("templateSet", "restassured-java")
            task.generatorOptions.put("package", "com.example")

            assertThat(task.generatorOptions.get())
                .containsEntry("templateSet", "restassured-java")
                .containsEntry("package", "com.example")
        }

        @Test
        @DisplayName("should set alwaysWriteTests")
        fun shouldSetAlwaysWriteTests() {
            task.alwaysWriteTests.set(true)

            assertThat(task.alwaysWriteTests.get()).isTrue()
        }

        @Test
        @DisplayName("should set logLevel")
        fun shouldSetLogLevel() {
            task.logLevel.set("DEBUG")

            assertThat(task.logLevel.get()).isEqualTo("DEBUG")
        }

        @Test
        @DisplayName("should set parser settings")
        fun shouldSetParserSettings() {
            task.parserSettings.yamlCodePointLimit.set(7_000_000)
            task.parserSettings.yamlAllowRecursiveKeys.set(true)

            assertThat(task.parserSettings.yamlCodePointLimit.get()).isEqualTo(7_000_000)
            assertThat(task.parserSettings.yamlAllowRecursiveKeys.get()).isTrue()
            assertThat(task.parserSettings.buildParserSettingsMap())
                .containsEntry("yamlCodePointLimit", 7_000_000)
                .containsEntry("yamlAllowRecursiveKeys", true)
        }
    }

    @Nested
    @DisplayName("testGenerationSettings DSL")
    inner class TestGenerationSettingsDslTest {

        @Test
        @DisplayName("should configure settings via lambda")
        fun shouldConfigureSettingsViaLambda() {
            task.testGenerationSettings {
                maxSchemaDepth.set(25)
                includeValidCase.set(true)
            }

            assertThat(task.testGenerationSettings.maxSchemaDepth.get()).isEqualTo(25)
            assertThat(task.testGenerationSettings.includeValidCase.get()).isTrue()
        }

        @Test
        @DisplayName("should configure errorMode")
        fun shouldConfigureErrorMode() {
            task.testGenerationSettings {
                errorMode.set(ErrorMode.FAIL_FAST)
            }

            assertThat(task.testGenerationSettings.errorMode.get()).isEqualTo(ErrorMode.FAIL_FAST)
        }

        @Test
        @DisplayName("should configure ignoreSchemaValidationRules")
        fun shouldConfigureIgnoreRules() {
            task.testGenerationSettings {
                ignoreSchemaValidationRules.addAll(listOf("Rule1", "Rule2"))
            }

            assertThat(task.testGenerationSettings.ignoreSchemaValidationRules.get())
                .containsExactlyInAnyOrder("Rule1", "Rule2")
        }

        @Test
        @DisplayName("should configure ignoreAuthValidationRules")
        fun shouldConfigureIgnoreAuthRules() {
            task.testGenerationSettings {
                ignoreAuthValidationRules.addAll(listOf("AuthRule1", "AuthRule2"))
            }

            assertThat(task.testGenerationSettings.ignoreAuthValidationRules.get())
                .containsExactlyInAnyOrder("AuthRule1", "AuthRule2")
        }

        @Test
        @DisplayName("should configure validSecurityValues")
        fun shouldConfigureSecurityValues() {
            task.testGenerationSettings {
                validSecurityValues.put("X-API-Key", "test-key")
                validSecurityValues.put("Authorization", "Bearer token")
            }

            assertThat(task.testGenerationSettings.validSecurityValues.get())
                .containsEntry("X-API-Key", "test-key")
                .containsEntry("Authorization", "Bearer token")
        }

        @Test
        @DisplayName("should configure ignoreTestCases")
        fun shouldConfigureIgnoreTestCases() {
            task.testGenerationSettings {
                ignoreTestCases.put("/users", mapOf("GET" to listOf("*")))
                ignoreTestCases.put("/posts", "*")
            }

            val ignoreTestCases = task.testGenerationSettings.ignoreTestCases.get()
            assertThat(ignoreTestCases).hasSize(2)
            assertThat(ignoreTestCases).containsKey("/users")
            assertThat(ignoreTestCases).containsKey("/posts")
        }

        @Test
        @DisplayName("should configure includeOperations")
        fun shouldConfigureIncludeOperations() {
            task.testGenerationSettings {
                includeOperations.put("/api/users", listOf("GET", "POST"))
                includeOperations.put("/api/orders", listOf("*"))
            }

            val includeOps = task.testGenerationSettings.includeOperations.get()
            assertThat(includeOps).hasSize(2)
            assertThat(includeOps).containsKey("/api/users")
            assertThat(includeOps).containsKey("/api/orders")
        }

        @Test
        @DisplayName("should configure overrideBasicTestData")
        fun shouldConfigureOverrideBasicTestData() {
            task.testGenerationSettings {
                overrideBasicTestData.put("string", "custom-value")
                overrideBasicTestData.put("integer", "42")
            }

            assertThat(task.testGenerationSettings.overrideBasicTestData.get())
                .containsEntry("string", "custom-value")
                .containsEntry("integer", "42")
        }

        @Test
        @DisplayName("should configure budget limits")
        fun shouldConfigureBudgetLimits() {
            task.testGenerationSettings {
                maxSchemaDepth.set(30)
                maxSchemaCombinations.set(150)
                maxMergedSchemaDepth.set(25)
                maxTestCasesPerOperation.set(500)
                maxErrors.set(100)
            }

            assertThat(task.testGenerationSettings.maxSchemaDepth.get()).isEqualTo(30)
            assertThat(task.testGenerationSettings.maxSchemaCombinations.get()).isEqualTo(150)
            assertThat(task.testGenerationSettings.maxMergedSchemaDepth.get()).isEqualTo(25)
            assertThat(task.testGenerationSettings.maxTestCasesPerOperation.get()).isEqualTo(500)
            assertThat(task.testGenerationSettings.maxErrors.get()).isEqualTo(100)
        }

        @Test
        @DisplayName("should configure exampleValues")
        fun shouldConfigureExampleValues() {
            task.testGenerationSettings {
                exampleValues.putAll(
                    mapOf(
                        "providers" to listOf("enum", "const"),
                        "maxExampleDepth" to 30,
                    )
                )
            }

            val exampleValues = task.testGenerationSettings.exampleValues.get()
            assertThat(exampleValues).containsKey("providers")
            assertThat(exampleValues).containsEntry("maxExampleDepth", 30)
        }

        @Test
        @DisplayName("should configure patternGeneration")
        fun shouldConfigurePatternGeneration() {
            task.testGenerationSettings {
                patternGeneration.put("enabled", true)
                patternGeneration.put("maxLength", 100)
            }

            assertThat(task.testGenerationSettings.patternGeneration.get())
                .containsEntry("enabled", true)
                .containsEntry("maxLength", 100)
        }
    }
}
