package art.galushko.openapi.testgen.rules.schema

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import art.galushko.openapi.testgen.testdata.BasicTestDataProvider
import art.galushko.openapi.testgen.util.Consts.DATE_FORMAT
import io.swagger.v3.oas.models.media.Schema

/**
 * Base class for date-format validation rules (`format = date`).
 *
 * Inputs: schema with `format = date`.
 * Output: subclasses emit [RuleValue] entries with invalid date strings.
 * Constraints: applies only to schemas tagged with the date format.
 * Determinism: deterministic given the subclass value provider.
 * Settings: invalid values come from `TestGenerationSettings.overrideBasicTestData` via [BasicTestDataProvider].
 */
internal abstract class WrongDateSchemaValidationRule : SimpleSchemaValidationRule {
    protected fun isApplicable(schema: Schema<*>): Boolean = schema.format != null && DATE_FORMAT == schema.format
}

/**
 * Parameterized rule for date format violations.
 *
 * Inputs: schema with `format = date` and [TestGenerationContext].
 * Output: single [RuleValue] with an invalid date value.
 * Constraints: returns empty when the schema format is not `date`.
 * Determinism: deterministic for identical context and value provider.
 * Settings: invalid values come from `TestGenerationSettings.overrideBasicTestData` via [BasicTestDataProvider].
 *
 * @param ruleName human-readable name for the rule
 * @param valueProvider function that extracts the invalid date value from [BasicTestDataProvider]
 */
internal class DateSchemaValidationRule(
    private val ruleName: String,
    private val valueProvider: (BasicTestDataProvider) -> String
) : WrongDateSchemaValidationRule() {

    override fun getRuleName(): String = ruleName

    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        return if (isApplicable(s)) {
            sequenceOf(RuleValue(ruleName, valueProvider(context.basicTestData)))
        } else {
            emptySequence()
        }
    }
}
