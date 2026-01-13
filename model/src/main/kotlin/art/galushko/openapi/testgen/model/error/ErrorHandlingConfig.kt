package art.galushko.openapi.testgen.model.error

/**
 * Configuration for error handling during generation.
 *
 * @property mode Error handling strategy (fail fast or collect all).
 * @property maxErrors Maximum number of errors to collect before stopping when in COLLECT_ALL mode.
 */
public data class ErrorHandlingConfig(
    val mode: ErrorMode = ErrorMode.COLLECT_ALL,
    val maxErrors: Int = 100,
)

public enum class ErrorMode {
    FAIL_FAST,
    COLLECT_ALL,
}

