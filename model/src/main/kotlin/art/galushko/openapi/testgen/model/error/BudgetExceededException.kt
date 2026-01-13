package art.galushko.openapi.testgen.model.error

/**
 * Exception thrown when a complexity budget is exceeded during test generation.
 *
 * This is a fail-fast mechanism to prevent combinatorial explosion and out-of-memory errors
 * when processing OpenAPI specs with deeply nested oneOf/anyOf/allOf schemas.
 *
 * ## Why This Is an Exception (Not [Outcome.Failure])
 *
 * This exception is **intentionally an exception** rather than an [Outcome.Failure] because:
 *
 * 1. **Immediate halt required**: Budget violations represent hard limits that should stop
 *    processing immediately. There's no partial result that makes sense.
 *
 * 2. **Deep recursion context**: Budget checks occur deep in recursive schema traversal
 *    ([SchemaMerger], [CombinationBudget]). Propagating [Outcome] at each recursion level
 *    would be verbose and error-prone.
 *
 * 3. **Clear boundary**: Exceptions bubble up naturally through the call stack and are caught
 *    at well-defined provider boundaries by `runProviderSafely`, which converts them to
 *    [Outcome.Failure] for consistent handling upstream.
 *
 * ## Handling
 *
 * Callers should **not** attempt to catch this exception directly. Instead:
 * - Providers wrap their logic in `runProviderSafely`, which converts this to [Outcome.Failure]
 * - The detailed [message] provides actionable guidance for increasing limits or simplifying schemas
 *
 * @see BudgetType for the types of budgets that can be exceeded
 */
public class BudgetExceededException(
    public val budgetType: BudgetType,
    public val context: ErrorContext,
    public val used: Int,
    public val limit: Int,
) : RuntimeException(buildMessage(budgetType, context, used, limit)) {

    public enum class BudgetType {
        SCHEMA_COMBINATIONS,
        TEST_CASES_PER_OPERATION,
    }

    private companion object {
        private fun buildMessage(
            budgetType: BudgetType,
            context: ErrorContext,
            used: Int,
            limit: Int,
        ): String {
            val contextDesc = when (context) {
                is ErrorContext.Operation -> "${context.method} ${context.path}"
                is ErrorContext.Parameter -> "${context.operation.method} ${context.operation.path} (parameter: ${context.parameterName})"
                is ErrorContext.RequestBody -> "${context.operation.method} ${context.operation.path} (request body)"
            }

            val budgetDesc = when (budgetType) {
                BudgetType.SCHEMA_COMBINATIONS -> "schema combinations"
                BudgetType.TEST_CASES_PER_OPERATION -> "test cases per operation"
            }

            val settingName = when (budgetType) {
                BudgetType.SCHEMA_COMBINATIONS -> "maxSchemaCombinations"
                BudgetType.TEST_CASES_PER_OPERATION -> "maxTestCasesPerOperation"
            }

            return buildString {
                appendLine("Budget exceeded for $contextDesc:")
                appendLine("  Generated: $used $budgetDesc")
                appendLine("  Limit: $limit")
                appendLine()
                appendLine("Possible solutions:")
                appendLine("  1. Increase the limit by setting testGenerationSettings.$settingName to a higher value")
                appendLine("  2. Simplify the OpenAPI schema by reducing nested oneOf/anyOf/allOf structures")
                appendLine("  3. Use ignoreTestCases or ignoreSchemaValidationRules to exclude specific scenarios")
            }
        }
    }
}
