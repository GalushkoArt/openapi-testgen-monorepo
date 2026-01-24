package art.galushko.openapi.testgen.plugin

import art.galushko.openapi.testgen.model.error.ErrorContext
import art.galushko.openapi.testgen.model.error.GenerationError
import art.galushko.openapi.testgen.model.error.GenerationReport
import art.galushko.openapi.testgen.model.error.GenerationSummary
import art.galushko.openapi.testgen.model.error.OperationInfo
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.internal.project.DefaultProject
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GradleReporter")
class GradleReporterTest {

    private lateinit var project: DefaultProject
    private lateinit var reporter: GradleReporter

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build() as DefaultProject
        reporter = GradleReporter(project.logger)
    }

    @Nested
    @DisplayName("logInfo")
    inner class LogInfoTest {

        @Test
        @DisplayName("should not throw when logging info message")
        fun shouldNotThrowWhenLoggingInfo() {
            // GradleReporter delegates to logger.lifecycle which should not throw
            reporter.logInfo("Test info message")
            // If we get here, the method completed successfully
        }

        @Test
        @DisplayName("should handle empty message")
        fun shouldHandleEmptyMessage() {
            reporter.logInfo("")
            // Should not throw
        }

        @Test
        @DisplayName("should handle multiple messages")
        fun shouldHandleMultipleMessages() {
            reporter.logInfo("First message")
            reporter.logInfo("Second message")
            // Should not throw
        }
    }

    @Nested
    @DisplayName("logError")
    inner class LogErrorTest {

        @Test
        @DisplayName("should not throw when logging error message")
        fun shouldNotThrowWhenLoggingError() {
            // GradleReporter delegates to logger.error which should not throw
            reporter.logError("Test error message")
            // If we get here, the method completed successfully
        }

        @Test
        @DisplayName("should handle empty message")
        fun shouldHandleEmptyMessage() {
            reporter.logError("")
            // Should not throw
        }

        @Test
        @DisplayName("should handle multiple error messages")
        fun shouldHandleMultipleMessages() {
            reporter.logError("First error")
            reporter.logError("Second error")
            // Should not throw
        }
    }

    @Nested
    @DisplayName("formatReport")
    inner class FormatReportTest {

        @Test
        @DisplayName("should format empty report")
        fun shouldFormatEmptyReport() {
            val report = GenerationReport(
                successfulSuites = emptyList(),
                errors = emptyList(),
                summary = GenerationSummary(
                    totalOperations = 0,
                    successfulOperations = emptyList(),
                    notTestedOperations = emptyList(),
                    partialOperations = emptyList(),
                    failedOperations = emptyList(),
                    totalTestCases = 0,
                    totalErrors = 0,
                ),
            )

            val formatted = reporter.formatReport(report)

            assertThat(formatted).contains("Generation Report")
            assertThat(formatted).contains("Total Operations: 0")
            assertThat(formatted).contains("Total Test Cases: 0")
            assertThat(formatted).contains("Successful: 0")
            assertThat(formatted).contains("Total Errors: 0")
        }

        @Test
        @DisplayName("should format report with successful operations")
        fun shouldFormatReportWithSuccessfulOperations() {
            val report = GenerationReport(
                successfulSuites = emptyList(),
                errors = emptyList(),
                summary = GenerationSummary(
                    totalOperations = 2,
                    successfulOperations = listOf(
                        OperationInfo("getUser", "/users/{id}", "GET"),
                        OperationInfo("listUsers", "/users", "GET"),
                    ),
                    notTestedOperations = emptyList(),
                    partialOperations = emptyList(),
                    failedOperations = emptyList(),
                    totalTestCases = 10,
                    totalErrors = 0,
                ),
            )

            val formatted = reporter.formatReport(report)

            assertThat(formatted).contains("Total Operations: 2")
            assertThat(formatted).contains("Total Test Cases: 10")
            assertThat(formatted).contains("Successful: 2")
        }

        @Test
        @DisplayName("should format report with errors")
        fun shouldFormatReportWithErrors() {
            val operationContext = ErrorContext.Operation(
                path = "/users",
                method = "POST",
                operationId = "createUser",
            )
            val report = GenerationReport(
                successfulSuites = emptyList(),
                errors = listOf(
                    GenerationError(
                        providerClass = "TestProvider",
                        message = "Test error message",
                        context = operationContext,
                    ),
                ),
                summary = GenerationSummary(
                    totalOperations = 1,
                    successfulOperations = emptyList(),
                    notTestedOperations = emptyList(),
                    partialOperations = emptyList(),
                    failedOperations = listOf(
                        OperationInfo("createUser", "/users", "POST"),
                    ),
                    totalTestCases = 0,
                    totalErrors = 1,
                ),
            )

            val formatted = reporter.formatReport(report)

            assertThat(formatted).contains("Total Errors: 1")
            assertThat(formatted).contains("Failed: 1")
            assertThat(formatted).contains("Errors:")
            assertThat(formatted).contains("Test error message")
        }

        @Test
        @DisplayName("should format report with partial operations")
        fun shouldFormatReportWithPartialOperations() {
            val report = GenerationReport(
                successfulSuites = emptyList(),
                errors = emptyList(),
                summary = GenerationSummary(
                    totalOperations = 1,
                    successfulOperations = emptyList(),
                    notTestedOperations = emptyList(),
                    partialOperations = listOf(
                        OperationInfo("updateUser", "/users/{id}", "PUT"),
                    ),
                    failedOperations = emptyList(),
                    totalTestCases = 5,
                    totalErrors = 0,
                ),
            )

            val formatted = reporter.formatReport(report)

            assertThat(formatted).contains("Partial: 1")
            assertThat(formatted).contains("/users/{id}: PUT (updateUser)")
        }

        @Test
        @DisplayName("should format report with not tested operations")
        fun shouldFormatReportWithNotTestedOperations() {
            val report = GenerationReport(
                successfulSuites = emptyList(),
                errors = emptyList(),
                summary = GenerationSummary(
                    totalOperations = 1,
                    successfulOperations = emptyList(),
                    notTestedOperations = listOf(
                        OperationInfo("deleteUser", "/users/{id}", "DELETE"),
                    ),
                    partialOperations = emptyList(),
                    failedOperations = emptyList(),
                    totalTestCases = 0,
                    totalErrors = 0,
                ),
            )

            val formatted = reporter.formatReport(report)

            assertThat(formatted).contains("Not Tested: 1")
            assertThat(formatted).contains("/users/{id}: DELETE (deleteUser)")
        }

        @Test
        @DisplayName("should format report with error that has exception text")
        fun shouldFormatReportWithExceptionText() {
            val operationContext = ErrorContext.Operation(
                path = "/users",
                method = "POST",
                operationId = "createUser",
            )
            val report = GenerationReport(
                successfulSuites = emptyList(),
                errors = listOf(
                    GenerationError(
                        providerClass = "TestProvider",
                        message = "Schema parsing failed",
                        context = operationContext,
                        exceptionText = "java.lang.RuntimeException: Invalid schema",
                    ),
                ),
                summary = GenerationSummary(
                    totalOperations = 1,
                    successfulOperations = emptyList(),
                    notTestedOperations = emptyList(),
                    partialOperations = emptyList(),
                    failedOperations = listOf(
                        OperationInfo("createUser", "/users", "POST"),
                    ),
                    totalTestCases = 0,
                    totalErrors = 1,
                ),
            )

            val formatted = reporter.formatReport(report)

            assertThat(formatted).contains("Schema parsing failed")
            assertThat(formatted).contains("java.lang.RuntimeException: Invalid schema")
        }
    }
}
