package art.galushko.openapi.testgen.config

import art.galushko.openapi.testgen.example.config.ConfigurationException

/**
 * Utility for deep-merging map-based configuration values.
 *
 * Merge semantics:
 * - If entry is not present in override (or is null) → use base
 * - If both base and override values are provided → use override
 * - If override is present and base is not → use override
 * - Nested maps are merged recursively following the same rules
 */
internal object ConfigMerger {

    /**
     * Deep-merges [overrides] into [base].
     *
     * @param base the base configuration map
     * @param overrides the override values to merge on top of base
     * @param rootField optional field name prefix for error messages (default: "config")
     * @return merged map with override values taking precedence
     */
    fun merge(
        base: Map<String, Any?>,
        overrides: Map<String, Any?>,
        rootField: String = "config",
    ): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()

        // Copy base values
        base.forEach { (key, value) ->
            result[key] = normalizeValue(value, "$rootField.$key")
        }

        // Apply overrides
        overrides.forEach { (key, overrideValue) ->
            val path = "$rootField.$key"
            val baseValue = result[key]
            result[key] = mergeValue(baseValue, overrideValue, path)
        }

        return result.toMap()
    }

    private fun mergeValue(
        baseValue: Any?,
        overrideValue: Any?,
        path: String,
    ): Any? {
        // No override value provided - use base
        if (overrideValue == null) {
            return baseValue?.let { normalizeValue(it, path) }
        }
        // No base value - use override
        if (baseValue == null) {
            return normalizeValue(overrideValue, path)
        }

        val baseMap = baseValue.asStringKeyedMap()
        val overrideMap = overrideValue.asStringKeyedMap()

        // Both maps - deep merge
        if (baseMap != null && overrideMap != null) {
            return merge(baseMap, overrideMap, path)
        }

        // Non-map values - override wins
        return overrideValue
    }

    private fun normalizeValue(value: Any?, path: String): Any? {
        val map = value.asStringKeyedMap() ?: return value
        val normalized = LinkedHashMap<String, Any?>()
        for ((key, v) in map) {
            normalized[key] = normalizeValue(v, "$path.$key")
        }
        return normalized
    }

    private fun Any?.asStringKeyedMap(): Map<String, Any?>? {
        if (this !is Map<*, *>) return null
        val result = LinkedHashMap<String, Any?>()
        for ((key, value) in this) {
            val stringKey = key as? String
                ?: throw ConfigurationException(
                    field = "map.keys",
                    expected = "String",
                    actual = key?.let { it::class.qualifiedName } ?: "null",
                )
            result[stringKey] = value
        }
        return result
    }
}
