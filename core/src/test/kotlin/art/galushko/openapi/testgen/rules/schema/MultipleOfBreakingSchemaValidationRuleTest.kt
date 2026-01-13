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

class MultipleOfBreakingSchemaValidationRuleTest : ValidationRuleTest() {
    private val rule = MultipleOfBreakingSchemaValidationRule()

    fun schemaProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Integer schema with multipleOf",
            IntegerSchema().multipleOf(BigDecimal.valueOf(5)),
            sequenceOf(BigDecimal.valueOf(4))
        ),
        Arguments.of(
            "Integer schema with multipleOf with minimum",
            IntegerSchema().multipleOf(BigDecimal.valueOf(5)).minimum(BigDecimal.valueOf(5)),
            sequenceOf(BigDecimal.valueOf(9))
        ),
        Arguments.of(
            "Number schema with multipleOf",
            NumberSchema().multipleOf(BigDecimal.valueOf(25, 1)),
            sequenceOf(BigDecimal.valueOf(125, 2))
        ),
        Arguments.of(
            "Number schema with high precision multipleOf (scale=9)",
            NumberSchema().multipleOf(BigDecimal("0.000000001")),
            sequenceOf(BigDecimal("0.0000000005"))
        ),
        Arguments.of(
            "Number schema with very large minimum and multipleOf",
            NumberSchema()
                .minimum(BigDecimal("999999999999999999.99"))
                .multipleOf(BigDecimal("0.01")),
            sequenceOf(BigDecimal("999999999999999999.995"))
        ),
        Arguments.of("Integer schema without multipleOf", IntegerSchema(), emptySequence<Any>()),
        Arguments.of(
            "String schema with multipleOf",
            StringSchema().multipleOf(BigDecimal.valueOf(5)),
            emptySequence<Any>()
        )
    )

    @ParameterizedTest
    @MethodSource("schemaProvider")
    @DisplayName("Multiple Of Breaking: apply() should return optional with invalid example when applicable")
    @Description("Verifies that the apply method returns a stream with an invalid example value when the rule is applicable or empty Stream")
    fun multipleOfBreakingApplyTest(scenario: String, schema: Schema<*>, expected: Sequence<Any>) {
        val result = step("Call apply") { rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI())) }
        assertThat(result.toList()).`is`(ruleAppliedTo(rule, expected))
    }
}


