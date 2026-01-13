package art.galushko.openapi.testgen.providers.body

import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.providers.TestProviderTest
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.parameters.RequestBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Feature("Missed Required Request Body Test Provider")
class MissedRequiredRequestBodyTestProviderTest : TestProviderTest() {
    private val provider = MissedRequiredRequestBodyTestProvider()

    fun requestBodyProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Required request body",
            RequestBody().required(true),
            createBasicTestCase(body = mapOf("param" to "value")),
            listOf(
                createBasicTestCase(
                    name = "Required Request Body is missing",
                    body = null,
                    expectedStatusCode = 400,
                    rule = MissedRequiredRequestBodyTestProvider::class.java.name,
                )
            )
        ),
        Arguments.of(
            "Non-required request body",
            RequestBody().required(false),
            createBasicTestCase(body = mapOf("param" to "value")),
            listOf<TestCase>()
        ),
        Arguments.of(
            "Request body with null required flag",
            RequestBody(),
            createBasicTestCase(body = mapOf("param" to "value")),
            listOf<TestCase>()
        )
    )

    @ParameterizedTest
    @MethodSource("requestBodyProvider")
    @DisplayName("provideTestCases should return test cases for missed required request body")
    @Description("Verifies that the provideTestCases method returns test cases for missed required request body")
    fun provideTestCasesShouldReturnTestCasesForMissedRequiredRequestBody(
        scenario: String,
        requestBody: RequestBody,
        validCase: TestCase,
        expectedTestCases: List<TestCase>
    ) {
        when (val outcome = provider.provideTestCases(requestBody, createTestContext(validCase, Operation(), OpenAPI()))) {
            is Outcome.Success -> assertThat(outcome.value).`as`("Test cases are different!").isEqualTo(expectedTestCases)
            is Outcome.PartialSuccess -> assertThat(outcome.value).`as`("Test cases are different!").isEqualTo(expectedTestCases)
            is Outcome.Failure -> assertThat(expectedTestCases).isEmpty()
        }
    }
}


