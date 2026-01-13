package art.galushko.openapi.testgen.filtering

import art.galushko.openapi.testgen.model.TestSuite
import org.slf4j.LoggerFactory

/**
 * Handles ignore configuration for paths, operations, and test cases.
 *
 * Supports wildcards and hierarchical filtering:
 * - Path level: ignore entire paths
 * - Operation level: ignore specific HTTP methods
 * - Test case level: ignore specific test case names
 */
internal class IgnoreConfigHandler(
    private val ignoreConfig: Map<String, Any> = emptyMap()
) {
    private val log = LoggerFactory.getLogger(IgnoreConfigHandler::class.java)

    public companion object {
        public const val WILDCARD: String = "*"
    }

    /**
     * Validates that paths in ignore config exist in the OpenAPI spec.
     * Logs warnings for non-existent paths.
     */
    public fun validatePathsExist(specPaths: Set<String>) {
        ignoreConfig.keys
            .filterNot { it == WILDCARD }
            .minus(specPaths)
            .forEach { path ->
                log.warn("{} path is not present in provided Open API spec", path)
            }
    }

    /**
     * Warns if wildcard *:*:* format is used (forbidden pattern).
     */
    public fun warnOnForbiddenWildcards() {
        (ignoreConfig[WILDCARD] as? Map<*, *>)?.let { wildcardMap ->
            if (wildcardMap[WILDCARD] == WILDCARD) {
                log.warn("Wildcard *:*:* format is forbidden and will be ignored")
            }
        }
    }

    /**
     * Checks if an entire path should be ignored.
     */
    public fun shouldIgnorePath(path: String): Boolean {
        return ignoreConfig[path] == WILDCARD
    }

    /**
     * Checks if a specific operation should be ignored.
     */
    public fun shouldIgnoreOperation(path: String, httpMethod: String): Boolean {
        val methodMap = getMethodMapForPath(path)
        return methodMap[httpMethod.uppercase()] == WILDCARD
    }

    /**
     * Returns a list of test case names to ignore for a given path and operation.
     */
    public fun getIgnoredTestCaseNames(path: String, httpMethod: String): List<*>? {
        val methodMap = getMethodMapForPath(path)
        return methodMap[httpMethod.uppercase()] as? List<*>
    }

    /**
     * Filters test cases from the suite based on ignored configuration.
     */
    public fun filterTestCases(
        suite: TestSuite,
        ignoredNames: List<*>?
    ): TestSuite {
        if (ignoredNames == null) return suite

        val filtered = suite.testCases.filterNot { test ->
            ignoredNames.contains(test.name)
        }
        return suite.copy(testCases = filtered)
    }

    /**
     * Returns a combined method map for a specific path and wildcard path.
     */
    private fun getMethodMapForPath(path: String): Map<String, Any?> {
        val pathSpecific = extractUppercaseMethodMap(path)
        val wildcardMethods = extractWildcardMethodMap()
        return pathSpecific + wildcardMethods
    }

    /**
     * Extracts method map for a specific path with uppercase keys.
     */
    private fun extractUppercaseMethodMap(path: String): Map<String, Any?> {
        return (ignoreConfig[path] as? Map<*, *>)
            ?.filter { it.key is String }
            ?.mapKeys { (it.key as String).uppercase() }
            ?: emptyMap()
    }

    /**
     * Extracts wildcard method map (applies to all paths).
     */
    private fun extractWildcardMethodMap(): Map<String, Any?> {
        return extractUppercaseMethodMap(WILDCARD)
            .filterNot { it.key == WILDCARD && it.value == WILDCARD }
    }
}


