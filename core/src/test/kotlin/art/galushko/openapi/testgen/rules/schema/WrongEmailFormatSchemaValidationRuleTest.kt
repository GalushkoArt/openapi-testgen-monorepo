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

class WrongEmailFormatSchemaValidationRuleTest : ValidationRuleTest() {
    private val rule = WrongEmailFormatSchemaValidationRule()

    fun schemaProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "String schema with email format",
            StringSchema().format("email"),
            sequenceOf("invalid.email@example")
        ),
        Arguments.of("String schema without format", StringSchema(), emptySequence<Any>()),
        Arguments.of("String schema with different format", StringSchema().format("uuid"), emptySequence<Any>()),
        Arguments.of(
            "Integer schema with email format",
            IntegerSchema().format("email"),
            sequenceOf("invalid.email@example")
        )
    )

    @ParameterizedTest
    @MethodSource("schemaProvider")
    @DisplayName("Wrong Email Format: apply() should return optional with invalid example when applicable")
    @Description("""
        Verifies that the apply method returns a stream with an invalid email value when the rule is applicable.

        RFC 5322 Compliance:
        Valid email format: local-part@domain (e.g., user@example.com)
        The test value 'invalid.email@example' violates RFC 5322 by missing a valid top-level domain.

        Per RFC 5322, a valid email must have:
        - Local part (before @)
        - @ symbol
        - Domain with at least one dot and valid TLD

        OpenAPI Spec: format: email maps to RFC 5322
        Reference: https://datatracker.ietf.org/doc/html/rfc5322#section-3.4.1
        OpenAPI: https://spec.openapis.org/oas/v3.0.3#data-types
    """)
    fun wrongEmailFormatApplyTest(scenario: String, schema: Schema<*>, expected: Sequence<Any>) {
        val result = step("Call apply") { rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI())) }
        assertThat(result.toList()).`is`(ruleAppliedTo(rule, expected))
    }
}


