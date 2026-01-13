package art.galushko.openapi.testgen.pattern.value

import art.galushko.openapi.testgen.example.config.ConfigurationException
import org.slf4j.LoggerFactory

/**
 * Configuration options for pattern-based value generation.
 *
 * These options control how the [PatternValueGenerator] generates strings
 * matching or not matching regular expression patterns.
 *
 * ## Character Classes
 *
 * The [spaceChars] option defines what characters match `\s` in patterns.
 * The default includes common whitespace: space, tab, form feed, newline, carriage return,
 * and non-breaking space (U+00A0).
 *
 * The [anyPrintableChars] option defines what characters match `.` (dot) and are used
 * in negated character classes `[^...]`. When `null`, the library default (Latin-1 printable
 * characters) is used.
 *
 * @property defaultMinLength default minimum length used when neither the schema nor the pattern
 *           specifies a minimum length
 * @property spaceChars characters that match `\s` in patterns; also affects `\S` (non-space)
 * @property anyPrintableChars characters that match `.` (any printable); `null` uses library default
 *
 * @see PatternValueGenerator
 */
public data class PatternGenerationOptions(
    val defaultMinLength: Int = DEFAULT_MIN_LENGTH,
    val spaceChars: String = RESTRICTED_SPACE_CHARS,
    val anyPrintableChars: String? = null,
) {

    init {
        require(defaultMinLength >= 0) {
            "defaultMinLength must be non-negative, was $defaultMinLength"
        }
        require(spaceChars.isNotEmpty()) {
            "spaceChars must not be empty"
        }
        anyPrintableChars?.let {
            require(it.isNotEmpty()) {
                "anyPrintableChars must not be empty when specified"
            }
        }
    }

    public companion object {
        private val log = LoggerFactory.getLogger(PatternGenerationOptions::class.java)

        /**
         * Default minimum length for generated strings when not specified by schema or pattern.
         */
        public const val DEFAULT_MIN_LENGTH: Int = 3

        /**
         * Restricted set of whitespace characters for `\s` matching.
         *
         * Includes: space, tab (\t), form feed (\f), newline (\n), carriage return (\r),
         * and non-breaking space (U+00A0).
         *
         * This is more restrictive than ECMA-262's full Unicode whitespace set but covers
         * the most common whitespace characters encountered in API testing.
         */
        public const val RESTRICTED_SPACE_CHARS: String = " \t\u000c\n\r\u00a0"

        /**
         * Parses options from an untyped map (used by CLI / Gradle plugin settings DSL).
         *
         * Supported types:
         * - `defaultMinLength`: Int, Long, Number, numeric String
         * - `spaceChars`: String
         * - `anyPrintableChars`: String or null
         *
         * @throws ConfigurationException when a field has an invalid type/value
         */
        public fun fromMap(map: Map<String, Any?>): PatternGenerationOptions {
            if (map.isEmpty()) return PatternGenerationOptions()

            val mutableMap = map.toMutableMap()

            val defaultMinLength = extractIntegerOrNull("defaultMinLength", mutableMap)
                ?: DEFAULT_MIN_LENGTH
            val spaceChars = extractStringOrNull("spaceChars", mutableMap)
                ?: RESTRICTED_SPACE_CHARS
            val anyPrintableChars = extractNullableString("anyPrintableChars", mutableMap)

            warnUnusedKeys(mutableMap)

            return PatternGenerationOptions(
                defaultMinLength = defaultMinLength,
                spaceChars = spaceChars,
                anyPrintableChars = anyPrintableChars,
            )
        }

        private fun warnUnusedKeys(remaining: Map<String, Any?>) {
            if (remaining.isEmpty()) return

            log.warn(
                "Unused configuration entries in testGenerationSettings.patternGeneration:\n {}",
                remaining.entries.sortedBy { it.key }.joinToString("\n ")
            )
        }

        private fun extractIntegerOrNull(field: String, map: MutableMap<String, Any?>): Int? {
            val value = map.remove(field) ?: return null
            return when (value) {
                is Int -> value
                is Long -> {
                    if (value > Int.MAX_VALUE || value < Int.MIN_VALUE) {
                        throw ConfigurationException(
                            field = "patternGeneration.$field",
                            expected = "Int (32-bit integer)",
                            actual = "Long out of Int range: $value",
                        )
                    }
                    value.toInt()
                }
                is Number -> value.toInt()
                is String -> value.toIntOrNull()
                    ?: throw ConfigurationException(
                        field = "patternGeneration.$field",
                        expected = "Integer or numeric string",
                        actual = "non-numeric string: '$value'",
                    )
                else -> throw ConfigurationException(
                    field = "patternGeneration.$field",
                    expected = "Number or numeric String",
                    actual = value::class.qualifiedName ?: "unknown",
                )
            }
        }

        private fun extractStringOrNull(field: String, map: MutableMap<String, Any?>): String? {
            val value = map.remove(field) ?: return null
            if (value !is String) {
                throw ConfigurationException(
                    field = "patternGeneration.$field",
                    expected = "String",
                    actual = value::class.qualifiedName ?: "unknown",
                )
            }
            return value
        }

        private fun extractNullableString(field: String, map: MutableMap<String, Any?>): String? {
            if (!map.containsKey(field)) return null
            val value = map.remove(field)
            if (value == null) return null
            if (value !is String) {
                throw ConfigurationException(
                    field = "patternGeneration.$field",
                    expected = "String or null",
                    actual = value::class.qualifiedName ?: "unknown",
                )
            }
            return value
        }
    }
}


