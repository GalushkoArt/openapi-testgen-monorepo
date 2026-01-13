package art.galushko.openapi.testgen.providers.parameter

import art.galushko.openapi.testgen.generation.TestGeneratorConfigurer
import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.model.with
import art.galushko.openapi.testgen.providers.TestProviderTest
import art.galushko.openapi.testgen.rules.composed.ArrayItemSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.AboveMaxItemsArraySchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.OutOfMinimumLengthStringSchemaValidationRule
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.StringSchema
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

@Feature("Parameter Schema Validation Test Provider")
class ParameterSchemaValidationTestProviderTest : TestProviderTest() {
    private val provider = ParameterSchemaValidationTestProvider(TestGeneratorConfigurer.getSchemaValidationRules())

    @Suppress("LongMethod")
    fun parameterProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Query parameter with string schema",
            QueryParameter().name("param").schema(StringSchema().minLength(3)),
            createBasicTestCase(queryParams = mapOf("param" to "valid")),
            listOf(
                createBasicTestCase(
                    name = "Invalid Query param parameter: Out Of Minimum Length String",
                    queryParams = mapOf("param" to "aa"),
                    expectedStatusCode = 400,
                    rule = OutOfMinimumLengthStringSchemaValidationRule::class.java.name,
                )
            )
        ),
        Arguments.of(
            "Path parameter with string schema",
            PathParameter().name("param").schema(StringSchema().minLength(3)),
            createBasicTestCase(pathParams = mapOf("param" to "valid")),
            listOf(
                createBasicTestCase(
                    name = "Invalid Path param parameter: Out Of Minimum Length String",
                    pathParams = mapOf("param" to "aa"),
                    expectedStatusCode = 400,
                    rule = OutOfMinimumLengthStringSchemaValidationRule::class.java.name,
                )
            )
        ),
        Arguments.of(
            "Header parameter with string schema",
            HeaderParameter().name("param").schema(StringSchema().minLength(3)),
            createBasicTestCase(headers = listOf("param" with "valid")),
            listOf(
                createBasicTestCase(
                    name = "Invalid Header param parameter: Out Of Minimum Length String",
                    headers = listOf("param" with "aa"),
                    expectedStatusCode = 400,
                    rule = OutOfMinimumLengthStringSchemaValidationRule::class.java.name,
                )
            )
        ),
        Arguments.of(
            "Cookie parameter with string schema",
            CookieParameter().name("param").schema(StringSchema().minLength(3)),
            createBasicTestCase(cookie = listOf("param" with "valid")),
            listOf(
                createBasicTestCase(
                    name = "Invalid Cookie param parameter: Out Of Minimum Length String",
                    cookie = listOf("param" with "aa"),
                    expectedStatusCode = 400,
                    rule = OutOfMinimumLengthStringSchemaValidationRule::class.java.name,
                )
            )
        ),
        Arguments.of(
            "Parameter with array schema requires several cases",
            CookieParameter().name("param")
                .schema(ArraySchema().items(StringSchema().minLength(3).maxLength(5).example("valid")).maxItems(2)),
            createBasicTestCase(cookie = listOf("param" with "valid")),
            listOf(
                createBasicTestCase(
                    name = "Invalid Cookie param parameter: Above Max Items Array",
                    cookie = listOf("param" with listOf("valid", "valid", "valid")),
                    expectedStatusCode = 400,
                    rule = AboveMaxItemsArraySchemaValidationRule::class.java.name,
                ),
                createBasicTestCase(
                    name = "Invalid Cookie param parameter: Array Item Out Of Maximum Length String",
                    cookie = listOf("param" with listOf("aaaaaa")),
                    expectedStatusCode = 400,
                    rule = ArrayItemSchemaValidationRule::class.java.name,
                ),
                createBasicTestCase(
                    name = "Invalid Cookie param parameter: Array Item Out Of Minimum Length String",
                    cookie = listOf("param" with listOf("aa")),
                    expectedStatusCode = 400,
                    rule = ArrayItemSchemaValidationRule::class.java.name,
                )
            )
        ),
        Arguments.of(
            "Parameter without schema",
            CookieParameter().name("query"),
            createBasicTestCase(cookie = listOf("param" with "valid")),
            listOf<TestCase>()
        )
    )

    @ParameterizedTest
    @MethodSource("parameterProvider")
    @DisplayName("provideTestCases should return test cases for parameter schema validation")
    @Description("Verifies that the provideTestCases method returns test cases for parameter schema validation")
    fun provideTestCasesShouldReturnTestCasesForParameterSchemaValidation(
        scenario: String,
        parameter: Parameter,
        validCase: TestCase,
        expectedTestCases: List<TestCase>,
    ) {
        when (val outcome = provider.provideTestCases(parameter, createTestContext(validCase = validCase))) {
            is Outcome.Success -> assertThat(outcome.value).`as`("Test cases are different!").isEqualTo(expectedTestCases)
            is Outcome.PartialSuccess -> assertThat(outcome.value).`as`("Test cases are different!").isEqualTo(expectedTestCases)
            is Outcome.Failure -> assertThat(expectedTestCases).isEmpty() // failure occurs only for unsupported params
        }
    }

    @Test
    @DisplayName("provideTestCases should return Failure on raw parameter with valid schema")
    @Description("Verifies that Failure outcome is returned on raw parameter type")
    fun shouldReturnFailureOnRawParameterWithValidSchema() {
        val param = Parameter().name("param").schema(StringSchema().minLength(3))
        val outcome = provider.provideTestCases(
            param,
            createTestContext(validCase = createBasicTestCase(cookie = listOf("param" with "valid"))),
        )
        assertThat(outcome).isInstanceOf(Outcome.Failure::class.java)
    }
}


