package art.galushko.openapi.testgen.model.error

import art.galushko.openapi.testgen.model.TestSuite

/**
 * Final report produced by the generation process.
 *
 * @property successfulSuites Test suites generated without fatal errors.
 * @property errors Aggregated domain errors collected during generation.
 * @property summary Summary metrics for reporting/CI.
 * @property hasErrors Convenience flag for error presence.
 */
public data class GenerationReport(
    val successfulSuites: List<TestSuite>,
    val errors: List<GenerationError>,
    val summary: GenerationSummary,
) {
    public val hasErrors: Boolean = errors.isNotEmpty()
}

/**
 * Summary metrics for reporting and CI/CD consumption.
 *
 * Lists contain per-operation metadata for each outcome category.
 */
public data class GenerationSummary(
    val totalOperations: Int,
    val successfulOperations: List<OperationInfo>,
    val notTestedOperations: List<OperationInfo>,
    val partialOperations: List<OperationInfo>,
    val failedOperations: List<OperationInfo>,
    val totalTestCases: Int,
    val totalErrors: Int,
)

/**
 * Operation metadata used in reports and logs.
 */
public data class OperationInfo(
    val operationId: String?,
    val path: String,
    val method: String,
) {
    override fun toString(): String = "$path: $method" + (operationId?.let { " ($it)" } ?: "")
}


