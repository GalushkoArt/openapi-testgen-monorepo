package art.galushko.openapi.testgen.model.error

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GenerationReport and error model")
class GenerationReportTest {

    private val emptySummary = GenerationSummary(
        totalOperations = 0,
        successfulOperations = emptyList(),
        notTestedOperations = emptyList(),
        partialOperations = emptyList(),
        failedOperations = emptyList(),
        totalTestCases = 0,
        totalErrors = 0,
    )

    private val error = GenerationError(
        providerClass = "art.galushko.openapi.testgen.providers.SomeProvider",
        message = "boom",
        context = ErrorContext.Operation(path = "/pets", method = "get", operationId = null),
    )

    @Test
    @DisplayName("hasErrors should be false for an error-free report")
    fun hasErrorsFalseWithoutErrors() {
        val report = GenerationReport(successfulSuites = emptyList(), errors = emptyList(), summary = emptySummary)

        assertThat(report.hasErrors).isFalse()
    }

    @Test
    @DisplayName("hasErrors should be true when errors are present")
    fun hasErrorsTrueWithErrors() {
        val report = GenerationReport(successfulSuites = emptyList(), errors = listOf(error), summary = emptySummary)

        assertThat(report.hasErrors).isTrue()
    }

    @Test
    @DisplayName("OperationInfo should render path, method, and optional operationId")
    fun operationInfoRendering() {
        assertThat(OperationInfo("listPets", "/pets", "GET").toString()).isEqualTo("/pets: GET (listPets)")
        assertThat(OperationInfo(null, "/pets", "GET").toString()).isEqualTo("/pets: GET")
    }

    @Test
    @DisplayName("ErrorHandlingConfig should default to collecting up to 100 errors")
    fun errorHandlingConfigDefaults() {
        val config = ErrorHandlingConfig()

        assertThat(config.mode).isEqualTo(ErrorMode.COLLECT_ALL)
        assertThat(config.maxErrors).isEqualTo(100)
    }

    @Test
    @DisplayName("GenerationError should carry optional exception text")
    fun generationErrorDefaults() {
        assertThat(error.exceptionText).isNull()
        assertThat(error.copy(exceptionText = "stack").exceptionText).isEqualTo("stack")
    }

    @Test
    @DisplayName("Outcome variants should expose their values and errors")
    fun outcomeVariants() {
        val success: Outcome<Int> = Outcome.Success(42)
        val partial: Outcome<Int> = Outcome.PartialSuccess(21, listOf(error))
        val failure: Outcome<Int> = Outcome.Failure(listOf(error))

        assertThat((success as Outcome.Success).value).isEqualTo(42)
        assertThat((partial as Outcome.PartialSuccess).value).isEqualTo(21)
        assertThat(partial.errors).containsExactly(error)
        assertThat((failure as Outcome.Failure).errors).containsExactly(error)
    }
}
