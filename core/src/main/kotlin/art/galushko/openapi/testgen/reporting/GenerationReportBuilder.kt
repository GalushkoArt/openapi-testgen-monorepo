package art.galushko.openapi.testgen.reporting

import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.model.error.ErrorHandlingConfig
import art.galushko.openapi.testgen.model.error.ErrorMode
import art.galushko.openapi.testgen.model.error.GenerationError
import art.galushko.openapi.testgen.model.error.GenerationReport
import art.galushko.openapi.testgen.model.error.GenerationSummary
import art.galushko.openapi.testgen.model.error.OperationInfo

/**
 * Builds generation reports while tracking state across operations.
 *
 * Encapsulates report building logic including error collection,
 * early exit conditions, and summary creation.
 */
internal class GenerationReportBuilder(
    errorConfig: ErrorHandlingConfig,
) {
    private val successfulSuites = mutableListOf<TestSuite>()
    private val errors = mutableListOf<GenerationError>()
    private var totalOperations = 0
    private val successfulOperations = mutableListOf<OperationInfo>()
    private val notTestedOperations = mutableListOf<OperationInfo>()
    private var partialOperations = mutableListOf<OperationInfo>()
    private var failedOperations = mutableListOf<OperationInfo>()

    private val failFast: Boolean = errorConfig.mode == ErrorMode.FAIL_FAST
    private val maxErrors: Int = errorConfig.maxErrors

    /**
     * Records a successful operation with a generated test suite.
     */
    fun recordSuccess(suite: TestSuite, operationInfo: OperationInfo) {
        successfulSuites += suite
        successfulOperations += operationInfo
    }

    /**
     * Records a not tested operation.
     */
    fun recordNotTestedOperation(operationInfo: OperationInfo) {
        notTestedOperations += operationInfo
    }

    /**
     * Records a partial success with some errors.
     */
    fun recordPartialSuccess(suite: TestSuite?, operationErrors: List<GenerationError>, operationInfo: OperationInfo): GenerationReport? {
        if (suite != null) {
            successfulSuites += suite
        }
        errors += operationErrors
        partialOperations += operationInfo
        return checkEarlyExit()
    }

    /**
     * Records a complete failure.
     */
    fun recordFailure(operationErrors: List<GenerationError>, operationInfo: OperationInfo): GenerationReport? {
        errors += operationErrors
        failedOperations += operationInfo
        return checkEarlyExit()
    }

    /**
     * Increments total operation count.
     */
    fun incrementTotalOperations() {
        totalOperations++
    }

    private fun checkEarlyExit(): GenerationReport? {
        return if (failFast || errors.size >= maxErrors) {
            buildReport()
        } else {
            null
        }
    }

    /**
     * Builds the final generation report with the current state.
     */
    fun buildReport(): GenerationReport {
        val totalTestCases = successfulSuites.sumOf { it.testCases.size }
        val summary = GenerationSummary(
            totalOperations = totalOperations,
            successfulOperations = successfulOperations,
            partialOperations = partialOperations,
            failedOperations = failedOperations,
            notTestedOperations = notTestedOperations,
            totalTestCases = totalTestCases,
            totalErrors = errors.size,
        )
        return GenerationReport(successfulSuites, errors, summary)
    }
}


