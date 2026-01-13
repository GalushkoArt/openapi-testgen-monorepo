package art.galushko.openapi.testgen.cli

/**
 * Parses key-value pairs with dot notation support for nested structures.
 *
 * Supports:
 * - Simple values: `maxErrors=10` → `{"maxErrors": "10"}`
 * - Nested maps: `validSecurityValues.ApiKey=test` → `{"validSecurityValues": {"ApiKey": "test"}}`
 * - Wildcard values: `ignoreTestCases./api/users=*` → `{"ignoreTestCases": {"/api/users": "*"}}`
 * - Nested wildcards: `ignoreTestCases./api/users.GET=*` → `{"ignoreTestCases": {"/api/users": {"GET": "*"}}}`
 * - Array notation: `ignoreTestCases./api/users.GET[]=test1` → `{"ignoreTestCases": {"/api/users": {"GET": ["test1"]}}}`
 * - Multiple array values with the same key automatically merge into a list
 */
@Suppress("UNCHECKED_CAST")
public object KeyValueParser {

    /**
     * Parses an array of key-value string entries into a nested map structure.
     *
     * @param entries Array of strings in the format "key.nested.path=value" or "key.nested.path[]=value"
     * @return A nested map representing the parsed structure
     * @throws IllegalArgumentException if any entry has an invalid format or conflicting values
     */
    public fun parse(entries: Array<String>): Map<String, Any> {
        if (entries.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, Any>()
        entries.forEach { rawEntry ->
            val keyValue = parseKeyValue(rawEntry)
            insertIntoMap(result, keyValue)
        }

        return result
    }

    /**
     * Parses a single key-value entry into a structured representation.
     */
    private fun parseKeyValue(raw: String): KeyValue {
        val equalsIndex = raw.indexOf('=')
        require(equalsIndex > 0 && equalsIndex < raw.length - 1) {
            "Invalid key=value: '$raw'"
        }

        val path = raw.take(equalsIndex)
        val value = raw.substring(equalsIndex + 1)

        val isArray = path.endsWith("[]")
        val cleanPath = if (isArray) path.removeSuffix("[]") else path
        val keys = cleanPath.split('.')

        return KeyValue(keys, value, isArray)
    }

    /**
     * Inserts a parsed key-value into the target map, creating nested structures as needed.
     */
    private fun insertIntoMap(target: MutableMap<String, Any>, keyValue: KeyValue) {
        var current = target

        keyValue.keys.forEachIndexed { index, key ->
            if (index == keyValue.keys.lastIndex) {
                setFinalValue(current, key, keyValue)
            } else {
                current = navigateToNestedMap(current, key, keyValue.fullPath)
            }
        }
    }

    /**
     * Sets the final value in the map, handling both array and scalar values.
     */
    private fun setFinalValue(map: MutableMap<String, Any>, key: String, keyValue: KeyValue) {
        if (keyValue.isArray) {
            setArrayValue(map, key, keyValue)
        } else {
            setScalarValue(map, key, keyValue)
        }
    }

    /**
     * Sets an array value, appending to an existing list or creating a new one.
     */
    private fun setArrayValue(map: MutableMap<String, Any>, key: String, keyValue: KeyValue) {
        when (val existingValue = map[key]) {
            null -> map[key] = mutableListOf(keyValue.value)
            is MutableList<*> -> (existingValue as MutableList<String>).add(keyValue.value)
            else -> throw IllegalArgumentException(
                "Conflicting values for '${keyValue.fullPath}': cannot add array element to key '$key' " +
                    "because it is already defined as a non-list value"
            )
        }
    }

    /**
     * Sets a scalar value, validating that the key is not already used for a nested structure.
     */
    private fun setScalarValue(map: MutableMap<String, Any>, key: String, keyValue: KeyValue) {
        val existingValue = map[key]
        if (existingValue is MutableMap<*, *> || existingValue is List<*>) {
            throw IllegalArgumentException(
                "Conflicting values for '${keyValue.fullPath}': cannot assign scalar value to key '$key' " +
                    "because it is already used as a nested object or list"
            )
        }
        map[key] = keyValue.value
    }

    /**
     * Navigates to or creates a nested map for intermediate keys in the path.
     */
    private fun navigateToNestedMap(
        current: MutableMap<String, Any>,
        key: String,
        fullPath: String
    ): MutableMap<String, Any> {
        return when (val existingValue = current[key]) {
            null -> {
                val newMap = mutableMapOf<String, Any>()
                current[key] = newMap
                newMap
            }
            is MutableMap<*, *> -> existingValue as MutableMap<String, Any>
            else -> throw IllegalArgumentException(
                "Conflicting values for '$fullPath': key '$key' is already defined as a scalar " +
                    "or list and cannot be treated as a nested object"
            )
        }
    }

    /**
     * Internal data structure representing a parsed key-value entry.
     */
    private data class KeyValue(
        val keys: List<String>,
        val value: String,
        val isArray: Boolean
    ) {
        val fullPath: String = keys.joinToString(".") + if (isArray) "[]" else ""
    }
}
