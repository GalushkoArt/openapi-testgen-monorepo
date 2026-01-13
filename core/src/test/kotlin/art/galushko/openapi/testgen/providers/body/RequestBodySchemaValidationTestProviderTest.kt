package art.galushko.openapi.testgen.providers.body

import art.galushko.openapi.testgen.generation.TestGeneratorConfigurer
import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.providers.TestProviderTest
import art.galushko.openapi.testgen.rules.composed.ObjectItemSchemaValidationRule
import art.galushko.openapi.testgen.util.Consts.APPLICATION_JSON
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.RequestBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.stream.Stream

@Feature("Request Body Schema Validation Test Provider")
class RequestBodySchemaValidationTestProviderTest : TestProviderTest() {
    private val provider = RequestBodySchemaValidationTestProvider(TestGeneratorConfigurer.getSchemaValidationRules())

    @Suppress("LongMethod")
    fun requestBodyProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "JSON request body with string schema property",
            jsonBody(ObjectSchema().addProperty("prop", StringSchema().minLength(3).example("valid"))),
            createBasicTestCase(body = mapOf("param" to "valid")),
            listOf(
                createBasicTestCase(
                    name = "Incorrect Request Body: Object Property prop Out Of Minimum Length String",
                    body = mapOf("prop" to "aa"),
                    expectedStatusCode = 400,
                    rule = ObjectItemSchemaValidationRule::class.java.name,
                )
            )
        ),
        Arguments.of(
            "JSON request body with string schema properties",
            jsonBody(
                ObjectSchema().addProperty(
                    "prop", StringSchema().minLength(3).maxLength(5).example("valid")
                ).addProperty(
                    "prop2", StringSchema().example("valid2")
                )
            ),
            createBasicTestCase(body = mapOf("param" to "valid")),
            listOf(
                createBasicTestCase(
                    name = "Incorrect Request Body: Object Property prop Out Of Maximum Length String",
                    body = mapOf("prop" to "aaaaaa"),
                    expectedStatusCode = 400,
                    rule = ObjectItemSchemaValidationRule::class.java.name,
                ),
                createBasicTestCase(
                    name = "Incorrect Request Body: Object Property prop Out Of Minimum Length String",
                    body = mapOf("prop" to "aa"),
                    expectedStatusCode = 400,
                    rule = ObjectItemSchemaValidationRule::class.java.name,
                )
            )
        ),
        Arguments.of(
            "JSON request body with general schema property",
            jsonBody(ObjectSchema().addProperty("prop", Schema<Any>().example("valid"))),
            createBasicTestCase(body = mapOf("param" to "valid")),
            listOf<TestCase>()
        ),
        Arguments.of(
            "JSON request body without properties",
            jsonBody(ObjectSchema()),
            createBasicTestCase(body = mapOf("param" to "valid")),
            listOf<TestCase>()
        ),
        Arguments.of(
            "JSON request body with general body schema",
            jsonBody(Schema<Any>()),
            createBasicTestCase(body = mapOf("param" to "valid")),
            listOf<TestCase>()
        ),
        Arguments.of(
            "JSON request body without body schema",
            jsonBody(null),
            createBasicTestCase(body = mapOf("param" to "valid")),
            listOf<TestCase>()
        ),
        Arguments.of(
            "Request body with unsupported media type",
            RequestBody().content(
                Content().addMediaType(
                    "text/html", MediaType().schema(
                        ObjectSchema().addProperty("prop", IntegerSchema().example(BigDecimal.TEN))
                    )
                )
            ),
            createBasicTestCase(body = mapOf("param" to "valid")),
            listOf<TestCase>()
        ),
        Arguments.of(
            "Request body without media types",
            RequestBody().content(Content()),
            createBasicTestCase(body = mapOf("param" to "valid")),
            listOf<TestCase>()
        ),
        Arguments.of(
            "Request body without content",
            RequestBody(),
            createBasicTestCase(body = mapOf("param" to "valid")),
            listOf<TestCase>()
        )
    )

    private fun jsonBody(schema: Schema<*>?): RequestBody =
        RequestBody().content(Content().addMediaType(APPLICATION_JSON, MediaType().schema(schema)))

    @ParameterizedTest
    @MethodSource("requestBodyProvider")
    @DisplayName("provideTestCases should return test cases for request body schema validation")
    @Description("Verifies that the provideTestCases method returns test cases for request body schema validation")
    fun provideTestCasesShouldReturnTestCasesForRequestBodySchemaValidation(
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


