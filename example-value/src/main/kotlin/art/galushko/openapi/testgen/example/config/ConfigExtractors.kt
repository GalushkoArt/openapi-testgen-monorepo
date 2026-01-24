package art.galushko.openapi.testgen.example.config

/**
 * Type-safe configuration of value extractors.
 * Each method removes the value from the map and returns null if not present.
 *
 * All extractors follow the same contract:
 * - If the key is absent or null, return null (allows caller to use default)
 * - If the key exists, remove it from the map (for detecting unused keys)
 * - If the value has wrong type, throw [ConfigurationException] with field context
 */
public object ConfigExtractors {

    /**
     * Extracts and validates a Map<String, Any> value.
     * Values in the map must be non-null.
     */
    public fun extractStringAnyMap(field: String, map: MutableMap<String, Any?>): Map<String, Any>? {
        val value = map.remove(field) ?: return null

        if (value !is Map<*, *>) {
            throw ConfigurationException(
                field = field,
                expected = "Map<String, Any>",
                actual = value::class.qualifiedName ?: "unknown",
            )
        }

        val result = mutableMapOf<String, Any>()
        value.forEach { (k, v) ->
            if (k !is String) {
                throw ConfigurationException(
                    field = "$field.keys",
                    expected = "String",
                    actual = k?.let { it::class.qualifiedName } ?: "null",
                )
            }
            if (v == null) {
                throw ConfigurationException(
                    field = "$field[$k]",
                    expected = "non-null value",
                    actual = "null",
                )
            }
            result[k] = v
        }
        return result.toMap()
    }

    /**
     * Extracts and validates a Map<String, String> value.
     */
    public fun extractStringStringMap(field: String, map: MutableMap<String, Any?>): Map<String, String>? {
        val value = map.remove(field) ?: return null

        if (value !is Map<*, *>) {
            throw ConfigurationException(
                field = field,
                expected = "Map<String, String>",
                actual = value::class.qualifiedName ?: "unknown",
            )
        }

        val result = mutableMapOf<String, String>()
        value.forEach { (k, v) ->
            if (k !is String) {
                throw ConfigurationException(
                    field = "$field.keys",
                    expected = "String",
                    actual = k?.let { it::class.qualifiedName } ?: "null",
                )
            }
            if (v !is String) {
                throw ConfigurationException(
                    field = "$field[$k]",
                    expected = "String",
                    actual = v?.let { it::class.qualifiedName } ?: "null",
                )
            }
            result[k] = v
        }
        return result.toMap()
    }

    /**
     * Extracts and validates a Set<String> value from a Collection.
     */
    public fun extractStringSet(field: String, map: MutableMap<String, Any?>): Set<String>? {
        val value = map.remove(field) ?: return null

        if (value !is Collection<*>) {
            throw ConfigurationException(
                field = field,
                expected = "Collection<String>",
                actual = value::class.qualifiedName ?: "unknown",
            )
        }

        return value.mapIndexed { index, item ->
            if (item !is String) {
                throw ConfigurationException(
                    field = "$field[$index]",
                    expected = "String",
                    actual = item?.let { it::class.qualifiedName } ?: "null",
                )
            }
            item
        }.toSet()
    }

    /**
     * Extracts and validates a List<String> value from a Collection.
     */
    public fun extractStringList(field: String, map: MutableMap<String, Any?>): List<String>? {
        val value = map.remove(field) ?: return null

        if (value !is Collection<*>) {
            throw ConfigurationException(
                field = field,
                expected = "Collection<String>",
                actual = value::class.qualifiedName ?: "unknown",
            )
        }

        return value.mapIndexed { index, item ->
            if (item !is String) {
                throw ConfigurationException(
                    field = "$field[$index]",
                    expected = "String",
                    actual = item?.let { it::class.qualifiedName } ?: "null",
                )
            }
            item
        }
    }

    /**
     * Extracts and validates a Map<String, Any?> value.
     *
     * Values in the map may be null. This is useful for optional nested configuration objects.
     */
    public fun extractStringAnyNullableMap(field: String, map: MutableMap<String, Any?>): Map<String, Any?>? {
        val value = map.remove(field) ?: return null

        if (value !is Map<*, *>) {
            throw ConfigurationException(
                field = field,
                expected = "Map<String, Any?>",
                actual = value::class.qualifiedName ?: "unknown",
            )
        }

        val result = mutableMapOf<String, Any?>()
        value.forEach { (k, v) ->
            if (k !is String) {
                throw ConfigurationException(
                    field = "$field.keys",
                    expected = "String",
                    actual = k?.let { it::class.qualifiedName } ?: "null",
                )
            }
            result[k] = v
        }
        return result.toMap()
    }

    /**
     * Extracts and validates a String value.
     */
    public fun extractString(field: String, map: MutableMap<String, Any?>): String? {
        val value = map.remove(field) ?: return null

        if (value !is String) {
            throw ConfigurationException(
                field = field,
                expected = "String",
                actual = value::class.qualifiedName ?: "unknown",
            )
        }

        return value
    }

    /**
     * Extracts and validates a Boolean value.
     * Supports Boolean and case-insensitive boolean String inputs.
     */
    public fun extractBoolean(field: String, map: MutableMap<String, Any?>): Boolean? {
        val value = map.remove(field) ?: return null

        return when (value) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull()
                ?: throw ConfigurationException(
                    field = field,
                    expected = "Boolean or boolean String",
                    actual = "non-boolean string: '$value'",
                )
            else -> throw ConfigurationException(
                field = field,
                expected = "Boolean or boolean String",
                actual = value::class.qualifiedName ?: "unknown",
            )
        }
    }

    /**
     * Extracts and validates an Integer value.
     * Supports Int, Long, Number, and numeric String inputs.
     */
    public fun extractInteger(field: String, map: MutableMap<String, Any?>): Int? {
        val value = map.remove(field) ?: return null

        return when (value) {
            is Int -> value
            is Long -> {
                if (value > Int.MAX_VALUE || value < Int.MIN_VALUE) {
                    throw ConfigurationException(
                        field = field,
                        expected = "Int (32-bit integer)",
                        actual = "Long out of Int range: $value",
                    )
                }
                value.toInt()
            }
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
                ?: throw ConfigurationException(
                    field = field,
                    expected = "Integer or numeric string",
                    actual = "non-numeric string: '$value'",
                )
            else -> throw ConfigurationException(
                field = field,
                expected = "Number or numeric String",
                actual = value::class.qualifiedName ?: "unknown",
            )
        }
    }

    /**
     * Extracts and validates an Enum value.
     * Supports both the enum type directly and String values.
     * String matching is case-insensitive and converts hyphens to underscores.
     */
    public inline fun <reified T : Enum<*>> extractEnum(field: String, map: MutableMap<String, Any?>): T? {
        val value = map.remove(field) ?: return null

        if (value is T) return value

        if (value !is String) {
            throw ConfigurationException(
                field = field,
                expected = "${T::class.simpleName} or String",
                actual = value::class.qualifiedName ?: "unknown",
            )
        }

        val normalized = value.replace('-', '_').uppercase()
        return T::class.java.enumConstants.find { it.name == normalized }
            ?: throw ConfigurationException(
                field = field,
                expected = "one of ${T::class.java.enumConstants.map { it.name }}",
                actual = "'$value'",
            )
    }

    /**
     * Extracts and validates a Map<String, List<String>> value.
     *
     * Supports flexible input formats:
     * - Map values can be List<String> (standard format)
     * - Map values can be String (converted to single-element list)
     *
     * This is useful for configurations like `includeOperations` where:
     * - `"/path": ["GET", "POST"]` is the standard form
     * - `"/path": "GET"` is shorthand for single method
     */
    public fun extractStringListMap(field: String, map: MutableMap<String, Any?>): Map<String, List<String>>? {
        val value = map.remove(field) ?: return null

        if (value !is Map<*, *>) {
            throw ConfigurationException(field, "Map<String, List<String>>", value::class.qualifiedName ?: "unknown")
        }

        return value.entries.associate { (k, v) ->
            val key = requireStringKey(field, k)
            val methods = extractMethodList(field, key, v)
            key to methods
        }
    }

    private fun requireStringKey(field: String, key: Any?): String {
        if (key !is String) {
            throw ConfigurationException("$field.keys", "String", key?.let { it::class.qualifiedName } ?: "null")
        }
        return key
    }

    private fun extractMethodList(field: String, key: String, value: Any?): List<String> = when (value) {
        is String -> listOf(value)
        is Collection<*> -> value.mapIndexed { index, item ->
            if (item !is String) {
                throw ConfigurationException("$field[$key][$index]", "String", item?.let { it::class.qualifiedName } ?: "null")
            }
            item
        }
        else -> throw ConfigurationException("$field[$key]", "List<String> or String", value?.let { it::class.qualifiedName } ?: "null")
    }
}
