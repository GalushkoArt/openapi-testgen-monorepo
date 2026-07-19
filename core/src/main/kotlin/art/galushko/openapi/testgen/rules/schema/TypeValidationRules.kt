package art.galushko.openapi.testgen.rules.schema

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isInteger
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import art.galushko.openapi.testgen.util.Consts.INT32_FORMAT
import art.galushko.openapi.testgen.util.Consts.INT64_FORMAT
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal

/**
 * Produces an invalid primitive type for integer/number/boolean schemas.
 *
 * Inputs: schema with type `integer`, `number`, or `boolean`.
 * Output: single [RuleValue] containing a value of the wrong type.
 * Constraints: returns empty for other schema types.
 * Determinism: deterministic for identical schema/context.
 * Settings: none.
 */
internal class InvalidTypeValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Invalid Type"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val deref = tryGetSchemaFromRef(schema, context.openAPI)
        val invalid = when (deref.type) {
            "integer", "number" -> "abc"
            "boolean" -> "not_boolean"
            else -> null
        }
        return if (invalid != null) listOf(RuleValue(getRuleName(), invalid)).asSequence() else emptySequence()
    }
}

/**
 * Produces a non-integer value for integer schemas.
 *
 * Inputs: integer schema.
 * Output: single [RuleValue] containing a decimal value.
 * Constraints: returns empty when schema is not an integer.
 * Determinism: deterministic for identical context.
 * Settings: invalid values come from `TestGenerationSettings.overrideBasicTestData`.
 */
internal class IntegerBreakingSchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Integer Breaking"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        if (!isInteger(s)) return emptySequence()
        return listOf(RuleValue(getRuleName(), BigDecimal(context.basicTestData.nonIntegerValue()))).asSequence()
    }
}

/**
 * Produces an out-of-range int32 value for integer schemas with `format = int32`.
 *
 * Inputs: integer schema with `format = int32`.
 * Output: single [RuleValue] containing an out-of-range integer value.
 * Constraints: returns empty when schema is not an int32 integer.
 * Determinism: deterministic for identical context.
 * Settings: invalid values come from `TestGenerationSettings.overrideBasicTestData`.
 */
internal class WrongInt32FormatSchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Wrong Int32 Format"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        if (!(isInteger(s) && s.format != null && INT32_FORMAT == s.format)) return emptySequence()
        return listOf(RuleValue(getRuleName(), BigDecimal(context.basicTestData.outOfInt32RangeValue()))).asSequence()
    }
}

/**
 * Produces an out-of-range int64 value for integer schemas with `format = int64`.
 *
 * Inputs: integer schema with `format = int64`.
 * Output: single [RuleValue] containing an out-of-range integer value.
 * Constraints: returns empty when schema is not an int64 integer.
 * Determinism: deterministic for identical context.
 * Settings: invalid values come from `TestGenerationSettings.overrideBasicTestData`.
 */
internal class WrongInt64FormatSchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Wrong Int64 Format"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        if (!(isInteger(s) && s.format != null && INT64_FORMAT == s.format)) return emptySequence()
        return listOf(RuleValue(getRuleName(), BigDecimal(context.basicTestData.outOfInt64RangeValue()))).asSequence()
    }
}

