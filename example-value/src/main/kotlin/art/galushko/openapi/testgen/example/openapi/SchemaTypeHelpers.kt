package art.galushko.openapi.testgen.example.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses

private const val STATUS_CODE_CLASS_DIVISOR = 100
private const val STATUS_CODE_RANGE_SUFFIX = "XX"
private const val DEFAULT_RESPONSE_KEY = "default"
private const val SUCCESS_STATUS_CODE_MIN = 200
private const val SUCCESS_STATUS_CODE_MAX = 299
private const val SUCCESS_STATUS_CODE_FALLBACK = 200

/**
 * Utility helpers to work with OpenAPI schema types and $ref dereferencing.
 */
public object SchemaTypeHelpers {

    /**
     * Attempts to dereference a schema `$ref` against the OpenAPI components.
     * Returns the dereferenced schema if found; otherwise returns the original schema.
     *
     * @param schema schema that may contain a `$ref`
     * @param openAPI OpenAPI document providing component definitions
     * @return dereferenced schema or the original input
     */
    @JvmStatic
    public fun tryGetSchemaFromRef(schema: Schema<*>, openAPI: OpenAPI): Schema<*> {
        if (schema.`$ref` == null) {
            return schema
        }
        val key = schema.`$ref`.replace("#/components/schemas/", "")
        val dereferenced = openAPI.components?.schemas[key]
        return dereferenced ?: schema
    }

    /**
     * Resolves a response by status code using OpenAPI matching rules:
     * exact code -> range (e.g. 2XX) -> default.
     *
     * @param operation OpenAPI operation
     * @param openAPI OpenAPI document providing component definitions
     * @param statusCode HTTP status code to resolve
     * @return resolved response or null when not present
     */
    @JvmStatic
    public fun resolveResponseByStatus(operation: Operation, openAPI: OpenAPI, statusCode: Int): ApiResponse? {
        val responses = operation.responses ?: return null
        return resolveResponseByStatus(responses, openAPI, statusCode)
    }

    /**
     * Resolves an example `$ref` against OpenAPI components.
     *
     * @param example example that may contain a `$ref`
     * @param openAPI OpenAPI document providing component definitions
     * @return resolved example or the original input when not found
     */
    @JvmStatic
    public fun resolveExampleRef(example: Example, openAPI: OpenAPI): Example {
        val ref = example.`$ref` ?: return example
        val key = extractComponentRefKey(ref, "#/components/examples/") ?: return example
        return openAPI.components?.examples?.get(key) ?: example
    }

    /**
     * Finds the first success status code (2xx) from operation responses.
     *
     * Resolution order:
     * 1. Minimum numeric 2xx status code (200-299)
     * 2. Range key "2XX" (case-insensitive) → returns 200
     * 3. "default" key (case-insensitive) → returns 200
     *
     * @param operation OpenAPI operation
     * @return the success status code
     * @throws IllegalStateException if no success response is found
     */
    @JvmStatic
    public fun findSuccessStatusCode(operation: Operation): Int {
        val responses = operation.responses
            ?: throw IllegalStateException("Operation responses are null")
        return findSuccessStatusCode(responses)
    }

    /**
     * Attempts to dereference a request body `$ref` against the OpenAPI components.
     * Returns the dereferenced request body if found; otherwise returns the original.
     *
     * @param requestBody request body that may contain a `$ref`
     * @param openAPI OpenAPI document providing component definitions
     * @return dereferenced request body or the original input
     */
    @JvmStatic
    public fun tryGetRequestBodyFromRef(requestBody: RequestBody, openAPI: OpenAPI): RequestBody {
        if (requestBody.`$ref` == null) {
            return requestBody
        }
        val key = requestBody.`$ref`.replace("#/components/requestBodies/", "")
        val dereferenced = openAPI.components.requestBodies[key]
        return dereferenced ?: requestBody
    }

    /**
     * Attempts to dereference a parameter `$ref` against the OpenAPI components.
     * Returns the dereferenced parameter if found; otherwise returns the original.
     *
     * @param parameter parameter that may contain a `$ref`
     * @param openAPI OpenAPI document providing component definitions
     * @return dereferenced parameter or the original input
     */
    @JvmStatic
    public fun tryGetParametersFromRef(parameter: Parameter, openAPI: OpenAPI): Parameter {
        if (parameter.`$ref` == null) {
            return parameter
        }
        val key = parameter.`$ref`.replace("#/components/parameters/", "")
        val dereferenced = openAPI.components.parameters[key]
        return dereferenced ?: parameter
    }

    /**
     * Determines if the given schema represents a numeric type (either "integer" or "number").
     */
    @JvmStatic
    public fun isNumber(schema: Schema<*>): Boolean {
        if (schema is IntegerSchema || schema is NumberSchema) return true
        val types = schema.types
        if (types != null && (types.contains("number") || types.contains("integer"))) return true
        return when (schema.type) {
            "integer", "number" -> true
            else -> false
        }
    }

    /**
     * Checks if the given schema represents a string type.
     */
    @JvmStatic
    public fun isString(schema: Schema<*>): Boolean {
        if (schema is StringSchema) return true
        val types = schema.types
        if (types != null && types.contains("string")) return true
        return schema.type == "string"
    }

    /**
     * Determines if the given schema represents an integer type.
     */
    @JvmStatic
    public fun isInteger(schema: Schema<*>): Boolean {
        if (schema is IntegerSchema) return true
        val types = schema.types
        if (types != null && types.contains("integer")) return true
        return schema.type == "integer"
    }

    /**
     * Determines if the given schema represents an array type.
     */
    @JvmStatic
    public fun isArray(schema: Schema<*>): Boolean {
        if (schema is ArraySchema) return true
        val types = schema.types
        if (types != null && types.contains("array")) return true
        return schema.type == "array"
    }

    /**
     * Determines if the given schema represents a boolean type.
     */
    @JvmStatic
    public fun isBoolean(schema: Schema<*>): Boolean {
        if (schema is BooleanSchema) return true
        val types = schema.types
        if (types != null && types.contains("boolean")) return true
        return schema.type == "boolean"
    }

    /**
     * Determines if the given schema represents an object type.
     */
    @JvmStatic
    public fun isObject(schema: Schema<*>): Boolean {
        if (schema is ObjectSchema) return true
        val types = schema.types
        if (types != null && types.contains("object")) return true
        if (schema.type == "object") return true
        // Implicit object: properties present but no explicit type (common in real-world specs)
        if (schema.type == null && types == null && schema.properties?.isNotEmpty() == true) return true
        return false
    }
}

private fun resolveResponseByStatus(responses: ApiResponses, openAPI: OpenAPI, statusCode: Int): ApiResponse? {
    val exact = responses[statusCode.toString()]
    if (exact != null) return resolveResponseRef(exact, openAPI)

    val rangeKey = "${statusCode / STATUS_CODE_CLASS_DIVISOR}$STATUS_CODE_RANGE_SUFFIX"
    val range = responses.entries.firstOrNull { it.key.equals(rangeKey, ignoreCase = true) }?.value
    if (range != null) return resolveResponseRef(range, openAPI)

    val default = responses.entries.firstOrNull { it.key.equals(DEFAULT_RESPONSE_KEY, ignoreCase = true) }?.value
    return default?.let { resolveResponseRef(it, openAPI) }
}

private fun resolveResponseRef(response: ApiResponse, openAPI: OpenAPI): ApiResponse {
    val ref = response.`$ref` ?: return response
    val key = extractComponentRefKey(ref, "#/components/responses/") ?: return response
    return openAPI.components?.responses?.get(key) ?: response
}

private fun extractComponentRefKey(ref: String, prefix: String): String? {
    if (!ref.startsWith(prefix)) return null
    return ref.removePrefix(prefix)
}

private fun findSuccessStatusCode(responses: ApiResponses): Int {
    val numericSuccess = responses.keys.mapNotNull { key ->
        key.toIntOrNull()?.takeIf { it in SUCCESS_STATUS_CODE_MIN..SUCCESS_STATUS_CODE_MAX }
    }.minOrNull()
    if (numericSuccess != null) return numericSuccess

    val rangeKey = "${SUCCESS_STATUS_CODE_MIN / STATUS_CODE_CLASS_DIVISOR}$STATUS_CODE_RANGE_SUFFIX"
    if (responses.keys.any { it.equals(rangeKey, ignoreCase = true) }) return SUCCESS_STATUS_CODE_FALLBACK
    if (responses.keys.any { it.equals(DEFAULT_RESPONSE_KEY, ignoreCase = true) }) return SUCCESS_STATUS_CODE_FALLBACK

    throw IllegalStateException("Success status code not found in responses: ${responses.keys}")
}
