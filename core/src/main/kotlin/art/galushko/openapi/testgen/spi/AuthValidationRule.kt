package art.galushko.openapi.testgen.spi

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase

/**
 * Defines a contract for authentication-related validation rules that produce negative test cases.
 *
 * Unlike [SchemaValidationRule] which returns low-level [RuleValue] objects, auth rules
 * return complete [TestCase] objects directly because auth validation requires changes across
 * headers, cookies, query params, and security values.
 *
 * Determinism: rule output order must be stable for identical inputs.
 */
public interface AuthValidationRule {
    /**
     * Provides a short, human-readable rule name used for identification and reporting.
     *
     * This method provides API consistency with [SchemaValidationRule.getRuleName].
     *
     * @return the rule name (e.g., "Missing Security Values", "Invalid Security Values")
     */
    public fun getRuleName(): String

    /**
     * Decides whether this rule is applicable to the provided [context].
     *
     * @param context with a valid test case, operation specification and OpenAPI document
     * @return true if the rule should be applied, false otherwise
     */
    public fun decide(context: TestGenerationContext): Boolean

    /**
     * Applies the rule to the given [context] with a valid case producing one or more derived negative test cases.
     *
     * Behavior must not mutate [context].
     *
     * @param context with a valid test case, operation specification and OpenAPI document
     * @return a sequence of generated test cases; may be empty if not applicable
     */
    public fun apply(context: TestGenerationContext): Sequence<TestCase>
}


