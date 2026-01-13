package art.galushko.openapi.testgen.rules.composed

import art.galushko.openapi.testgen.generation.Conditions.ruleAppliedTo
import art.galushko.openapi.testgen.generation.TestGeneratorConfigurer
import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.rules.ValidationRuleTest
import art.galushko.openapi.testgen.spi.RuleValue
import io.qameta.allure.Description
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.*
import java.util.stream.Stream

class ObjectItemSchemaValidationRuleTest : ValidationRuleTest() {
    private val rule: ObjectItemSchemaValidationRule = TestGeneratorConfigurer.getSchemaValidationRules()
        .firstOrNull { it is ObjectItemSchemaValidationRule } as? ObjectItemSchemaValidationRule
        ?: error("ObjectItemSchemaValidationRule not found")

    @Suppress("LongMethod")
    fun schemaProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Nested Object schema with enum in element of Array schema in property of Object schema",
            ObjectSchema().properties(
                mapOf(
                    "prop" to ArraySchema().items(
                        ObjectSchema().properties(
                            mapOf("prop2" to StringSchema()._enum(listOf("enum_1")))
                        )
                    )
                )
            ),
            sequenceOf(
                RuleValue(
                    ArrayDeque(
                        listOf(
                            "Object Property prop ",
                            "Array Item ",
                            "Object Property prop2 ",
                            "Invalid Enum Value"
                        )
                    ),
                    mapOf("prop" to listOf(mapOf("prop2" to "invalid_enum1")))
                )
            )
        ),
        Arguments.of(
            "Object schema with required properties",
            ObjectSchema().properties(
                mapOf(
                    "prop" to StringSchema().minLength(3).example("value"),
                    "prop2" to NumberSchema().example(BigDecimal.TEN)
                )
            ).required(listOf("prop2")),
            sequenceOf(
                RuleValue(
                    ArrayDeque(listOf("Object Property prop2 ", "Invalid Type")),
                    mapOf("prop2" to "abc")
                ),
                RuleValue(
                    ArrayDeque(listOf("Object Property prop ", "Out Of Minimum Length String")),
                    mapOf("prop" to "aa", "prop2" to BigDecimal.TEN)
                )
            )
        ),
        Arguments.of(
            "Non-object schema",
            StringSchema().example("value"),
            emptySequence<Any>()
        ),
        Arguments.of(
            "Object schema with no properties",
            ObjectSchema(),
            emptySequence<Any>()
        ),
        Arguments.of(
            "Object schema with empty properties",
            ObjectSchema().properties(mapOf()),
            emptySequence<Any>()
        )
    )

    @ParameterizedTest
    @MethodSource("schemaProvider")
    @DisplayName("Object Item Schema: apply() should return stream with invalid example when applicable")
    @Description("Verifies that the apply method returns a stream with an invalid example value " +
        "when the rule is applicable to object properties or empty Stream")
    fun objectItemSchemaApplyTest(scenario: String, schema: Schema<*>, expected: Sequence<RuleValue>) {
        val resultList = step("Call apply") { rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI())).toList() }
        assertThat(resultList).`is`(ruleAppliedTo(expected))
    }
}


