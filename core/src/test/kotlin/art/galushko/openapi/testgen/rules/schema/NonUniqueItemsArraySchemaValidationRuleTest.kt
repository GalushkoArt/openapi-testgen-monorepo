package art.galushko.openapi.testgen.rules.schema

import art.galushko.openapi.testgen.generation.Conditions.ruleAppliedTo
import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.rules.ValidationRuleTest
import io.qameta.allure.Description
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class NonUniqueItemsArraySchemaValidationRuleTest : ValidationRuleTest() {
    private val rule = NonUniqueItemsArraySchemaValidationRule()

    fun schemaProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Array schema with uniqueItems=true and maxItems=3",
            ArraySchema().items(StringSchema().example("item")).uniqueItems(true).maxItems(3),
            sequenceOf(listOf("item", "item"))
        ),
        Arguments.of(
            "Array schema with uniqueItems=true and no maxItems",
            ArraySchema().items(StringSchema().example("item")).uniqueItems(true),
            sequenceOf(listOf("item", "item"))
        ),
        Arguments.of(
            "Array schema with uniqueItems=true and maxItems=1",
            ArraySchema().items(StringSchema().example("item")).uniqueItems(true).maxItems(1),
            emptySequence<Any>()
        ),
        Arguments.of(
            "Array schema with uniqueItems=false",
            ArraySchema().items(StringSchema().example("item")).uniqueItems(false),
            emptySequence<Any>()
        ),
        Arguments.of(
            "Array schema without uniqueItems",
            ArraySchema().items(StringSchema().example("item")),
            emptySequence<Any>()
        ),
        Arguments.of(
            "Non-array schema with uniqueItems",
            StringSchema().uniqueItems(true),
            emptySequence<Any>()
        )
    )

    @ParameterizedTest
    @MethodSource("schemaProvider")
    @DisplayName("Non Unique Items Array: apply() should return stream with invalid example when applicable")
    @Description("Verifies that the apply method returns a stream with an invalid example value when the rule is applicable or empty Stream")
    fun nonUniqueItemsArrayApplyTest(scenario: String, schema: Schema<*>, expected: Sequence<Any>) {
        // Act
        val result = step("Call apply") {
            rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI()))
        }

        // Assert
        assertThat(result.toList()).`is`(ruleAppliedTo(rule, expected))
    }
}
