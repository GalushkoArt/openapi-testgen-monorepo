package art.galushko.openapi.testgen.config

import art.galushko.openapi.testgen.example.config.ConfigurationException
import art.galushko.openapi.testgen.example.config.EmailProviderSettings
import art.galushko.openapi.testgen.example.config.ExampleValueSettings
import art.galushko.openapi.testgen.model.error.ErrorMode
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Epic("Configuration")
@Feature("Settings Validation")
@DisplayName("TestGenerationSettings")
class TestGenerationSettingsTest {

    @Nested
    @Story("Validation")
    @DisplayName("Input Validation")
    inner class Validation {

        @Test
        @DisplayName("should accept valid default settings")
        @Description("Default settings should pass validation")
        fun shouldAcceptDefaults() {
            assertThatCode { TestGenerationSettings() }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("should reject non-positive maxSchemaDepth")
        fun shouldRejectInvalidMaxSchemaDepth() {
            assertThatThrownBy { TestGenerationSettings(maxSchemaDepth = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("maxSchemaDepth must be positive, was 0")

            assertThatThrownBy { TestGenerationSettings(maxSchemaDepth = -1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("maxSchemaDepth must be positive, was -1")
        }

        @Test
        @DisplayName("should reject non-positive maxSchemaCombinations")
        fun shouldRejectInvalidMaxSchemaCombinations() {
            assertThatThrownBy { TestGenerationSettings(maxSchemaCombinations = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("maxSchemaCombinations must be positive, was 0")
        }

        @Test
        @DisplayName("should reject non-positive maxTestCasesPerOperation")
        fun shouldRejectInvalidMaxTestCasesPerOperation() {
            assertThatThrownBy { TestGenerationSettings(maxTestCasesPerOperation = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("maxTestCasesPerOperation must be positive, was 0")
        }

        @Test
        @DisplayName("should reject non-positive maxErrors")
        fun shouldRejectInvalidMaxErrors() {
            assertThatThrownBy { TestGenerationSettings(maxErrors = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("maxErrors must be positive, was 0")
        }

        @Test
        @DisplayName("should reject non-positive maxMergedSchemaDepth")
        fun shouldRejectInvalidMaxMergedSchemaDepth() {
            assertThatThrownBy { TestGenerationSettings(maxMergedSchemaDepth = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("maxMergedSchemaDepth must be positive, was 0")

            assertThatThrownBy { TestGenerationSettings(maxMergedSchemaDepth = -1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("maxMergedSchemaDepth must be positive, was -1")
        }

        @Test
        @DisplayName("should validate ignore configuration")
        fun shouldValidateIgnoreConfig() {
            // Invalid ignore config
            val invalidConfig = mapOf("/path" to 123)

            assertThatThrownBy { TestGenerationSettings(ignoreTestCases = invalidConfig) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Value for path '/path' must be \"*\" or Map<String, ...>, was kotlin.Int")
        }
    }

    @Nested
    @Story("Map Parsing")
    @DisplayName("fromMap Parsing")
    inner class MapParsing {

        @Test
        @DisplayName("should parse empty map as defaults")
        fun shouldParseEmptyMap() {
            val settings = TestGenerationSettings.fromMap(emptyMap())
            val defaults = TestGenerationSettings()

            assertThat(settings).usingRecursiveComparison().isEqualTo(defaults)
        }

        @Test
        @DisplayName("should parse includeValidCase as true")
        fun shouldParseIncludeValidCaseTrue() {
            val map = mapOf("includeValidCase" to true)

            val settings = TestGenerationSettings.fromMap(map)

            val expected = TestGenerationSettings(includeValidCase = true)
            assertThat(settings).usingRecursiveComparison().isEqualTo(expected)
        }

        @Test
        @DisplayName("should parse includeValidCase from string 'true'")
        fun shouldParseIncludeValidCaseFromString() {
            val map = mapOf("includeValidCase" to "true")

            val settings = TestGenerationSettings.fromMap(map)

            val expected = TestGenerationSettings(includeValidCase = true)
            assertThat(settings).usingRecursiveComparison().isEqualTo(expected)
        }

        @Test
        @DisplayName("should default includeValidCase to false")
        fun shouldDefaultIncludeValidCaseToFalse() {
            val settings = TestGenerationSettings.fromMap(emptyMap())

            val expected = TestGenerationSettings()
            assertThat(settings).usingRecursiveComparison().isEqualTo(expected)
            assertThat(settings.includeValidCase).isFalse()
        }

        @Test
        @DisplayName("should parse all supported fields")
        fun shouldParseAllFields() {
            val map = mapOf(
                "ignoreTestCases" to mapOf("/api" to "*"),
                "ignoreSchemaValidationRules" to listOf("Rule1"),
                "ignoreAuthValidationRules" to listOf("AuthRule1"),
                "maxSchemaDepth" to 10,
                "overrideBasicTestData" to mapOf("string" to "test"),
                "maxSchemaCombinations" to 5,
                "maxTestCasesPerOperation" to 20,
                "validSecurityValues" to mapOf("api_key" to "123"),
                "errorMode" to "FAIL_FAST",
                "includeValidCase" to true,
                "maxErrors" to 50,
            )

            val settings = TestGenerationSettings.fromMap(map)

            assertThat(settings.ignoreTestCases).isEqualTo(mapOf("/api" to "*"))
            assertThat(settings.ignoreSchemaValidationRules).containsExactly("Rule1")
            assertThat(settings.ignoreAuthValidationRules).containsExactly("AuthRule1")
            assertThat(settings.maxSchemaDepth).isEqualTo(10)
            assertThat(settings.overrideBasicTestData).isEqualTo(mapOf("string" to "test"))
            assertThat(settings.maxSchemaCombinations).isEqualTo(5)
            assertThat(settings.maxTestCasesPerOperation).isEqualTo(20)
            assertThat(settings.validSecurityValues).isEqualTo(mapOf("api_key" to "123"))
            assertThat(settings.errorMode).isEqualTo(ErrorMode.FAIL_FAST)
            assertThat(settings.includeValidCase).isTrue()
            assertThat(settings.maxErrors).isEqualTo(50)
        }

        @Test
        @DisplayName("should parse nested exampleValues settings")
        fun shouldParseNestedExampleValuesSettings() {
            val map = mapOf(
                "exampleValues" to mapOf(
                    "providers" to listOf("enum", "const", "pattern"),
                    "maxExampleDepth" to 30,
                    "email" to mapOf("template" to "user%s@mycompany.com"),
                    "date" to mapOf("startDate" to "2025-01-01"),
                    "dateTime" to mapOf(
                        "startDate" to "2025-01-01",
                        "timeSuffixTemplate" to "%sT00:00:00Z",
                    ),
                    "plainString" to mapOf("validChars" to "abc123"),
                )
            )

            val settings = TestGenerationSettings.fromMap(map)

            assertThat(settings.exampleValues.providers).containsExactly("enum", "const", "pattern")
            assertThat(settings.exampleValues.maxExampleDepth).isEqualTo(30)
            assertThat(settings.exampleValues.email.template).isEqualTo("user%s@mycompany.com")
            assertThat(settings.exampleValues.date.startDate).isEqualTo("2025-01-01")
            assertThat(settings.exampleValues.dateTime.startDate).isEqualTo("2025-01-01")
            assertThat(settings.exampleValues.dateTime.timeSuffixTemplate).isEqualTo("%sT00:00:00Z")
            assertThat(settings.exampleValues.plainString.validChars).isEqualTo("abc123")
        }

        @Test
        @DisplayName("should throw ConfigurationException for invalid exampleValues type")
        fun shouldThrowForInvalidExampleValuesType() {
            val map = mapOf("exampleValues" to "not-a-map")

            assertThatThrownBy { TestGenerationSettings.fromMap(map) }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'exampleValues': expected Map<String, Any?>, got kotlin.String")
        }

        @Test
        @DisplayName("should ignore patternGeneration (feature module config) in core settings")
        fun shouldIgnorePatternGenerationInCoreSettings() {
            val map = mapOf(
                "patternGeneration" to mapOf(
                    "defaultMinLength" to 10,
                    "spaceChars" to " \t",
                    "anyPrintableChars" to "abc",
                )
            )

            val settings = TestGenerationSettings.fromMap(map)

            assertThat(settings).usingRecursiveComparison().isEqualTo(TestGenerationSettings())
        }

        @Test
        @DisplayName("should ignore invalid patternGeneration type (feature module config) in core settings")
        fun shouldIgnoreInvalidPatternGenerationTypeInCoreSettings() {
            val map = mapOf("patternGeneration" to "not-a-map")

            val settings = TestGenerationSettings.fromMap(map)

            assertThat(settings).usingRecursiveComparison().isEqualTo(TestGenerationSettings())
        }

        @Test
        @DisplayName("should handle string-to-int type conversion")
        fun shouldHandleStringToIntConversion() {
            val map = mapOf(
                "maxSchemaDepth" to "15",
                "maxErrors" to "100"
            )

            val settings = TestGenerationSettings.fromMap(map)

            assertThat(settings.maxSchemaDepth).isEqualTo(15)
            assertThat(settings.maxErrors).isEqualTo(100)
        }

        @Test
        @DisplayName("should handle enum case insensitivity and normalization")
        fun shouldHandleEnumNormalization() {
            val map = mapOf(
                "errorMode" to "fail-fast"
            )

            val settings = TestGenerationSettings.fromMap(map)

            assertThat(settings.errorMode).isEqualTo(ErrorMode.FAIL_FAST)
        }

        @Test
        @DisplayName("should ignore unknown keys")
        fun shouldIgnoreUnknownKeys() {
            val map = mapOf(
                "unknownKey" to "someValue",
                "maxSchemaDepth" to 5
            )

            val settings = TestGenerationSettings.fromMap(map)

            assertThat(settings.maxSchemaDepth).isEqualTo(5)
        }

        @Test
        @DisplayName("should throw ConfigurationException for invalid integer type")
        fun shouldThrowForInvalidIntType() {
            val map = mapOf("maxSchemaDepth" to true) // boolean is not number

            assertThatThrownBy { TestGenerationSettings.fromMap(map) }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'maxSchemaDepth': expected Number or numeric String, got kotlin.Boolean")
        }

        @Test
        @DisplayName("should throw ConfigurationException for invalid collection type")
        fun shouldThrowForInvalidCollectionType() {
            val map = mapOf("ignoreSchemaValidationRules" to "not-a-list")

            assertThatThrownBy { TestGenerationSettings.fromMap(map) }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'ignoreSchemaValidationRules': expected Collection<String>, got kotlin.String")
        }

        @Test
        @DisplayName("should throw ConfigurationException for invalid map type")
        fun shouldThrowForInvalidMapType() {
            val map = mapOf("ignoreTestCases" to listOf("not-a-map"))

            assertThatThrownBy { TestGenerationSettings.fromMap(map) }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'ignoreTestCases': expected Map<String, Any>, got java.util.Collections.SingletonList")
        }

        @Test
        @DisplayName("should throw ConfigurationException for non-numeric string")
        fun shouldThrowForNonNumericString() {
            val map = mapOf("maxSchemaDepth" to "not-a-number")

            assertThatThrownBy { TestGenerationSettings.fromMap(map) }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'maxSchemaDepth': expected Integer or numeric string, got non-numeric string: 'not-a-number'")
        }

        @Test
        @DisplayName("should throw ConfigurationException for invalid enum value")
        fun shouldThrowForInvalidEnumValue() {
            val map = mapOf("errorMode" to "INVALID_MODE")

            assertThatThrownBy { TestGenerationSettings.fromMap(map) }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'errorMode': expected one of [FAIL_FAST, COLLECT_ALL], got 'INVALID_MODE'")
        }

        @Test
        @DisplayName("should handle null values gracefully")
        fun shouldHandleNullValues() {
            val map = mapOf<String, Any?>("maxSchemaDepth" to null)

            // null is treated as absent, uses default (DEFAULT_MAX_SCHEMA_DEPTH = 50)
            val settings = TestGenerationSettings.fromMap(map)

            assertThat(settings.maxSchemaDepth).isEqualTo(50)
        }

        @Test
        @DisplayName("should parse maxMergedSchemaDepth field")
        fun shouldParseMaxMergedSchemaDepth() {
            val map = mapOf("maxMergedSchemaDepth" to 100)

            val settings = TestGenerationSettings.fromMap(map)

            assertThat(settings.maxMergedSchemaDepth).isEqualTo(100)
        }
    }

    @Nested
    @Story("Custom Defaults")
    @DisplayName("fromMap with Custom Defaults")
    inner class CustomDefaults {

        @Test
        @DisplayName("should use custom defaults when map is empty")
        fun shouldUseCustomDefaultsWhenEmpty() {
            val customDefaults = TestGenerationSettings(
                maxSchemaDepth = 99,
                maxSchemaCombinations = 77,
                maxMergedSchemaDepth = 33,
            )

            val settings = TestGenerationSettings.fromMap(emptyMap(), customDefaults)

            assertThat(settings.maxSchemaDepth).isEqualTo(99)
            assertThat(settings.maxSchemaCombinations).isEqualTo(77)
            assertThat(settings.maxMergedSchemaDepth).isEqualTo(33)
        }

        @Test
        @DisplayName("should use custom default for includeValidCase")
        fun shouldUseCustomDefaultForIncludeValidCase() {
            val customDefaults = TestGenerationSettings(includeValidCase = true)

            val settings = TestGenerationSettings.fromMap(emptyMap(), customDefaults)

            assertThat(settings).usingRecursiveComparison().isEqualTo(customDefaults)
        }

        @Test
        @DisplayName("should use map values over custom defaults")
        fun shouldUseMapOverCustomDefaults() {
            val customDefaults = TestGenerationSettings(
                maxSchemaDepth = 99,
                maxSchemaCombinations = 77,
            )

            val map = mapOf("maxSchemaDepth" to 25)
            val settings = TestGenerationSettings.fromMap(map, customDefaults)

            assertThat(settings.maxSchemaDepth).isEqualTo(25) // From map
            assertThat(settings.maxSchemaCombinations).isEqualTo(77) // From defaults
        }

        @Test
        @DisplayName("should propagate custom defaults through nested exampleValues")
        fun shouldPropagateCustomDefaultsThroughNested() {
            val customDefaults = TestGenerationSettings(
                exampleValues = ExampleValueSettings(
                    providers = listOf("custom1", "custom2"),
                    email = EmailProviderSettings(template = "custom%s@example.org"),
                    maxExampleDepth = 100,
                ),
            )

            val map = mapOf(
                "exampleValues" to mapOf(
                    "maxExampleDepth" to 50, // Only override this
                ),
            )

            val settings = TestGenerationSettings.fromMap(map, customDefaults)

            assertThat(settings.exampleValues.maxExampleDepth).isEqualTo(50) // From map
            assertThat(settings.exampleValues.providers).containsExactly("custom1", "custom2") // From defaults
            assertThat(settings.exampleValues.email.template).isEqualTo("custom%s@example.org") // From defaults
        }

        @Test
        @DisplayName("should preserve default validSecurityValues when not in map")
        fun shouldPreserveDefaultValidSecurityValues() {
            val customDefaults = TestGenerationSettings(
                validSecurityValues = mapOf("api_key" to "secret123"),
            )

            val settings = TestGenerationSettings.fromMap(emptyMap(), customDefaults)

            assertThat(settings.validSecurityValues).isEqualTo(mapOf("api_key" to "secret123"))
        }

        @Test
        @DisplayName("should preserve default ignoreTestCases when not in map")
        fun shouldPreserveDefaultIgnoreTestCases() {
            val customDefaults = TestGenerationSettings(
                ignoreTestCases = mapOf("/api/v1" to "*"),
            )

            val settings = TestGenerationSettings.fromMap(emptyMap(), customDefaults)

            assertThat(settings.ignoreTestCases).isEqualTo(mapOf("/api/v1" to "*"))
        }
    }
}

