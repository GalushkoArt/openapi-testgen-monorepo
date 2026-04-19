package art.galushko.openapi.testgen.example.response

import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGeneratorFactory
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

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
}
