package art.galushko.openapi.testgen.generator.reporting

import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.model.error.ErrorContext
import art.galushko.openapi.testgen.model.error.GenerationError
import art.galushko.openapi.testgen.model.error.GenerationReport
import art.galushko.openapi.testgen.model.error.GenerationSummary
import art.galushko.openapi.testgen.model.error.OperationInfo
import art.galushko.openapi.testgen.reporting.reporter.ConsoleReporter
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Epic("Generator")
@Feature("ConsoleReporter")
@DisplayName("ConsoleReporter Tests")
class ConsoleReporterTest {

    private lateinit var reporter: ConsoleReporter

    @BeforeEach
    fun setUp() {
        reporter = ConsoleReporter()
    }

    @Nested
    @DisplayName("Empty Report Scenarios")
    inner class EmptyReportTests {

        @Test
        @DisplayName("should format empty report with zero counts")
        @Description("Verifies that an empty report displays all counts as zero with no operation lists")
        fun shouldFormatEmptyReport() {
            val report = createReport(
                summary = GenerationSummary(
                    totalOperations = 0,
                    successfulOperations = emptyList(),
                    notTestedOperations = emptyList(),
                    partialOperations = emptyList(),
                    failedOperations = emptyList(),
                    totalTestCases = 0,
                    totalErrors = 0,
                )
            )

            val result = reporter.format(report)

            assertThat(result).isEqualTo(
                """
                |Generation Report
                |==================================================
                |Total Operations: 0
                |Total Test Cases: 0
                |Successful: 0
                |Partial: 0
                |Failed: 0
                |Not Tested: 0
                |Total Errors: 0
                |""".trimMargin()
            )
        }
    }

    @Nested
    @DisplayName("Successful Operations Scenarios")
    inner class SuccessfulOperationsTests {

        @Test
        @DisplayName("should format report with successful operations only")
        @Description("Verifies that successful operations are counted but not listed individually")
        fun shouldFormatReportWithSuccessfulOperationsOnly() {
            val report = createReport(
                summary = GenerationSummary(
                    totalOperations = 3,
                    successfulOperations = listOf(
                        OperationInfo("getUsers", "/users", "GET"),
                        OperationInfo("createUser", "/users", "POST"),
                        OperationInfo(null, "/health", "GET"),
                    ),
                    notTestedOperations = emptyList(),
                    partialOperations = emptyList(),
                    failedOperations = emptyList(),
                    totalTestCases = 45,
                    totalErrors = 0,
                )
            )

            val result = reporter.format(report)

            assertThat(result).isEqualTo(
                """
                |Generation Report
                |==================================================
                |Total Operations: 3
                |Total Test Cases: 45
                |Successful: 3
                |Partial: 0
                |Failed: 0
                |Not Tested: 0
                |Total Errors: 0
                |""".trimMargin()
            )
        }
    }

    @Nested
    @DisplayName("Partial Operations Scenarios")
    inner class PartialOperationsTests {

        @Test
        @DisplayName("should format report with partial operations")
        @Description("Verifies that partial operations are listed with proper indentation")
        fun shouldFormatReportWithPartialOperations() {
            val report = createReport(
                summary = GenerationSummary(
                    totalOperations = 2,
                    successfulOperations = emptyList(),
                    notTestedOperations = emptyList(),
                    partialOperations = listOf(
                        OperationInfo("updateUser", "/users/{id}", "PUT"),
                        OperationInfo(null, "/projects", "POST"),
                    ),
                    failedOperations = emptyList(),
                    totalTestCases = 10,
                    totalErrors = 2,
                )
            )

            val result = reporter.format(report)

            assertThat(result).isEqualTo(
                """
                |Generation Report
                |==================================================
                |Total Operations: 2
                |Total Test Cases: 10
                |Successful: 0
                |Partial: 2
                |  -> /users/{id}: PUT (updateUser)
                |  -> /projects: POST
                |Failed: 0
                |Not Tested: 0
                |Total Errors: 2
                |""".trimMargin()
            )
        }
    }

    @Nested
    @DisplayName("Failed Operations Scenarios")
    inner class FailedOperationsTests {

        @Test
        @DisplayName("should format report with failed operations")
        @Description("Verifies that failed operations are listed with proper indentation")
        fun shouldFormatReportWithFailedOperations() {
            val report = createReport(
                summary = GenerationSummary(
                    totalOperations = 2,
                    successfulOperations = emptyList(),
                    notTestedOperations = emptyList(),
                    partialOperations = emptyList(),
                    failedOperations = listOf(
                        OperationInfo("complexOperation", "/complex", "POST"),
                    ),
                    totalTestCases = 0,
                    totalErrors = 1,
                )
            )

            val result = reporter.format(report)

            assertThat(result).isEqualTo(
                """
                |Generation Report
                |==================================================
                |Total Operations: 2
                |Total Test Cases: 0
                |Successful: 0
                |Partial: 0
                |Failed: 1
                |  -> /complex: POST (complexOperation)
                |Not Tested: 0
                |Total Errors: 1
                |""".trimMargin()
            )
        }
    }

    @Nested
    @DisplayName("Not Tested Operations Scenarios")
    inner class NotTestedOperationsTests {

        @Test
        @DisplayName("should format report with not tested operations")
        @Description("Verifies that not tested operations are listed with proper indentation")
        fun shouldFormatReportWithNotTestedOperations() {
            val report = createReport(
                summary = GenerationSummary(
                    totalOperations = 3,
                    successfulOperations = listOf(
                        OperationInfo("getUsers", "/users", "GET"),
                    ),
                    notTestedOperations = listOf(
                        OperationInfo("deleteUser", "/users/{id}", "DELETE"),
                        OperationInfo("headUser", "/users/{id}", "HEAD"),
                    ),
                    partialOperations = emptyList(),
                    failedOperations = emptyList(),
                    totalTestCases = 15,
                    totalErrors = 0,
                )
            )

            val result = reporter.format(report)

            assertThat(result).isEqualTo(
                """
                |Generation Report
                |==================================================
                |Total Operations: 3
                |Total Test Cases: 15
                |Successful: 1
                |Partial: 0
                |Failed: 0
                |Not Tested: 2
                |  -> /users/{id}: DELETE (deleteUser)
                |  -> /users/{id}: HEAD (headUser)
                |Total Errors: 0
                |""".trimMargin()
            )
        }
    }

    @Nested
    @DisplayName("Error Reporting Scenarios")
    inner class ErrorReportingTests {

        @Test
        @DisplayName("should format report with operation-level error")
        @Description("Verifies that errors with Operation context are formatted with human-readable context")
        fun shouldFormatReportWithOperationError() {
            val operationContext = ErrorContext.Operation(
                path = "/users",
                method = "POST",
                operationId = "createUser",
            )
            val report = createReport(
                errors = listOf(
                    GenerationError(
                        providerClass = "RequestBodyProvider",
                        message = "Unsupported media type: text/xml",
                        context = operationContext,
                    )
                ),
                summary = GenerationSummary(
                    totalOperations = 1,
                    successfulOperations = emptyList(),
                    notTestedOperations = emptyList(),
                    partialOperations = emptyList(),
                    failedOperations = listOf(OperationInfo("createUser", "/users", "POST")),
                    totalTestCases = 0,
                    totalErrors = 1,
                )
            )

            val result = reporter.format(report)

            assertThat(result).isEqualTo(
                """
                |Generation Report
                |==================================================
                |Total Operations: 1
                |Total Test Cases: 0
                |Successful: 0
                |Partial: 0
                |Failed: 1
                |  -> /users: POST (createUser)
                |Not Tested: 0
                |Total Errors: 1
                |
                |Errors:
                |  - POST /users (createUser): Unsupported media type: text/xml
                |""".trimMargin()
            )
        }

        @Test
        @DisplayName("should format report with parameter-level error")
        @Description("Verifies that errors with Parameter context include parameter details")
        fun shouldFormatReportWithParameterError() {
            val operationContext = ErrorContext.Operation(
                path = "/users/{id}",
                method = "GET",
                operationId = "getUserById",
            )
            val parameterContext = ErrorContext.Parameter(
                operation = operationContext,
                parameterName = "id",
                location = "path",
                ref = null,
            )
            val report = createReport(
                errors = listOf(
                    GenerationError(
                        providerClass = "ParameterProvider",
                        message = "Schema combination limit exceeded",
                        context = parameterContext,
                    )
                ),
                summary = GenerationSummary(
                    totalOperations = 1,
                    successfulOperations = emptyList(),
                    notTestedOperations = emptyList(),
                    partialOperations = listOf(OperationInfo("getUserById", "/users/{id}", "GET")),
                    failedOperations = emptyList(),
                    totalTestCases = 5,
                    totalErrors = 1,
                )
            )

            val result = reporter.format(report)

            assertThat(result).isEqualTo(
                """
                |Generation Report
                |==================================================
                |Total Operations: 1
                |Total Test Cases: 5
                |Successful: 0
                |Partial: 1
                |  -> /users/{id}: GET (getUserById)
                |Failed: 0
                |Not Tested: 0
                |Total Errors: 1
                |
                |Errors:
                |  - GET /users/{id} (getUserById) -> path parameter 'id': Schema combination limit exceeded
                |""".trimMargin()
            )
        }

        @Test
        @DisplayName("should format report with request body error")
        @Description("Verifies that errors with RequestBody context include request body details")
        fun shouldFormatReportWithRequestBodyError() {
            val operationContext = ErrorContext.Operation(
                path = "/orders",
                method = "POST",
                operationId = null,
            )
            val requestBodyContext = ErrorContext.RequestBody(
                operation = operationContext,
                ref = "#/components/schemas/Order",
            )
            val report = createReport(
                errors = listOf(
                    GenerationError(
                        providerClass = "RequestBodyProvider",
                        message = "Complex schema exceeded depth limit",
                        context = requestBodyContext,
                    )
                ),
                summary = GenerationSummary(
                    totalOperations = 1,
                    successfulOperations = emptyList(),
                    notTestedOperations = emptyList(),
                    partialOperations = listOf(OperationInfo(null, "/orders", "POST")),
                    failedOperations = emptyList(),
                    totalTestCases = 3,
                    totalErrors = 1,
                )
            )

            val result = reporter.format(report)

            assertThat(result).isEqualTo(
                """
                |Generation Report
                |==================================================
                |Total Operations: 1
                |Total Test Cases: 3
                |Successful: 0
                |Partial: 1
                |  -> /orders: POST
                |Failed: 0
                |Not Tested: 0
                |Total Errors: 1
                |
                |Errors:
                |  - POST /orders -> request body (#/components/schemas/Order): Complex schema exceeded depth limit
                |""".trimMargin()
            )
        }

        @Test
        @DisplayName("should format report with error containing exception text")
        @Description("Verifies that errors with exception text include the cause details")
        fun shouldFormatReportWithExceptionText() {
            val operationContext = ErrorContext.Operation(
                path = "/products",
                method = "PUT",
                operationId = "updateProduct",
            )
            val report = createReport(
                errors = listOf(
                    GenerationError(
                        providerClass = "SchemaProvider",
                        message = "Failed to process schema",
                        context = operationContext,
                        exceptionText = "java.lang.NullPointerException: Schema reference is null",
                    )
                ),
                summary = GenerationSummary(
                    totalOperations = 1,
                    successfulOperations = emptyList(),
                    notTestedOperations = emptyList(),
                    partialOperations = emptyList(),
                    failedOperations = listOf(OperationInfo("updateProduct", "/products", "PUT")),
                    totalTestCases = 0,
                    totalErrors = 1,
                )
            )

            val result = reporter.format(report)

            assertThat(result).isEqualTo(
                """
                |Generation Report
                |==================================================
                |Total Operations: 1
                |Total Test Cases: 0
                |Successful: 0
                |Partial: 0
                |Failed: 1
                |  -> /products: PUT (updateProduct)
                |Not Tested: 0
                |Total Errors: 1
                |
                |Errors:
                |  - PUT /products (updateProduct): Failed to process schema, cause:
                |java.lang.NullPointerException: Schema reference is null
                |""".trimMargin()
            )
        }

        @Test
        @DisplayName("should format report with multiple errors")
        @Description("Verifies that multiple errors are all listed in the errors section")
        fun shouldFormatReportWithMultipleErrors() {
            val operation1 = ErrorContext.Operation("/users", "POST", "createUser")
            val operation2 = ErrorContext.Operation("/orders", "POST", null)
            val report = createReport(
                errors = listOf(
                    GenerationError(
                        providerClass = "RequestBodyProvider",
                        message = "First error",
                        context = operation1,
                    ),
                    GenerationError(
                        providerClass = "ParameterProvider",
                        message = "Second error",
                        context = ErrorContext.Parameter(operation2, "status", "query", null),
                    ),
                ),
                summary = GenerationSummary(
                    totalOperations = 2,
                    successfulOperations = emptyList(),
                    notTestedOperations = emptyList(),
                    partialOperations = listOf(
                        OperationInfo("createUser", "/users", "POST"),
                        OperationInfo(null, "/orders", "POST"),
                    ),
                    failedOperations = emptyList(),
                    totalTestCases = 8,
                    totalErrors = 2,
                )
            )

            val result = reporter.format(report)

            assertThat(result).isEqualTo(
                """
                |Generation Report
                |==================================================
                |Total Operations: 2
                |Total Test Cases: 8
                |Successful: 0
                |Partial: 2
                |  -> /users: POST (createUser)
                |  -> /orders: POST
                |Failed: 0
                |Not Tested: 0
                |Total Errors: 2
                |
                |Errors:
                |  - POST /users (createUser): First error
                |  - POST /orders -> query parameter 'status': Second error
                |""".trimMargin()
            )
        }
    }

    @Nested
    @DisplayName("Combined Scenarios")
    inner class CombinedScenariosTests {

        @Test
        @DisplayName("should format comprehensive report with all operation types and errors")
        @Description("Verifies that a complex report with mixed operation statuses and errors is formatted correctly")
        fun shouldFormatComprehensiveReport() {
            val report = createReport(
                successfulSuites = listOf(
                    TestSuite("/users", "GET", "getUsers", emptyList()),
                    TestSuite("/health", "GET", "healthCheck", emptyList()),
                ),
                errors = listOf(
                    GenerationError(
                        providerClass = "RequestBodyProvider",
                        message = "Budget exceeded for complex schema",
                        context = ErrorContext.Operation("/complex", "POST", "createComplex"),
                    ),
                ),
                summary = GenerationSummary(
                    totalOperations = 5,
                    successfulOperations = listOf(
                        OperationInfo("getUsers", "/users", "GET"),
                        OperationInfo("healthCheck", "/health", "GET"),
                    ),
                    notTestedOperations = listOf(
                        OperationInfo("deleteUser", "/users/{id}", "DELETE"),
                    ),
                    partialOperations = listOf(
                        OperationInfo("updateUser", "/users/{id}", "PUT"),
                    ),
                    failedOperations = listOf(
                        OperationInfo("createComplex", "/complex", "POST"),
                    ),
                    totalTestCases = 42,
                    totalErrors = 1,
                )
            )

            val result = reporter.format(report)

            assertThat(result).isEqualTo(
                """
                |Generation Report
                |==================================================
                |Total Operations: 5
                |Total Test Cases: 42
                |Successful: 2
                |Partial: 1
                |  -> /users/{id}: PUT (updateUser)
                |Failed: 1
                |  -> /complex: POST (createComplex)
                |Not Tested: 1
                |  -> /users/{id}: DELETE (deleteUser)
                |Total Errors: 1
                |
                |Errors:
                |  - POST /complex (createComplex): Budget exceeded for complex schema
                |""".trimMargin()
            )
        }
    }

    @Nested
    @DisplayName("OperationInfo toString Tests")
    inner class OperationInfoToStringTests {

        @Test
        @DisplayName("should format operation info with operationId")
        @Description("Verifies that OperationInfo.toString() includes operationId in parentheses")
        fun shouldFormatOperationInfoWithOperationId() {
            val operationInfo = OperationInfo("getUsers", "/users", "GET")

            assertThat(operationInfo.toString()).isEqualTo("/users: GET (getUsers)")
        }

        @Test
        @DisplayName("should format operation info without operationId")
        @Description("Verifies that OperationInfo.toString() omits parentheses when operationId is null")
        fun shouldFormatOperationInfoWithoutOperationId() {
            val operationInfo = OperationInfo(null, "/health", "GET")

            assertThat(operationInfo.toString()).isEqualTo("/health: GET")
        }
    }

    @Nested
    @DisplayName("ErrorContext toString Tests")
    inner class ErrorContextToStringTests {

        @Test
        @DisplayName("should format Operation context with operationId")
        @Description("Verifies that Operation.toString() produces human-readable format with operationId")
        fun shouldFormatOperationContextWithOperationId() {
            val context = ErrorContext.Operation(
                path = "/users",
                method = "post",
                operationId = "createUser",
            )

            assertThat(context.toString()).isEqualTo("POST /users (createUser)")
        }

        @Test
        @DisplayName("should format Operation context without operationId")
        @Description("Verifies that Operation.toString() omits parentheses when operationId is null")
        fun shouldFormatOperationContextWithoutOperationId() {
            val context = ErrorContext.Operation(
                path = "/health",
                method = "GET",
                operationId = null,
            )

            assertThat(context.toString()).isEqualTo("GET /health")
        }

        @Test
        @DisplayName("should format Parameter context")
        @Description("Verifies that Parameter.toString() includes operation and parameter details")
        fun shouldFormatParameterContext() {
            val context = ErrorContext.Parameter(
                operation = ErrorContext.Operation("/users/{id}", "GET", "getUserById"),
                parameterName = "id",
                location = "path",
                ref = null,
            )

            assertThat(context.toString()).isEqualTo("GET /users/{id} (getUserById) -> path parameter 'id'")
        }

        @Test
        @DisplayName("should format RequestBody context with ref")
        @Description("Verifies that RequestBody.toString() includes operation and ref details")
        fun shouldFormatRequestBodyContextWithRef() {
            val context = ErrorContext.RequestBody(
                operation = ErrorContext.Operation("/users", "POST", "createUser"),
                ref = "#/components/schemas/User",
            )

            assertThat(context.toString()).isEqualTo("POST /users (createUser) -> request body (#/components/schemas/User)")
        }

        @Test
        @DisplayName("should format RequestBody context without ref")
        @Description("Verifies that RequestBody.toString() omits ref when null")
        fun shouldFormatRequestBodyContextWithoutRef() {
            val context = ErrorContext.RequestBody(
                operation = ErrorContext.Operation("/users", "POST", null),
                ref = null,
            )

            assertThat(context.toString()).isEqualTo("POST /users -> request body")
        }
    }

    private fun createReport(
        successfulSuites: List<TestSuite> = emptyList(),
        errors: List<GenerationError> = emptyList(),
        summary: GenerationSummary,
    ): GenerationReport = GenerationReport(
        successfulSuites = successfulSuites,
        errors = errors,
        summary = summary,
    )
}

