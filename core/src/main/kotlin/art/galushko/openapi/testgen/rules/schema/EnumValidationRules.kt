@file:Suppress("Filename", "MatchingDeclarationName")

package art.galushko.openapi.testgen.rules.schema

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import io.swagger.v3.oas.models.media.Schema

/**
 * Produces a value not present in the enum list.
 *
 * Inputs: schema with a non-empty `enum`.
 * Output: single [RuleValue] containing an invalid enum value.
 * Constraints: returns empty when the enum is missing or empty.
 * Determinism: deterministic for identical context.
 * Settings: invalid values come from `TestGenerationSettings.overrideBasicTestData`.
 */
internal class InvalidEnumValueSchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Invalid Enum Value"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        return if (s.enum != null && s.enum.isNotEmpty()) listOf(
            RuleValue(
                getRuleName(),
                context.basicTestData.invalidEnumValue()
            )
        ).asSequence() else emptySequence()
    }
}

