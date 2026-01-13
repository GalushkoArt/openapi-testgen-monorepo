package art.galushko.openapi.testgen.reporting.reporter

import art.galushko.openapi.testgen.model.error.GenerationError
import art.galushko.openapi.testgen.model.error.GenerationReport

/**
 * Renders a [GenerationReport] as a human-readable multi-line summary.
 *
 * Inputs: a [GenerationReport] with summary, successes, and errors.
 * Output: a single formatted String intended for console logging.
 * Determinism: output order follows the report's recorded order of operations and errors.
 */
public class ConsoleReporter : ErrorReporter {
    override fun format(report: GenerationReport): String = buildString {
        appendLine("Generation Report")
        appendLine("==================================================")
        with(report.summary) {
            appendLine("Total Operations: $totalOperations")
            appendLine("Total Test Cases: $totalTestCases")
            appendLine("Successful: ${successfulOperations.size}")
            appendLine("Partial: ${partialOperations.size}")
            if (partialOperations.isNotEmpty()) {
                appendLine(partialOperations.joinToString(separator = "\n  -> ", prefix = "  -> "))
            }
            appendLine("Failed: ${failedOperations.size}")
            if (failedOperations.isNotEmpty()) {
                appendLine(failedOperations.joinToString(separator = "\n  -> ", prefix = "  -> "))
            }
            appendLine("Not Tested: ${notTestedOperations.size}")
            if (notTestedOperations.isNotEmpty()) {
                appendLine(notTestedOperations.joinToString(separator = "\n  -> ", prefix = "  -> "))
            }
            appendLine("Total Errors: $totalErrors")
        }
        if (report.errors.isNotEmpty()) {
            appendLine()
            appendLine("Errors:")
            report.errors.forEach { err: GenerationError ->
                appendLine("  - ${err.context}: ${err.message}${err.exceptionText?.let { ", cause:\n$it" } ?: ""}")
            }
        }
    }
}

