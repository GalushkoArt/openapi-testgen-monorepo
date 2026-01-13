package art.galushko.openapi.testgen.example.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
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
     * Attempts to dereference a response `$ref` by status code.
     *
     * @param operation OpenAPI operation
     * @param openAPI OpenAPI document providing component definitions
     * @param statusCode HTTP status code to resolve
     * @return dereferenced response or null when not present
     */
    public fun tryGetResponseFromRef(operation: Operation, openAPI: OpenAPI, statusCode: Int): ApiResponse? {
        val responses = operation.responses ?: return null
        val resp = responses[statusCode.toString()] ?: return null
        return if (resp.`$ref` != null) {
            val key = resp.`$ref`.replace("#/components/responses/", "")
            openAPI.components?.responses?.get(key) ?: resp
        } else {
            resp
        }
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
        return schema.type == "object"
    }
}
