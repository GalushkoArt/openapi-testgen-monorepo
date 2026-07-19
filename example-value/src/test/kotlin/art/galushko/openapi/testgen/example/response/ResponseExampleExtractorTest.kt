package art.galushko.openapi.testgen.example.response

import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGeneratorFactory
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class ResponseExampleExtractorTest {
    private val extractor = ResponseExampleExtractor(SchemaExampleValueGeneratorFactory().create())
    private val openApi = OpenAPI()

    @Test
    @DisplayName("extractExpectedResponseExampleWithMediaType should return named example with its media type")
    fun shouldReturnNamedExampleAndMediaType() {
        val operation = Operation().responses(
            ApiResponses().addApiResponse(
                "200",
                ApiResponse().content(
                    Content()
                        .addMediaType(
                            "application/json",
                            MediaType().examples(
                                mapOf("sample" to Example().value(mapOf("source" to "json")))
                            )
                        )
                        .addMediaType(
                            "application/xml",
                            MediaType().examples(
                                mapOf("sample" to Example().value("<source>xml</source>"))
                            )
                        )
                )
            )
        )

        val extracted = extractor.extractExpectedResponseExampleWithMediaType(operation, openApi, 200, "sample")

        assertThat(extracted.body).isEqualTo(mapOf("source" to "json"))
        assertThat(extracted.mediaType).isEqualTo("application/json")
    }

    @Test
    @DisplayName("extractExpectedResponseExampleWithMediaType should return explicit example by media priority")
    fun shouldReturnExplicitExampleByPriority() {
        val operation = Operation().responses(
            ApiResponses().addApiResponse(
                "200",
                ApiResponse().content(
                    Content()
                        .addMediaType("text/plain", MediaType().example("plain"))
                        .addMediaType("application/json", MediaType().example(mapOf("source" to "json")))
                )
            )
        )

        val extracted = extractor.extractExpectedResponseExampleWithMediaType(operation, openApi, 200)

        assertThat(extracted.body).isEqualTo(mapOf("source" to "json"))
        assertThat(extracted.mediaType).isEqualTo("application/json")
    }

    @Test
    @DisplayName("extractExpectedResponseExampleWithMediaType should return schema fallback with media type")
    fun shouldReturnSchemaFallbackAndMediaType() {
        val schema = ObjectSchema().apply {
            addProperty("status", StringSchema().example("ok"))
            required = listOf("status")
        }
        val operation = Operation().responses(
            ApiResponses().addApiResponse(
                "200",
                ApiResponse().content(
                    Content()
                        .addMediaType("application/json", MediaType().schema(schema))
                        .addMediaType("application/xml", MediaType().schema(StringSchema()))
                )
            )
        )

        val extracted = extractor.extractExpectedResponseExampleWithMediaType(operation, openApi, 200)

        assertThat(extracted.body).isEqualTo(mapOf("status" to "ok"))
        assertThat(extracted.mediaType).isEqualTo("application/json")
    }

    @Test
    @DisplayName("extractExpectedResponseExampleWithMediaType should return schema fallback for jwt media type")
    fun shouldReturnSchemaFallbackForJwtMediaType() {
        val schema = ObjectSchema().apply {
            addProperty("status", StringSchema().example("signed"))
            required = listOf("status")
        }
        val operation = Operation().responses(
            ApiResponses().addApiResponse(
                "200",
                ApiResponse().content(
                    Content()
                        .addMediaType("application/jwt", MediaType().schema(schema))
                        .addMediaType("application/xml", MediaType().schema(StringSchema()))
                )
            )
        )

        val extracted = extractor.extractExpectedResponseExampleWithMediaType(operation, openApi, 200)

        assertThat(extracted.body).isEqualTo(mapOf("status" to "signed"))
        assertThat(extracted.mediaType).isEqualTo("application/jwt")
    }

    @Test
    @DisplayName("extractExpectedResponseExampleWithMediaType should return schema fallback for text/json media type")
    fun shouldReturnSchemaFallbackForTextJsonMediaType() {
        val schema = ObjectSchema().apply {
            addProperty("status", StringSchema().example("text-json"))
            required = listOf("status")
        }
        val operation = Operation().responses(
            ApiResponses().addApiResponse(
                "200",
                ApiResponse().content(
                    Content()
                        .addMediaType("text/json", MediaType().schema(schema))
                        .addMediaType("application/xml", MediaType().schema(StringSchema()))
                )
            )
        )

        val extracted = extractor.extractExpectedResponseExampleWithMediaType(operation, openApi, 200)

        assertThat(extracted.body).isEqualTo(mapOf("status" to "text-json"))
        assertThat(extracted.mediaType).isEqualTo("text/json")
    }

    @Test
    @DisplayName("extractExpectedResponseExampleWithMediaType should return nulls when response content is missing")
    fun shouldReturnNullsWhenContentMissing() {
        val operation = Operation().responses(
            ApiResponses().addApiResponse("200", ApiResponse().description("OK"))
        )

        val extracted = extractor.extractExpectedResponseExampleWithMediaType(operation, openApi, 200)

        assertThat(extracted.body).isNull()
        assertThat(extracted.mediaType).isNull()
    }

    @Test
    @DisplayName("pluggable body generator should receive the negotiated schema and produce the fallback body")
    fun shouldUsePluggableBodyGeneratorForFallback() {
        val schema = ObjectSchema().apply {
            addProperty("status", StringSchema())
            required = listOf("status")
        }
        val capturedSchemas = mutableListOf<Schema<*>>()
        val capturedOpenApis = mutableListOf<OpenAPI>()
        val pluggedExtractor = ResponseExampleExtractor { negotiatedSchema, openAPI ->
            capturedSchemas.add(negotiatedSchema)
            capturedOpenApis.add(openAPI)
            mapOf("status" to "from-custom-generator")
        }
        val operation = Operation().responses(
            ApiResponses().addApiResponse(
                "200",
                ApiResponse().content(
                    Content().addMediaType("application/json", MediaType().schema(schema))
                )
            )
        )

        val extracted = pluggedExtractor.extractExpectedResponseExampleWithMediaType(operation, openApi, 200)

        assertThat(extracted.body).isEqualTo(mapOf("status" to "from-custom-generator"))
        assertThat(extracted.mediaType).isEqualTo("application/json")
        assertThat(capturedSchemas).hasSize(1)
        assertThat(capturedSchemas[0]).isSameAs(schema)
        assertThat(capturedOpenApis).hasSize(1)
        assertThat(capturedOpenApis[0]).isSameAs(openApi)
    }

    @Test
    @DisplayName("pluggable body generator should not be invoked when an explicit example exists")
    fun shouldNotInvokeBodyGeneratorWhenExplicitExampleExists() {
        val invocations = AtomicInteger()
        val pluggedExtractor = ResponseExampleExtractor { _, _ ->
            invocations.incrementAndGet()
            mapOf("status" to "generated")
        }
        val operation = Operation().responses(
            ApiResponses().addApiResponse(
                "200",
                ApiResponse().content(
                    Content().addMediaType(
                        "application/json",
                        MediaType().schema(ObjectSchema()).example(mapOf("status" to "explicit"))
                    )
                )
            )
        )

        val extracted = pluggedExtractor.extractExpectedResponseExampleWithMediaType(operation, openApi, 200)

        assertThat(extracted.body).isEqualTo(mapOf("status" to "explicit"))
        assertThat(extracted.mediaType).isEqualTo("application/json")
        assertThat(invocations.get()).isEqualTo(0)
    }

    @Test
    @DisplayName("should return negotiated media type with null body when generator returns null")
    fun shouldReturnNegotiatedMediaTypeWhenGeneratorReturnsNull() {
        val pluggedExtractor = ResponseExampleExtractor { _, _ -> null }
        val operation = Operation().responses(
            ApiResponses().addApiResponse(
                "200",
                ApiResponse().content(
                    Content().addMediaType("application/json", MediaType().schema(ObjectSchema()))
                )
            )
        )

        val extracted = pluggedExtractor.extractExpectedResponseExampleWithMediaType(operation, openApi, 200)

        assertThat(extracted.body).isNull()
        assertThat(extracted.mediaType).isEqualTo("application/json")
    }

    @Test
    @DisplayName("should return negotiated media type with null body when generator throws IllegalStateException")
    fun shouldReturnNegotiatedMediaTypeWhenGeneratorThrowsIllegalState() {
        val pluggedExtractor = ResponseExampleExtractor { _, _ ->
            throw IllegalStateException("no value for schema")
        }
        val operation = Operation().responses(
            ApiResponses().addApiResponse(
                "200",
                ApiResponse().content(
                    Content().addMediaType("application/json", MediaType().schema(ObjectSchema()))
                )
            )
        )

        val extracted = pluggedExtractor.extractExpectedResponseExampleWithMediaType(operation, openApi, 200)

        assertThat(extracted.body).isNull()
        assertThat(extracted.mediaType).isEqualTo("application/json")
    }

    @Test
    @DisplayName("should return negotiated media type with null body when generator throws RuntimeException")
    fun shouldReturnNegotiatedMediaTypeWhenGeneratorThrowsRuntime() {
        val pluggedExtractor = ResponseExampleExtractor { _, _ ->
            throw UnsupportedOperationException("provider failure")
        }
        val operation = Operation().responses(
            ApiResponses().addApiResponse(
                "200",
                ApiResponse().content(
                    Content().addMediaType("application/json", MediaType().schema(ObjectSchema()))
                )
            )
        )

        val extracted = pluggedExtractor.extractExpectedResponseExampleWithMediaType(operation, openApi, 200)

        assertThat(extracted.body).isNull()
        assertThat(extracted.mediaType).isEqualTo("application/json")
    }

    @Test
    @DisplayName("should return negotiated media type with null body when content has no extractable example")
    fun shouldReturnNegotiatedMediaTypeWhenNothingExtractable() {
        val operation = Operation().responses(
            ApiResponses().addApiResponse(
                "200",
                ApiResponse().content(
                    Content().addMediaType("text/plain", MediaType().schema(StringSchema()))
                )
            )
        )

        val extracted = extractor.extractExpectedResponseExampleWithMediaType(operation, openApi, 200)

        assertThat(extracted.body).isNull()
        assertThat(extracted.mediaType).isEqualTo("text/plain")
    }

    @Test
    @DisplayName("legacy constructor should apply response defaults: optional example properties in, writeOnly out")
    fun legacyConstructorShouldApplyResponseDefaults() {
        val schema = ObjectSchema().apply {
            addProperty("id", StringSchema().example("id-1"))
            addProperty("optionalWithExample", StringSchema().example("opt-1"))
            addProperty("secret", StringSchema().example("s3cret").apply { writeOnly = true })
            required = listOf("id", "secret")
        }
        val operation = Operation().responses(
            ApiResponses().addApiResponse(
                "200",
                ApiResponse().content(
                    Content().addMediaType("application/json", MediaType().schema(schema))
                )
            )
        )

        val extracted = extractor.extractExpectedResponseExampleWithMediaType(operation, openApi, 200)

        assertThat(extracted.body).isEqualTo(mapOf("id" to "id-1", "optionalWithExample" to "opt-1"))
        assertThat(extracted.mediaType).isEqualTo("application/json")
    }

    @Test
    @DisplayName("should return highest-priority media type with null body when several declare nothing extractable")
    fun shouldReturnHighestPriorityMediaTypeWhenNothingExtractable() {
        val pluggedExtractor = ResponseExampleExtractor { _, _ -> null }
        val operation = Operation().responses(
            ApiResponses().addApiResponse(
                "200",
                ApiResponse().content(
                    Content()
                        .addMediaType("text/plain", MediaType().schema(StringSchema()))
                        .addMediaType("application/xml", MediaType().schema(StringSchema()))
                        .addMediaType("application/json", MediaType().schema(ObjectSchema()))
                )
            )
        )

        val extracted = pluggedExtractor.extractExpectedResponseExampleWithMediaType(operation, openApi, 200)

        assertThat(extracted.body).isNull()
        assertThat(extracted.mediaType).isEqualTo("application/json")
    }
}
