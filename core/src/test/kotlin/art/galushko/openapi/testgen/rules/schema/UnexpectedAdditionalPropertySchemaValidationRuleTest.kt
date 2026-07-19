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
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Suppress("UNCHECKED_CAST")
class UnexpectedAdditionalPropertySchemaValidationRuleTest : ValidationRuleTest() {
    private val rule = UnexpectedAdditionalPropertySchemaValidationRule()

    fun schemaProvider(): Stream<Arguments> {
        val properties = mapOf("prop1" to StringSchema().example("value1"))
        val collidingProperties = mapOf(
            "unexpectedProperty" to StringSchema().example("declared")
        )
        return Stream.of(
            Arguments.of(
                "Closed object schema (additionalProperties: false)",
                ObjectSchema().properties(properties).required(listOf("prop1")).additionalProperties(false),
                sequenceOf(
                    RuleValue(
                        "Unexpected Additional Property",
                        mapOf("prop1" to "value1", "unexpectedProperty" to "unexpected-additional-property-value")
                    )
                )
            ),
            Arguments.of(
                "Closed object schema without required properties",
                ObjectSchema().properties(properties).additionalProperties(false),
                sequenceOf(
                    RuleValue(
                        "Unexpected Additional Property",
                        mapOf("unexpectedProperty" to "unexpected-additional-property-value")
                    )
                )
            ),
            Arguments.of(
                "Extra property name avoids declared properties",
                ObjectSchema().properties(collidingProperties).required(listOf("unexpectedProperty")).additionalProperties(false),
                sequenceOf(
                    RuleValue(
                        "Unexpected Additional Property",
                        mapOf("unexpectedProperty" to "declared", "unexpectedPropertyX" to "unexpected-additional-property-value")
                    )
                )
            ),
            Arguments.of(
                "Open object schema (additionalProperties unset)",
                ObjectSchema().properties(properties),
                emptySequence<RuleValue>()
            ),
            Arguments.of(
                "Object schema explicitly allowing additional properties",
                ObjectSchema().properties(properties).additionalProperties(true),
                emptySequence<RuleValue>()
            ),
            Arguments.of(
                "Object schema with additionalProperties as a schema",
                ObjectSchema().properties(properties).additionalProperties(StringSchema()),
                emptySequence<RuleValue>()
            ),
            Arguments.of(
                "Non-object schema",
                StringSchema(),
                emptySequence<RuleValue>()
            )
        )
    }

    @ParameterizedTest
    @MethodSource("schemaProvider")
    @DisplayName("Unexpected Additional Property: apply() should add an undeclared property only for closed objects")
    @Description("Verifies that the apply method injects an undeclared property when additionalProperties is false and stays silent otherwise")
    fun unexpectedAdditionalPropertyApplyTest(
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
