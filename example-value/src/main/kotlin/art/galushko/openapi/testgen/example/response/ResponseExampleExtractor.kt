package art.galushko.openapi.testgen.example.response

import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGenerator
import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGeneratorOptions
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.resolveExampleRef
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.resolveResponseByStatus
import art.galushko.openapi.testgen.example.util.MediaTypePrioritizer
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import org.slf4j.LoggerFactory

/**
 * Result of expected response extraction with selected media type.
 *
 * @property body extracted response body example or null when unavailable
 * @property mediaType negotiated response media type, or null when the response declares no content.
 *   A non-null [mediaType] with a null [body] means content was declared but nothing extractable
 *   (no explicit example and no generatable schema body).
 */
public data class ExtractedResponseExample(
    val body: Any?,
    val mediaType: String?,
)

/**
 * Extracts expected response examples from OpenAPI operations.
 *
 * This class handles the extraction of response examples from OpenAPI specifications,
 * using a defined selection order:
 * - Response: exact status -> range (e.g. 2XX) -> default
 * - Media types: JSON/JWT-like -> XML -> other (alphabetical)
 * - Example selection: explicit examples are chosen by media type priority; schema-derived fallback is JSON/JWT-like only
 *
 * The schema-derived fallback slot is pluggable: pass a [ResponseBodyGenerator] to combine the
 * extractor's explicit-example precedence with a custom body generator.
 *
 * @property responseBodyGenerator generator invoked when no usable explicit example exists
 */
public class ResponseExampleExtractor(
    private val responseBodyGenerator: ResponseBodyGenerator,
) {
    private val log = LoggerFactory.getLogger(ResponseExampleExtractor::class.java)

    /**
     * Creates an extractor whose fallback bodies are derived by [schemaExampleValueGenerator].
     *
     * Note: this constructor applies [SchemaExampleValueGeneratorOptions.RESPONSE_DEFAULTS] for the
     * include/fallback flags (`includeOptionalExampleProperties=true`, `includeWriteOnly=false`,
     * `useSchemaExampleFallback=true`); only `maxExampleDepth` and `fullExample` are taken from the
     * generator's configured options. Use the [ResponseBodyGenerator] constructor to keep full control
     * over generation options.
     *
     * @param schemaExampleValueGenerator generator for deriving fallback examples from schemas
     */
    public constructor(schemaExampleValueGenerator: SchemaExampleValueGenerator) : this(
        ResponseBodyGenerator { schema, openAPI ->
            schemaExampleValueGenerator.getExampleValueWithOptions(
                name = FALLBACK_GENERATION_NAME,
                schema = schema,
                openAPI = openAPI,
                options = schemaExampleValueGenerator.responseOptions(),
            )
        },
    )

    /**
     * Extracts expected response example from an operation for the given status code.
     *
     * @param operation the OpenAPI operation
     * @param openAPI the OpenAPI specification
     * @param statusCode the HTTP status code to look for
     * @return the example value if found, null otherwise
     */
    public fun extractExpectedResponseExample(operation: Operation, openAPI: OpenAPI, statusCode: Int): Any? =
        extractExpectedResponseExample(operation, openAPI, statusCode, null)

    /**
     * Extracts expected response example from an operation for the given status code and example name.
     *
     * When exampleName is provided, named examples are selected first using media type priority.
     * If no named example is found (or it has no usable value), the standard selection order is used.
     *
     * @param operation the OpenAPI operation
     * @param openAPI the OpenAPI specification
     * @param statusCode the HTTP status code to look for
     * @param exampleName optional named example to select from examples map
     * @return the example value if found, null otherwise
     */
    public fun extractExpectedResponseExample(
        operation: Operation,
        openAPI: OpenAPI,
        statusCode: Int,
        exampleName: String?,
    ): Any? = extractExpectedResponseExampleWithMediaType(operation, openAPI, statusCode, exampleName).body

    /**
     * Extracts expected response example and media type from an operation for the given status code.
     *
     * @param operation the OpenAPI operation
     * @param openAPI the OpenAPI specification
     * @param statusCode the HTTP status code to look for
     * @return extracted response body and selected media type, if any
     */
    public fun extractExpectedResponseExampleWithMediaType(
        operation: Operation,
        openAPI: OpenAPI,
        statusCode: Int,
    ): ExtractedResponseExample = extractExpectedResponseExampleWithMediaType(operation, openAPI, statusCode, null)

    /**
     * Extracts expected response example and media type from an operation for the given status code and example name.
     *
     * @param operation the OpenAPI operation
     * @param openAPI the OpenAPI specification
     * @param statusCode the HTTP status code to look for
     * @param exampleName optional named example to select from examples map
     * @return extracted response body and selected media type, if any
     */
    public fun extractExpectedResponseExampleWithMediaType(
        operation: Operation,
        openAPI: OpenAPI,
        statusCode: Int,
        exampleName: String?,
    ): ExtractedResponseExample {
        val resp = resolveResponseByStatus(operation, openAPI, statusCode) ?: return ExtractedResponseExample(body = null, mediaType = null)
        val content = resp.content ?: return ExtractedResponseExample(body = null, mediaType = null)
        val orderedMediaTypes = MediaTypePrioritizer.orderedMediaTypeKeys(content)

        if (!exampleName.isNullOrBlank()) {
            val namedResult = findNamedExample(content, orderedMediaTypes, openAPI, exampleName)
            if (namedResult != null) return namedResult
        }

        return extractFromMediaTypes(content, orderedMediaTypes, openAPI)
    }

    private fun extractFromMediaTypes(content: Content, orderedMediaTypes: List<String>, openAPI: OpenAPI): ExtractedResponseExample {
        val mediaTypes = orderedMediaTypes.mapNotNull { key -> content[key]?.let { key to it } }

        val explicitExample = mediaTypes
            .firstNotNullOfOrNull { (mediaTypeName, mediaType) ->
                extractExampleFromMediaType(mediaType, openAPI)?.let { ExtractedResponseExample(it, mediaTypeName) }
            }
        if (explicitExample != null) return explicitExample

        val structuredSchema = mediaTypes.firstOrNull { (key, mediaType) ->
            mediaType.schema != null && MediaTypePrioritizer.isExpectedStructuredSchema(key)
        }
        if (structuredSchema != null) {
            val fallbackBody = safeResponseExampleValue(structuredSchema.second.schema, openAPI)
            if (fallbackBody != null) {
                return ExtractedResponseExample(body = fallbackBody, mediaType = structuredSchema.first)
            }
        }
        return ExtractedResponseExample(body = null, mediaType = orderedMediaTypes.firstOrNull())
    }

    private fun findNamedExample(
        content: Content,
        orderedMediaTypes: List<String>,
        openAPI: OpenAPI,
        exampleName: String,
    ): ExtractedResponseExample? {
        val namedExamplesByMediaType = orderedMediaTypes.mapNotNull { mediaTypeName ->
            content[mediaTypeName]?.examples?.get(exampleName)?.let { mediaTypeName to it }
        }
        if (namedExamplesByMediaType.isEmpty()) return null

        val resolved = namedExamplesByMediaType.firstNotNullOfOrNull { (mediaTypeName, example) ->
            extractExampleValue(resolveExampleRef(example, openAPI))?.let { value ->
                ExtractedResponseExample(body = value, mediaType = mediaTypeName)
            }
        }
        if (resolved == null) {
            log.debug("Named example '{}' found but has no usable value; falling back to default selection", exampleName)
        }
        return resolved
    }

    private fun extractExampleFromMediaType(mediaType: MediaType, openAPI: OpenAPI): Any? {
        mediaType.example?.let { return it }

        val examples = mediaType.examples ?: return null
        if (examples.isEmpty()) return null

        return examples.toSortedMap().values.firstNotNullOfOrNull { ex ->
            extractExampleValue(resolveExampleRef(ex, openAPI))
        }
    }

    private fun extractExampleValue(example: io.swagger.v3.oas.models.examples.Example?): Any? {
        if (example == null) return null
        example.value?.let { return it }
        if (example.externalValue != null) {
            log.debug("Skipping externalValue example (not supported): {}", example.externalValue)
        }
        return null
    }

    private fun safeResponseExampleValue(schema: Schema<*>?, openAPI: OpenAPI): Any? {
        if (schema == null) return null
        return try {
            responseBodyGenerator.generate(schema, openAPI)
        } catch (e: IllegalStateException) {
            // Expected: schema-related issues (missing properties, invalid structure)
            log.debug("Failed to derive response example from schema: {}", e.message)
            null
        } catch (e: RuntimeException) {
            // Unexpected: other runtime issues (provider failures, etc.) - log at warn level
            log.warn("Unexpected error deriving response example from schema", e)
            null
        }
    }

    private companion object {
        private const val FALLBACK_GENERATION_NAME = "response"
    }
}
