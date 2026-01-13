package art.galushko.openapi.testgen.generation.budget

import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.BudgetExceededException
import art.galushko.openapi.testgen.model.error.ErrorContext

/**
 * Validates that the number of generated test cases stays within budget.
 *
 * This is a single-responsibility component extracted from
 * [art.galushko.openapi.testgen.generation.DefaultTestSuiteGenerator]
 * to handle budget enforcement logic in isolation.
 *
 * @param maxTestCasesPerOperation maximum number of test cases allowed per operation
 */
internal class TestCaseBudgetValidator(
    private val maxTestCasesPerOperation: Int,
) {
    /**
     * Validates that the collected test cases do not exceed the budget.
     *
     * @param testCases the list of test cases to validate
     * @param operationContext the error context for the current operation
     * @throws BudgetExceededException if the number of test cases exceeds the limit
     */
    fun validate(testCases: List<TestCase>, operationContext: ErrorContext.Operation) {
        if (testCases.size > maxTestCasesPerOperation) {
            throw BudgetExceededException(
                budgetType = BudgetExceededException.BudgetType.TEST_CASES_PER_OPERATION,
                context = operationContext,
                used = testCases.size,
                limit = maxTestCasesPerOperation,
            )
        }
    }
}
