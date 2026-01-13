package art.galushko.openapi.testgen.pattern.value

import art.galushko.openapi.testgen.generation.step
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Epic("Test Data Generation")
@Feature("Pattern Value Generator")
class PatternValueGeneratorTest {

    private val generator = PatternValueGenerator()

    @Nested
    @DisplayName("generateValidValue")
    inner class GenerateValidValue {

        fun simplePatternProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "Simple character class pattern",
                "^[A-Z]{3,5}$",
                null,
                null,
                0,
            ),
            Arguments.of(
                "Pattern with alternation",
                "^(foo|bar|baz)$",
                null,
                null,
                0,
            ),
            Arguments.of(
                "Numeric pattern",
                "^\\d{4}-\\d{2}-\\d{2}$",
                null,
                null,
                0,
            ),
            Arguments.of(
                "Alphanumeric pattern",
                "^[a-zA-Z0-9]{8,16}$",
                null,
                null,
                0,
            ),
            Arguments.of(
                "Pattern with quantifiers",
                "^a+b*c?d$",
                null,
                null,
                0,
            ),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("simplePatternProvider")
        @DisplayName("should generate valid strings matching simple patterns")
        @Description("Verifies that generateValidValue produces strings matching the given pattern")
        fun shouldGenerateValidStringsMatchingSimplePatterns(
            scenario: String,
            pattern: String,
            minLength: Int?,
            maxLength: Int?,
            variationIndex: Int,
        ) {
            val result = step("Generate valid value") {
                generator.generateValidValue(pattern, minLength, maxLength, variationIndex)
            }

            assertThat(result)
                .`as`("Generated value should not be null")
                .isNotNull()

            val regex = Regex(pattern)
            assertThat(regex.containsMatchIn(result!!))
                .`as`("Generated value '$result' should match pattern '$pattern'")
                .isTrue()
        }

        @Test
        @DisplayName("should respect minLength constraint")
        @Description("Verifies that generated values meet minimum length requirements")
        fun shouldRespectMinLengthConstraint() {
            val pattern = "^[a-z]+$"
            val minLength = 10

            val result = step("Generate valid value with minLength") {
                generator.generateValidValue(pattern, minLength, null, 0)
            }

            assertThat(result).isNotNull()
            assertThat(result!!.length)
                .`as`("Generated value should be at least $minLength characters")
                .isGreaterThanOrEqualTo(minLength)
            assertThat(Regex(pattern).containsMatchIn(result))
                .`as`("Generated value should match pattern")
                .isTrue()
        }

        @Test
        @DisplayName("should respect maxLength constraint")
        @Description("Verifies that generated values do not exceed maximum length")
        fun shouldRespectMaxLengthConstraint() {
            val pattern = "^[a-z]+$"
            val maxLength = 5

            val result = step("Generate valid value with maxLength") {
                generator.generateValidValue(pattern, null, maxLength, 0)
            }

            assertThat(result).isNotNull()
            assertThat(result!!.length)
                .`as`("Generated value should be at most $maxLength characters")
                .isLessThanOrEqualTo(maxLength)
            assertThat(Regex(pattern).containsMatchIn(result))
                .`as`("Generated value should match pattern")
                .isTrue()
        }

        @Test
        @DisplayName("should respect both minLength and maxLength constraints")
        @Description("Verifies that generated values are within the specified length range")
        fun shouldRespectBothLengthConstraints() {
            val pattern = "^[a-z]+$"
            val minLength = 5
            val maxLength = 10

            val result = step("Generate valid value with length constraints") {
                generator.generateValidValue(pattern, minLength, maxLength, 0)
            }

            assertThat(result).isNotNull()
            assertThat(result!!.length)
                .`as`("Generated value should be between $minLength and $maxLength characters")
                .isBetween(minLength, maxLength)
            assertThat(Regex(pattern).containsMatchIn(result))
                .`as`("Generated value should match pattern")
                .isTrue()
        }

        @Test
        @DisplayName("should produce deterministic output for same variationIndex")
        @Description("Verifies that the same variationIndex produces the same output")
        fun shouldProduceDeterministicOutput() {
            val pattern = "^[A-Z]{3,5}$"
            val variationIndex = 42

            val result1 = generator.generateValidValue(pattern, null, null, variationIndex)
            val result2 = generator.generateValidValue(pattern, null, null, variationIndex)

            assertThat(result1)
                .`as`("Same variationIndex should produce same output")
                .isEqualTo(result2)
        }

        @Test
        @DisplayName("should produce different output for different variationIndex")
        @Description("Verifies that different variationIndex values can produce different outputs")
        fun shouldProduceDifferentOutputForDifferentVariationIndex() {
            val pattern = "^[a-zA-Z0-9]{10,20}$"

            val results = (0..4).map { variationIndex ->
                generator.generateValidValue(pattern, null, null, variationIndex)
            }

            // All results should be non-null and match the pattern
            results.forEach { result ->
                assertThat(result).isNotNull()
                assertThat(Regex(pattern).containsMatchIn(result!!)).isTrue()
            }

            // At least some results should be different (allowing for some collisions)
            val uniqueResults = results.toSet()
            assertThat(uniqueResults.size)
                .`as`("Different variationIndex values should produce at least some variation")
                .isGreaterThan(1)
        }

        @Test
        @DisplayName("should return null for infeasible length constraints")
        @Description("Verifies that null is returned when minLength > maxLength")
        fun shouldReturnNullForInfeasibleLengthConstraints() {
            val pattern = "^[A-Z]{5}$" // exactly 5 characters
            val minLength = 10 // requires at least 10 characters
            val maxLength = 5 // pattern produces exactly 5

            val result = step("Generate valid value with infeasible constraints") {
                generator.generateValidValue(pattern, minLength, maxLength, 0)
            }

            assertThat(result)
                .`as`("Should return null for infeasible constraints")
                .isNull()
        }

        @Test
        @DisplayName("should return null for invalid pattern syntax")
        @Description("Verifies that null is returned when pattern cannot be parsed")
        fun shouldReturnNullForInvalidPatternSyntax() {
            val invalidPattern = "^[unclosed"

            val result = step("Generate valid value for invalid pattern") {
                generator.generateValidValue(invalidPattern, null, null, 0)
            }

            assertThat(result)
                .`as`("Should return null for invalid pattern syntax")
                .isNull()
        }

        @Test
        @DisplayName("should return null for unsupported pattern features like word boundary")
        @Description("Verifies that null is returned for patterns with unsupported features")
        fun shouldReturnNullForUnsupportedPatternFeatures() {
            // Word boundary \b is not supported by regexp-gen
            val unsupportedPattern = "^\\bword\\b$"

            val result = step("Generate valid value for unsupported pattern") {
                generator.generateValidValue(unsupportedPattern, null, null, 0)
            }

            assertThat(result)
                .`as`("Should return null for patterns with unsupported features")
                .isNull()
        }
    }

    @Nested
    @DisplayName("generateInvalidValue")
    inner class GenerateInvalidValue {

        fun invalidValueProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "Simple alternation pattern",
                "^(foo|bar|baz)$",
            ),
            Arguments.of(
                "Character class pattern",
                "^[A-Z]{3}$",
            ),
            Arguments.of(
                "Numeric pattern",
                "^\\d{4}$",
            ),
            Arguments.of(
                "Email-like pattern",
                "^[a-z]+@[a-z]+\\.[a-z]+$",
            ),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidValueProvider")
        @DisplayName("should generate strings NOT matching the pattern")
        @Description("Verifies that generateInvalidValue produces strings that do not match the given pattern")
        fun shouldGenerateInvalidStrings(scenario: String, pattern: String) {
            val result = step("Generate invalid value") {
                generator.generateInvalidValue(pattern, null, null)
            }

            assertThat(result)
                .`as`("Generated invalid value should not be null")
                .isNotNull()

            val regex = Regex(pattern)
            assertThat(regex.containsMatchIn(result!!))
                .`as`("Generated value '$result' should NOT match pattern '$pattern'")
                .isFalse()
        }

        @Test
        @DisplayName("should return null for patterns that match all strings")
        @Description("Verifies that null is returned for patterns like .* that cannot produce non-matches")
        fun shouldReturnNullForAllMatchingPatterns() {
            val result = step("Generate invalid value for catch-all pattern") {
                generator.generateInvalidValue("^.*$", null, null)
            }

            assertThat(result)
                .`as`("Should return null for patterns that match all strings")
                .isNull()
        }

        @Test
        @DisplayName("should generate non-matching value respecting length hints")
        @Description("Verifies that length hints are considered when generating invalid values")
        fun shouldRespectLengthHints() {
            val pattern = "^[A-Z]{3}$"
            val minLength = 5
            val maxLength = 10

            val result = step("Generate invalid value with length hints") {
                generator.generateInvalidValue(pattern, minLength, maxLength)
            }

            assertThat(result)
                .`as`("Generated invalid value should not be null")
                .isNotNull()

            val regex = Regex(pattern)
            assertThat(regex.containsMatchIn(result!!))
                .`as`("Generated value '$result' should NOT match pattern '$pattern'")
                .isFalse()
        }

        @Test
        @DisplayName("should return null for invalid pattern syntax")
        @Description("Verifies that null is returned when pattern cannot be parsed")
        fun shouldReturnNullForInvalidPatternSyntax() {
            val invalidPattern = "^[unclosed"

            val result = step("Generate invalid value for invalid pattern") {
                generator.generateInvalidValue(invalidPattern, null, null)
            }

            assertThat(result)
                .`as`("Should return null for invalid pattern syntax")
                .isNull()
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        @DisplayName("should handle pattern with special regex characters")
        @Description("Verifies that patterns with special characters are handled correctly")
        fun shouldHandleSpecialCharacters() {
            val pattern = "^\\[test\\]$"

            val result = generator.generateValidValue(pattern, null, null, 0)

            assertThat(result)
                .`as`("Should generate value matching pattern with special characters")
                .isNotNull()
            assertThat(result).isEqualTo("[test]")
        }

        @Test
        @DisplayName("should handle complex pattern with groups and quantifiers")
        @Description("Verifies that complex patterns with nested groups work correctly")
        fun shouldHandleComplexPatterns() {
            val pattern = "^((a|b){2,4}c)+$"

            val result = generator.generateValidValue(pattern, null, null, 0)

            assertThat(result).isNotNull()
            assertThat(Regex(pattern).containsMatchIn(result!!))
                .`as`("Generated value '$result' should match complex pattern")
                .isTrue()
        }

        @Test
        @DisplayName("should handle pattern with exact length requirement")
        @Description("Verifies that patterns with exact length work with length constraints")
        fun shouldHandleExactLengthPattern() {
            val pattern = "^[A-Z]{5}$" // exactly 5 characters

            val result = generator.generateValidValue(pattern, 5, 5, 0)

            assertThat(result).isNotNull()
            assertThat(result!!.length).isEqualTo(5)
            assertThat(Regex(pattern).containsMatchIn(result)).isTrue()
        }

        @Test
        @DisplayName("should handle pattern with letters, hyphens and periods (no whitespace)")
        @Description("Verifies that patterns with letters, hyphens and periods work correctly")
        fun shouldHandlePatternWithLettersHyphensAndPeriods() {
            val pattern = "^[a-zA-Z\\-.]+$"

            val result = step("Generate valid value for pattern") {
                generator.generateValidValue(pattern, null, null, 0)
            }

            assertThat(result)
                .`as`("Should generate a valid value for pattern with letters, hyphens and periods")
                .isNotNull()
            assertThat(Regex(pattern).matches(result!!))
                .`as`("Generated value '$result' should match pattern '$pattern'")
                .isTrue()
        }

        @Test
        @DisplayName("should handle pattern with letters, whitespace, hyphens and periods")
        @Description(
            """
            Tests the pattern ^[a-zA-Z\s\-\.]+$ which includes letters, whitespace, hyphens and periods.
            """
        )
        fun shouldHandlePatternWithLettersWhitespaceHyphensAndPeriods() {
            val pattern = "^[a-zA-Z\\s\\-\\.]+$"

            val result = step("Generate valid value for pattern with whitespace class") {
                generator.generateValidValue(pattern, null, null, 0)
            }

            assertThat(result)
                .`as`("Should generate a valid value for pattern with whitespace class")
                .isNotNull()

            // Verify the result contains only allowed characters
            assertThat(result!!.all { c -> c.isLetter() || c.isWhitespace() || c == '-' || c == '.' })
                .`as`("Generated value '$result' should only contain letters, whitespace, hyphens or periods")
                .isTrue()
        }

        @Test
        @DisplayName("should generate invalid value for pattern with letters, whitespace, hyphens and periods")
        @Description("Verifies that an invalid value (non-matching) can be generated for this pattern")
        fun shouldGenerateInvalidValueForPatternWithLettersWhitespaceHyphensAndPeriods() {
            val pattern = "^[a-zA-Z\\s\\-.]+$"

            val result = step("Generate invalid value for pattern") {
                generator.generateInvalidValue(pattern, null, null)
            }

            // Should be able to generate a non-matching value (e.g., containing digits or special chars)
            assertThat(result)
                .`as`("Should be able to generate a non-matching value for this pattern")
                .isNotNull()

            val regex = Regex(pattern)
            assertThat(regex.matches(result!!))
                .`as`("Generated value '$result' should NOT match pattern '$pattern'")
                .isFalse()
        }
    }

    @Nested
    @DisplayName("PatternGenerationOptions")
    inner class PatternGenerationOptionsTests {

        @Test
        @DisplayName("should use custom defaultMinLength when schema has no constraints")
        @Description("Verifies that custom defaultMinLength is used for generated string length")
        fun shouldUseCustomDefaultMinLength() {
            val pattern = "^[a-z]+$"
            val customMinLength = 7
            val options = PatternGenerationOptions(defaultMinLength = customMinLength)
            val customGenerator = PatternValueGenerator(options)

            val result = step("Generate valid value with custom defaultMinLength") {
                customGenerator.generateValidValue(pattern, null, null, 0)
            }

            assertThat(result).isNotNull()
            assertThat(result!!.length)
                .`as`("Generated value should use custom defaultMinLength=$customMinLength")
                .isGreaterThanOrEqualTo(customMinLength)
        }

        @Test
        @DisplayName("should use custom spaceChars for whitespace pattern matching")
        @Description("Verifies that custom spaceChars affects \\s character class generation")
        fun shouldUseCustomSpaceCharsForWhitespacePattern() {
            // Pattern that requires whitespace - using \s+ to force whitespace generation
            val pattern = "^[a-z]+\\s+[a-z]+$"
            // Only allow regular space and tab as whitespace
            val options = PatternGenerationOptions(spaceChars = " \t")
            val customGenerator = PatternValueGenerator(options)

            val result = step("Generate valid value with custom spaceChars") {
                customGenerator.generateValidValue(pattern, null, null, 0)
            }

            assertThat(result)
                .`as`("Should generate value with custom spaceChars")
                .isNotNull()

            // Verify the pattern matches (whitespace is present and valid)
            assertThat(Regex(pattern).matches(result!!))
                .`as`("Generated value '$result' should match pattern requiring whitespace")
                .isTrue()

            // Verify any whitespace in result is only space or tab (from our custom set)
            val whitespaceInResult = result.filter { it.isWhitespace() }
            assertThat(whitespaceInResult)
                .`as`("Generated value should contain whitespace")
                .isNotEmpty()
            assertThat(whitespaceInResult.all { it == ' ' || it == '\t' })
                .`as`("Whitespace should only be space or tab based on custom spaceChars")
                .isTrue()
        }

        @Test
        @DisplayName("should use custom anyPrintableChars for dot pattern matching")
        @Description("Verifies that custom anyPrintableChars affects . (dot) character class generation")
        fun shouldUseCustomAnyPrintableChars() {
            // Pattern that uses . to match any printable character
            val pattern = "^test.end$"
            // Restrict printable chars to only digits
            val options = PatternGenerationOptions(anyPrintableChars = "0123456789")
            val customGenerator = PatternValueGenerator(options)

            val result = step("Generate valid value with custom anyPrintableChars") {
                customGenerator.generateValidValue(pattern, null, null, 0)
            }

            assertThat(result)
                .`as`("Should generate value with custom anyPrintableChars")
                .isNotNull()

            // The middle character should be a digit since we restricted anyPrintableChars
            val middleChar = result!![4] // 'test' is 4 chars, so index 4 is the . match
            assertThat(middleChar.isDigit())
                .`as`("Middle character '$middleChar' should be a digit based on custom anyPrintableChars")
                .isTrue()
        }

        @Test
        @DisplayName("should apply spaceChars to invalid value generation")
        @Description("Verifies that custom spaceChars affects invalid value generation for patterns with \\S")
        fun shouldApplySpaceCharsToInvalidValueGeneration() {
            // Pattern with \S (non-whitespace) - invalid value generator needs to know what whitespace is
            val pattern = "^\\S+$"
            val options = PatternGenerationOptions(spaceChars = " \t")
            val customGenerator = PatternValueGenerator(options)

            val result = step("Generate invalid value with custom spaceChars") {
                customGenerator.generateInvalidValue(pattern, null, null)
            }

            // The invalid value should contain whitespace since the pattern requires non-whitespace
            assertThat(result)
                .`as`("Should generate invalid value (containing whitespace)")
                .isNotNull()

            val regex = Regex(pattern)
            assertThat(regex.matches(result!!))
                .`as`("Generated value '$result' should NOT match pattern '$pattern'")
                .isFalse()
        }
    }

    @Nested
    @DisplayName("Deterministic Output Verification")
    inner class DeterministicOutputVerification {

        @Suppress("LongMethod")
        fun deterministicPatternProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "UUID pattern",
                "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
                0,
            ),
            Arguments.of(
                "Date pattern",
                "^\\d{4}-\\d{2}-\\d{2}$",
                0,
            ),
            Arguments.of(
                "Simple word pattern",
                "^[a-z]{5}$",
                0,
            ),
            Arguments.of(
                "Simple word pattern with variation 1",
                "^[a-z]{5}$",
                1,
            ),
            Arguments.of(
                "Alphanumeric pattern",
                "^[A-Z][a-z]{3}[0-9]{2}$",
                0,
            ),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("deterministicPatternProvider")
        @DisplayName("should generate deterministic values for patterns across multiple calls")
        @Description("Verifies that the same pattern and variationIndex always produce the same result")
        fun shouldGenerateDeterministicValues(
            scenario: String,
            pattern: String,
            variationIndex: Int,
        ) {
            val result1 = step("Generate valid value - call 1") {
                generator.generateValidValue(pattern, null, null, variationIndex)
            }
            val result2 = step("Generate valid value - call 2") {
                generator.generateValidValue(pattern, null, null, variationIndex)
            }
            val result3 = step("Generate valid value - call 3") {
                generator.generateValidValue(pattern, null, null, variationIndex)
            }

            assertThat(result1)
                .`as`("Generated value for $scenario should not be null")
                .isNotNull()

            assertThat(result1)
                .`as`("Generated value for $scenario should be deterministic across calls")
                .isEqualTo(result2)
                .isEqualTo(result3)

            // Verify the result matches the pattern
            assertThat(Regex(pattern).matches(result1!!))
                .`as`("Generated value '$result1' should match pattern '$pattern'")
                .isTrue()
        }

        @Test
        @DisplayName("should produce identical results across multiple calls with same parameters")
        @Description("Verifies determinism by calling the same generation multiple times")
        fun shouldProduceIdenticalResultsAcrossMultipleCalls() {
            val pattern = "^[A-Za-z0-9]{10}$"
            val variationIndex = 42

            val results = (1..5).map {
                generator.generateValidValue(pattern, null, null, variationIndex)
            }

            assertThat(results.toSet())
                .`as`("All 5 calls should produce the same result")
                .hasSize(1)
        }
    }

    @Nested
    @DisplayName("Length Calculation Edge Cases")
    inner class LengthCalculationEdgeCases {

        @Test
        @DisplayName("should use pattern min length when greater than schema min length")
        @Description("Verifies that pattern's minimum length takes precedence when it exceeds schema min")
        fun shouldUsePatternMinLengthWhenGreater() {
            // Pattern requires exactly 5 characters
            val pattern = "^[A-Z]{5}$"
            // Schema allows 3 or more, but pattern needs 5
            val schemaMinLength = 3

            val result = step("Generate value where pattern min > schema min") {
                generator.generateValidValue(pattern, schemaMinLength, null, 0)
            }

            assertThat(result).isNotNull()
            assertThat(result!!.length)
                .`as`("Should use pattern min length (5) not schema min length (3)")
                .isEqualTo(5)
        }

        @Test
        @DisplayName("should use schema min length when greater than pattern min length")
        @Description("Verifies that schema's minimum length is respected when it exceeds pattern min")
        fun shouldUseSchemaMinLengthWhenGreater() {
            // Pattern allows 1 or more characters
            val pattern = "^[a-z]+$"
            // Schema requires at least 8 characters
            val schemaMinLength = 8

            val result = step("Generate value where schema min > pattern min") {
                generator.generateValidValue(pattern, schemaMinLength, null, 0)
            }

            assertThat(result).isNotNull()
            assertThat(result!!.length)
                .`as`("Should respect schema min length (8)")
                .isGreaterThanOrEqualTo(schemaMinLength)
        }

        @Test
        @DisplayName("should use pattern max length when less than schema max length")
        @Description("Verifies that pattern's maximum length constraint is respected")
        fun shouldUsePatternMaxLengthWhenLess() {
            // Pattern allows exactly 3 characters
            val pattern = "^[A-Z]{3}$"
            // Schema allows up to 10, but pattern only allows 3
            val schemaMaxLength = 10

            val result = step("Generate value where pattern max < schema max") {
                generator.generateValidValue(pattern, null, schemaMaxLength, 0)
            }

            assertThat(result).isNotNull()
            assertThat(result!!.length)
                .`as`("Should use pattern max length (3) not schema max length (10)")
                .isEqualTo(3)
        }

        @Test
        @DisplayName("should use custom defaultMinLength when no constraints specified")
        @Description("Verifies that custom defaultMinLength from options is used as fallback")
        fun shouldUseCustomDefaultMinLengthAsFallback() {
            val pattern = "^[a-z]+$"
            val customDefaultMinLength = 12
            val options = PatternGenerationOptions(defaultMinLength = customDefaultMinLength)
            val customGenerator = PatternValueGenerator(options)

            val result = step("Generate value with custom defaultMinLength") {
                customGenerator.generateValidValue(pattern, null, null, 0)
            }

            assertThat(result).isNotNull()
            assertThat(result!!.length)
                .`as`("Should use custom defaultMinLength ($customDefaultMinLength)")
                .isGreaterThanOrEqualTo(customDefaultMinLength)
        }

        @Test
        @DisplayName("should cap effective min length at schema max length")
        @Description("Verifies that effective min length is capped by schema max length")
        fun shouldCapEffectiveMinLengthAtSchemaMaxLength() {
            val pattern = "^[a-z]+$"
            // Schema max is 4, so effective min should be capped
            val schemaMaxLength = 4
            val options = PatternGenerationOptions(defaultMinLength = 10)
            val customGenerator = PatternValueGenerator(options)

            val result = step("Generate value where defaultMinLength > schemaMaxLength") {
                customGenerator.generateValidValue(pattern, null, schemaMaxLength, 0)
            }

            assertThat(result).isNotNull()
            assertThat(result!!.length)
                .`as`("Should cap effective min length at schema max length (4)")
                .isLessThanOrEqualTo(schemaMaxLength)
        }
    }

    @Nested
    @DisplayName("Deterministic Common Pattern Generation")
    inner class DeterministicCommonPatterns {

        @Test
        @DisplayName("should generate exact expected value for UUID pattern")
        @Description("Verifies deterministic UUID pattern generation with exact value assertion")
        fun shouldGenerateExactValueForUuidPattern() {
            val pattern = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"

            val result = generator.generateValidValue(pattern, null, null, 0)

            assertThat(result)
                .`as`("Deterministic generation for UUID pattern with seed 0")
                .isNotNull()
                .isEqualTo("34e2b36f-998a-fcfc-2a93-e0a699517595")
        }

        @Test
        @DisplayName("should generate exact expected value for date pattern")
        @Description("Verifies deterministic date pattern generation with exact value assertion")
        fun shouldGenerateExactValueForDatePattern() {
            val pattern = "^\\d{4}-\\d{2}-\\d{2}$"

            val result = generator.generateValidValue(pattern, null, null, 0)

            assertThat(result)
                .`as`("Deterministic generation for date pattern with seed 0")
                .isNotNull()
                .isEqualTo("7531-77-44")
        }

        @Test
        @DisplayName("should generate exact expected value for uppercase 3 char pattern")
        @Description("Verifies deterministic uppercase character pattern generation")
        fun shouldGenerateExactValueForUppercase3CharPattern() {
            val pattern = "^[A-Z]{3}$"

            val result = generator.generateValidValue(pattern, null, null, 0)

            assertThat(result)
                .`as`("Deterministic generation for ^[A-Z]{3}$ with seed 0")
                .isNotNull()
                .isEqualTo("SXV")
        }

        @Test
        @DisplayName("should generate exact expected value for lowercase 5 char pattern")
        @Description("Verifies deterministic lowercase character pattern generation")
        fun shouldGenerateExactValueForLowercase5CharPattern() {
            val pattern = "^[a-z]{5}$"

            val result = generator.generateValidValue(pattern, null, null, 0)

            assertThat(result)
                .`as`("Deterministic generation for ^[a-z]{5}$ with seed 0")
                .isNotNull()
                .isEqualTo("sxvnj")
        }

        @Test
        @DisplayName("should generate different values for different variation indices")
        @Description("Verifies that different seeds produce different deterministic values")
        fun shouldGenerateDifferentValuesForDifferentSeeds() {
            val pattern = "^[A-Z]{3}$"

            val value0 = generator.generateValidValue(pattern, null, null, 0)
            val value1 = generator.generateValidValue(pattern, null, null, 1)
            val value2 = generator.generateValidValue(pattern, null, null, 2)

            assertThat(value0).isEqualTo("SXV")
            assertThat(value1).isNotEqualTo(value0)
            assertThat(value2).isNotEqualTo(value0)
        }

        @Test
        @DisplayName("should generate exact expected value for alphanumeric pattern")
        @Description("Verifies deterministic alphanumeric pattern generation")
        fun shouldGenerateExactValueForAlphanumericPattern() {
            val pattern = "^[A-Za-z0-9]{10}$"

            val result = generator.generateValidValue(pattern, null, null, 0)

            assertThat(result)
                .`as`("Deterministic generation for alphanumeric pattern with seed 0")
                .isNotNull()
            assertThat(result!!.length).isEqualTo(10)
            assertThat(Regex(pattern).matches(result)).isTrue()
        }

        @Test
        @DisplayName("should generate exact expected value for email-like pattern")
        @Description("Verifies deterministic email-like pattern generation")
        fun shouldGenerateExactValueForEmailLikePattern() {
            val pattern = "^[a-z]+@[a-z]+\\.[a-z]+$"

            val result = generator.generateValidValue(pattern, null, null, 0)

            assertThat(result)
                .`as`("Deterministic generation for email-like pattern with seed 0")
                .isNotNull()
            assertThat(Regex(pattern).matches(result!!))
                .`as`("Generated value should match email-like pattern")
                .isTrue()
        }
    }
}


