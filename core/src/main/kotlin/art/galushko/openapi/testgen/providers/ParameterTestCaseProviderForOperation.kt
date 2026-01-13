package art.galushko.openapi.testgen.providers

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.spi.TestCaseProvider
import art.galushko.openapi.testgen.util.mergeTestCaseOutcomes
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.parameters.Parameter

/**
 * Aggregates parameter-focused [TestCaseProvider]s and applies them to each parameter of an [Operation].
 *
 * Inputs: operation parameters and [TestGenerationContext] with a valid baseline.
 * Output: merged [Outcome] of parameter-derived test cases (400 for built-in providers).
 * Constraints: skips when the operation has no parameters; per-parameter providers decide applicability.
 * Determinism: preserves parameter order and provider order.
 * Settings: nested schema rules are filtered via `TestGenerationSettings.ignoreSchemaValidationRules` during wiring;
 * `ignoreTestCases` filtering happens after suite assembly.
 *
 * @param parameterTestCaseProviders providers invoked per parameter
 */
internal class ParameterTestCaseProviderForOperation(
    private val parameterTestCaseProviders: List<TestCaseProvider<Parameter>>
) : TestCaseProvider<Operation> {
    override fun provideTestCases(spec: Operation, context: TestGenerationContext): Outcome<List<TestCase>> {
        if (spec.parameters.isNullOrEmpty()) return Outcome.Success(emptyList())

        val outcomes = spec.parameters.asSequence().filterNotNull()
            .flatMap { parameter ->
                parameterTestCaseProviders.asSequence().map { provider ->
                    provider.provideTestCases(parameter, context)
                }
            }
        return mergeTestCaseOutcomes(outcomes)
    }
}


