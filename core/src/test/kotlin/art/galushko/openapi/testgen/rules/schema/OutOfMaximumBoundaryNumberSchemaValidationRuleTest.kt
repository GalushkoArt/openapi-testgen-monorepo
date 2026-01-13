package art.galushko.openapi.testgen.rules.schema

import art.galushko.openapi.testgen.generation.Conditions.ruleAppliedTo
import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.rules.ValidationRuleTest
import io.qameta.allure.Description
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.stream.Stream

class OutOfMaximumBoundaryNumberSchemaValidationRuleTest : ValidationRuleTest() {
    private val rule = OutOfMaximumBoundaryNumberSchemaValidationRule()

    fun schemaProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Integer schema with maximum",
            IntegerSchema().maximum(BigDecimal.valueOf(10)),
            sequenceOf(BigDecimal.valueOf(11))
        ),
        Arguments.of(
            "Number schema with maximum",
            NumberSchema().maximum(BigDecimal.valueOf(55, 1)),
            sequenceOf(BigDecimal.valueOf(65, 1))
        ),
        Arguments.of(
            "Number schema with maximum with multipleOf",
            NumberSchema().maximum(BigDecimal.valueOf(55, 1)).multipleOf(BigDecimal.valueOf(5, 1)),
            sequenceOf(BigDecimal.valueOf(60, 1))
        ),
        Arguments.of(
            "Number schema with maximum and exclusiveMaximum=true",
            NumberSchema().maximum(BigDecimal.valueOf(100)).exclusiveMaximum(true),
            sequenceOf(BigDecimal.valueOf(100))
        ),
        Arguments.of(
            "Number schema with maximum and exclusiveMaximum=false",
            NumberSchema().maximum(BigDecimal.valueOf(100)).exclusiveMaximum(false),
            sequenceOf(BigDecimal.valueOf(101))
        ),
        Arguments.of(
            "Number schema with maximum, exclusiveMaximum=true, and multipleOf",
            NumberSchema().maximum(BigDecimal.valueOf(500, 1))
                .exclusiveMaximum(true)
                .multipleOf(BigDecimal.valueOf(5, 1)),
            sequenceOf(BigDecimal.valueOf(500, 1))
        ),
        Arguments.of(
            "Integer schema with maximum and exclusiveMaximum=true",
            IntegerSchema().maximum(BigDecimal.valueOf(200)).exclusiveMaximum(true),
            sequenceOf(BigDecimal.valueOf(200))
        ),
        Arguments.of(
            "Integer schema with maximum, exclusiveMaximum=true, and multipleOf",
            IntegerSchema().maximum(BigDecimal.valueOf(100))
                .exclusiveMaximum(true)
                .multipleOf(BigDecimal.valueOf(10)),
            sequenceOf(BigDecimal.valueOf(100))
        ),
        Arguments.of("Integer schema without maximum", IntegerSchema(), emptySequence<Any>()),
        Arguments.of("String schema with maximum", StringSchema().maximum(BigDecimal.valueOf(10)), emptySequence<Any>())
    )

    @ParameterizedTest
    @MethodSource("schemaProvider")
    @DisplayName("Out Of Maximum Boundary Number: apply() should return optional with invalid example when applicable")
    @Description("Verifies that the apply method returns a stream with an invalid example value when the rule is applicable or empty Stream")
    fun outOfMaximumBoundaryNumberApplyTest(scenario: String, schema: Schema<*>, expected: Sequence<Any>) {
        val result = step("Call apply") { rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI())) }
        assertThat(result.toList()).`is`(ruleAppliedTo(rule, expected))
    }
}


