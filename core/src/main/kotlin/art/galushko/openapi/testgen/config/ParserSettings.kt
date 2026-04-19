package art.galushko.openapi.testgen.config

import art.galushko.openapi.testgen.example.config.ConfigExtractors
import org.slf4j.LoggerFactory

/**
 * Settings for configuring the SnakeYAML parser used by swagger-parser.
 *
 * YAML settings default to `null`, which preserves SnakeYAML's built-in defaults:
 * - `yamlCodePointLimit`: ~3MB (3,145,728 code points)
 * - `yamlMaxAliasesForCollections`: 50
 * - `yamlAllowRecursiveKeys`: false
 * - `yamlNestingDepthLimit`: 50
 *
 * Thread Safety: These settings configure `DeserializationUtils.getOptions()`, which is a global static
 * in swagger-parser. Parsing calls are synchronized by [art.galushko.openapi.testgen.openapi.OpenApiSpecParser]
 * to ensure each parse run applies settings atomically.
 *
 * @property yamlCodePointLimit Maximum code points to parse. Increase for large YAML specs (>3MB).
 * @property yamlMaxAliasesForCollections Maximum YAML aliases for collections (DoS protection).
 * @property yamlAllowRecursiveKeys Whether to allow recursive keys. Keep false to prevent infinite loops.
 * @property yamlNestingDepthLimit Maximum nesting depth for YAML structures.
 */
public data class ParserSettings(
    val yamlCodePointLimit: Int? = null,
    val yamlMaxAliasesForCollections: Int? = null,
    val yamlAllowRecursiveKeys: Boolean? = null,
    val yamlNestingDepthLimit: Int? = null,
) {

    init {
        require(yamlCodePointLimit == null || yamlCodePointLimit > 0) {
            "yamlCodePointLimit must be positive or null, was $yamlCodePointLimit"
        }
        require(yamlMaxAliasesForCollections == null || yamlMaxAliasesForCollections > 0) {
            "yamlMaxAliasesForCollections must be positive or null, was $yamlMaxAliasesForCollections"
        }
        require(yamlNestingDepthLimit == null || yamlNestingDepthLimit > 0) {
            "yamlNestingDepthLimit must be positive or null, was $yamlNestingDepthLimit"
        }
    }

    public companion object {
        private val log = LoggerFactory.getLogger(ParserSettings::class.java)

        private val yamlCodePointLimitName = ParserSettings::yamlCodePointLimit.name
        private val yamlMaxAliasesForCollectionsName = ParserSettings::yamlMaxAliasesForCollections.name
        private val yamlAllowRecursiveKeysName = ParserSettings::yamlAllowRecursiveKeys.name
        private val yamlNestingDepthLimitName = ParserSettings::yamlNestingDepthLimit.name

        /**
         * Builds [ParserSettings] from a map-based configuration.
         *
         * @param map Configuration map with parser settings
         * @param default Default values used for missing entries
         * @return Parsed [ParserSettings]
         * @throws art.galushko.openapi.testgen.example.config.ConfigurationException if a field has an invalid type
         * @throws IllegalArgumentException if validation fails (e.g., non-positive integers)
         */
        public fun fromMap(
            map: Map<String, Any?>,
            default: ParserSettings = ParserSettings(),
        ): ParserSettings {
            val mutableMap = map.toMutableMap()

            val result = ParserSettings(
                yamlCodePointLimit = ConfigExtractors.extractInteger(
                    yamlCodePointLimitName, mutableMap
                ) ?: default.yamlCodePointLimit,
                yamlMaxAliasesForCollections = ConfigExtractors.extractInteger(
                    yamlMaxAliasesForCollectionsName, mutableMap
                ) ?: default.yamlMaxAliasesForCollections,
                yamlAllowRecursiveKeys = ConfigExtractors.extractBoolean(
                    yamlAllowRecursiveKeysName, mutableMap
                ) ?: default.yamlAllowRecursiveKeys,
                yamlNestingDepthLimit = ConfigExtractors.extractInteger(
                    yamlNestingDepthLimitName, mutableMap
                ) ?: default.yamlNestingDepthLimit,
            )

            // Warn about unused keys
            if (mutableMap.isNotEmpty()) {
                log.warn(
                    "Unused configuration entries in parserSettings:\n {}",
                    mutableMap.entries.sortedBy { it.key }.joinToString("\n ")
                )
            }

            return result
        }
    }
}
