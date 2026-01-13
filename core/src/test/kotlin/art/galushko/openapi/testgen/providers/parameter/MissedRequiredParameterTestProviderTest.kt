package art.galushko.openapi.testgen.providers.parameter

import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.model.with
import art.galushko.openapi.testgen.providers.TestProviderTest
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.parameters.CookieParameter
import io.swagger.v3.oas.models.parameters.HeaderParameter
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.PathParameter
import io.swagger.v3.oas.models.parameters.QueryParameter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Feature("Missed Required Parameter Test Provider")
class MissedRequiredParameterTestProviderTest : TestProviderTest() {
    private val provider = MissedRequiredParameterTestProvider()

    fun parameterProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Required query parameter",
            QueryParameter().name("param").required(true),
            createBasicTestCase(queryParams = mapOf("param" to "value")),
            listOf(
                createBasicTestCase(
                    name = "Missed Required Query Parameter param",
                    queryParams = mapOf(),
                    expectedStatusCode = 400,
                    rule = MissedRequiredParameterTestProvider::class.java.name
                )
            )
        ),
        Arguments.of(
            "Required header parameter",
            HeaderParameter().name("param").required(true),
            createBasicTestCase(headers = listOf("param" with "value")),
            listOf(
                createBasicTestCase(
                    name = "Missed Required Header Parameter param",
                    headers = listOf(),
                    expectedStatusCode = 400,
                    rule = MissedRequiredParameterTestProvider::class.java.name
                )
            )
        ),
        Arguments.of(
            "Required cookie parameter",
            CookieParameter().name("param").required(true),
            createBasicTestCase(cookie = listOf("param" with "value")),
            listOf(
                createBasicTestCase(
                    name = "Missed Required Cookie Parameter param",
                    cookie = listOf(),
                    expectedStatusCode = 400,
                    rule = MissedRequiredParameterTestProvider::class.java.name
                )
            )
        ),
        Arguments.of(
            "Non-required query parameter",
            QueryParameter().name("param").required(false),
            createBasicTestCase(queryParams = mapOf("param" to "value")),
            listOf<TestCase>()
        ),
        Arguments.of(
            "Required path parameter (should be skipped)",
            PathParameter().name("param").required(true),
            createBasicTestCase(pathParams = mapOf("param" to "value")),
            listOf<TestCase>()
        )
    )

    @ParameterizedTest
    @MethodSource("parameterProvider")
    @DisplayName("provideTestCases should return test cases for missed required parameters")
    @Description("Verifies that the provideTestCases method returns test cases for missed required parameters")
    fun provideTestCasesShouldReturnTestCasesForMissedRequiredParameters(
        scenario: String,
        parameter: Parameter,
        validCase: TestCase,
        expectedTestCases: List<TestCase>,
    ) {
        when (val outcome = provider.provideTestCases(parameter, createTestContext(validCase, Operation(), OpenAPI()))) {
            is Outcome.Success -> assertThat(outcome.value).`as`("Test cases are different!").isEqualTo(expectedTestCases)
            is Outcome.PartialSuccess -> assertThat(outcome.value).`as`("Test cases are different!").isEqualTo(expectedTestCases)
            is Outcome.Failure -> assertThat(expectedTestCases).isEmpty() // failure occurs only for unsupported params
        }
    }

    @Test
    @DisplayName("provideTestCases should return Failure on raw parameter")
    @Description("Verifies that Failure outcome is returned on raw parameter type")
    fun shouldReturnFailureOnRawParameter() {
        val param = Parameter().name("param").required(true)
        val outcome = provider.provideTestCases(
            param, createTestContext(
                createBasicTestCase(queryParams = mapOf("param" to "value")),
                Operation(),
                OpenAPI(),
            )
        )
        assertThat(outcome).isInstanceOf(Outcome.Failure::class.java)
    }
}


