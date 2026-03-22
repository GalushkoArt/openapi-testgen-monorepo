package art.galushko.openapi.testgen.generation

import art.galushko.openapi.testgen.filtering.IgnoreConfigHandler
import art.galushko.openapi.testgen.filtering.IncludeOperationsHandler
import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.model.error.ErrorHandlingConfig
import art.galushko.openapi.testgen.model.error.GenerationReport
import art.galushko.openapi.testgen.model.error.OperationInfo
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.reporting.GenerationReportBuilder
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import org.slf4j.LoggerFactory

/** Orchestrates generation of test suites from an OpenAPI model. */
internal class TestGenerationProcessor(
    private val testSuiteGenerator: TestSuiteGenerator,
    private val includeHandler: IncludeOperationsHandler,
    private val ignoreHandler: IgnoreConfigHandler,
) {
    private val log = LoggerFactory.getLogger(TestGenerationProcessor::class.java)

    /**
     * Generates test suites using the provided [errorConfig] and returns a [GenerationReport].
     */
    public fun generateReport(openAPI: OpenAPI, errorConfig: ErrorHandlingConfig): GenerationReport {
        val generationReportBuilder = GenerationReportBuilder(errorConfig)

        validateConfig(openAPI)

        val pathsToProcess = filterPaths(openAPI)

        for ((path, pathItem) in pathsToProcess.entries) {
            val operationsToProcess = filterOperations(path, pathItem)

            for ((httpMethod, operation) in operationsToProcess.entries) {
                generationReportBuilder.incrementTotalOperations()

                val earlyExit = processOperation(
                    openAPI = openAPI,
                    path = path,
                    httpMethod = httpMethod,
                    operation = operation,
                    generationReportBuilder = generationReportBuilder
                )

                if (earlyExit != null) return earlyExit
            }
        }

        return generationReportBuilder.buildReport()
    }

    /**
     * Validates include and ignore configurations, logging warnings for invalid paths or patterns.
     *
     * Three-state behavior:
     * - **Paths present**: validates include/ignore configs and returns normally
     * - **No paths, webhooks present**: warns that webhooks are skipped by test generation
     * - **No paths, no webhooks**: warns that no operations are available
     */
    private fun validateConfig(openAPI: OpenAPI) {
        val specPaths = openAPI.paths?.keys ?: emptySet()
        includeHandler.validatePathsExist(specPaths)
        ignoreHandler.validatePathsExist(specPaths)
        ignoreHandler.warnOnForbiddenWildcards()

        if (specPaths.isNotEmpty()) return

        val webhookCount = openAPI.webhooks?.size ?: 0
        if (webhookCount > 0) {
            log.warn(
                "OpenAPI spec contains {} webhook(s) but no paths. Webhook operations are currently skipped by test generation.",
                webhookCount,
            )
        } else {
            log.warn("OpenAPI spec contains no paths. No operations available for test generation.")
        }
    }

    /**
     * Filters paths based on include and ignore configurations.
     * Include filter runs first (for performance), then ignore filter.
     */
    private fun filterPaths(
        openAPI: OpenAPI,
    ): Map<String, PathItem> {
        return openAPI.paths.orEmpty()
            .filter { (path, _) -> includeHandler.shouldIncludePath(path) }
            .filterNot { (path, _) -> ignoreHandler.shouldIgnorePath(path) }
    }

    /**
     * Filters operations based on include and ignore configurations.
     * Include filter runs first (for performance), then ignore filter.
     */
    private fun filterOperations(
        path: String,
        pathItem: PathItem,
    ): Map<PathItem.HttpMethod, Operation> {
        return pathItem.readOperationsMap()
            .filter { (httpMethod, _) -> includeHandler.shouldIncludeOperation(path, httpMethod.name) }
            .filterNot { (httpMethod, _) -> ignoreHandler.shouldIgnoreOperation(path, httpMethod.name) }
    }

    /**
     * Processes a single operation and updates the report builder.
     * Returns a GenerationReport if early exit conditions are met, null otherwise.
     */
    private fun processOperation(
        openAPI: OpenAPI,
        path: String,
        httpMethod: PathItem.HttpMethod,
        operation: Operation,
        generationReportBuilder: GenerationReportBuilder
    ): GenerationReport? {
        val ignoredTestCaseNames = ignoreHandler.getIgnoredTestCaseNames(path, httpMethod.name)
        val operationInfo = OperationInfo(operation.operationId, path, httpMethod.name)

        return when (val outcome = testSuiteGenerator.generateTestSuite(openAPI, path, httpMethod.name, operation)) {
            is Outcome.Success -> {
                handleSuccessOutcome(outcome, ignoredTestCaseNames, operationInfo, generationReportBuilder)
            }
            is Outcome.PartialSuccess -> {
                handlePartialSuccessOutcome(outcome, ignoredTestCaseNames, operationInfo, generationReportBuilder)
            }
            is Outcome.Failure -> {
                generationReportBuilder.recordFailure(outcome.errors, operationInfo)
            }
        }
    }

    /**
     * Handles a successful test suite generation outcome.
     */
    private fun handleSuccessOutcome(
        outcome: Outcome.Success<TestSuite>,
        ignoredTestCaseNames: List<*>?,
        operationInfo: OperationInfo,
        generationReportBuilder: GenerationReportBuilder
    ): GenerationReport? {
        val filteredSuite = ignoreHandler.filterTestCases(outcome.value, ignoredTestCaseNames)

        if (filteredSuite.testCases.isEmpty()) {
            log.warn("No test cases found for path: {}, operation: {}", operationInfo.path, operationInfo.method)
            generationReportBuilder.recordNotTestedOperation(operationInfo)
        } else {
            generationReportBuilder.recordSuccess(filteredSuite, operationInfo)
        }

        return null
    }

    /**
     * Handles a partial success outcome with some errors.
     */
    private fun handlePartialSuccessOutcome(
        outcome: Outcome.PartialSuccess<TestSuite>,
        ignoredTestCaseNames: List<*>?,
        operationInfo: OperationInfo,
        generationReportBuilder: GenerationReportBuilder
    ): GenerationReport? {
        val filteredSuite = ignoreHandler.filterTestCases(outcome.value, ignoredTestCaseNames)
        val suiteToRecord = if (filteredSuite.testCases.isNotEmpty()) filteredSuite else null

        if (suiteToRecord == null) {
            generationReportBuilder.recordNotTestedOperation(operationInfo)
        }

        return generationReportBuilder.recordPartialSuccess(suiteToRecord, outcome.errors, operationInfo)
    }
}


