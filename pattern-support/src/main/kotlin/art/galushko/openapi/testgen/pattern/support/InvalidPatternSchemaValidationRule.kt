package art.galushko.openapi.testgen.pattern.support

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers
import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import art.galushko.openapi.testgen.pattern.value.PatternValueGenerator
import io.swagger.v3.oas.models.media.Schema
import org.slf4j.LoggerFactory

/**
 * Produces a string that does NOT match the schema pattern.
 *
 * Applies to string schemas with `pattern`. Returns a single [RuleValue] with rule name
 * "Invalid Pattern" when generation succeeds.
 *
 * Uses the regexp-gen library to generate non-matching strings. If generation fails
 * (pattern accepts all strings, unsupported regex features), logs an error and returns an
 * empty sequence (skips the rule).
 *
 * ## Library Limitations (from regexp-gen)
 * - Negative lookahead/lookbehind not supported (`(?!...)`, `(?<!...)`)
 * - Word boundary assertions not supported (`\b`, `\B`)
 * - Backreferences not supported
 * - Some patterns may not produce non-matching strings (e.g., `.*`)
 */
internal class InvalidPatternSchemaValidationRule(
    private val patternValueGenerator: PatternValueGenerator,
) : SimpleSchemaValidationRule {

    private val log = LoggerFactory.getLogger(InvalidPatternSchemaValidationRule::class.java)

    override fun getRuleName(): String = "Invalid Pattern"

    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = SchemaTypeHelpers.tryGetSchemaFromRef(schema, context.openAPI)
        if (s.pattern == null || !SchemaTypeHelpers.isString(s)) return emptySequence()

        val invalidValue = patternValueGenerator.generateInvalidValue(
            pattern = s.pattern,
            minLength = s.minLength,
            maxLength = s.maxLength,
        )

        if (invalidValue == null) {
            log.error("Failed to generate invalid value for pattern '{}' - skipping rule", s.pattern)
            return emptySequence()
        }

        return listOf(RuleValue(getRuleName(), invalidValue)).asSequence()
    }
}


