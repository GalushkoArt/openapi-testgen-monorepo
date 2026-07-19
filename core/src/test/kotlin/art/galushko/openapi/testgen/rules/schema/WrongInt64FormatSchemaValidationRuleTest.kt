package art.galushko.openapi.testgen.rules.schema

import art.galushko.openapi.testgen.generation.Conditions.correctAppliedTo
import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.rules.ValidationRuleTest
import art.galushko.openapi.testgen.spi.RuleValue
import io.qameta.allure.Description
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.stream.Stream

class WrongInt64FormatSchemaValidationRuleTest : ValidationRuleTest() {
    private val rule = WrongInt64FormatSchemaValidationRule()

    fun schemaProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Integer schema with int64 format",
            IntegerSchema().format("int64"),
            sequenceOf(RuleValue("Wrong Int64 Format", BigDecimal("9223372036854775808")))
        ),
        Arguments.of(
            "Integer schema with int32 format",
            IntegerSchema().format("int32"),
            emptySequence<RuleValue>()
        ),
        Arguments.of(
            "Integer schema without format",
            IntegerSchema().format(null),
            emptySequence<RuleValue>()
        ),
        Arguments.of(
            "Non-integer schema",
            StringSchema(),
            emptySequence<RuleValue>()
        )
    )

    @ParameterizedTest
    @MethodSource("schemaProvider")
    @DisplayName("Wrong Int64 Format: apply() should return an out-of-range value only for int64 integers")
    @Description("Verifies that the apply method emits an out-of-64-bit-range value for int64 schemas and stays silent otherwise")
    fun wrongInt64FormatApplyTest(
        scenario: String,
        schema: Schema<*>,
        expected: Sequence<RuleValue>
    ) {
        // Act
        val result = step("Call apply") {
            rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI()))
        }

        // Assert
        assertThat(result.toList()).`is`(correctAppliedTo(expected))
    }
}
