package art.galushko.openapi.testgen.model.error

/**
 * Domain error captured during generation.
 *
 * Domain errors are values, not exceptions. Unexpected exceptions can be attached
 * to [exceptionText] for diagnostics without driving control flow.
 *
 * @property providerClass Fully qualified class name of the provider that raised the error.
 * @property message Human-readable error message.
 * @property context Context describing where the error occurred.
 * @property exceptionText Optional stack trace for unexpected failures.
 */
public data class GenerationError(
    public val providerClass: String,
    public val message: String,
    public val context: ErrorContext,
    public val exceptionText: String? = null,
)
