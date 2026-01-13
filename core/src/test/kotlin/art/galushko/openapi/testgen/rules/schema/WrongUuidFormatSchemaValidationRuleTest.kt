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

class WrongUuidFormatSchemaValidationRuleTest : ValidationRuleTest() {
    private val rule = WrongUuidFormatSchemaValidationRule()

    fun schemaProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "String schema with UUID format",
            StringSchema().format("uuid"),
            sequenceOf("8e258b27-c787-49ef-9539-11461b251ffg")
        ),
        Arguments.of("String schema without format", StringSchema(), emptySequence<Any>()),
        Arguments.of("String schema with different format", StringSchema().format("email"), emptySequence<Any>()),
        Arguments.of(
            "Integer schema with UUID format",
            IntegerSchema().format("uuid"),
            sequenceOf("8e258b27-c787-49ef-9539-11461b251ffg")
        )
    )

    @ParameterizedTest
    @MethodSource("schemaProvider")
    @DisplayName("Wrong UUID Format: apply() should return optional with invalid example when applicable")
    @Description("""
        Verifies that the apply method returns a stream with an invalid UUID value when the rule is applicable.

        RFC 4122 Compliance:
        Valid UUID format: 8-4-4-4-12 hexadecimal digits (e.g., 550e8400-e29b-41d4-a716-446655440000)
        The test value '8e258b27-c787-49ef-9539-11461b251ffg' violates RFC 4122 by using 'g' (non-hex character).

        OpenAPI Spec: format: uuid maps to RFC 4122
        Reference: https://datatracker.ietf.org/doc/html/rfc4122
        OpenAPI: https://spec.openapis.org/oas/v3.0.3#data-types
    """)
    fun wrongUuidFormatApplyTest(scenario: String, schema: Schema<*>, expected: Sequence<Any>) {
        val result = step("Call apply") { rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI())) }
        assertThat(result.toList()).`is`(ruleAppliedTo(rule, expected))
    }
}


