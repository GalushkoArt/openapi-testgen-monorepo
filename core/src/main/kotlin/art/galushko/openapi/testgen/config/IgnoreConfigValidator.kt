package art.galushko.openapi.testgen.config

import art.galushko.openapi.testgen.filtering.IgnoreConfigHandler.Companion.WILDCARD


/**
 * Validates ignore the configuration structure.
 *
 * Expected format:
 * - Path key → `"*"` : ignore all operations for this path
 * - Path key → `Map<String, "*">` : ignore specific HTTP method
 * - Path key → `Map<String, List<String>>` : ignore specific test case names
 *
 * Example valid configurations:
 * ```kotlin
 * // Ignore entire path
 * mapOf("/api/users" to "*")
 *
 * // Ignore specific method
 * mapOf("/api/users" to mapOf("DELETE" to "*"))
 *
 * // Ignore specific test cases
 * mapOf(
 *     "/api/users" to mapOf(
 *         "GET" to listOf("Invalid parameter test", "Missing auth test")
 *     )
 * )
 *
 * // Wildcard path (applies to all paths)
 * mapOf(
 *     "*" to mapOf("HEAD" to "*")  // Ignore HEAD on all paths
 * )
 *
 * // Complex nested structure
 * mapOf(
 *     "/api/users" to mapOf(
 *         "GET" to listOf("test1", "test2"),
 *         "POST" to "*"
 *     ),
 *     "/api/products" to "*",
 *     "*" to mapOf("OPTIONS" to "*")
 * )
 * ```
 */
public object IgnoreConfigValidator {

    /**
     * Validates ignore the configuration structure and throw with details on the first failure.
     *
     * @param input the ignore configuration map to validate
     * @throws IllegalArgumentException with a clear message indicating exactly what's wrong
     */
    public fun validateIgnoreConfig(input: Map<*, *>?) {
        input?.forEach { (key, value) ->
            requirePathKey(key)
            requireValidPathValue(key as String, value)
        }
    }

    private fun requirePathKey(key: Any?) {
        require(key is String) {
            "Path key must be String, was ${key?.let { it::class.qualifiedName } ?: "null"}"
        }
    }

    private fun requireValidPathValue(path: String, value: Any?) {
        when (value) {
            WILDCARD -> return // Valid: ignore an entire path
            is Map<*, *> -> requireValidMethodMap(path, value)
            else -> throw IllegalArgumentException(
                "Value for path '$path' must be \"$WILDCARD\" or Map<String, ...>, was ${value?.let { it::class.qualifiedName } ?: "null"}"
            )
        }
    }

    private fun requireValidMethodMap(path: String, methodMap: Map<*, *>) {
        methodMap.forEach { (method, testCases) ->
            requireMethodKey(path, method)
            requireValidTestCaseValue(path, method as String, testCases)
        }
    }

    private fun requireMethodKey(path: String, method: Any?) {
        require(method is String) {
            "Method key for path '$path' must be String, was ${method?.let { it::class.qualifiedName } ?: "null"}"
        }
    }

    @Suppress("MaxLineLength")
    private fun requireValidTestCaseValue(path: String, method: String, testCases: Any?) {
        when (testCases) {
            WILDCARD -> return // Valid: ignore all test cases for this method
            is List<*> -> requireStringList(path, method, testCases)
            else -> throw IllegalArgumentException(
                "Test case value for path '$path' method '$method' must be \"$WILDCARD\" or List<String>, was ${testCases?.let { it::class.qualifiedName } ?: "null"}"
            )
        }
    }

    private fun requireStringList(path: String, method: String, list: List<*>) {
        list.forEachIndexed { index, item ->
            require(item is String) {
                "Test case name at index $index for path '$path' method '$method' must be String, was ${item?.let { it::class.qualifiedName } ?: "null"}"
            }
        }
    }
}
