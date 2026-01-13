package art.galushko.openapi.testgen.generation.budget

import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.BudgetExceededException
import art.galushko.openapi.testgen.model.error.ErrorContext
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@Feature("Test Generation")
@Story("Budget Validation")
class TestCaseBudgetValidatorTest {

    private val operationContext = ErrorContext.Operation(
        path = "/pets",
        method = "GET",
        operationId = "listPets",
    )

    @Test
    @DisplayName("Should not throw when test cases are under budget")
    fun shouldNotThrowWhenUnderBudget() {
        val validator = TestCaseBudgetValidator(maxTestCasesPerOperation = 10)
        val testCases = listOf(
            TestCase(name = "test1", method = "GET", path = "/pets"),
            TestCase(name = "test2", method = "GET", path = "/pets"),
        )

        // Should not throw
        validator.validate(testCases, operationContext)
    }

    @Test
    @DisplayName("Should not throw when test cases are exactly at budget limit")
    fun shouldNotThrowWhenAtLimit() {
        val validator = TestCaseBudgetValidator(maxTestCasesPerOperation = 2)
        val testCases = listOf(
            TestCase(name = "test1", method = "GET", path = "/pets"),
            TestCase(name = "test2", method = "GET", path = "/pets"),
        )

        // Should not throw
        validator.validate(testCases, operationContext)
    }

    @Test
    @DisplayName("Should throw BudgetExceededException when test cases exceed budget")
    fun shouldThrowWhenOverBudget() {
        val validator = TestCaseBudgetValidator(maxTestCasesPerOperation = 2)
        val testCases = listOf(
            TestCase(name = "test1", method = "GET", path = "/pets"),
            TestCase(name = "test2", method = "GET", path = "/pets"),
            TestCase(name = "test3", method = "GET", path = "/pets"),
        )

        assertThatThrownBy { validator.validate(testCases, operationContext) }
            .isInstanceOf(BudgetExceededException::class.java)
            .satisfies({ ex ->
                val budgetEx = ex as BudgetExceededException
                assertThat(budgetEx.budgetType).isEqualTo(BudgetExceededException.BudgetType.TEST_CASES_PER_OPERATION)
                assertThat(budgetEx.used).isEqualTo(3)
                assertThat(budgetEx.limit).isEqualTo(2)
                assertThat(budgetEx.context).isEqualTo(operationContext)
            })
    }

    @Test
    @DisplayName("Should not throw when test cases list is empty")
    fun shouldNotThrowWhenEmpty() {
        val validator = TestCaseBudgetValidator(maxTestCasesPerOperation = 10)
        val testCases = emptyList<TestCase>()

        // Should not throw
        validator.validate(testCases, operationContext)
    }
}
