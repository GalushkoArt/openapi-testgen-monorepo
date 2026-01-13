package art.galushko.openapi.testgen.providers

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.spi.AuthValidationRule
import art.galushko.openapi.testgen.spi.TestCaseProvider
import art.galushko.openapi.testgen.util.buildOperationContext
import art.galushko.openapi.testgen.util.runProviderSafely
import io.swagger.v3.oas.models.Operation

/**
 * Generates auth-related negative test cases for an [Operation] by applying ordered [AuthValidationRule]s.
 *
 * Inputs: operation security requirements and [TestGenerationContext] with a valid baseline.
 * Output: list of auth-negative [TestCase]s; built-in rules use 401/403, custom rules may set other codes.
 * Constraints: returns empty when both the operation and root spec define no security.
 * Determinism: preserves rule order as provided.
 * Settings: rule list is filtered via `TestGenerationSettings.ignoreAuthValidationRules` during wiring; values use
 * `TestGenerationSettings.validSecurityValues` and `TestGenerationSettings.overrideBasicTestData`.
 *
 * @param rules ordered list of auth validation rules to apply
 */
internal class AuthTestCaseProviderForOperation(
    private val rules: List<AuthValidationRule>,
) : TestCaseProvider<Operation> {

    override fun provideTestCases(spec: Operation, context: TestGenerationContext): Outcome<List<TestCase>> =
        runProviderSafely(this, buildOperationContext(context)) {
            processSpec(spec, context)
        }

    public fun processSpec(spec: Operation, context: TestGenerationContext): List<TestCase> {
        if (spec.security == null && context.openAPI.security == null) return emptyList()
        return rules.asSequence()
            .filter { rule -> rule.decide(context) }
            .flatMap { rule -> rule.apply(context) }
            .toList()
    }
}


