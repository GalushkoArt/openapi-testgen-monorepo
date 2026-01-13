package art.galushko.openapi.testgen.generation.orchestration

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.spi.TestCaseProvider
import io.swagger.v3.oas.models.Operation

/**
 * Result of executing a single provider.
 *
 * @property provider the provider that was executed
 * @property outcome the outcome from the provider execution
 */
internal data class ProviderResult(
    val provider: TestCaseProvider<Operation>,
    val outcome: Outcome<List<TestCase>>,
)

/**
 * Orchestrates the execution of test case providers.
 *
 * This is a single-responsibility component extracted from [art.galushko.openapi.testgen.generation.DefaultTestSuiteGenerator]
 * to handle provider iteration and execution in isolation.
 *
 * @param providers ordered list of providers to execute
 */
internal class ProviderOrchestrator(
    private val providers: List<TestCaseProvider<Operation>>,
) {
    /**
     * Executes all providers and yields their results in provider order.
     *
     * @param operation the OpenAPI operation being processed
     * @param context the test generation context
     * @return sequence of provider results in provider order
     */
    fun executeProviders(
        operation: Operation,
        context: TestGenerationContext,
    ): Sequence<ProviderResult> {
        return providers.asSequence().map { provider ->
            ProviderResult(
                provider = provider,
                outcome = provider.provideTestCases(operation, context),
            )
        }
    }
}
