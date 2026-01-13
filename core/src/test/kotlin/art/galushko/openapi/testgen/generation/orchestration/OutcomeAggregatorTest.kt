package art.galushko.openapi.testgen.generation.orchestration

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.model.error.ErrorContext
import art.galushko.openapi.testgen.model.error.GenerationError
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.spi.TestCaseProvider
import io.qameta.allure.Feature
import io.qameta.allure.Story
import io.swagger.v3.oas.models.Operation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Feature("Test Generation")
@Story("Outcome Aggregation")
class OutcomeAggregatorTest {

    private val aggregator = OutcomeAggregator()
    private val path = "/pets"
    private val method = "GET"
    private val operationName = "listPets"

    @Nested
    @DisplayName("createEmptyResult")
    inner class CreateEmptyResultTest {
        @Test
        @DisplayName("Should create result with empty test cases and errors")
        fun shouldCreateEmptyResult() {
            val result = aggregator.createEmptyResult()

            assertThat(result.testCases).isEmpty()
            assertThat(result.errors).isEmpty()
        }
    }

    @Nested
    @DisplayName("addProviderResult")
    inner class AddProviderResultTest {

        @Test
        @DisplayName("Should add test cases from Success outcome")
        fun shouldAddTestCasesFromSuccess() {
            val case1 = TestCase(name = "test1", method = "GET", path = "/pets")
            val case2 = TestCase(name = "test2", method = "GET", path = "/pets")
            val providerResult = ProviderResult(
                provider = dummyProvider(),
                outcome = Outcome.Success(listOf(case1, case2)),
            )

            val aggregated = aggregator.createEmptyResult()
            aggregator.addProviderResult(aggregated, providerResult)

            assertThat(aggregated.testCases).containsExactly(case1, case2)
            assertThat(aggregated.errors).isEmpty()
        }

        @Test
        @DisplayName("Should add test cases and errors from PartialSuccess outcome")
        fun shouldAddTestCasesAndErrorsFromPartialSuccess() {
            val case1 = TestCase(name = "test1", method = "GET", path = "/pets")
            val error = GenerationError(
                providerClass = "TestProvider",
                message = "Warning",
                context = ErrorContext.Operation("/pets", "GET", "listPets"),
            )
            val providerResult = ProviderResult(
                provider = dummyProvider(),
                outcome = Outcome.PartialSuccess(listOf(case1), listOf(error)),
            )

            val aggregated = aggregator.createEmptyResult()
            aggregator.addProviderResult(aggregated, providerResult)

            assertThat(aggregated.testCases).containsExactly(case1)
            assertThat(aggregated.errors).containsExactly(error)
        }

        @Test
        @DisplayName("Should add errors from Failure outcome")
        fun shouldAddErrorsFromFailure() {
            val error = GenerationError(
                providerClass = "TestProvider",
                message = "Error occurred",
                context = ErrorContext.Operation("/pets", "GET", "listPets"),
            )
            val providerResult = ProviderResult(
                provider = dummyProvider(),
                outcome = Outcome.Failure(listOf(error)),
            )

            val aggregated = aggregator.createEmptyResult()
            aggregator.addProviderResult(aggregated, providerResult)

            assertThat(aggregated.testCases).isEmpty()
            assertThat(aggregated.errors).containsExactly(error)
        }

        @Test
        @DisplayName("Should accumulate results from multiple providers")
        fun shouldAccumulateResultsFromMultipleProviders() {
            val case1 = TestCase(name = "test1", method = "GET", path = "/pets")
            val case2 = TestCase(name = "test2", method = "GET", path = "/pets")
            val error = GenerationError(
                providerClass = "TestProvider",
                message = "Warning",
                context = ErrorContext.Operation("/pets", "GET", "listPets"),
            )

            val successResult = ProviderResult(
                provider = dummyProvider(),
                outcome = Outcome.Success(listOf(case1)),
            )
            val partialResult = ProviderResult(
                provider = dummyProvider(),
                outcome = Outcome.PartialSuccess(listOf(case2), listOf(error)),
            )

            val aggregated = aggregator.createEmptyResult()
            aggregator.addProviderResult(aggregated, successResult)
            aggregator.addProviderResult(aggregated, partialResult)

            assertThat(aggregated.testCases).containsExactly(case1, case2)
            assertThat(aggregated.errors).containsExactly(error)
        }
    }

    @Nested
    @DisplayName("buildOutcome")
    inner class BuildOutcomeTest {

        @Test
        @DisplayName("Should prepend valid case when provided")
        fun shouldPrependValidCaseWhenProvided() {
            val validCase = TestCase(
                name = "Test Valid Case",
                method = "GET",
                path = "/pets",
                expectedStatusCode = 200,
            )
            val invalidCase = TestCase(
                name = "Invalid test",
                method = "GET",
                path = "/pets",
                expectedStatusCode = 400,
                rule = "SomeRule",
            )
            val aggregated = AggregatedResult(
                testCases = mutableListOf(invalidCase),
                errors = mutableListOf(),
            )

            val outcome = aggregator.buildOutcome(path, method, operationName, aggregated, validCase)

            val expectedSuite = TestSuite(
                path = path,
                method = method,
                operationName = operationName,
                testCases = listOf(validCase, invalidCase),
            )

            assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
            val success = outcome as Outcome.Success
            assertThat(success.value).usingRecursiveComparison().isEqualTo(expectedSuite)
        }

        @Test
        @DisplayName("Should not include valid case when null")
        fun shouldNotIncludeValidCaseWhenNull() {
            val invalidCase = TestCase(
                name = "Invalid test",
                method = "GET",
                path = "/pets",
                expectedStatusCode = 400,
                rule = "SomeRule",
            )
            val aggregated = AggregatedResult(
                testCases = mutableListOf(invalidCase),
                errors = mutableListOf(),
            )

            val outcome = aggregator.buildOutcome(path, method, operationName, aggregated, null)

            val expectedSuite = TestSuite(
                path = path,
                method = method,
                operationName = operationName,
                testCases = listOf(invalidCase),
            )

            assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
            val success = outcome as Outcome.Success
            assertThat(success.value).usingRecursiveComparison().isEqualTo(expectedSuite)
        }

        @Test
        @DisplayName("Should include only valid case when no provider results")
        fun shouldIncludeOnlyValidCaseWhenNoProviderResults() {
            val validCase = TestCase(
                name = "Test Valid Case",
                method = "POST",
                path = "/pets",
                expectedStatusCode = 201,
            )
            val aggregated = AggregatedResult(
                testCases = mutableListOf(),
                errors = mutableListOf(),
            )

            val outcome = aggregator.buildOutcome(path, method, operationName, aggregated, validCase)

            val expectedSuite = TestSuite(
                path = path,
                method = method,
                operationName = operationName,
                testCases = listOf(validCase),
            )

            assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
            val success = outcome as Outcome.Success
            assertThat(success.value).usingRecursiveComparison().isEqualTo(expectedSuite)
        }

        @Test
        @DisplayName("Should return Success when test cases present and no errors")
        fun shouldReturnSuccessWhenNoErrors() {
            val case1 = TestCase(name = "test1", method = "GET", path = "/pets")
            val aggregated = AggregatedResult(
                testCases = mutableListOf(case1),
                errors = mutableListOf(),
            )

            val outcome = aggregator.buildOutcome(path, method, operationName, aggregated)

            assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
            val success = outcome as Outcome.Success
            assertThat(success.value.path).isEqualTo(path)
            assertThat(success.value.method).isEqualTo(method)
            assertThat(success.value.operationName).isEqualTo(operationName)
            assertThat(success.value.testCases).containsExactly(case1)
        }

        @Test
        @DisplayName("Should return PartialSuccess when test cases and errors present")
        fun shouldReturnPartialSuccessWhenTestCasesAndErrors() {
            val case1 = TestCase(name = "test1", method = "GET", path = "/pets")
            val error = GenerationError(
                providerClass = "TestProvider",
                message = "Warning",
                context = ErrorContext.Operation("/pets", "GET", "listPets"),
            )
            val aggregated = AggregatedResult(
                testCases = mutableListOf(case1),
                errors = mutableListOf(error),
            )

            val outcome = aggregator.buildOutcome(path, method, operationName, aggregated)

            assertThat(outcome).isInstanceOf(Outcome.PartialSuccess::class.java)
            val partial = outcome as Outcome.PartialSuccess
            assertThat(partial.value.testCases).containsExactly(case1)
            assertThat(partial.errors).containsExactly(error)
        }

        @Test
        @DisplayName("Should return Failure when only errors present")
        fun shouldReturnFailureWhenOnlyErrors() {
            val error = GenerationError(
                providerClass = "TestProvider",
                message = "Error occurred",
                context = ErrorContext.Operation("/pets", "GET", "listPets"),
            )
            val aggregated = AggregatedResult(
                testCases = mutableListOf(),
                errors = mutableListOf(error),
            )

            val outcome = aggregator.buildOutcome(path, method, operationName, aggregated)

            assertThat(outcome).isInstanceOf(Outcome.Failure::class.java)
            val failure = outcome as Outcome.Failure
            assertThat(failure.errors).containsExactly(error)
        }

        @Test
        @DisplayName("Should return Success when no test cases and no errors")
        fun shouldReturnSuccessWhenEmpty() {
            val aggregated = AggregatedResult(
                testCases = mutableListOf(),
                errors = mutableListOf(),
            )

            val outcome = aggregator.buildOutcome(path, method, operationName, aggregated)

            assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
            val success = outcome as Outcome.Success
            assertThat(success.value.testCases).isEmpty()
        }
    }

    private fun dummyProvider(): TestCaseProvider<Operation> =
        object : TestCaseProvider<Operation> {
            override fun provideTestCases(spec: Operation, context: TestGenerationContext): Outcome<List<TestCase>> =
                Outcome.Success(emptyList())
        }
}
