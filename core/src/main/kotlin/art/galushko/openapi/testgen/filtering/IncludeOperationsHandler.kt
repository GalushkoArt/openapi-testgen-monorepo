package art.galushko.openapi.testgen.filtering

import org.slf4j.LoggerFactory

/**
 * Handles include operations configuration for filtering paths and HTTP methods.
 *
 * When [includeOperations] is empty, all operations are included (backward compatible default).
 * When [includeOperations] is non-empty, only matching path+method combinations are included.
 *
 * Supports wildcards:
 * - Path `"*"`: matches all paths
 * - Method `"*"`: matches all HTTP methods
 *
 * @param includeOperations map of paths to HTTP methods to include
 */
internal class IncludeOperationsHandler(
    private val includeOperations: Map<String, List<String>>
) {
    private val log = LoggerFactory.getLogger(IncludeOperationsHandler::class.java)

    /**
     * Normalized config with uppercase HTTP methods for case-insensitive matching.
     */
    private val normalizedConfig: Map<String, Set<String>> = includeOperations
        .mapValues { (_, methods) -> methods.map { it.uppercase() }.toSet() }

    companion object {
        const val WILDCARD = "*"
    }

    /**
     * Returns true if include filtering is enabled (non-empty config).
     */
    fun isEnabled(): Boolean = includeOperations.isNotEmpty()

    /**
     * Validates that paths in include config exist in the OpenAPI spec.
     * Logs warnings for non-existent paths.
     */
    fun validatePathsExist(specPaths: Set<String>) {
        if (!isEnabled()) return

        normalizedConfig.keys
            .filterNot { it == WILDCARD }
            .minus(specPaths)
            .forEach { path ->
                log.warn("includeOperations path '{}' is not present in provided OpenAPI spec", path)
            }
    }

    /**
     * Checks if a path should be considered for processing.
     *
     * A path is included if:
     * - Include config is empty (all paths included by default)
     * - Path is explicitly listed in include config
     * - Wildcard path ("*") is present in include config
     */
    fun shouldIncludePath(path: String): Boolean {
        if (!isEnabled()) return true
        return normalizedConfig.containsKey(path) || normalizedConfig.containsKey(WILDCARD)
    }

    /**
     * Checks if a specific operation (path + HTTP method) should be included.
     *
     * An operation is included if:
     * - Include config is empty (all operations included by default)
     * - Path is explicitly listed with matching method or wildcard method
     * - Path is NOT explicitly listed, but wildcard path matches
     *
     * Note: When an exact path is configured, wildcard path methods are NOT additive.
     * The exact path configuration takes full precedence.
     */
    fun shouldIncludeOperation(path: String, httpMethod: String): Boolean {
        if (!isEnabled()) return true
        val upperMethod = httpMethod.uppercase()

        // Get methods for this path: exact match takes precedence, then wildcard
        val methods = normalizedConfig[path] ?: normalizedConfig[WILDCARD] ?: return false

        return methods.contains(WILDCARD) || methods.contains(upperMethod)
    }
}
