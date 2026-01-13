package art.galushko.openapi.testgen.rules.composed

import art.galushko.openapi.testgen.generation.Conditions.ruleAppliedTo
import art.galushko.openapi.testgen.generation.TestGeneratorConfigurer
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.rules.ValidationRuleTest
import art.galushko.openapi.testgen.spi.RuleValue
import io.qameta.allure.Description
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.*
import java.util.stream.Stream

class ArrayItemSchemaValidationRuleTest : ValidationRuleTest() {
    private val rule: ArrayItemSchemaValidationRule = TestGeneratorConfigurer.getSchemaValidationRules()
        .firstOrNull { it is ArrayItemSchemaValidationRule } as? ArrayItemSchemaValidationRule
        ?: error("ArrayItemSchemaValidationRule not found")

    fun schemaProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Nested Array schema with enum and minItems in property of Object schema in element of Array schema",
            ArraySchema().items(
                ObjectSchema().properties(
                    mapOf(
                        "prop" to ArraySchema().items(
                            StringSchema()._enum(listOf("enum_1"))
                        ).minItems(1)
                    )
                )
            ),
            sequenceOf(
                RuleValue(
                    ArrayDeque(listOf("Array Item ", "Object Property prop ", "Below Min Items Array")),
                    listOf(mapOf("prop" to listOf<Any>()))
                ),
                RuleValue(
                    ArrayDeque(listOf("Array Item ", "Object Property prop ", "Array Item ", "Invalid Enum Value")),
                    listOf(mapOf("prop" to listOf("invalid_enum1")))
                )
            )
        ),
        Arguments.of(
            "Array schema with string items having minLength with minItems",
            ArraySchema().items(StringSchema().minLength(3).example("item")).minItems(3),
            sequenceOf(
                RuleValue(
                    ArrayDeque(listOf("Array Item ", "Out Of Minimum Length String")),
                    listOf("aa", "item", "item")
                )
            )
        ),
        Arguments.of(
            "Array schema without item constraints",
            ArraySchema().items(StringSchema().example("item")),
            emptySequence<Any>()
        ),
        Arguments.of(
            "Non-array schema",
            StringSchema().example("item"),
            emptySequence<Any>()
        ),
        Arguments.of(
            "Array schema with maxItems = 0",
            ArraySchema().items(StringSchema().minLength(3).example("item")).maxItems(0),
            emptySequence<Any>()
        ),
        Arguments.of(
            "Array schema with null items",
            ArraySchema(),
            emptySequence<Any>()
        )
    )

    @ParameterizedTest
    @MethodSource("schemaProvider")
    @DisplayName("Array Item Schema: apply() should return stream with invalid example when applicable")
    @Description("Verifies that the apply method returns a stream with an invalid example value when the rule is applicable to array items or empty Stream")
    fun arrayItemSchemaApplyTest(scenario: String, schema: Schema<*>, expected: Sequence<RuleValue>) {
        val result = step("Call apply") { rule.apply(schema, createTestContext()) }
        assertThat(result.toList()).`is`(ruleAppliedTo(expected))
    }
}


