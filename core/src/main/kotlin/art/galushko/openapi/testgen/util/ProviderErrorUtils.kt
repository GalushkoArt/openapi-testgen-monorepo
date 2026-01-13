package art.galushko.openapi.testgen.util

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.BudgetExceededException
import art.galushko.openapi.testgen.model.error.ErrorContext
import art.galushko.openapi.testgen.model.error.GenerationError
import art.galushko.openapi.testgen.model.error.Outcome
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import org.apache.commons.lang3.exception.ExceptionUtils

/** Builds an [ErrorContext.Operation] from the current generation context. */
internal fun buildOperationContext(context: TestGenerationContext): ErrorContext.Operation =
    ErrorContext.Operation(
        path = context.validCase.path,
        method = context.validCase.method,
        operationId = context.operation.operationId
    )

/** Builds an [ErrorContext.Parameter] from the current context and parameter. */
internal fun buildParameterContext(context: TestGenerationContext, parameter: Parameter): ErrorContext.Parameter =
    ErrorContext.Parameter(
        operation = buildOperationContext(context),
        parameterName = parameter.name ?: "unknown",
        location = parameter.`in` ?: "unknown",
        ref = parameter.`$ref`,
    )

/** Builds an [ErrorContext.RequestBody] with provided or default content type. */
internal fun buildRequestBodyContext(context: TestGenerationContext, requestBody: RequestBody): ErrorContext.RequestBody =
    ErrorContext.RequestBody(
        operation = buildOperationContext(context),
        ref = requestBody.`$ref`
    )

/**
 * Executes provider logic and converts thrown exceptions into [Outcome.Failure] with a [GenerationError].
 *
 * This function establishes the **error boundary** between rule/traversal code (which may throw)
 * and provider code (which must return [Outcome]).
 *
 * ## Exception Handling
 *
 * - **[BudgetExceededException]**: Converted to [Outcome.Failure] with the detailed message.
 *   Stack trace is omitted since this is an expected, documented error.
 *
 * - **Other exceptions**: Converted to [Outcome.Failure] with stack trace attached to
 *   [GenerationError.exceptionText] for debugging unexpected errors.
 *
 * ## Usage
 *
 * Providers should wrap their logic in this function:
 *
 * ```kotlin
 * override fun provideTestCases(spec: Parameter, context: TestGenerationContext): Outcome<List<TestCase>> =
 *     runProviderSafely(this, buildParameterContext(context, spec)) {
 *         // Logic that may throw exceptions
 *         processSpec(spec, context)
 *     }
 * ```
 *
 * @param provider the provider instance (for error attribution)
 * @param errorContext the context where the error occurred
 * @param block the provider logic to execute
 * @return [Outcome.Success] with the result, or [Outcome.Failure] if an exception was thrown
 */
internal inline fun runProviderSafely(
    provider: Any,
    errorContext: ErrorContext,
    block: () -> List<TestCase>
): Outcome<List<TestCase>> {
    return try {
        Outcome.Success(block())
    } catch (e: BudgetExceededException) {
        // Budget exceptions get special treatment - preserve the detailed message
        Outcome.Failure(
            listOf(
                GenerationError(
                    providerClass = provider::class.java.name,
                    message = e.message ?: "Budget exceeded",
                    context = errorContext,
                    exceptionText = null, // Don't include stack trace for expected errors
                )
            )
        )
    } catch (e: Exception) {
        Outcome.Failure(
            listOf(
                GenerationError(
                    providerClass = provider::class.java.name,
                    message = e.message ?: "Unexpected error",
                    context = errorContext,
                    exceptionText = ExceptionUtils.getStackTrace(e),
                )
            )
        )
    }
}


