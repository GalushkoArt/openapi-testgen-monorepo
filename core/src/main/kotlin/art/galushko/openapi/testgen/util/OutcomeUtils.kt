package art.galushko.openapi.testgen.util

import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.GenerationError
import art.galushko.openapi.testgen.model.error.Outcome

/**
 * Merges a sequence of Outcome<List<TestCase>> into a single Outcome<List<TestCase>>.
 *
 * The merge strategy is:
 * - If all outcomes are [Outcome.Success]: returns [Outcome.Success] with combined test cases
 * - If some succeed and some fail: returns [Outcome.PartialSuccess] with all test cases and all errors
 * - If all fail: returns [Outcome.Failure] with all errors
 */
internal fun mergeTestCaseOutcomes(outcomes: Sequence<Outcome<List<TestCase>>>): Outcome<List<TestCase>> {
    val accumulated = mutableListOf<TestCase>()
    val errors = mutableListOf<GenerationError>()
    outcomes.forEach { result ->
        when (result) {
            is Outcome.Success -> accumulated.addAll(result.value)
            is Outcome.PartialSuccess -> {
                accumulated.addAll(result.value)
                errors.addAll(result.errors)
            }
            is Outcome.Failure -> errors.addAll(result.errors)
        }
    }
    return when {
        errors.isEmpty() -> Outcome.Success(accumulated)
        accumulated.isNotEmpty() -> Outcome.PartialSuccess(accumulated, errors)
        else -> Outcome.Failure(errors)
    }
}


