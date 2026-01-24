package art.galushko.openapi.testgen.generation

import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGeneratorFactory
import art.galushko.openapi.testgen.example.openapi.SchemaMerger
import art.galushko.openapi.testgen.example.response.ResponseExampleExtractor
import art.galushko.openapi.testgen.generation.budget.TestCaseBudgetValidator
import art.galushko.openapi.testgen.generation.orchestration.OutcomeAggregator
import art.galushko.openapi.testgen.generation.orchestration.ProviderOrchestrator
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.TestSuite
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
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@Feature("DefaultTestSuiteGenerator")
class DefaultTestSuiteGeneratorTest {

    private val openApi = OpenAPI()
    private val path = "/pets"
    private val method = "GET"
    private val schemaExampleValueGenerator = SchemaExampleValueGeneratorFactory().create()
    private val operation = Operation()
        .operationId("listPets")
        .responses(ApiResponses().addApiResponse("200", ApiResponse().description("Success")))

    @Test
    @DisplayName("Should return Success when no provider errors")
    @Story("Aggregate Success")
    fun shouldReturnSuccessWhenNoErrors() {
        val case = TestCase(name = "t1", method = method, path = path)
        val successProvider = constantOutcomeProvider(Outcome.Success(listOf(case)))
        val generator = createGenerator(listOf(successProvider))

        val outcome = generator.generateTestSuite(openApi, path, method, operation)
        assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
        val suite = (outcome as Outcome.Success<TestSuite>).value
        assertThat(suite.testCases).containsExactly(case)
        assertThat(suite.path).isEqualTo(path)
        assertThat(suite.method).isEqualTo(method)
    }

    @Test
    @DisplayName("Should include valid case with 2xx status when includeValidCase is true")
    @Story("Include Valid Case")
    fun shouldIncludeValidCaseWhenEnabled() {
        val invalidCase = TestCase(
            name = "t1",
            method = method,
            path = path,
            expectedStatusCode = 400,
        )
        val successProvider = constantOutcomeProvider(Outcome.Success(listOf(invalidCase)))
        val generator = createGenerator(listOf(successProvider), includeValidCase = true)

        val outcome = generator.generateTestSuite(openApi, path, method, operation)

        val expectedValidCase = TestCase(
            name = "Test Valid Case",
            method = method,
            path = path,
            expectedStatusCode = 200,
            needToComplete = true,
        )
        val expectedSuite = TestSuite(
            path = path,
            method = method,
            operationName = "listPets",
            testCases = listOf(expectedValidCase, invalidCase),
        )

        assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
        assertThat((outcome as Outcome.Success<TestSuite>).value)
            .usingRecursiveComparison()
            .isEqualTo(expectedSuite)
    }

    @Test
    @DisplayName("Should not include valid case when includeValidCase is false (default)")
    @Story("Include Valid Case")
    fun shouldNotIncludeValidCaseByDefault() {
        val invalidCase = TestCase(
            name = "t1",
            method = method,
            path = path,
            expectedStatusCode = 400,
        )
        val successProvider = constantOutcomeProvider(Outcome.Success(listOf(invalidCase)))
        val generator = createGenerator(listOf(successProvider), includeValidCase = false)

        val outcome = generator.generateTestSuite(openApi, path, method, operation)

        val expectedSuite = TestSuite(
            path = path,
            method = method,
            operationName = "listPets",
            testCases = listOf(invalidCase),
        )

        assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
        assertThat((outcome as Outcome.Success<TestSuite>).value)
            .usingRecursiveComparison()
            .isEqualTo(expectedSuite)
    }

    @Test
    @DisplayName("Should include valid case with 201 status when operation returns 201")
    @Story("Include Valid Case")
    fun shouldIncludeValidCaseWith201Status() {
        val operation201 = Operation()
            .operationId("createPet")
            .responses(ApiResponses().addApiResponse("201", ApiResponse().description("Created")))

        val generator = createGenerator(emptyList(), includeValidCase = true)

        val outcome = generator.generateTestSuite(openApi, path, "POST", operation201)

        val expectedValidCase = TestCase(
            name = "Test Valid Case",
            method = "POST",
            path = path,
            expectedStatusCode = 201,
            needToComplete = true,
        )
        val expectedSuite = TestSuite(
            path = path,
            method = "POST",
            operationName = "createPet",
            testCases = listOf(expectedValidCase),
        )

        assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
        assertThat((outcome as Outcome.Success<TestSuite>).value)
            .usingRecursiveComparison()
            .isEqualTo(expectedSuite)
    }

    @Test
    @DisplayName("Should return PartialSuccess when some provider errors with cases")
    @Story("Aggregate PartialSuccess")
    fun shouldReturnPartialSuccessWhenErrorsWithCases() {
        val case = TestCase(name = "t1", method = method, path = path)
        val successProvider = constantOutcomeProvider(Outcome.Success(listOf(case)))
        val error = GenerationError(
            providerClass = "some class",
            message = "oops",
            context = ErrorContext.Operation(path, "POST", "op2"),
        )
        val partialProvider = constantOutcomeProvider(
            Outcome.PartialSuccess(emptyList(), listOf(error))
        )
        val generator = createGenerator(listOf(successProvider, partialProvider))

        val outcome = generator.generateTestSuite(openApi, path, method, operation)
        assertThat(outcome).isInstanceOf(Outcome.PartialSuccess::class.java)
        val partial = outcome as Outcome.PartialSuccess<TestSuite>
        assertThat(partial.value.testCases).containsExactly(case)
        assertThat(partial.errors).containsExactly(error)
    }

    @Test
    @DisplayName("Should return Failure when only errors without cases")
    @Story("Aggregate Failure")
    fun shouldReturnFailureWhenOnlyErrors() {
        val error = GenerationError(
            providerClass = "some class",
            message = "bad",
            context = ErrorContext.Operation(path, "GET", "op1"),
        )
        val failingProvider = constantOutcomeProvider(
            Outcome.Failure(listOf(error))
        )
        val generator = createGenerator(listOf(failingProvider))

        val outcome = generator.generateTestSuite(openApi, path, method, operation)
        assertThat(outcome).isInstanceOf(Outcome.Failure::class.java)
        val failure = outcome as Outcome.Failure
        assertThat(failure.errors).containsExactly(error)
    }

    @Test
    @DisplayName("Should return Failure when operation is missing required responses")
    @Story("Valid Case Builder Failure")
    fun shouldReturnFailureWhenOperationMissingResponses() {
        val invalidOperation = Operation().operationId("noResponses")
        val successProvider = constantOutcomeProvider(
            Outcome.Success(listOf(TestCase(name = "test", method = method, path = path)))
        )
        val generator = createGenerator(listOf(successProvider))

        val outcome = generator.generateTestSuite(openApi, path, method, invalidOperation)
        assertThat(outcome).isInstanceOf(Outcome.Failure::class.java)
        val failure = outcome as Outcome.Failure
        assertThat(failure.errors).hasSize(1)
        assertThat(failure.errors[0].message).contains("Operation responses are null")
    }

    @Test
    @DisplayName("Should return Failure when operation has no success status code")
    @Story("Valid Case Builder Failure")
    fun shouldReturnFailureWhenOperationHasNoSuccessCode() {
        val operationWithoutSuccess = Operation()
            .operationId("noSuccess")
            .responses(ApiResponses().addApiResponse("400", ApiResponse().description("Bad Request")))
        val successProvider = constantOutcomeProvider(
            Outcome.Success(listOf(TestCase(name = "test", method = method, path = path)))
        )
        val generator = createGenerator(listOf(successProvider))

        val outcome = generator.generateTestSuite(openApi, path, method, operationWithoutSuccess)
        assertThat(outcome).isInstanceOf(Outcome.Failure::class.java)
        val failure = outcome as Outcome.Failure
        assertThat(failure.errors).hasSize(1)
        assertThat(failure.errors[0].message).contains("Success status code not found")
    }

    private fun constantOutcomeProvider(outcome: Outcome<List<TestCase>>): TestCaseProvider<Operation> =
        object : TestCaseProvider<Operation> {
            override fun provideTestCases(spec: Operation, context: TestGenerationContext): Outcome<List<TestCase>> = outcome
        }

    private fun createGenerator(
        providers: List<TestCaseProvider<Operation>>,
        maxTestCasesPerOperation: Int = 1000,
        includeValidCase: Boolean = false,
    ): DefaultTestSuiteGenerator {
        val components = DefaultTestSuiteGeneratorComponents(
            providerOrchestrator = ProviderOrchestrator(providers),
            outcomeAggregator = OutcomeAggregator(),
            budgetValidator = TestCaseBudgetValidator(maxTestCasesPerOperation),
            securityValueProvider = SecurityValueProvider(emptyMap()),
            basicTestDataProvider = BasicTestDataProvider(),
            schemaExampleValueGenerator = schemaExampleValueGenerator,
            responseExampleExtractor = ResponseExampleExtractor(schemaExampleValueGenerator),
            schemaMerger = SchemaMerger(),
        )
        return DefaultTestSuiteGenerator(
            components = components,
            includeValidCase = includeValidCase,
        )
    }
}


