package art.galushko.openapi.testgen.config

/**
 * Validates the `includeOperations` configuration structure.
 *
 * Expected format: `Map<String, List<String>>`
 * - Key: Path string (e.g., "/users/{userId}") or `"*"` (wildcard for all paths)
 * - Value: List of HTTP methods (e.g., ["GET", "POST"]) or `["*"]` (wildcard for all methods)
 *
 * Example valid configurations:
 * ```kotlin
 * // Target single operation
 * mapOf("/users/{userId}" to listOf("GET"))
 *
 * // Target multiple methods on same path
 * mapOf("/users/{userId}" to listOf("GET", "DELETE"))
 *
 * // Target multiple paths
 * mapOf(
 *     "/users/{userId}" to listOf("GET"),
 *     "/orders" to listOf("POST")
 * )
 *
 * // All methods on specific path
 * mapOf("/users" to listOf("*"))
 *
 * // Specific method on all paths
 * mapOf("*" to listOf("GET"))
 *
 * // All operations (equivalent to empty/not set)
 * mapOf("*" to listOf("*"))
 * ```
 */
public object IncludeOperationsValidator {

    private val VALID_METHODS = setOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE", "*")

    /**
     * Validates the `includeOperations` configuration structure.
     *
     * @param config the include operations configuration map to validate
     * @throws IllegalArgumentException with a clear message indicating exactly what's wrong
     */
    public fun validate(config: Map<String, List<String>>?) {
        config?.forEach { (path, methods) ->
            requireValidPath(path)
            requireValidMethods(path, methods)
        }
    }

    private fun requireValidPath(path: String) {
        require(path.isNotBlank()) {
            "includeOperations path cannot be blank"
        }
    }

    private fun requireValidMethods(path: String, methods: List<String>) {
        require(methods.isNotEmpty()) {
            "includeOperations methods list cannot be empty for path '$path'"
        }
        methods.forEach { method ->
            val upperMethod = method.uppercase()
            require(upperMethod in VALID_METHODS) {
                "Invalid HTTP method '$method' for path '$path' in includeOperations. " +
                    "Valid methods: ${VALID_METHODS.sorted().joinToString(", ")}"
            }
        }
    }
}
