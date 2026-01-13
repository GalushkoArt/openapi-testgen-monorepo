package art.galushko.openapi.testgen.pattern.value

import art.galushko.openapi.testgen.example.config.ConfigurationException
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Epic("Test Data Generation")
@Feature("Pattern Generation Options")
class PatternGenerationOptionsTest {

    @Test
    @DisplayName("should reject negative defaultMinLength")
    fun shouldRejectNegativeDefaultMinLength() {
        assertThatThrownBy {
            PatternGenerationOptions(defaultMinLength = -1)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("defaultMinLength must be non-negative, was -1")
    }

    @Test
    @DisplayName("should reject empty spaceChars")
    fun shouldRejectEmptySpaceChars() {
        assertThatThrownBy {
            PatternGenerationOptions(spaceChars = "")
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("spaceChars must not be empty")
    }

    @Test
    @DisplayName("should reject empty anyPrintableChars when specified")
    fun shouldRejectEmptyAnyPrintableCharsWhenSpecified() {
        assertThatThrownBy {
            PatternGenerationOptions(anyPrintableChars = "")
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("anyPrintableChars must not be empty when specified")
    }

    @Test
    @DisplayName("should allow null anyPrintableChars")
    fun shouldAllowNullAnyPrintableChars() {
        val options = PatternGenerationOptions(anyPrintableChars = null)

        assertThat(options.anyPrintableChars)
            .`as`("null anyPrintableChars should be allowed")
            .isNull()
    }

    @Test
    @DisplayName("should allow zero defaultMinLength")
    fun shouldAllowZeroDefaultMinLength() {
        val options = PatternGenerationOptions(defaultMinLength = 0)

        assertThat(options.defaultMinLength)
            .`as`("Zero defaultMinLength should be allowed")
            .isEqualTo(0)
    }

    @Nested
    @DisplayName("fromMap")
    inner class FromMap {

        @Test
        @DisplayName("should parse empty map as defaults")
        fun shouldParseEmptyMap() {
            val options = PatternGenerationOptions.fromMap(emptyMap())

            assertThat(options).isEqualTo(PatternGenerationOptions())
        }

        @Test
        @DisplayName("should parse supported fields and ignore unknown keys")
        fun shouldParseSupportedFieldsAndIgnoreUnknownKeys() {
            val options = PatternGenerationOptions.fromMap(
                mapOf(
                    "defaultMinLength" to 10,
                    "spaceChars" to " \t",
                    "anyPrintableChars" to "abc",
                    "unusedKey" to "ignored",
                )
            )

            assertThat(options).isEqualTo(
                PatternGenerationOptions(
                    defaultMinLength = 10,
                    spaceChars = " \t",
                    anyPrintableChars = "abc",
                )
            )
        }

        @Test
        @DisplayName("should parse numeric defaultMinLength from string")
        fun shouldParseNumericDefaultMinLengthFromString() {
            val options = PatternGenerationOptions.fromMap(
                mapOf(
                    "defaultMinLength" to "15",
                )
            )

            assertThat(options.defaultMinLength).isEqualTo(15)
        }

        @Test
        @DisplayName("should allow explicit null anyPrintableChars")
        fun shouldAllowExplicitNullAnyPrintableChars() {
            val options = PatternGenerationOptions.fromMap(
                mapOf(
                    "anyPrintableChars" to null,
                )
            )

            assertThat(options.anyPrintableChars).isNull()
        }

        @Test
        @DisplayName("should throw ConfigurationException for non-numeric defaultMinLength string")
        fun shouldThrowForNonNumericDefaultMinLengthString() {
            assertThatThrownBy {
                PatternGenerationOptions.fromMap(
                    mapOf(
                        "defaultMinLength" to "not-a-number",
                    )
                )
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage(
                    "Configuration error for 'patternGeneration.defaultMinLength': expected " +
                        "Integer or numeric string, got non-numeric string: 'not-a-number'"
                )
        }

        @Test
        @DisplayName("should throw ConfigurationException for spaceChars type mismatch")
        fun shouldThrowForSpaceCharsTypeMismatch() {
            assertThatThrownBy {
                PatternGenerationOptions.fromMap(
                    mapOf(
                        "spaceChars" to 123,
                    )
                )
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage(
                    "Configuration error for 'patternGeneration.spaceChars': expected String, got kotlin.Int"
                )
        }

        @Test
        @DisplayName("should throw ConfigurationException for anyPrintableChars type mismatch")
        fun shouldThrowForAnyPrintableCharsTypeMismatch() {
            assertThatThrownBy {
                PatternGenerationOptions.fromMap(
                    mapOf(
                        "anyPrintableChars" to 1,
                    )
                )
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage(
                    "Configuration error for 'patternGeneration.anyPrintableChars': expected String or null, got kotlin.Int"
                )
        }

        @Test
        @DisplayName("should throw ConfigurationException when defaultMinLength Long is outside Int range")
        fun shouldThrowWhenDefaultMinLengthLongIsOutsideIntRange() {
            assertThatThrownBy {
                PatternGenerationOptions.fromMap(
                    mapOf(
                        "defaultMinLength" to Long.MAX_VALUE,
                    )
                )
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage(
                    "Configuration error for 'patternGeneration.defaultMinLength': expected " +
                        "Int (32-bit integer), got Long out of Int range: 9223372036854775807"
                )
        }
    }
}


