package art.galushko.openapi.testgen.providers.body

import art.galushko.openapi.testgen.example.util.APPLICATION_JSON
import art.galushko.openapi.testgen.example.util.TEXT_JSON
import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.generation.TestGeneratorConfigurer
import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.logging.TestLogCapture
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.providers.TestProviderTest
import art.galushko.openapi.testgen.rules.composed.ObjectItemSchemaValidationRule
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SchemaValidationRule
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.RequestBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.OffsetDateTime
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
                ).copy(requestBodyMediaType = APPLICATION_JSON)
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
                ).copy(requestBodyMediaType = APPLICATION_JSON),
                createBasicTestCase(
                    name = "Incorrect Request Body: Object Property prop Out Of Minimum Length String",
                    body = mapOf("prop" to "aa"),
                    expectedStatusCode = 400,
                    rule = ObjectItemSchemaValidationRule::class.java.name,
                ).copy(requestBodyMediaType = APPLICATION_JSON)
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

    @Test
    fun `provideTestCases should process all supported media types`() {
        val provider = RequestBodySchemaValidationTestProvider(listOf(schemaTitleRule))
        val requestBody = RequestBody().content(
            Content()
                .addMediaType(APPLICATION_JSON, MediaType().schema(ObjectSchema().title("json-schema")))
                .addMediaType(TEXT_JSON, MediaType().schema(ObjectSchema().title("text-json-schema")))
                .addMediaType("application/xml", MediaType().schema(ObjectSchema().title("xml-schema")))
        )
        val context = createTestContext(createBasicTestCase(body = mapOf("param" to "valid")), Operation(), OpenAPI())

        val testCases = extractOutcomeValue(provider.provideTestCases(requestBody, context))

        assertThat(testCases.map { it.body })
            .containsExactly(
                mapOf("source" to "json-schema"),
                mapOf("source" to "text-json-schema"),
                mapOf("source" to "xml-schema"),
            )
        assertThat(testCases.map { it.requestBodyMediaType })
            .containsExactly(
                APPLICATION_JSON,
                TEXT_JSON,
                "application/xml",
            )
    }

    @Test
    fun `provideTestCases should process jwt and plus jwt media types`() {
        val provider = RequestBodySchemaValidationTestProvider(listOf(schemaTitleRule))
        val requestBody = RequestBody().content(
            Content()
                .addMediaType("application/jwt", MediaType().schema(ObjectSchema().title("jwt-schema")))
                .addMediaType("application/secevent+jwt", MediaType().schema(ObjectSchema().title("jwt-suffix-schema")))
        )
        val context = createTestContext(createBasicTestCase(body = mapOf("param" to "valid")), Operation(), OpenAPI())

        val testCases = extractOutcomeValue(provider.provideTestCases(requestBody, context))

        assertThat(testCases.map { it.body })
            .containsExactly(
                mapOf("source" to "jwt-schema"),
                mapOf("source" to "jwt-suffix-schema"),
            )
    }

    @Test
    fun `provideTestCases should deduplicate identical cases from multiple supported media types`() {
        val duplicateRule = object : SchemaValidationRule {
            override fun getRuleName(): String = "duplicate"

            override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> =
                sequenceOf(RuleValue("Duplicate Case", mapOf("value" to "same")))
        }
        val provider = RequestBodySchemaValidationTestProvider(listOf(duplicateRule))
        val requestBody = RequestBody().content(
            Content()
                .addMediaType(APPLICATION_JSON, MediaType().schema(ObjectSchema().title("json-schema")))
                .addMediaType(TEXT_JSON, MediaType().schema(ObjectSchema().title("text-json-schema")))
                .addMediaType("application/hal+json", MediaType().schema(ObjectSchema().title("hal-schema")))
        )
        val context = createTestContext(createBasicTestCase(body = mapOf("param" to "valid")), Operation(), OpenAPI())

        val testCases = extractOutcomeValue(provider.provideTestCases(requestBody, context))

        assertThat(testCases).hasSize(1)
        assertThat(testCases.single().body).isEqualTo(mapOf("value" to "same"))
    }

    @Test
    fun `provideTestCases should log unsupported media types and continue with supported ones`() {
        val provider = RequestBodySchemaValidationTestProvider(listOf(schemaTitleRule))
        val requestBody = RequestBody().content(
            Content()
                .addMediaType(TEXT_JSON, MediaType().schema(ObjectSchema().title("text-json-schema")))
                .addMediaType("text/html", MediaType().schema(ObjectSchema().title("html-schema")))
        )
        val context = createTestContext(createBasicTestCase(body = mapOf("param" to "valid")), Operation(), OpenAPI())

        val (outcome, logs) = TestLogCapture.capture {
            provider.provideTestCases(requestBody, context)
        }
        val testCases = extractOutcomeValue(outcome)

        assertThat(logs).contains("Unsupported request body media type: text/html")
        assertThat(testCases.map { it.body }).containsExactly(mapOf("source" to "text-json-schema"))
    }

    @Test
    fun `provideTestCases should handle date-time examples represented as OffsetDateTime`() {
        val provider = RequestBodySchemaValidationTestProvider(listOf(schemaTitleRule))
        val requestBody = RequestBody().content(
            Content()
                .addMediaType(
                    APPLICATION_JSON,
                    MediaType().schema(
                        ObjectSchema().addProperty(
                            "end_time",
                            DateTimeSchema().example(OffsetDateTime.parse("2020-06-11T16:32:50-03:00"))
                        )
                    )
                )
        )
        val context = createTestContext(createBasicTestCase(body = mapOf("param" to "valid")), Operation(), OpenAPI())

        val outcome = provider.provideTestCases(requestBody, context)
        val testCases = extractOutcomeValue(outcome)

        assertThat(outcome).isNotInstanceOf(Outcome.Failure::class.java)
        assertThat(testCases.map { it.body }).containsExactly(mapOf("source" to "unknown"))
    }

    private fun extractOutcomeValue(outcome: Outcome<List<TestCase>>): List<TestCase> = when (outcome) {
        is Outcome.Success -> outcome.value
        is Outcome.PartialSuccess -> outcome.value
        is Outcome.Failure -> emptyList()
    }

    private val schemaTitleRule = object : SchemaValidationRule {
        override fun getRuleName(): String = "schema-title"

        override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> =
            sequenceOf(
                RuleValue(
                    description = "Schema ${schema.title ?: "unknown"}",
                    value = mapOf("source" to (schema.title ?: "unknown")),
                )
            )
    }
}
