package art.galushko.openapi.testgen.config

import art.galushko.openapi.testgen.example.config.ConfigurationException
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Epic("Configuration")
@Feature("Parser Settings")
@DisplayName("ParserSettings")
class ParserSettingsTest {

    @Nested
    @Story("Validation")
    @DisplayName("Input Validation")
    inner class Validation {

        @Test
        @DisplayName("should accept valid default settings")
        @Description("Default settings should pass validation")
        fun shouldAcceptDefaults() {
            assertThatCode { ParserSettings() }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("should accept all null values")
        @Description("All null values preserves SnakeYAML defaults")
        fun shouldAcceptAllNullValues() {
            val settings = ParserSettings(
                yamlCodePointLimit = null,
                yamlMaxAliasesForCollections = null,
                yamlAllowRecursiveKeys = null,
                yamlNestingDepthLimit = null,
            )
            assertThat(settings.yamlCodePointLimit).isNull()
            assertThat(settings.yamlMaxAliasesForCollections).isNull()
            assertThat(settings.yamlAllowRecursiveKeys).isNull()
            assertThat(settings.yamlNestingDepthLimit).isNull()
        }

        @Test
        @DisplayName("should accept positive yamlCodePointLimit")
        fun shouldAcceptPositiveYamlCodePointLimit() {
            assertThatCode { ParserSettings(yamlCodePointLimit = 10_000_000) }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("should reject non-positive yamlCodePointLimit")
        fun shouldRejectInvalidYamlCodePointLimit() {
            assertThatThrownBy { ParserSettings(yamlCodePointLimit = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("yamlCodePointLimit must be positive or null, was 0")

            assertThatThrownBy { ParserSettings(yamlCodePointLimit = -1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("yamlCodePointLimit must be positive or null, was -1")
        }

        @Test
        @DisplayName("should accept positive yamlMaxAliasesForCollections")
        fun shouldAcceptPositiveYamlMaxAliasesForCollections() {
            assertThatCode { ParserSettings(yamlMaxAliasesForCollections = 100) }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("should reject non-positive yamlMaxAliasesForCollections")
        fun shouldRejectInvalidYamlMaxAliasesForCollections() {
            assertThatThrownBy { ParserSettings(yamlMaxAliasesForCollections = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("yamlMaxAliasesForCollections must be positive or null, was 0")

            assertThatThrownBy { ParserSettings(yamlMaxAliasesForCollections = -1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("yamlMaxAliasesForCollections must be positive or null, was -1")
        }

        @Test
        @DisplayName("should accept positive yamlNestingDepthLimit")
        fun shouldAcceptPositiveYamlNestingDepthLimit() {
            assertThatCode { ParserSettings(yamlNestingDepthLimit = 100) }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("should reject non-positive yamlNestingDepthLimit")
        fun shouldRejectInvalidYamlNestingDepthLimit() {
            assertThatThrownBy { ParserSettings(yamlNestingDepthLimit = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("yamlNestingDepthLimit must be positive or null, was 0")

            assertThatThrownBy { ParserSettings(yamlNestingDepthLimit = -1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("yamlNestingDepthLimit must be positive or null, was -1")
        }

        @Test
        @DisplayName("should accept boolean values for yamlAllowRecursiveKeys")
        fun shouldAcceptBooleanYamlAllowRecursiveKeys() {
            assertThatCode { ParserSettings(yamlAllowRecursiveKeys = true) }.doesNotThrowAnyException()
            assertThatCode { ParserSettings(yamlAllowRecursiveKeys = false) }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("should accept all valid settings together")
        fun shouldAcceptAllValidSettings() {
            val settings = ParserSettings(
                yamlCodePointLimit = 10_000_000,
                yamlMaxAliasesForCollections = 100,
                yamlAllowRecursiveKeys = true,
                yamlNestingDepthLimit = 100,
            )

            assertThat(settings.yamlCodePointLimit).isEqualTo(10_000_000)
            assertThat(settings.yamlMaxAliasesForCollections).isEqualTo(100)
            assertThat(settings.yamlAllowRecursiveKeys).isTrue()
            assertThat(settings.yamlNestingDepthLimit).isEqualTo(100)
        }
    }

    @Nested
    @Story("Map Parsing")
    @DisplayName("fromMap Parsing")
    inner class MapParsing {

        @Test
        @DisplayName("should parse empty map as defaults")
        fun shouldParseEmptyMap() {
            val settings = ParserSettings.fromMap(emptyMap())
            val defaults = ParserSettings()

            assertThat(settings).usingRecursiveComparison().isEqualTo(defaults)
        }

        @Test
        @DisplayName("should parse all supported fields")
        fun shouldParseAllFields() {
            val map = mapOf(
                "yamlCodePointLimit" to 10_000_000,
                "yamlMaxAliasesForCollections" to 100,
                "yamlAllowRecursiveKeys" to true,
                "yamlNestingDepthLimit" to 100,
            )

            val settings = ParserSettings.fromMap(map)

            assertThat(settings.yamlCodePointLimit).isEqualTo(10_000_000)
            assertThat(settings.yamlMaxAliasesForCollections).isEqualTo(100)
            assertThat(settings.yamlAllowRecursiveKeys).isTrue()
            assertThat(settings.yamlNestingDepthLimit).isEqualTo(100)
        }

        @Test
        @DisplayName("should parse integer from string")
        fun shouldParseIntegerFromString() {
            val map = mapOf(
                "yamlCodePointLimit" to "10000000",
                "yamlNestingDepthLimit" to "100",
            )

            val settings = ParserSettings.fromMap(map)

            assertThat(settings.yamlCodePointLimit).isEqualTo(10_000_000)
            assertThat(settings.yamlNestingDepthLimit).isEqualTo(100)
        }

        @Test
        @DisplayName("should parse boolean from string")
        fun shouldParseBooleanFromString() {
            val map = mapOf(
                "yamlAllowRecursiveKeys" to "true",
            )

            val settings = ParserSettings.fromMap(map)

            assertThat(settings.yamlAllowRecursiveKeys).isTrue()
        }

        @Test
        @DisplayName("should handle null values gracefully")
        fun shouldHandleNullValues() {
            val map = mapOf<String, Any?>(
                "yamlCodePointLimit" to null,
                "yamlAllowRecursiveKeys" to null,
            )

            val settings = ParserSettings.fromMap(map)

            assertThat(settings.yamlCodePointLimit).isNull()
            assertThat(settings.yamlAllowRecursiveKeys).isNull()
        }

        @Test
        @DisplayName("should throw ConfigurationException for invalid integer type")
        fun shouldThrowForInvalidIntType() {
            val map = mapOf("yamlCodePointLimit" to true)

            assertThatThrownBy { ParserSettings.fromMap(map) }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'yamlCodePointLimit': expected Number or numeric String, got kotlin.Boolean")
        }

        @Test
        @DisplayName("should throw ConfigurationException for non-numeric string")
        fun shouldThrowForNonNumericString() {
            val map = mapOf("yamlCodePointLimit" to "not-a-number")

            assertThatThrownBy { ParserSettings.fromMap(map) }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'yamlCodePointLimit': expected Integer or numeric string, got non-numeric string: 'not-a-number'")
        }

        @Test
        @DisplayName("should throw ConfigurationException for invalid boolean type")
        fun shouldThrowForInvalidBooleanType() {
            val map = mapOf("yamlAllowRecursiveKeys" to 123)

            assertThatThrownBy { ParserSettings.fromMap(map) }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'yamlAllowRecursiveKeys': expected Boolean or boolean String, got kotlin.Int")
        }

        @Test
        @DisplayName("should throw ConfigurationException for invalid boolean string")
        fun shouldThrowForInvalidBooleanString() {
            val map = mapOf("yamlAllowRecursiveKeys" to "not-a-boolean")

            assertThatThrownBy { ParserSettings.fromMap(map) }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'yamlAllowRecursiveKeys': expected Boolean or boolean String, got non-boolean string: 'not-a-boolean'")
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for non-positive integer from map")
        fun shouldThrowForNonPositiveIntegerFromMap() {
            val map = mapOf("yamlCodePointLimit" to 0)

            assertThatThrownBy { ParserSettings.fromMap(map) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("yamlCodePointLimit must be positive or null, was 0")
        }

        @Test
        @DisplayName("should ignore unknown keys")
        fun shouldIgnoreUnknownKeys() {
            val map = mapOf(
                "unknownKey" to "someValue",
                "yamlCodePointLimit" to 5_000_000,
            )

            val settings = ParserSettings.fromMap(map)

            assertThat(settings.yamlCodePointLimit).isEqualTo(5_000_000)
        }
    }

    @Nested
    @Story("Custom Defaults")
    @DisplayName("fromMap with Custom Defaults")
    inner class CustomDefaults {

        @Test
        @DisplayName("should use custom defaults when map is empty")
        fun shouldUseCustomDefaultsWhenEmpty() {
            val customDefaults = ParserSettings(
                yamlCodePointLimit = 5_000_000,
                yamlMaxAliasesForCollections = 75,
                yamlAllowRecursiveKeys = true,
                yamlNestingDepthLimit = 75,
            )

            val settings = ParserSettings.fromMap(emptyMap(), customDefaults)

            assertThat(settings).usingRecursiveComparison().isEqualTo(customDefaults)
        }

        @Test
        @DisplayName("should use map values over custom defaults")
        fun shouldUseMapOverCustomDefaults() {
            val customDefaults = ParserSettings(
                yamlCodePointLimit = 5_000_000,
                yamlMaxAliasesForCollections = 75,
            )

            val map = mapOf(
                "yamlCodePointLimit" to 10_000_000,
            )
            val settings = ParserSettings.fromMap(map, customDefaults)

            assertThat(settings.yamlCodePointLimit).isEqualTo(10_000_000) // From map
            assertThat(settings.yamlMaxAliasesForCollections).isEqualTo(75) // From defaults
        }

        @Test
        @DisplayName("should preserve default boolean values when not in map")
        fun shouldPreserveDefaultBooleanValues() {
            val customDefaults = ParserSettings(
                yamlAllowRecursiveKeys = true,
            )

            val settings = ParserSettings.fromMap(emptyMap(), customDefaults)

            assertThat(settings.yamlAllowRecursiveKeys).isTrue()
        }
    }
}
