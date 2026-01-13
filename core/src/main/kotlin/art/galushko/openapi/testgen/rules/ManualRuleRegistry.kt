package art.galushko.openapi.testgen.rules

import art.galushko.openapi.testgen.spi.AuthValidationRule
import art.galushko.openapi.testgen.spi.RuleRegistry
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import org.slf4j.LoggerFactory

/**
 * Manual [RuleRegistry] implementation with explicit rule wiring and deterministic ordering.
 *
 * Inputs: built-in rules from [BuiltInRules] and any additional rules supplied to the constructor.
 * Output: ordered list of rules for the requested type with ignored class names filtered out.
 * Constraints: unknown rule classes or unknown ignore entries are logged as warnings.
 * Determinism: rules are sorted by class name for stable output.
 * Settings: respects `TestGenerationSettings.ignoreSchemaValidationRules` and `ignoreAuthValidationRules` at wiring time.
 *
 * @param extraSimpleSchemaRules additional simple schema validation rules to register
 * @param extraAuthRules additional auth validation rules to register
 */
public class ManualRuleRegistry(
    private val extraSimpleSchemaRules: List<SimpleSchemaValidationRule> = emptyList(),
    private val extraAuthRules: List<AuthValidationRule> = emptyList(),
) : RuleRegistry {

    private val log = LoggerFactory.getLogger(ManualRuleRegistry::class.java)

    override fun <T : Any> getRules(
        ruleClass: Class<T>,
        ignoredClassNames: Set<String>,
    ): List<T> {
        val rules = getRulesForClass(ruleClass)
        validateIgnoredRules(ignoredClassNames, rules)
        return filterIgnoredClasses(rules, ignoredClassNames)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getRulesForClass(ruleClass: Class<T>): List<T> = when (ruleClass) {
        SimpleSchemaValidationRule::class.java ->
            (BuiltInRules.simpleSchemaValidationRules() + extraSimpleSchemaRules)
                .sortedBy { it::class.java.name } as List<T>

        AuthValidationRule::class.java ->
            (BuiltInRules.authValidationRules() + extraAuthRules)
                .sortedBy { it::class.java.name } as List<T>

        else -> {
            log.warn("Unknown rule class requested: {}", ruleClass.name)
            emptyList()
        }
    }

    private fun <T : Any> validateIgnoredRules(
        ignoredRuleClassNames: Set<String>,
        rules: List<T>,
    ) {
        if (ignoredRuleClassNames.isEmpty()) return

        val ruleNames = rules.mapTo(HashSet()) { it::class.java.name }
        ignoredRuleClassNames
            .filter { it !in ruleNames }
            .forEach { missing ->
                log.warn("No such rule to ignore: {}", missing)
            }
    }

    private fun <T : Any> filterIgnoredClasses(
        rules: List<T>,
        ignoredRuleClassNames: Set<String>,
    ): List<T> = if (ignoredRuleClassNames.isEmpty()) {
        rules
    } else {
        rules.filter { it::class.java.name !in ignoredRuleClassNames }
    }
}
