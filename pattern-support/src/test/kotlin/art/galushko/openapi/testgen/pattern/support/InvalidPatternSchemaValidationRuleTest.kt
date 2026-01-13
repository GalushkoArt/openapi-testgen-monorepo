package art.galushko.openapi.testgen.pattern.support

import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.rules.ValidationRuleTest
import art.galushko.openapi.testgen.pattern.value.PatternValueGenerator
import io.qameta.allure.Description
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class InvalidPatternSchemaValidationRuleTest : ValidationRuleTest() {
    private val rule = InvalidPatternSchemaValidationRule(PatternValueGenerator())

    fun schemaProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "String schema with simple pattern",
            StringSchema().pattern("^[A-Z]{3}$"),
            true,
        ),
        Arguments.of(
            "String schema with alternation pattern",
            StringSchema().pattern("^(foo|bar|baz)$"),
            true,
        ),
        Arguments.of(
            "String schema without pattern",
            StringSchema(),
            false,
        ),
        Arguments.of(
            "Integer schema (not applicable)",
            IntegerSchema(),
            false,
        ),
        Arguments.of(
            "Integer schema with pattern (type mismatch)",
            IntegerSchema().pattern("^\\d+$"),
            false,
        ),
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("schemaProvider")
    @DisplayName("Invalid Pattern: apply() should return correct results based on schema")
    @Description(
        """
        Verifies that the apply method returns a sequence with an invalid pattern value when applicable.

        The rule should:
        - Generate a non-matching string for string schemas with a pattern
        - Return empty sequence for schemas without a pattern
        - Return empty sequence for non-string schemas
        """
    )
    fun invalidPatternApplyTest(scenario: String, schema: Schema<*>, shouldApply: Boolean) {
        val result = step("Call apply") {
            rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI()))
        }

        val resultList = result.toList()

        if (shouldApply) {
            assertThat(resultList)
                .`as`("Rule should produce at least one result for pattern schema")
                .isNotEmpty()
            assertThat(resultList.first().description.first)
                .`as`("Rule name should be 'Invalid Pattern'")
                .isEqualTo("Invalid Pattern")

            // Verify the generated value does NOT match the pattern
            val generatedValue = resultList.first().value as String
            val pattern = schema.pattern
            assertThat(Regex(pattern!!).containsMatchIn(generatedValue))
                .`as`("Generated value '$generatedValue' should NOT match pattern '$pattern'")
                .isFalse()
        } else {
            assertThat(resultList)
                .`as`("Rule should not apply to this schema")
                .isEmpty()
        }
    }

    @Test
    @DisplayName("should return empty sequence for patterns that match all strings")
    @Description("Verifies that patterns like .* gracefully return empty sequence")
    fun shouldReturnEmptyForCatchAllPatterns() {
        val schema = StringSchema().pattern("^.*$")

        val result = step("Call apply with catch-all pattern") {
            rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI()))
        }

        assertThat(result.toList())
            .`as`("Should return empty sequence for patterns that cannot produce non-matches")
            .isEmpty()
    }

    @Test
    @DisplayName("should use rule name 'Invalid Pattern'")
    @Description("Verifies the rule name is correctly set")
    fun shouldHaveCorrectRuleName() {
        assertThat(rule.getRuleName())
            .isEqualTo("Invalid Pattern")
    }

    @Test
    @DisplayName("should generate value with length constraints from schema")
    @Description("Verifies that minLength and maxLength from schema are passed to generator")
    fun shouldRespectSchemaLengthConstraints() {
        val schema = StringSchema()
            .pattern("^[a-z]+$")
            .minLength(5)
            .maxLength(10)

        val result = step("Call apply with length constraints") {
            rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI()))
        }

        val resultList = result.toList()
        assertThat(resultList).isNotEmpty()

        // The generated invalid value may or may not respect length constraints
        // but it must NOT match the pattern
        val generatedValue = resultList.first().value as String
        assertThat(Regex("^[a-z]+$").containsMatchIn(generatedValue))
            .`as`("Generated value should NOT match pattern")
            .isFalse()
    }

    @Test
    @DisplayName("should generate deterministic invalid values")
    @Description("Verifies that the rule produces consistent output for the same schema")
    fun shouldProduceDeterministicOutput() {
        val schema = StringSchema().pattern("^[A-Z]{3}$")

        val result1 = rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI())).toList()
        val result2 = rule.apply(schema, createTestContext(createBasicTestCase(), Operation(), OpenAPI())).toList()

        assertThat(result1)
            .`as`("Same schema should produce same invalid value")
            .isEqualTo(result2)
    }

    @Test
    @DisplayName("should dereference schema \$ref via OpenAPI components")
    fun shouldDereferenceSchemaRef() {
        val openAPI = OpenAPI().components(
            Components().schemas(
                mapOf(
                    "Phone" to StringSchema().pattern("^[A-Z]{3}$"),
                ),
            ),
        )
        val schemaRef = Schema<Any>().apply {
            `$ref` = "#/components/schemas/Phone"
        }

        val result = rule.apply(schemaRef, createTestContext(createBasicTestCase(), Operation(), openAPI)).toList()

        assertThat(result).isNotEmpty
        val generatedValue = result.first().value as String
        assertThat(Regex("^[A-Z]{3}$").matches(generatedValue)).isFalse()
    }
}


