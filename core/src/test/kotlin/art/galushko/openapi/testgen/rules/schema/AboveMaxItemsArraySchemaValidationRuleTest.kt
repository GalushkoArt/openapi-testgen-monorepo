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

class AboveMaxItemsArraySchemaValidationRuleTest : ValidationRuleTest() {
    private val rule = AboveMaxItemsArraySchemaValidationRule()

    fun schemaProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Array schema with maxItems = 1",
            ArraySchema().items(StringSchema().example("item")).maxItems(1),
            sequenceOf(listOf("item", "item"))
        ),
        Arguments.of(
            "Array schema with maxItems = 0",
            ArraySchema().items(StringSchema().example("item")).maxItems(0),
            sequenceOf(listOf("item"))
        ),
        Arguments.of(
            "Array schema without maxItems",
            ArraySchema().items(StringSchema().example("item")),
            emptySequence<Any>()
        ),
        Arguments.of("Non-array schema with maxItems", StringSchema().maxItems(3), emptySequence<Any>())
    )


    @ParameterizedTest
    @MethodSource("schemaProvider")
    @DisplayName("Above Max Items Array: apply() should return stream with invalid example when applicable")
    @Description("Verifies that the apply method returns a stream with an invalid example value when the rule is applicable or empty Stream")
    fun aboveMaxItemsArrayApplyTest(scenario: String, schema: Schema<*>, expected: Sequence<Any>) {
        val result = step("Call apply") { rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI())) }
        assertThat(result.toList()).`is`(ruleAppliedTo(rule, expected))
    }
}


