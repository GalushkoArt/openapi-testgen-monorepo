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
class MissedRequiredObjectPropertiesSchemaValidationRuleTest : ValidationRuleTest() {
    private val rule = MissedRequiredObjectPropertiesSchemaValidationRule()

    fun schemaProvider(): Stream<Arguments> {
        val properties = mapOf(
            "prop1" to StringSchema().example("value1"),
            "prop2" to StringSchema().example("value2")
        )
        return Stream.of(
            Arguments.of(
                "Object schema with required properties",
                ObjectSchema().properties(properties).required(listOf("prop1", "prop2")),
                sequenceOf(
                    RuleValue("Missed Required Object Properties prop1", mapOf("prop2" to "value2")),
                    RuleValue("Missed Required Object Properties prop2", mapOf("prop1" to "value1"))
                )
            ),
            Arguments.of(
                "Object schema without required properties",
                ObjectSchema().properties(properties),
                emptySequence<RuleValue>()
            ),
            Arguments.of(
                "Object schema with no properties",
                ObjectSchema(),
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
    @DisplayName("Missed Required Object Properties: apply() should return stream with invalid examples when applicable")
    @Description("Verifies that the apply method returns a stream with invalid examples for each required property or empty Stream")
    fun missedRequiredObjectPropertiesApplyTest(
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
