package art.galushko.openapi.testgen.rules.schema

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isNumber
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Produces an invalid number below `minimum` for numeric schemas.
 *
 * Inputs: numeric schema with `minimum` and optional `exclusiveMinimum`/`multipleOf`.
 * Output: single [RuleValue] with a value outside the lower bound.
 * Constraints: returns empty when schema is not numeric or lacks `minimum`.
 * Determinism: deterministic for identical schema/context.
 * Settings: none.
 */
@Suppress("DuplicatedCode")
internal class OutOfMinimumBoundaryNumberSchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Out Of Minimum Boundary Number"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        if (s.minimum == null || !isNumber(s)) return emptySequence()
        val exclusive = s.exclusiveMinimum == true
        val multiple = s.multipleOf ?: BigDecimal.ONE
        val value = if (exclusive) s.minimum else s.minimum.subtract(multiple)
        return listOf(RuleValue(getRuleName(), value)).asSequence()
    }
}

/**
 * Produces an invalid number above `maximum` for numeric schemas.
 *
 * Inputs: numeric schema with `maximum` and optional `exclusiveMaximum`/`multipleOf`.
 * Output: single [RuleValue] with a value outside the upper bound.
 * Constraints: returns empty when schema is not numeric or lacks `maximum`.
 * Determinism: deterministic for identical schema/context.
 * Settings: none.
 */
@Suppress("DuplicatedCode")
internal class OutOfMaximumBoundaryNumberSchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Out Of Maximum Boundary Number"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        if (s.maximum == null || !isNumber(s)) return emptySequence()
        val exclusive = s.exclusiveMaximum == true
        val multiple = s.multipleOf ?: BigDecimal.ONE
        val value = if (exclusive) s.maximum else s.maximum.add(multiple)
        return listOf(RuleValue(getRuleName(), value)).asSequence()
    }
}

/**
 * Produces an invalid number that violates the `multipleOf` constraint.
 *
 * Inputs: numeric schema with `multipleOf` and optional `minimum`.
 * Output: single [RuleValue] with a value that is not a multiple of the constraint.
 * Constraints: returns empty when schema is not numeric or lacks `multipleOf`.
 * Determinism: deterministic for identical schema/context.
 * Settings: none.
 */
internal class MultipleOfBreakingSchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "MultipleOf Breaking"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        if (s.multipleOf == null || !isNumber(s)) return emptySequence()
        val minimum = s.minimum ?: BigDecimal.ZERO
        val multipleOf = s.multipleOf
        val value = if (multipleOf.scale() <= 0 && multipleOf > BigDecimal.ONE) {
            multipleOf.subtract(BigDecimal.ONE).add(minimum)
        } else {
            multipleOf.divide(BigDecimal(2), multipleOf.scale() + 1, RoundingMode.HALF_DOWN).add(minimum)
        }
        return listOf(RuleValue(getRuleName(), value)).asSequence()
    }
}

