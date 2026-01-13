package art.galushko.openapi.testgen.util

import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.ErrorContext
import art.galushko.openapi.testgen.model.error.GenerationError
import art.galushko.openapi.testgen.model.error.Outcome
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for [mergeTestCaseOutcomes] helper functions.
 *
 * These tests verify the behavior of utility functions for working with [Outcome].
 */
@Epic("Error Handling")
@Feature("Outcome Utilities")
@DisplayName("Outcome Utilities Tests")
class OutcomeUtilsTest {

    private val operationContext = ErrorContext.Operation(
        path = "/test",
        method = "GET",
        operationId = "testOp"
    )

    private fun createTestCase(name: String): TestCase = TestCase(
        name = name,
        method = "GET",
        path = "/test"
    )

    private fun createError(message: String): GenerationError = GenerationError(
        providerClass = "TestProvider",
        message = message,
        context = operationContext
    )

    @Nested
    @Story("mergeTestCaseOutcomes")
    @DisplayName("mergeTestCaseOutcomes")
    inner class MergeTestCaseOutcomesTest {

        @Test
        @DisplayName("should return Success when all outcomes are Success")
        fun shouldReturnSuccessWhenAllOutcomesAreSuccess() {
            val outcomes = sequenceOf(
                Outcome.Success(listOf(createTestCase("test1"))),
                Outcome.Success(listOf(createTestCase("test2"))),
                Outcome.Success(listOf(createTestCase("test3")))
            )

            val result = mergeTestCaseOutcomes(outcomes)

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val success = result as Outcome.Success<List<TestCase>>
            assertThat(success.value).hasSize(3)
            assertThat(success.value.map { it.name }).containsExactly("test1", "test2", "test3")
        }

        @Test
        @DisplayName("should return PartialSuccess when some succeed and some fail")
        fun shouldReturnPartialSuccessWhenSomeSucceedAndSomeFail() {
            val error = createError("Test error")
            val outcomes = sequenceOf(
                Outcome.Success(listOf(createTestCase("test1"))),
                Outcome.Failure(listOf(error))
            )

            val result = mergeTestCaseOutcomes(outcomes)

            assertThat(result).isInstanceOf(Outcome.PartialSuccess::class.java)
            val partial = result as Outcome.PartialSuccess<List<TestCase>>
            assertThat(partial.value).hasSize(1)
            assertThat(partial.value[0].name).isEqualTo("test1")
            assertThat(partial.errors).hasSize(1)
            assertThat(partial.errors[0]).isEqualTo(error)
        }

        @Test
        @DisplayName("should return Failure when all outcomes are Failure")
        fun shouldReturnFailureWhenAllOutcomesAreFailure() {
            val error1 = createError("Error 1")
            val error2 = createError("Error 2")
            val outcomes = sequenceOf(
                Outcome.Failure(listOf(error1)),
                Outcome.Failure(listOf(error2))
            )

            val result = mergeTestCaseOutcomes(outcomes)

            assertThat(result).isInstanceOf(Outcome.Failure::class.java)
            val failure = result as Outcome.Failure
            assertThat(failure.errors).hasSize(2)
            assertThat(failure.errors).containsExactly(error1, error2)
        }

        @Test
        @DisplayName("should merge PartialSuccess outcomes correctly")
        fun shouldMergePartialSuccessOutcomesCorrectly() {
            val error1 = createError("Error 1")
            val error2 = createError("Error 2")
            val outcomes = sequenceOf(
                Outcome.PartialSuccess(listOf(createTestCase("test1")), listOf(error1)),
                Outcome.PartialSuccess(listOf(createTestCase("test2")), listOf(error2))
            )

            val result = mergeTestCaseOutcomes(outcomes)

            assertThat(result).isInstanceOf(Outcome.PartialSuccess::class.java)
            val partial = result as Outcome.PartialSuccess<List<TestCase>>
            assertThat(partial.value).hasSize(2)
            assertThat(partial.errors).hasSize(2)
        }

        @Test
        @DisplayName("should return Success for empty sequence")
        fun shouldReturnSuccessForEmptySequence() {
            val outcomes = emptySequence<Outcome<List<TestCase>>>()

            val result = mergeTestCaseOutcomes(outcomes)

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            assertThat((result as Outcome.Success<List<TestCase>>).value).isEmpty()
        }

        @Test
        @DisplayName("should preserve test case order")
        fun shouldPreserveTestCaseOrder() {
            val outcomes = sequenceOf(
                Outcome.Success(listOf(createTestCase("first"), createTestCase("second"))),
                Outcome.Success(listOf(createTestCase("third")))
            )

            val result = mergeTestCaseOutcomes(outcomes)

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val success = result as Outcome.Success<List<TestCase>>
            assertThat(success.value.map { it.name }).containsExactly("first", "second", "third")
        }

        @Test
        @DisplayName("should handle mixed Success, PartialSuccess, and Failure")
        fun shouldHandleMixedSuccessPartialSuccessAndFailure() {
            val error1 = createError("Partial error")
            val error2 = createError("Full failure error")
            val outcomes = sequenceOf(
                Outcome.Success(listOf(createTestCase("from-success"))),
                Outcome.PartialSuccess(listOf(createTestCase("from-partial")), listOf(error1)),
                Outcome.Failure(listOf(error2))
            )

            val result = mergeTestCaseOutcomes(outcomes)

            assertThat(result).isInstanceOf(Outcome.PartialSuccess::class.java)
            val partial = result as Outcome.PartialSuccess<List<TestCase>>
            assertThat(partial.value).hasSize(2)
            assertThat(partial.value.map { it.name }).containsExactly("from-success", "from-partial")
            assertThat(partial.errors).hasSize(2)
            assertThat(partial.errors).containsExactly(error1, error2)
        }
    }
}

