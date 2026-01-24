package art.galushko.openapi.testgen.example.response

import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGenerator
import art.galushko.openapi.testgen.example.generator.internal.MediaTypePrioritizer
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.resolveExampleRef
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.resolveResponseByStatus
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import org.slf4j.LoggerFactory

/**
 * Extracts expected response examples from OpenAPI operations.
 *
 * This class handles the extraction of response examples from OpenAPI specifications,
 * using a defined selection order:
 * - Response: exact status -> range (e.g. 2XX) -> default
 * - Media types: JSON-like -> XML -> other (alphabetical)
 * - Example selection: explicit examples are chosen by media type priority; schema-derived fallback is JSON-like only
 *
 * @property schemaExampleValueGenerator generator for deriving examples from schemas
 */
public class ResponseExampleExtractor(
    private val schemaExampleValueGenerator: SchemaExampleValueGenerator,
) {
    private val log = LoggerFactory.getLogger(ResponseExampleExtractor::class.java)

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
    ): Any? {
        val resp = resolveResponseByStatus(operation, openAPI, statusCode) ?: return null
        val content = resp.content ?: return null
        val orderedMediaTypes = MediaTypePrioritizer.orderedMediaTypeKeys(content)

        if (!exampleName.isNullOrBlank()) {
            val namedResult = findNamedExample(content, orderedMediaTypes, openAPI, exampleName)
            if (namedResult != null) return namedResult
        }

        return extractFromMediaTypes(content, orderedMediaTypes, openAPI)
    }

    private fun extractFromMediaTypes(content: Content, orderedMediaTypes: List<String>, openAPI: OpenAPI): Any? {
        val mediaTypes = orderedMediaTypes
            .asSequence()
            .mapNotNull { key -> content[key]?.let { key to it } }
            .toList()

        val explicitExample = mediaTypes
            .asSequence()
            .firstNotNullOfOrNull { (_, mediaType) ->
                extractExampleFromMediaType(mediaType, openAPI)
            }
        if (explicitExample != null) return explicitExample

        val jsonFallback = mediaTypes.firstOrNull { (key, mediaType) ->
            mediaType.schema != null && MediaTypePrioritizer.isJsonLike(key)
        }
        return jsonFallback?.let { safeResponseExampleValue(it.second.schema, openAPI) }
    }

    private fun findNamedExample(
        content: Content,
        orderedMediaTypes: List<String>,
        openAPI: OpenAPI,
        exampleName: String,
    ): Any? {
        val namedExamples = orderedMediaTypes.asSequence()
            .mapNotNull { content[it]?.examples?.get(exampleName) }
            .toList()
        if (namedExamples.isEmpty()) return null

        val resolved = namedExamples.firstNotNullOfOrNull { extractExampleValue(resolveExampleRef(it, openAPI)) }
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
            schemaExampleValueGenerator.getExampleValueWithOptions(
                name = "response",
                schema = schema,
                openAPI = openAPI,
                options = schemaExampleValueGenerator.responseOptions(),
            )
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
}
