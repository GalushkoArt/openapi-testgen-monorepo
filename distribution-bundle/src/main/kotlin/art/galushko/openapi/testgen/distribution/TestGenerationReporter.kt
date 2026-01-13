package art.galushko.openapi.testgen.distribution

import art.galushko.openapi.testgen.model.error.GenerationReport

/**
 * Abstraction for test generation output reporting.
 *
 * This interface allows CLI and plugin to use the same execution logic
 * with different output mechanisms (SLF4J logger vs Gradle project logger).
 */
public interface TestGenerationReporter {
    /**
     * Logs an informational message.
     */
    public fun logInfo(message: String)

    /**
     * Logs an error message.
     */
    public fun logError(message: String)

    /**
     * Formats the generation report into a human-readable string.
     *
     * @param report the generation report to format
     * @return formatted string representation of the report
     */
    public fun formatReport(report: GenerationReport): String
}
