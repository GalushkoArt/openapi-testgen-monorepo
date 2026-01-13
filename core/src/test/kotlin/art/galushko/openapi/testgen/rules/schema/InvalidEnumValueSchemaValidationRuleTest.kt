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
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class InvalidEnumValueSchemaValidationRuleTest : ValidationRuleTest() {
    private val rule = InvalidEnumValueSchemaValidationRule()

    fun schemaProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "String schema with enum values",
            StringSchema()._enum(listOf("value1", "value2", "value3")),
            sequenceOf("invalid_enum1")
        ),
        Arguments.of(
            "Integer schema with enum values",
            IntegerSchema()._enum(listOf(1, 2, 3)),
            sequenceOf("invalid_enum1")
        ),
        Arguments.of("String schema without enum values", StringSchema(), emptySequence<Any>()),
        Arguments.of("String schema with empty enum values", StringSchema()._enum(listOf()), emptySequence<Any>())
    )

    @ParameterizedTest
    @MethodSource("schemaProvider")
    @DisplayName("Invalid Enum Value: apply() should return optional with invalid example when applicable")
    @Description("Verifies that the apply method returns a stream with an invalid example value when the rule is applicable or empty Stream")
    fun invalidEnumValueApplyTest(scenario: String, schema: Schema<*>, expected: Sequence<Any>) {
        val result = step("Call apply") { rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI())) }
        assertThat(result.toList()).`is`(ruleAppliedTo(rule, expected))
    }
}


