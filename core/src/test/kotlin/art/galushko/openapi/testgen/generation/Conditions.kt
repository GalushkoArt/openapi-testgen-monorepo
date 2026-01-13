package art.galushko.openapi.testgen.generation

import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SchemaValidationRule
import org.assertj.core.api.Condition

/**
 * AssertJ helper conditions for validating collections of [RuleValue]s in tests.
 */
object Conditions {
    /**
     * Creates a condition that checks the list equals the expected sequence exactly.
     */
    @JvmStatic
    fun correctAppliedTo(expected: Sequence<RuleValue>): Condition<in List<RuleValue>> {
        val list = expected.toList()
        return Condition({ list == it }, "equal to %s", list)
    }

    /**
     * Creates a condition that checks a rule was applied to all expected values.
     */
    @JvmStatic
    fun ruleAppliedTo(rule: SchemaValidationRule, expected: Sequence<Any>): Condition<in List<RuleValue>> {
        val list = expected.toList()
        return Condition(
            { present -> list.map { value -> RuleValue(rule.getRuleName(), value) } == present },
            "containing values %s with %s description", list.toString(), rule.getRuleName()
        )
    }

    /**
     * Creates a condition that checks the present list contains and only contains expected values.
     */
    @JvmStatic
    fun ruleAppliedTo(expected: Sequence<RuleValue>): Condition<in List<RuleValue>> {
        val list = expected.toList()
        return Condition(
            { present -> list.containsAll(present) && list.size == present.size },
            "containing values %s", list.toString()
        )
    }
}


