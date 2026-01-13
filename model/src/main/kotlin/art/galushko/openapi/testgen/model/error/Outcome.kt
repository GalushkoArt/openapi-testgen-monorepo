package art.galushko.openapi.testgen.model.error

/**
 * Outcome of a computation with explicit success, partial success, or failure.
 *
 * Prefer returning [Outcome] over throwing exceptions for domain errors.
 * Use exceptions only for truly unexpected situations and capture them into domain errors.
 *
 * # Error Handling Policy
 *
 * This codebase uses two complementary error handling mechanisms with clear boundaries.
 *
 * ## Result Type: [Outcome]
 *
 * Use [Outcome] for domain errors that allow partial results or graceful degradation:
 * - **[Outcome.Success]**: Operation completed successfully with the full result
 * - **[Outcome.PartialSuccess]**: Operation completed with partial results and some errors
 * - **[Outcome.Failure]**: Operation failed completely; no usable result available
 *
 * ### When to return Outcome.Failure:
 * - Unsupported parameter types (e.g., unknown parameter location)
 * - Missing schema definitions that cannot be resolved
 * - Invalid OpenAPI structures that can be skipped without stopping generation
 * - Recoverable errors where other operations can proceed
 *
 * ### When to return Outcome.PartialSuccess:
 * - Some test cases were generated, but errors occurred for others
 * - Non-critical warnings that shouldn't stop generation
 * - Best-effort generation where partial output is still valuable
 *
 * ## Exceptions
 *
 * Use exceptions for unrecoverable errors or budget violations that should halt processing:
 *
 * ### [BudgetExceededException]:
 * - Thrown when schema complexity exceeds configured limits
 * - Represents a hard limit that should stop processing immediately
 * - May occur deep in recursive schema traversal
 * - Caught and converted to [Outcome.Failure] at provider boundaries by `runProviderSafely`
 * - Some budget checks (for example, test case count per operation) can occur outside providers
 *   and may propagate unless explicitly handled by callers
 *
 * ### [IllegalStateException] / [IllegalArgumentException]:
 * - Programming errors (missing required configuration)
 * - Invalid inputs that violate API contracts
 * - Should crash immediately; not caught by error handlers
 *
 * ## Error Boundaries
 *
 * The error handling boundary is at the provider level:
 *
 * ```
 * ┌─────────────────────────────────────────────────────────┐
 * │  TestSuiteGenerator                                     │
 * │  - Receives Outcome<List<TestCase>> from providers      │
 * │  - Aggregates results using mergeTestCaseOutcomes       │
 * │  - May throw BudgetExceededException for test case limit│
 * └─────────────────────────────────────────────────────────┘
 *                           │
 *                           ▼
 * ┌─────────────────────────────────────────────────────────┐
 * │  Provider (via runProviderSafely)                       │
 * │  - Always returns Outcome, never throws to caller       │
 * │  - Catches BudgetExceededException → Outcome.Failure    │
 * │  - Catches unexpected exceptions → Outcome.Failure      │
 * │  - with stack trace for debugging                       │
 * └─────────────────────────────────────────────────────────┘
 *                           │
 *                           ▼
 * ┌─────────────────────────────────────────────────────────┐
 * │  Rules / Schema Traversal                               │
 * │  - May throw BudgetExceededException for limits         │
 * │  - May throw UnsupportedParameterType                   │
 * │  - Caught by provider's runProviderSafely wrapper       │
 * └─────────────────────────────────────────────────────────┘
 * ```
 *
 * ## Caller Responsibilities
 *
 * - **Providers**: Always return [Outcome], never throw. Use `runProviderSafely` wrapper.
 * - **Rules**: May throw for budget-exceeded or unsupported types; caught by provider.
 * - **Generators**: Receive [Outcome.Success] only; errors handled upstream. The
 *   [GenerationReport] contains aggregated errors for reporting.
 *
 * ## Error Mode Configuration
 *
 * [ErrorHandlingConfig] controls error behavior at the generation level:
 * - **[ErrorMode.FAIL_FAST]**: Stop on first error encountered
 * - **[ErrorMode.COLLECT_ALL]**: Collect all errors up to [ErrorHandlingConfig.maxErrors] limit
 *
 * ## Implementation Notes
 *
 * 1. Budget exceptions use the exception mechanism (not [Outcome]) because:
 *    - They occur deep in recursive schema traversal
 *    - Unwinding via [Outcome] at each recursion level would be verbose
 *    - They represent hard limits that should stop processing immediately
 *
 * 2. [Outcome.PartialSuccess] is useful for "best effort" generation where:
 *    - Some test cases are better than none
 *    - Errors should be reported but not block other generation
 *    - The caller can decide how to handle partial results
 *
 * 3. Use `mergeTestCaseOutcomes` to combine multiple [Outcome] results, which:
 *    - Combines all successful test cases
 *    - Aggregates all errors
 *    - Returns the appropriate [Outcome] variant based on results
 */
public sealed interface Outcome<out T> {
    public data class Success<T>(val value: T) : Outcome<T>
    public data class PartialSuccess<T>(
        val value: T,
        val errors: List<GenerationError>,
    ) : Outcome<T>
    public data class Failure(
        val errors: List<GenerationError>,
    ) : Outcome<Nothing>
}

