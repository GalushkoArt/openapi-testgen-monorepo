package art.galushko.openapi.testgen.rules.schema

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import art.galushko.openapi.testgen.testdata.BasicTestDataProvider
import art.galushko.openapi.testgen.util.Consts.DATE_TIME_FORMAT
import io.swagger.v3.oas.models.media.Schema

/**
 * Base class for date-time format validation rules (`format = date-time`).
 *
 * Inputs: schema with `format = date-time`.
 * Output: subclasses emit [RuleValue] entries with invalid date-time strings.
 * Constraints: applies only to schemas tagged with the date-time format.
 * Determinism: deterministic given the subclass value provider.
 * Settings: invalid values come from `TestGenerationSettings.overrideBasicTestData` via [BasicTestDataProvider].
 */
internal abstract class WrongDateTimeSchemaValidationRule : SimpleSchemaValidationRule {
    protected fun isApplicable(schema: Schema<*>): Boolean = schema.format != null && DATE_TIME_FORMAT == schema.format
}

/**
 * Parameterized rule for date-time format violations.
 *
 * Inputs: schema with `format = date-time` and [TestGenerationContext].
 * Output: single [RuleValue] with an invalid date-time value.
 * Constraints: returns empty when the schema format is not `date-time`.
 * Determinism: deterministic for identical context and value provider.
 * Settings: invalid values come from `TestGenerationSettings.overrideBasicTestData` via [BasicTestDataProvider].
 *
 * @param ruleName human-readable name for the rule
 * @param valueProvider function that extracts the invalid date-time value from [BasicTestDataProvider]
 */
internal class DateTimeSchemaValidationRule(
    private val ruleName: String,
    private val valueProvider: (BasicTestDataProvider) -> String
) : WrongDateTimeSchemaValidationRule() {

    override fun getRuleName(): String = ruleName

    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val s = tryGetSchemaFromRef(schema, context.openAPI)
        return if (isApplicable(s)) {
            listOf(RuleValue(ruleName, valueProvider(context.basicTestData))).asSequence()
        } else {
            emptySequence()
        }
    }
}
