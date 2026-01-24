package art.galushko.openapi.testgen.generation.orchestration

import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGeneratorFactory
import art.galushko.openapi.testgen.example.openapi.SchemaMerger
import art.galushko.openapi.testgen.example.response.ResponseExampleExtractor
import art.galushko.openapi.testgen.generation.DefaultTestGenerationContext
import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.ErrorContext
import art.galushko.openapi.testgen.model.error.GenerationError
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.spi.TestCaseProvider
import art.galushko.openapi.testgen.testdata.BasicTestDataProvider
import art.galushko.openapi.testgen.testdata.SecurityValueProvider
import io.qameta.allure.Feature
import io.qameta.allure.Story
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@Feature("Test Generation")
@Story("Provider Orchestration")
class ProviderOrchestratorTest {

    private val operation = Operation().operationId("testOp")
    private val context: TestGenerationContext = DefaultTestGenerationContext(
        openAPI = OpenAPI(),
        operation = operation,
        validCase = TestCase(name = "valid", method = "GET", path = "/pets"),
        basicTestData = BasicTestDataProvider(),
        securityValueProvider = SecurityValueProvider(emptyMap()),
        schemaExampleValueGenerator = SchemaExampleValueGeneratorFactory().create(),
        responseExampleExtractor = ResponseExampleExtractor(SchemaExampleValueGeneratorFactory().create()),
        schemaMerger = SchemaMerger(),
        maxDepth = 10,
        combinationBudget = null,
    )

    @Test
    @DisplayName("Should execute all providers in order")
    fun shouldExecuteAllProvidersInOrder() {
        val case1 = TestCase(name = "test1", method = "GET", path = "/pets")
        val case2 = TestCase(name = "test2", method = "GET", path = "/pets")

        val provider1 = constantOutcomeProvider(Outcome.Success(listOf(case1)))
        val provider2 = constantOutcomeProvider(Outcome.Success(listOf(case2)))

        val orchestrator = ProviderOrchestrator(listOf(provider1, provider2))
        val results = orchestrator.executeProviders(operation, context).toList()

        assertThat(results).hasSize(2)
        assertThat(results[0].provider).isSameAs(provider1)
        assertThat(results[0].outcome).isInstanceOf(Outcome.Success::class.java)
        assertThat((results[0].outcome as Outcome.Success).value).containsExactly(case1)

        assertThat(results[1].provider).isSameAs(provider2)
        assertThat(results[1].outcome).isInstanceOf(Outcome.Success::class.java)
        assertThat((results[1].outcome as Outcome.Success).value).containsExactly(case2)
    }

    @Test
    @DisplayName("Should handle empty provider list")
    fun shouldHandleEmptyProviderList() {
        val orchestrator = ProviderOrchestrator(emptyList())
        val results = orchestrator.executeProviders(operation, context).toList()

        assertThat(results).isEmpty()
    }

    @Test
    @DisplayName("Should preserve provider outcomes including failures")
    fun shouldPreserveProviderOutcomes() {
        val case1 = TestCase(name = "test1", method = "GET", path = "/pets")
        val error = GenerationError(
            providerClass = "TestProvider",
            message = "Error occurred",
            context = ErrorContext.Operation("/pets", "GET", "testOp"),
        )

        val successProvider = constantOutcomeProvider(Outcome.Success(listOf(case1)))
        val failureProvider = constantOutcomeProvider(Outcome.Failure(listOf(error)))

        val orchestrator = ProviderOrchestrator(listOf(successProvider, failureProvider))
        val results = orchestrator.executeProviders(operation, context).toList()

        assertThat(results).hasSize(2)
        assertThat(results[0].outcome).isInstanceOf(Outcome.Success::class.java)
        assertThat(results[1].outcome).isInstanceOf(Outcome.Failure::class.java)
        assertThat((results[1].outcome as Outcome.Failure).errors).containsExactly(error)
    }

    @Test
    @DisplayName("Should handle partial success outcomes")
    fun shouldHandlePartialSuccessOutcomes() {
        val case1 = TestCase(name = "test1", method = "GET", path = "/pets")
        val error = GenerationError(
            providerClass = "TestProvider",
            message = "Warning occurred",
            context = ErrorContext.Operation("/pets", "GET", "testOp"),
        )

        val partialProvider = constantOutcomeProvider(
            Outcome.PartialSuccess(listOf(case1), listOf(error))
        )

        val orchestrator = ProviderOrchestrator(listOf(partialProvider))
        val results = orchestrator.executeProviders(operation, context).toList()

        assertThat(results).hasSize(1)
        assertThat(results[0].outcome).isInstanceOf(Outcome.PartialSuccess::class.java)
        val partial = results[0].outcome as Outcome.PartialSuccess
        assertThat(partial.value).containsExactly(case1)
        assertThat(partial.errors).containsExactly(error)
    }

    private fun constantOutcomeProvider(outcome: Outcome<List<TestCase>>): TestCaseProvider<Operation> =
        object : TestCaseProvider<Operation> {
            override fun provideTestCases(spec: Operation, context: TestGenerationContext): Outcome<List<TestCase>> = outcome
        }
}
