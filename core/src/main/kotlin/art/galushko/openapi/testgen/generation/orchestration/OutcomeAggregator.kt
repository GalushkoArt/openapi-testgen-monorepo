package art.galushko.openapi.testgen.generation.orchestration

import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.model.error.GenerationError
import art.galushko.openapi.testgen.model.error.Outcome

/**
 * Aggregated result from multiple provider executions.
 *
 * @property testCases all successfully generated test cases
 * @property errors all errors collected during generation
 */
internal data class AggregatedResult(
    val testCases: MutableList<TestCase>,
    val errors: MutableList<GenerationError>,
)

/**
 * Aggregates outcomes from multiple provider executions into a final result.
 *
 * This is a single-responsibility component extracted from [art.galushko.openapi.testgen.generation.DefaultTestSuiteGenerator]
 * to handle result aggregation logic in isolation.
 */
internal class OutcomeAggregator {

    /**
     * Creates an empty aggregated result for collecting provider outcomes.
     *
     * @return a new empty [AggregatedResult]
     */
    fun createEmptyResult(): AggregatedResult {
        return AggregatedResult(
            testCases = mutableListOf(),
            errors = mutableListOf(),
        )
    }

    /**
     * Adds a provider result to the aggregated result.
     *
     * @param aggregated the aggregated result to update
     * @param providerResult the provider result to add
     */
    fun addProviderResult(aggregated: AggregatedResult, providerResult: ProviderResult) {
        when (val outcome = providerResult.outcome) {
            is Outcome.Success -> {
                aggregated.testCases.addAll(outcome.value)
            }
            is Outcome.PartialSuccess -> {
                aggregated.testCases.addAll(outcome.value)
                aggregated.errors.addAll(outcome.errors)
            }
            is Outcome.Failure -> {
                aggregated.errors.addAll(outcome.errors)
            }
        }
    }

    /**
     * Builds the final [Outcome] from the aggregated result.
     *
     * @param path the API path
     * @param method the HTTP method
     * @param operationName the operation name (operationId or method+path)
     * @param aggregated the aggregated result
     * @return the final outcome wrapping the test suite
     */
    fun buildOutcome(
        path: String,
        method: String,
        operationName: String,
        aggregated: AggregatedResult,
        validCase: TestCase? = null,
    ): Outcome<TestSuite> {
        val testCases = if (validCase == null) {
            aggregated.testCases
        } else {
            listOf(validCase) + aggregated.testCases
        }

        val suite = TestSuite(
            path = path,
            method = method,
            operationName = operationName,
            testCases = testCases,
        )

        return when {
            aggregated.errors.isEmpty() -> Outcome.Success(suite)
            testCases.isNotEmpty() -> Outcome.PartialSuccess(suite, aggregated.errors)
            else -> Outcome.Failure(aggregated.errors)
        }
    }
}
