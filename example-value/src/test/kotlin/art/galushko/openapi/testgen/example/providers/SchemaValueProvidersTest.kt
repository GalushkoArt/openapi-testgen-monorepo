package art.galushko.openapi.testgen.example.providers

import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.util.stream.Stream

@Epic("Test Data Generation")
@Feature("Schema Value Providers")
@DisplayName("Schema Value Providers")
class SchemaValueProvidersTest {

    @Nested
    @DisplayName("BooleanValueProvider")
    inner class BooleanValueProviderTest {

        private val provider = BooleanValueProvider()

        @Test
        @DisplayName("should return null for non-boolean schema")
        fun shouldReturnNullForNonBooleanSchema() {
            assertThat(provider.provide(StringSchema(), 0)).isNull()
            assertThat(provider.provide(NumberSchema(), 0)).isNull()
            assertThat(provider.provide(ObjectSchema(), 0)).isNull()
        }

        @ParameterizedTest(name = "variationIndex={0} should return {1}")
        @CsvSource(
            "0, true",
            "1, false",
            "2, true",
            "3, false",
            "100, true",
            "101, false"
        )
        @DisplayName("should alternate between true and false based on variationIndex")
        fun shouldAlternateBasedOnVariationIndex(variationIndex: Int, expected: Boolean) {
            val result = provider.provide(BooleanSchema(), variationIndex)
            assertThat(result).isEqualTo(expected)
        }

        @Test
        @DisplayName("should handle schema with boolean type string")
        fun shouldHandleSchemaWithBooleanTypeString() {
            val schema = Schema<Boolean>().apply { type = "boolean" }
            assertThat(provider.provide(schema, 0)).isEqualTo(true)
        }
    }

    @Nested
    @DisplayName("NumberValueProvider")
    inner class NumberValueProviderTest {

        private val provider = NumberValueProvider()

        @Test
        @DisplayName("should return null for non-number schema")
        fun shouldReturnNullForNonNumberSchema() {
            assertThat(provider.provide(StringSchema(), 0)).isNull()
            assertThat(provider.provide(BooleanSchema(), 0)).isNull()
            assertThat(provider.provide(ObjectSchema(), 0)).isNull()
        }

        @Test
        @DisplayName("should return base value for NumberSchema without constraints")
        fun shouldReturnBaseValueWithoutConstraints() {
            val result = provider.provide(NumberSchema(), 0)
            assertThat(result).isEqualTo(BigDecimal.ONE)
        }

        @Test
        @DisplayName("should return minimum as base value when present")
        fun shouldReturnMinimumAsBaseValue() {
            val schema = NumberSchema().apply { minimum = BigDecimal.TEN }
            assertThat(provider.provide(schema, 0)).isEqualTo(BigDecimal.TEN)
        }

        @ParameterizedTest(name = "variationIndex={0} should increment by step")
        @CsvSource(
            "0, 5",
            "1, 6",
            "2, 7",
            "3, 8"
        )
        @DisplayName("should increment by 1 step for each variationIndex")
        fun shouldIncrementByStep(variationIndex: Int, expected: Int) {
            val schema = NumberSchema().apply { minimum = BigDecimal.valueOf(5) }
            val result = provider.provide(schema, variationIndex) as BigDecimal
            assertThat(result).isEqualTo(BigDecimal.valueOf(expected.toLong()))
        }

        @Test
        @DisplayName("should respect multipleOf constraint")
        fun shouldRespectMultipleOf() {
            val schema = NumberSchema().apply {
                minimum = BigDecimal.ZERO
                multipleOf = BigDecimal.valueOf(5)
            }
            assertThat(provider.provide(schema, 0)).isEqualTo(BigDecimal.ZERO)
            assertThat(provider.provide(schema, 1)).isEqualTo(BigDecimal.valueOf(5))
            assertThat(provider.provide(schema, 2)).isEqualTo(BigDecimal.TEN)
        }

        @Test
        @DisplayName("should wrap around when maximum is exceeded")
        fun shouldWrapAroundWhenMaximumExceeded() {
            val schema = NumberSchema().apply {
                minimum = BigDecimal.valueOf(10)
                maximum = BigDecimal.valueOf(12)
            }
            // min=10, max=12, step=1 -> 3 variations: 10, 11, 12
            assertThat(provider.provide(schema, 0)).isEqualTo(BigDecimal.valueOf(10))
            assertThat(provider.provide(schema, 1)).isEqualTo(BigDecimal.valueOf(11))
            assertThat(provider.provide(schema, 2)).isEqualTo(BigDecimal.valueOf(12))
            // Wraps around: variationIndex=3 -> 3 % 3 = 0 -> 10
            assertThat(provider.provide(schema, 3)).isEqualTo(BigDecimal.valueOf(10))
        }

        @Test
        @DisplayName("should handle IntegerSchema")
        fun shouldHandleIntegerSchema() {
            val result = provider.provide(IntegerSchema(), 0)
            assertThat(result).isEqualTo(BigDecimal.ONE)
        }

        @Test
        @DisplayName("should handle schema with number types set")
        fun shouldHandleSchemaWithNumberTypesSet() {
            val schema = Schema<Number>().apply { types = setOf("number") }
            assertThat(provider.provide(schema, 0)).isEqualTo(BigDecimal.ONE)
        }

        @Test
        @DisplayName("should handle schema with integer types set")
        fun shouldHandleSchemaWithIntegerTypesSet() {
            val schema = Schema<Number>().apply { types = setOf("integer") }
            assertThat(provider.provide(schema, 0)).isEqualTo(BigDecimal.ONE)
        }
    }

    @Nested
    @DisplayName("ConstValueProvider")
    inner class ConstValueProviderTest {

        private val provider = ConstValueProvider()

        @Test
        @DisplayName("should return const value when present")
        fun shouldReturnConstValueWhenPresent() {
            val schema = StringSchema().also { it.setConst("fixed-value") }
            assertThat(provider.provide(schema, 0)).isEqualTo("fixed-value")
        }

        @Test
        @DisplayName("should return null when const is not present")
        fun shouldReturnNullWhenConstNotPresent() {
            assertThat(provider.provide(StringSchema(), 0)).isNull()
        }

        @Test
        @DisplayName("should ignore variationIndex")
        fun shouldIgnoreVariationIndex() {
            val schema = StringSchema().also { it.setConst("fixed") }
            assertThat(provider.provide(schema, 0)).isEqualTo("fixed")
            assertThat(provider.provide(schema, 1)).isEqualTo("fixed")
            assertThat(provider.provide(schema, 100)).isEqualTo("fixed")
        }
    }

    @Nested
    @DisplayName("EnumValueProvider")
    inner class EnumValueProviderTest {

        private val provider = EnumValueProvider()

        @Test
        @DisplayName("should return null when enum is not present")
        fun shouldReturnNullWhenEnumNotPresent() {
            assertThat(provider.provide(StringSchema(), 0)).isNull()
        }

        @Test
        @DisplayName("should return null when enum is empty")
        fun shouldReturnNullWhenEnumIsEmpty() {
            val schema = StringSchema().apply { enum = emptyList() }
            assertThat(provider.provide(schema, 0)).isNull()
        }

        @ParameterizedTest(name = "variationIndex={0} should return enum at index {1}")
        @CsvSource(
            "0, red",
            "1, green",
            "2, blue",
            "3, red",
            "4, green"
        )
        @DisplayName("should cycle through enum values")
        fun shouldCycleThroughEnumValues(variationIndex: Int, expected: String) {
            val schema = StringSchema().apply {
                addEnumItem("red")
                addEnumItem("green")
                addEnumItem("blue")
            }
            assertThat(provider.provide(schema, variationIndex)).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("DateValueProvider")
    inner class DateValueProviderTest {

        private val provider = DateValueProvider()

        @Test
        @DisplayName("should return null for non-string schema")
        fun shouldReturnNullForNonStringSchema() {
            assertThat(provider.provide(NumberSchema(), 0)).isNull()
        }

        @Test
        @DisplayName("should return null for string schema without date format")
        fun shouldReturnNullForStringSchemaWithoutDateFormat() {
            assertThat(provider.provide(StringSchema(), 0)).isNull()
            assertThat(provider.provide(StringSchema().format("uuid"), 0)).isNull()
        }

        @ParameterizedTest(name = "variationIndex={0} should return date +{0} days")
        @CsvSource(
            "0, 2025-05-05",
            "1, 2025-05-06",
            "5, 2025-05-10",
            "30, 2025-06-04"
        )
        @DisplayName("should increment date by variationIndex days")
        fun shouldIncrementDateByVariationIndex(variationIndex: Int, expected: String) {
            val schema = StringSchema().format("date")
            assertThat(provider.provide(schema, variationIndex)).isEqualTo(expected)
        }

        @Test
        @DisplayName("should use custom start date")
        fun shouldUseCustomStartDate() {
            val provider = DateValueProvider(startDateString = "2024-01-01")
            val schema = StringSchema().format("date")
            assertThat(provider.provide(schema, 0)).isEqualTo("2024-01-01")
            assertThat(provider.provide(schema, 1)).isEqualTo("2024-01-02")
        }
    }

    @Nested
    @DisplayName("DateTimeValueProvider")
    inner class DateTimeValueProviderTest {

        private val provider = DateTimeValueProvider()

        @Test
        @DisplayName("should return null for non-string schema")
        fun shouldReturnNullForNonStringSchema() {
            assertThat(provider.provide(NumberSchema(), 0)).isNull()
        }

        @Test
        @DisplayName("should return null for string schema without date-time format")
        fun shouldReturnNullForStringSchemaWithoutDateTimeFormat() {
            assertThat(provider.provide(StringSchema(), 0)).isNull()
            assertThat(provider.provide(StringSchema().format("date"), 0)).isNull()
        }

        @ParameterizedTest(name = "variationIndex={0} should return datetime +{0} days")
        @CsvSource(
            "0, 2025-05-05T17:32:28Z",
            "1, 2025-05-06T17:32:28Z",
            "5, 2025-05-10T17:32:28Z"
        )
        @DisplayName("should increment date portion by variationIndex days")
        fun shouldIncrementDatePortionByVariationIndex(variationIndex: Int, expected: String) {
            val schema = StringSchema().format("date-time")
            assertThat(provider.provide(schema, variationIndex)).isEqualTo(expected)
        }

        @Test
        @DisplayName("should use custom start date and time suffix")
        fun shouldUseCustomStartDateAndTimeSuffix() {
            val provider = DateTimeValueProvider(
                startDateString = "2024-01-01",
                timeSuffixTemplate = "%sT00:00:00Z"
            )
            val schema = StringSchema().format("date-time")
            assertThat(provider.provide(schema, 0)).isEqualTo("2024-01-01T00:00:00Z")
        }
    }

    @Nested
    @DisplayName("EmailValueProvider")
    inner class EmailValueProviderTest {

        private val provider = EmailValueProvider()

        @Test
        @DisplayName("should return null for non-string schema")
        fun shouldReturnNullForNonStringSchema() {
            assertThat(provider.provide(NumberSchema(), 0)).isNull()
        }

        @Test
        @DisplayName("should return null for string schema without email format")
        fun shouldReturnNullForStringSchemaWithoutEmailFormat() {
            assertThat(provider.provide(StringSchema(), 0)).isNull()
            assertThat(provider.provide(StringSchema().format("uuid"), 0)).isNull()
        }

        @ParameterizedTest(name = "variationIndex={0} should return test{0}@example.com")
        @ValueSource(ints = [0, 1, 5, 100])
        @DisplayName("should generate unique emails based on variationIndex")
        fun shouldGenerateUniqueEmails(variationIndex: Int) {
            val schema = StringSchema().format("email")
            assertThat(provider.provide(schema, variationIndex))
                .isEqualTo("test$variationIndex@example.com")
        }

        @Test
        @DisplayName("should use custom email template")
        fun shouldUseCustomEmailTemplate() {
            val provider = EmailValueProvider(emailTemplate = "user%s@company.com")
            val schema = StringSchema().format("email")
            assertThat(provider.provide(schema, 0)).isEqualTo("user0@company.com")
        }
    }

    @Nested
    @DisplayName("UuidValueProvider")
    inner class UuidValueProviderTest {

        private val provider = UuidValueProvider()

        @Test
        @DisplayName("should return null for non-string schema")
        fun shouldReturnNullForNonStringSchema() {
            assertThat(provider.provide(NumberSchema(), 0)).isNull()
        }

        @Test
        @DisplayName("should return null for string schema without uuid format")
        fun shouldReturnNullForStringSchemaWithoutUuidFormat() {
            assertThat(provider.provide(StringSchema(), 0)).isNull()
            assertThat(provider.provide(StringSchema().format("email"), 0)).isNull()
        }

        @ParameterizedTest(name = "variationIndex={0}")
        @MethodSource("art.galushko.openapi.testgen.example.providers.SchemaValueProvidersTest#uuidProvider")
        @DisplayName("should generate unique UUIDs based on variationIndex")
        fun shouldGenerateUniqueUuids(variationIndex: Int, expected: String) {
            val schema = StringSchema().format("uuid")
            assertThat(provider.provide(schema, variationIndex)).isEqualTo(expected)
        }

        @Test
        @DisplayName("should use custom uuid template")
        fun shouldUseCustomUuidTemplate() {
            val provider = UuidValueProvider(uuidTemplate = "00000000-0000-0000-0000-%s")
            val schema = StringSchema().format("uuid")
            assertThat(provider.provide(schema, 0)).isEqualTo("00000000-0000-0000-0000-000000000000")
        }
    }

    @Nested
    @DisplayName("PlainStringValueProvider")
    inner class PlainStringValueProviderTest {

        private val provider = PlainStringValueProvider()

        @Test
        @DisplayName("should return null for non-string schema")
        fun shouldReturnNullForNonStringSchema() {
            assertThat(provider.provide(NumberSchema(), 0)).isNull()
            assertThat(provider.provide(BooleanSchema(), 0)).isNull()
        }

        @Test
        @DisplayName("should return single character for variationIndex 0")
        fun shouldReturnSingleCharacterForIndex0() {
            val schema = StringSchema()
            assertThat(provider.provide(schema, 0)).isEqualTo("a")
        }

        @ParameterizedTest(name = "variationIndex={0} should return {1}")
        @CsvSource(
            "0, a",
            "1, b",
            "25, z",
            "26, A",
            "51, Z",
            "52, 0",
            "61, 9"
        )
        @DisplayName("should encode variationIndex in base-62")
        fun shouldEncodeVariationIndexInBase62(variationIndex: Int, expected: String) {
            val schema = StringSchema()
            assertThat(provider.provide(schema, variationIndex)).isEqualTo(expected)
        }

        @Test
        @DisplayName("should respect minLength constraint")
        fun shouldRespectMinLengthConstraint() {
            val schema = StringSchema().apply { minLength = 5 }
            val result = provider.provide(schema, 0) as String
            assertThat(result).hasSize(6) // baseLength = maxLength ?: (minLength + 1)
        }

        @Test
        @DisplayName("should respect maxLength constraint")
        fun shouldRespectMaxLengthConstraint() {
            val schema = StringSchema().apply { maxLength = 3 }
            val result = provider.provide(schema, 0) as String
            assertThat(result).hasSize(3)
        }

        @Test
        @DisplayName("should use custom validChars")
        fun shouldUseCustomValidChars() {
            val provider = PlainStringValueProvider(validCharsString = "01")
            val schema = StringSchema()
            assertThat(provider.provide(schema, 0)).isEqualTo("0")
            assertThat(provider.provide(schema, 1)).isEqualTo("1")
        }

        @Test
        @DisplayName("should handle large variationIndex with multiple chars")
        fun shouldHandleLargeVariationIndexWithMultipleChars() {
            val schema = StringSchema().apply { maxLength = 5 }
            // 62 is base, so variationIndex 62 should wrap and produce 2 chars
            val result = provider.provide(schema, 62) as String
            assertThat(result).hasSize(5)
            assertThat(result).endsWith("ba") // 62 in base-62 is "10" -> chars 'b' and 'a'
        }
    }

    companion object {
        @JvmStatic
        fun uuidProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(0, "d5a5495b-cbdc-4237-a66e-000000000000"),
            Arguments.of(1, "d5a5495b-cbdc-4237-a66e-000000000001"),
            Arguments.of(123, "d5a5495b-cbdc-4237-a66e-000000000123")
        )
    }
}
