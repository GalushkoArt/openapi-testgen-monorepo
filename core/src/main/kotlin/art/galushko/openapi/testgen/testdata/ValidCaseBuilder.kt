package art.galushko.openapi.testgen.testdata

import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGenerator
import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGeneratorFactory
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.resolveExampleRef
import art.galushko.openapi.testgen.example.response.ResponseExampleExtractor
import art.galushko.openapi.testgen.example.util.MediaTypePrioritizer.orderedMediaTypeKeys
import art.galushko.openapi.testgen.model.KeyValuePair
import art.galushko.openapi.testgen.model.SecurityValues
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.ErrorContext
import art.galushko.openapi.testgen.model.error.GenerationError
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.model.with
import art.galushko.openapi.testgen.openapi.SecurityHelpers
import art.galushko.openapi.testgen.util.exceptions.UnsupportedParameterType
import art.galushko.openapi.testgen.util.isMediaTypeSupported
import art.galushko.openapi.testgen.util.resolveParameterSchema
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.parameters.CookieParameter
import io.swagger.v3.oas.models.parameters.HeaderParameter
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.PathParameter
import io.swagger.v3.oas.models.parameters.QueryParameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.security.SecurityScheme
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.LoggerFactory

/**
 * Builds a valid positive [TestCase] for a given OpenAPI operation.
 *
 * Inputs: operation metadata, OpenAPI model, and providers for security and example values.
 * Output: [Outcome.Success] with a baseline valid case or [Outcome.Failure] with errors.
 * Determinism: parameter iteration follows the OpenAPI parameter order; request body media type
 * selection uses [orderedMediaTypeKeys] with [isMediaTypeSupported] filtering.
 *
 * @param path API path template (e.g. "/pets/{id}")
 * @param method HTTP method in lowercase as defined in the OpenAPI spec
 * @param operation OpenAPI [Operation] describing the endpoint
 * @param openAPI OpenAPI model used to resolve references
 * @param securityValueProvider provider for security values used in authentication/authorization
 * @param schemaExampleValueGenerator generator for example values from schemas
 */
@Suppress("LongParameterList")
public class ValidCaseBuilder(
    private val path: String,
    private val method: String,
    private val operation: Operation,
    private val openAPI: OpenAPI,
    private val securityValueProvider: SecurityValueProvider = SecurityValueProvider(),
    private val schemaExampleValueGenerator: SchemaExampleValueGenerator = SchemaExampleValueGeneratorFactory().create(),
    private val responseExampleExtractor: ResponseExampleExtractor =
        ResponseExampleExtractor(schemaExampleValueGenerator),
) {
    private val log = LoggerFactory.getLogger(ValidCaseBuilder::class.java)
    private val reference: String = operation.operationId ?: "${method.lowercase()} $path:"
    private val queryParams: MutableMap<String, Any> = linkedMapOf()
    private val pathParams: MutableMap<String, Any> = linkedMapOf()
    private val headers: MutableList<KeyValuePair<String, Any>> = mutableListOf()
    private val cookie: MutableList<KeyValuePair<String, Any>> = mutableListOf()
    private var securityValues: SecurityValues = SecurityValues()
    private var body: Any? = null
    private var requestBodyMediaType: String? = null

    /**
     * Constructs a valid positive test case that satisfies required parameters,
     * request body, and security requirements (when applicable).
     *
     * Behavior:
     * - Only required parameters are populated (path/query/header/cookie).
     * - Parameter schema resolution uses `schema` first; if absent, falls back to `content` schema.
     * - Request body uses the first supported media type with example(s) or a schema.
     * - Success status code is the first 2xx response, then `2XX`, then `default` if present.
     * - Security uses the first security requirement object if multiple are defined.
     *
     * @return [Outcome.Success] with a [TestCase] representing a successful request,
     *         or [Outcome.Failure] if required information is missing or unsupported
     */
    public fun generateValidCase(): Outcome<TestCase> {
        return try {
            addRequiredSecurityItems()

            for (param in getRequiredParams()) {
                val resolved = resolveParameterSchema(param, openAPI)
                val schema = resolved.schema ?: throw IllegalStateException(
                    "$reference Required ${param.`in`} parameter '${param.name}' does not define schema or content schema"
                )
                val value = schemaExampleValueGenerator.getExampleValue(param.name, schema, openAPI)
                when (param) {
                    is QueryParameter -> queryParams[param.name] = value
                    is PathParameter -> pathParams[param.name] = value
                    is HeaderParameter -> headers.add(param.name with value)
                    is CookieParameter -> cookie.add(param.name with value)
                    else -> throw UnsupportedParameterType(param)
                }
            }

            if (operation.requestBody != null) {
                val requestBody = SchemaTypeHelpers.tryGetRequestBodyFromRef(operation.requestBody, openAPI)
                val extractedBody = extractBodyFromMediaType(requestBody, openAPI)
                if (extractedBody.body != null) {
                    body = extractedBody.body
                    requestBodyMediaType = extractedBody.mediaType
                } else if (requestBody.required == true) {
                    throw IllegalStateException("$reference Unsupported request body media type ${requestBody.content.keys}")
                }
            }

            val successStatusCode = getSuccessStatusCode()
            val expectedResponse = responseExampleExtractor.extractExpectedResponseExampleWithMediaType(operation, openAPI, successStatusCode)

            val testCase = TestCase(
                name = "Test Valid Case",
                method = method.uppercase(),
                path = path,
                queryParams = queryParams.toMap(),
                pathParams = pathParams.toMap(),
                headers = headers.toList(),
                cookie = cookie.toList(),
                securityValues = securityValues,
                body = body,
                requestBodyMediaType = requestBodyMediaType,
                expectedBody = expectedResponse.body,
                responseBodyMediaType = expectedResponse.mediaType,
                expectedStatusCode = successStatusCode
            )

            Outcome.Success(testCase)
        } catch (e: Exception) {
            log.error("Unexpected error generating valid case for $reference", e)
            Outcome.Failure(
                listOf(
                    GenerationError(
                        providerClass = ValidCaseBuilder::class.java.name,
                        message = "Unexpected error: ${e.message ?: e.javaClass.simpleName}",
                        context = ErrorContext.Operation(path, method, operation.operationId ?: "$method $path"),
                        exceptionText = ExceptionUtils.getStackTrace(e),
                    )
                )
            )
        }
    }

    private fun extractBodyFromMediaType(requestBody: RequestBody, openAPI: OpenAPI): ExtractedRequestBody {
        val content = requestBody.content ?: return ExtractedRequestBody(body = null, mediaType = null)
        val mediaTypeKeys = orderedMediaTypeKeys(content)
        mediaTypeKeys
            .filterNot { isMediaTypeSupported(it) }
            .forEach { mediaType ->
                log.warn("$reference Unsupported request body media type: {}", mediaType)
            }

        mediaTypeKeys
            .filter { isMediaTypeSupported(it) }
            .forEach { mediaTypeName ->
                val mediaType = checkNotNull(content[mediaTypeName])

                // 1. Try media type-level example/examples first (most authoritative)
                extractExampleFromMediaType(mediaType, openAPI)?.let {
                    return ExtractedRequestBody(body = it, mediaType = mediaTypeName)
                }

                // 2. Fall back to schema-based generation
                mediaType.schema?.let { schema ->
                    return ExtractedRequestBody(
                        body = schemaExampleValueGenerator.getExampleValue("request body", schema, openAPI),
                        mediaType = mediaTypeName,
                    )
                }
            }

        return ExtractedRequestBody(body = null, mediaType = null)
    }

    private fun extractExampleFromMediaType(mediaType: MediaType, openAPI: OpenAPI): Any? {
        mediaType.example?.let { return it }
        val examples = mediaType.examples ?: return null
        if (examples.isEmpty()) return null
        return examples.toSortedMap().values.firstNotNullOfOrNull { ex ->
            resolveExampleRef(ex, openAPI).value
        }
    }

    private fun addRequiredSecurityItems() {
        val securityRequirements = SecurityHelpers.getSecurityRequirementSchemas(operation, openAPI)
        if (securityRequirements.isEmpty()) {
            return
        }

        val securityRequirement = checkNotNull(securityRequirements.firstOrNull()) {
            "$reference could not resolved security requirement"
        }

        if (securityRequirement.isEmpty()) {
            log.warn("$reference security requirement is empty but it's not expected")
            return
        }
        val securityHeaders = mutableListOf<KeyValuePair<String, Any>>()
        val securityCookies = mutableListOf<KeyValuePair<String, Any>>()
        val securityQueryParams = mutableMapOf<String, Any>()

        val apiKeyRequirements = securityRequirement.asSequence().filter { it.scheme.type == SecurityScheme.Type.APIKEY }.toSet()
        for (requirement in apiKeyRequirements) {
            val validApiTokenValue = securityValueProvider.getValidApiKeyValue(requirement)
            val scheme = requirement.scheme
            when (scheme.`in`) {
                SecurityScheme.In.QUERY -> {
                    queryParams[scheme.name] = validApiTokenValue
                    securityQueryParams[scheme.name] = validApiTokenValue
                }

                SecurityScheme.In.HEADER -> (scheme.name with validApiTokenValue).also {
                    headers.add(it)
                    securityHeaders.add(it)
                }

                SecurityScheme.In.COOKIE -> (scheme.name with validApiTokenValue).also {
                    cookie.add(it)
                    securityCookies.add(it)
                }

                else -> throw IllegalStateException("$reference Unsupported API-KEY security scheme type ${scheme.type}")
            }
        }
        if (apiKeyRequirements.size != securityRequirement.size) {
            val authenticationValue = securityValueProvider.getAuthorizationSchemaValue(securityRequirement.minus(apiKeyRequirements))
            headers.add("authorization" with authenticationValue)
            securityHeaders.add("authorization" with authenticationValue)
        }

        securityValues = SecurityValues(
            queryParams = securityQueryParams.toMap(),
            headers = securityHeaders.toList(),
            cookie = securityCookies.toList(),
            other = SecurityHelpers.buildAuthorizationScopes(securityRequirement)?.let {
                mapOf(SecurityHelpers.AUTHORIZATION_SCOPES_KEY to it)
            } ?: emptyMap(),
        )
    }

    private fun getRequiredParams(): List<Parameter> {
        return if (operation.parameters == null) {
            emptyList()
        } else {
            operation.parameters.mapNotNull { p -> SchemaTypeHelpers.tryGetParametersFromRef(p, openAPI) }
                .filter { p -> p.required == true }
        }
    }

    private fun getSuccessStatusCode(): Int {
        return try {
            SchemaTypeHelpers.findSuccessStatusCode(operation)
        } catch (e: IllegalStateException) {
            throw IllegalStateException("$reference ${e.message}", e)
        }
    }
}

private data class ExtractedRequestBody(
    val body: Any?,
    val mediaType: String?,
)


