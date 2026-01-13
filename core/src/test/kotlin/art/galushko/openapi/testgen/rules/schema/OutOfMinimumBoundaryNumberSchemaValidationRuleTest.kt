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

class OutOfMinimumBoundaryNumberSchemaValidationRuleTest : ValidationRuleTest() {
    private val rule = OutOfMinimumBoundaryNumberSchemaValidationRule()

    fun schemaProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Integer schema with minimum",
            IntegerSchema().minimum(BigDecimal.valueOf(10)),
            sequenceOf(BigDecimal.valueOf(9))
        ),
        Arguments.of(
            "Number schema with minimum",
            NumberSchema().minimum(BigDecimal.valueOf(55, 1)),
            sequenceOf(BigDecimal.valueOf(45, 1))
        ),
        Arguments.of(
            "Number schema with minimum with multipleOf",
            NumberSchema().minimum(BigDecimal.valueOf(55, 1)).multipleOf(BigDecimal.valueOf(5, 1)),
            sequenceOf(BigDecimal.valueOf(50, 1))
        ),
        Arguments.of(
            "Number schema with minimum and exclusiveMinimum=true",
            NumberSchema().minimum(BigDecimal.valueOf(10)).exclusiveMinimum(true),
            sequenceOf(BigDecimal.valueOf(10))
        ),
        Arguments.of(
            "Number schema with minimum and exclusiveMinimum=false",
            NumberSchema().minimum(BigDecimal.valueOf(10)).exclusiveMinimum(false),
            sequenceOf(BigDecimal.valueOf(9))
        ),
        Arguments.of(
            "Number schema with minimum, exclusiveMinimum=true, and multipleOf",
            NumberSchema().minimum(BigDecimal.valueOf(100, 1))
                .exclusiveMinimum(true)
                .multipleOf(BigDecimal.valueOf(5, 1)),
            sequenceOf(BigDecimal.valueOf(100, 1))
        ),
        Arguments.of(
            "Integer schema with minimum and exclusiveMinimum=true",
            IntegerSchema().minimum(BigDecimal.valueOf(50)).exclusiveMinimum(true),
            sequenceOf(BigDecimal.valueOf(50))
        ),
        Arguments.of(
            "Integer schema with minimum, exclusiveMinimum=true, and multipleOf",
            IntegerSchema().minimum(BigDecimal.valueOf(20))
                .exclusiveMinimum(true)
                .multipleOf(BigDecimal.valueOf(5)),
            sequenceOf(BigDecimal.valueOf(20))
        ),
        Arguments.of("Integer schema without minimum", IntegerSchema(), emptySequence<Any>()),
        Arguments.of("String schema with minimum", StringSchema().minimum(BigDecimal.valueOf(10)), emptySequence<Any>())
    )

    @ParameterizedTest
    @MethodSource("schemaProvider")
    @DisplayName("Out Of Minimum Boundary Number: apply() should return optional with invalid example when applicable")
    @Description("Verifies that the apply method returns a stream with an invalid example value when the rule is applicable or empty Stream")
    fun outOfMinimumBoundaryNumberApplyTest(scenario: String, schema: Schema<*>, expected: Sequence<Any>) {
        val resultList = step("Call apply") { rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI())).toList() }
        assertThat(resultList).`is`(ruleAppliedTo(rule, expected))
    }
}


