package art.galushko.openapi.testgen.rules.schema

import art.galushko.openapi.testgen.generation.Conditions.correctAppliedTo
import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.rules.ValidationRuleTest
import art.galushko.openapi.testgen.spi.RuleValue
import io.qameta.allure.Description
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Suppress("UNCHECKED_CAST")
class NullForRequiredPropertySchemaValidationRuleTest : ValidationRuleTest() {
    private val rule = NullForRequiredPropertySchemaValidationRule()

    private fun propertiesWith(prop2: Schema<*>): Map<String, Schema<*>> = mapOf(
        "prop1" to StringSchema().example("value1"),
        "prop2" to prop2
    )

    /** All "skipped" scenarios share the same expectation: only `prop1` is nulled out. */
    private fun skippedProp2Case(scenario: String, prop2: Schema<*>): Arguments = Arguments.of(
        scenario,
        ObjectSchema().properties(propertiesWith(prop2)).required(listOf("prop1", "prop2")),
        sequenceOf(
            RuleValue("Null For Required Property prop1", mapOf("prop1" to null, "prop2" to "value2"))
        )
    )

    fun schemaProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Object schema with required non-nullable properties",
            ObjectSchema()
                .properties(propertiesWith(StringSchema().example("value2")))
                .required(listOf("prop1", "prop2")),
            sequenceOf(
                RuleValue("Null For Required Property prop1", mapOf("prop1" to null, "prop2" to "value2")),
                RuleValue("Null For Required Property prop2", mapOf("prop1" to "value1", "prop2" to null))
            )
        ),
        skippedProp2Case(
            "Nullable (3.0) required property is skipped",
            StringSchema().example("value2").nullable(true)
        ),
        skippedProp2Case(
            "Nullable (3.1 type array) required property is skipped",
            StringSchema().example("value2").apply { types = setOf("string", "null") }
        ),
        skippedProp2Case(
            "Nullable via oneOf null-type branch (3.1) required property is skipped",
            Schema<Any>().example("value2").oneOf(listOf(StringSchema(), Schema<Any>().apply { types = setOf("null") }))
        ),
        skippedProp2Case(
            "Nullable via anyOf nullable branch (3.0) required property is skipped",
            Schema<Any>().example("value2").anyOf(listOf(StringSchema(), StringSchema().nullable(true)))
        ),
        skippedProp2Case(
            "Nullable via allOf nullable branch required property is skipped",
            Schema<Any>().example("value2").allOf(listOf(StringSchema().nullable(true)))
        ),
        Arguments.of(
            "Object schema without required properties",
            ObjectSchema().properties(propertiesWith(StringSchema().example("value2"))),
            emptySequence<RuleValue>()
        ),
        Arguments.of("Object schema with no properties", ObjectSchema(), emptySequence<RuleValue>()),
        Arguments.of("Non-object schema", StringSchema(), emptySequence<RuleValue>())
    )

    @ParameterizedTest
    @MethodSource("schemaProvider")
    @DisplayName("Null For Required Property: apply() should null out each required non-nullable property")
    @Description("Verifies that the apply method emits one object per required non-nullable property with that property set to null")
    fun nullForRequiredPropertyApplyTest(
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

    @Test
    @DisplayName("Null For Required Property: nullability declared behind a \$ref branch is honored")
    @Description("Verifies that a required property whose oneOf branch references a null-type component schema is skipped")
    fun nullableViaRefBranchIsSkippedTest() {
        // Arrange
        val openAPI = OpenAPI().components(
            Components().schemas(mapOf("NullType" to Schema<Any>().apply { types = setOf("null") }))
        )
        val schema = ObjectSchema().properties(
            mapOf(
                "prop1" to StringSchema().example("value1"),
                "prop2" to Schema<Any>().example("value2")
                    .oneOf(listOf(StringSchema(), Schema<Any>().`$ref`("#/components/schemas/NullType")))
            )
        ).required(listOf("prop1", "prop2"))

        // Act
        val result = step("Call apply") {
            rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), openAPI))
        }

        // Assert
        assertThat(result.toList()).`is`(
            correctAppliedTo(
                sequenceOf(
                    RuleValue("Null For Required Property prop1", mapOf("prop1" to null, "prop2" to "value2"))
                )
            )
        )
    }
}
