package art.galushko.openapi.testgen.model.error

/**
 * Hierarchical context describing where in the OpenAPI structure an error occurred.
 */
public sealed interface ErrorContext {
    /**
     * Error context for an API operation.
     *
     * @property path OpenAPI path template.
     * @property method HTTP method (usually uppercase).
     * @property operationId Optional operation id from the spec.
     */
    public data class Operation(
        val path: String,
        val method: String,
        val operationId: String?,
    ) : ErrorContext {
        override fun toString(): String = buildString {
            append("${method.uppercase()} $path")
            if (operationId != null) append(" ($operationId)")
        }
    }

    /**
     * Error context for a specific parameter.
     *
     * @property operation Operation context.
     * @property parameterName Parameter name.
     * @property location Parameter location (query, header, path, cookie).
     * @property ref Optional `$ref` pointer when the parameter is referenced.
     */
    public data class Parameter(
        val operation: Operation,
        val parameterName: String,
        val location: String, // query, header, path, cookie
        val ref: String?,
    ) : ErrorContext {
        override fun toString(): String = buildString {
            append(operation.toString())
            append(" -> $location parameter '$parameterName'")
        }
    }

    /**
     * Error context for a request body.
     *
     * @property operation Operation context.
     * @property ref Optional `$ref` pointer when the request body is referenced.
     */
    public data class RequestBody(
        val operation: Operation,
        val ref: String?,
    ) : ErrorContext {
        override fun toString(): String = buildString {
            append(operation.toString())
            append(" -> request body")
            if (ref != null) append(" ($ref)")
        }
    }
}

