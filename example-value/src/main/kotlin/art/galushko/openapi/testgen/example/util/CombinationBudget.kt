package art.galushko.openapi.testgen.example.util

import art.galushko.openapi.testgen.model.error.BudgetExceededException
import art.galushko.openapi.testgen.model.error.ErrorContext

/**
 * Tracks and enforces budget limits during schema combination generation.
 *
 * Prevents combinatorial explosion by failing fast when the number of schema
 * combinations exceeds the configured limit. This protects against OOM errors
 * when processing OpenAPI specs with deeply nested oneOf/anyOf/allOf structures.
 */
public class CombinationBudget(
    private val limit: Int,
    private val errorContext: ErrorContext,
) {
    private var used: Int = 0

    /**
     * Current number of combinations generated.
     */
    public val combinationsUsed: Int
        get() = used

    /**
     * Records additional combinations and throws if the budget exceeded.
     *
     * @param count Number of new combinations to add
     * @throws BudgetExceededException if adding these combinations would exceed the limit
     */
    public fun consume(count: Int) {
        used += count
        if (used > limit) {
            throw BudgetExceededException(
                budgetType = BudgetExceededException.BudgetType.SCHEMA_COMBINATIONS,
                context = errorContext,
                used = used,
                limit = limit,
            )
        }
    }
}
