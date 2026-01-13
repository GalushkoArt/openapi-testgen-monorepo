@file:Suppress("Filename", "MatchingDeclarationName")

package art.galushko.openapi.testgen.rules.schema

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isObject
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import art.galushko.openapi.testgen.testdata.tryGetExample
import art.galushko.openapi.testgen.util.remove
import io.swagger.v3.oas.models.media.Schema

/**
 * Produces objects missing one required property at a time.
 *
 * Inputs: object schema with `required` and `properties`, plus [TestGenerationContext].
 * Output: [RuleValue] sequence where each value omits a single required property; the description includes the property name.
 * Constraints: returns empty when schema is not an object or lacks required properties.
 * Determinism: deterministic for identical schema/context.
 * Settings: example generation follows `TestGenerationSettings.exampleValues`.
 */
internal class MissedRequiredObjectPropertiesSchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Missed Required Object Properties "
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val deref = tryGetSchemaFromRef(schema, context.openAPI)
        if (!isObject(deref) || deref.properties.isNullOrEmpty() || deref.required.isNullOrEmpty()) return emptySequence()
        val exampleObject = tryGetExample(context, schema, deref) {
            context.schemaExampleValueGenerator.getExampleObject("object", schema, context.openAPI)
        }
        return deref.required.asSequence().map { required: String ->
            RuleValue(getRuleName() + required, exampleObject.remove(required))
        }
    }
}

