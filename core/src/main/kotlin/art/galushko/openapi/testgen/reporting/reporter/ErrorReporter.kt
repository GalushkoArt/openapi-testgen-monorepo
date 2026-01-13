package art.galushko.openapi.testgen.reporting.reporter

import art.galushko.openapi.testgen.model.error.GenerationReport

/**
 * Formats a [GenerationReport] into a string representation for logging or UI output.
 *
 * Inputs: a fully built [GenerationReport] from the generation pipeline.
 * Output: a formatted String; implementations should avoid performing IO.
 * Determinism: formatting should preserve the order of operations and errors as supplied in the report.
 */
public interface ErrorReporter {
    /**
     * Formats the provided report into a string.
     */
    public fun format(report: GenerationReport): String
}

