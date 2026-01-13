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

class OutOfMinimumLengthStringSchemaValidationRuleTest : ValidationRuleTest() {
    private val rule = OutOfMinimumLengthStringSchemaValidationRule()

    fun schemaProvider(): Stream<Arguments> = Stream.of(
        Arguments.of("String schema with minLength", StringSchema().minLength(5), sequenceOf("aaaa")),
        Arguments.of("String schema with 1 minLength", StringSchema().minLength(1), sequenceOf("")),
        Arguments.of("String schema with 0 minLength", StringSchema().minLength(0), emptySequence<Any>()),
        Arguments.of("String schema without minLength", StringSchema(), emptySequence<Any>()),
        Arguments.of("Integer schema with minLength", IntegerSchema().minLength(5), emptySequence<Any>())
    )

    @ParameterizedTest
    @MethodSource("schemaProvider")
    @DisplayName("Out Of Minimum Length String: apply() should return optional with invalid example when applicable")
    @Description("Verifies that the apply method returns a stream with an invalid example value when the rule is applicable or empty Stream")
    fun outOfMinimumLengthStringApplyTest(scenario: String, schema: Schema<*>, expected: Sequence<Any>) {
        val result = step("Call apply") { rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI())) }
        assertThat(result.toList()).`is`(ruleAppliedTo(rule, expected))
    }
}


