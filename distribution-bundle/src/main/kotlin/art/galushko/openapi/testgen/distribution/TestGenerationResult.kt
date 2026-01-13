package art.galushko.openapi.testgen.distribution

import art.galushko.openapi.testgen.model.error.GenerationReport

/**
 * Sealed class representing the result of test generation execution.
 *
 * Allows CLI and plugin to handle success and failure cases differently:
 * - CLI returns exit code (0 for success, 1 for failure)
 * - Plugin throws exception on failure
 */
public sealed class TestGenerationResult {

    /**
     * Successful test generation.
     *
     * @property report the generation report containing test suites and summary
     * @property testsWritten whether tests were written to output directory
     */
    public data class Success(
        val report: GenerationReport,
        val testsWritten: Boolean,
    ) : TestGenerationResult()

    /**
     * Failed test generation.
     *
     * @property report the generation report containing errors and partial results
     * @property message human-readable error message
     */
    public data class Failure(
        val report: GenerationReport,
        val message: String,
    ) : TestGenerationResult()
}
