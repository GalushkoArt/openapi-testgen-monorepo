package art.galushko.openapi.testgen.rules.schema

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isString
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import art.galushko.openapi.testgen.util.Consts.EMAIL_FORMAT
import art.galushko.openapi.testgen.util.Consts.UUID_FORMAT
import io.swagger.v3.oas.models.media.Schema

/**
 * Produces a string shorter than `minLength`.
 *
 * Inputs: string schema with `minLength > 0`.
 * Output: single [RuleValue] containing a string of length `minLength - 1`.
 * Constraints: returns empty when schema is not a string or lacks `minLength`.
 * Determinism: deterministic for identical schema/context.
 * Settings: none.
 */
internal class OutOfMinimumLengthStringSchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Out Of Minimum Length String"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        if (s.minLength == null || s.minLength <= 0 || !isString(s)) return emptySequence()
        val below = s.minLength - 1
        return listOf(RuleValue(getRuleName(), "a".repeat(below))).asSequence()
    }
}

/**
 * Produces a string longer than `maxLength`.
 *
 * Inputs: string schema with `maxLength`.
 * Output: single [RuleValue] containing a string of length `maxLength + 1`.
 * Constraints: returns empty when schema is not a string or lacks `maxLength`.
 * Determinism: deterministic for identical schema/context.
 * Settings: none.
 */
internal class OutOfMaximumLengthStringSchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Out Of Maximum Length String"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        if (s.maxLength == null || !isString(s)) return emptySequence()
        val above = s.maxLength + 1
        val value: String? = if (above > 0) "a".repeat(above) else null
        return value?.let { listOf(RuleValue(getRuleName(), it)).asSequence() } ?: emptySequence()
    }
}

/**
 * Produces an invalid UUID when `format = uuid`.
 *
 * Inputs: string schema with `format = uuid`.
 * Output: single [RuleValue] containing an invalid UUID value.
 * Constraints: returns empty when format is not `uuid`.
 * Determinism: deterministic for identical context.
 * Settings: invalid values come from `TestGenerationSettings.overrideBasicTestData`.
 */
internal class WrongUuidFormatSchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Wrong UUID Format"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        return if (s.format != null && UUID_FORMAT == s.format) listOf(
            RuleValue(
                getRuleName(),
                context.basicTestData.invalidUuidValue()
            )
        ).asSequence() else emptySequence()
    }
}

/**
 * Produces an invalid email when `format = email`.
 *
 * Inputs: string schema with `format = email`.
 * Output: single [RuleValue] containing an invalid email value.
 * Constraints: returns empty when format is not `email`.
 * Determinism: deterministic for identical context.
 * Settings: invalid values come from `TestGenerationSettings.overrideBasicTestData`.
 */
internal class WrongEmailFormatSchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Wrong Email Format"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        return if (s.format != null && EMAIL_FORMAT == s.format) {
            listOf(
                RuleValue(
                    getRuleName(),
                    context.basicTestData.invalidEmailValue()
                )
            ).asSequence()
        } else emptySequence()
    }
}

