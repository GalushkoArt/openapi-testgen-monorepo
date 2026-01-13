package art.galushko.openapi.testgen.example.util

import art.galushko.openapi.testgen.model.error.BudgetExceededException
import art.galushko.openapi.testgen.model.error.ErrorContext.Operation
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@Epic("Utilities")
@Feature("Combination Budget")
@DisplayName("CombinationBudget")
class CombinationBudgetTest {

    private val errorContext = Operation(
        path = "/test",
        method = "GET",
        operationId = "testOp"
    )

    @Test
    @DisplayName("should track combinations used")
    fun shouldTrackCombinationsUsed() {
        val budget = CombinationBudget(limit = 100, errorContext = errorContext)

        assertThat(budget.combinationsUsed).isEqualTo(0)

        budget.consume(10)
        assertThat(budget.combinationsUsed).isEqualTo(10)

        budget.consume(20)
        assertThat(budget.combinationsUsed).isEqualTo(30)
    }

    @Test
    @DisplayName("should allow consumption within limit")
    fun shouldAllowConsumptionWithinLimit() {
        val budget = CombinationBudget(limit = 100, errorContext = errorContext)

        budget.consume(50)
        budget.consume(50)

        assertThat(budget.combinationsUsed).isEqualTo(100)
    }

    @Test
    @DisplayName("should throw BudgetExceededException when limit exceeded")
    fun shouldThrowWhenLimitExceeded() {
        val budget = CombinationBudget(limit = 100, errorContext = errorContext)

        budget.consume(50)

        assertThatThrownBy { budget.consume(51) }
            .isInstanceOf(BudgetExceededException::class.java)
            .satisfies({ ex ->
                val budgetEx = ex as BudgetExceededException
                assertThat(budgetEx.budgetType)
                    .isEqualTo(BudgetExceededException.BudgetType.SCHEMA_COMBINATIONS)
                assertThat(budgetEx.used).isEqualTo(101)
                assertThat(budgetEx.limit).isEqualTo(100)
                assertThat(budgetEx.context).isEqualTo(errorContext)
            })
    }

    @Test
    @DisplayName("should throw on single consume exceeding limit")
    fun shouldThrowOnSingleConsumeExceedingLimit() {
        val budget = CombinationBudget(limit = 10, errorContext = errorContext)

        assertThatThrownBy { budget.consume(11) }
            .isInstanceOf(BudgetExceededException::class.java)
    }

    @ParameterizedTest(name = "limit={0}, consume={1} should succeed={2}")
    @CsvSource(
        "100, 100, true",
        "100, 101, false",
        "50, 50, true",
        "50, 51, false",
        "1, 1, true",
        "1, 2, false"
    )
    @DisplayName("should enforce budget limit correctly")
    fun shouldEnforceBudgetLimitCorrectly(limit: Int, consume: Int, shouldSucceed: Boolean) {
        val budget = CombinationBudget(limit = limit, errorContext = errorContext)

        if (shouldSucceed) {
            budget.consume(consume)
            assertThat(budget.combinationsUsed).isEqualTo(consume)
        } else {
            assertThatThrownBy { budget.consume(consume) }
                .isInstanceOf(BudgetExceededException::class.java)
        }
    }

    @Test
    @DisplayName("should accumulate multiple consume calls")
    fun shouldAccumulateMultipleConsumeCalls() {
        val budget = CombinationBudget(limit = 100, errorContext = errorContext)

        repeat(10) { budget.consume(10) }

        assertThat(budget.combinationsUsed).isEqualTo(100)
    }
}
