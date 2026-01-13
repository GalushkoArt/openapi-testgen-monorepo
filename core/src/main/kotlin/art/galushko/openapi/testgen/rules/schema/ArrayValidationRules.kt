package art.galushko.openapi.testgen.rules.schema

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isArray
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import art.galushko.openapi.testgen.testdata.tryGetExample
import io.swagger.v3.oas.models.media.Schema
import kotlin.math.max

/**
 * Produces an array with fewer items than `minItems`.
 *
 * Inputs: array schema with `minItems > 0` and items schema.
 * Output: single [RuleValue] containing an array of size `minItems - 1`.
 * Constraints: returns empty when schema is not an array or lacks `minItems`/items.
 * Determinism: deterministic for identical schema/context.
 * Settings: example generation follows `TestGenerationSettings.exampleValues`.
 */
@Suppress("ComplexCondition")
internal class BelowMinItemsArraySchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Below Min Items Array"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        if (!(isArray(s) && s.minItems != null && s.minItems > 0 && s.items != null)) return emptySequence()
        val min = s.minItems
        val arr = tryGetExample(context, schema, s) {
            context.schemaExampleValueGenerator.getExampleArrayValues("array", s, context.openAPI).take(min - 1)
        }
        return listOf(RuleValue(getRuleName(), arr)).asSequence()
    }
}

/**
 * Produces an array with more items than `maxItems`.
 *
 * Inputs: array schema with `maxItems` and items schema.
 * Output: single [RuleValue] containing an array of size `maxItems + 1`.
 * Constraints: returns empty when schema is not an array or lacks `maxItems`/items.
 * Determinism: deterministic for identical schema/context.
 * Settings: example generation follows `TestGenerationSettings.exampleValues`.
 */
internal class AboveMaxItemsArraySchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Above Max Items Array"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        if (!(isArray(s) && s.maxItems != null && s.items != null)) return emptySequence()
        val max = s.maxItems
        val values = (0..<(max + 1)).map {
            val index = if (s.uniqueItems == true) it else 0
            tryGetExample(context, schema, s) {
                context.schemaExampleValueGenerator.getExampleValue("arrayItem", s.items, context.openAPI, index)
            }
        }
        return listOf(RuleValue(getRuleName(), values)).asSequence()
    }
}

/**
 * Produces an array with duplicate items when `uniqueItems = true`.
 *
 * Inputs: array schema with `uniqueItems = true` and items schema.
 * Output: single [RuleValue] containing a list with duplicated elements.
 * Constraints: returns empty when schema is not an array, lacks items, or `maxItems <= 1`.
 * Determinism: deterministic for identical schema/context.
 * Settings: example generation follows `TestGenerationSettings.exampleValues`.
 */
@Suppress("ComplexCondition")
internal class NonUniqueItemsArraySchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Non Unique Items Array"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        val singleItemArray = s.maxItems != null && s.maxItems <= 1
        if (!isArray(s) || s.uniqueItems != true || singleItemArray || s.items == null) return emptySequence()
        val values = (0..<max(2, schema.minItems ?: 2)).map {
            tryGetExample(context, schema, s) {
                context.schemaExampleValueGenerator.getExampleValue("arrayItem", s.items, context.openAPI, it)
            }
        }.toMutableList()
        values[1] = values[0]
        return listOf(RuleValue(getRuleName(), values.toList())).asSequence()
    }
}

