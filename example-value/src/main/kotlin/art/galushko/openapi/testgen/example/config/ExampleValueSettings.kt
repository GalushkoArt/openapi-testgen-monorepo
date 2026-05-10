package art.galushko.openapi.testgen.example.config

import art.galushko.openapi.testgen.example.config.ConfigExtractors.extractBoolean
import art.galushko.openapi.testgen.example.config.ConfigExtractors.extractStringAnyNullableMap
import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGeneratorOptions
import art.galushko.openapi.testgen.example.providers.DateTimeValueProvider
import art.galushko.openapi.testgen.example.providers.DateValueProvider
import art.galushko.openapi.testgen.example.providers.EmailValueProvider
import art.galushko.openapi.testgen.example.providers.PlainStringValueProvider
import art.galushko.openapi.testgen.example.providers.UuidValueProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Configuration for schema example value generation.
 *
 * Controls how synthetic test data is generated from OpenAPI schemas when building valid baselines
 * and when providers need schema-derived values.
 */
public data class ExampleValueSettings(
    /**
     * Ordered list of provider IDs. The first matching provider wins.
     * Omit a provider to disable it.
     *
     * Core providers: enum, const, uuid, email, date, date-time, plain-string, number, boolean
     *
     * Additional providers can be contributed via modules.
     */
    val providers: List<String> = DEFAULT_PROVIDER_ORDER,

    /**
     * Maximum depth for recursive schema traversal while generating examples.
     */
    val maxExampleDepth: Int = SchemaExampleValueGeneratorOptions.DEFAULT_MAX_EXAMPLE_DEPTH,

    /**
     * When true, optional properties with explicit examples/defaults are included for object examples.
     */
    val includeOptionalExampleProperties: Boolean = SchemaExampleValueGeneratorOptions.DEFAULT_INCLUDE_OPTIONAL_EXAMPLES,

    /**
     * When false, writeOnly properties are excluded from generated examples.
     */
    val includeWriteOnly: Boolean = SchemaExampleValueGeneratorOptions.DEFAULT_INCLUDE_WRITE_ONLY,

    /**
     * When true, schema.examples and schema.default are used as fallbacks when example is missing.
     */
    val useSchemaExampleFallback: Boolean = SchemaExampleValueGeneratorOptions.DEFAULT_USE_SCHEMA_EXAMPLE_FALLBACK,

    /** UUID provider settings. */
    val uuid: UuidProviderSettings = UuidProviderSettings(),

    /** Email provider settings. */
    val email: EmailProviderSettings = EmailProviderSettings(),

    /** Date provider settings. */
    val date: DateProviderSettings = DateProviderSettings(),

    /** Date-time provider settings. */
    val dateTime: DateTimeProviderSettings = DateTimeProviderSettings(),

    /** Plain string provider settings. */
    val plainString: PlainStringProviderSettings = PlainStringProviderSettings(),

    /**
     * When true, generated examples are "full": every declared property is populated (required and optional,
     * regardless of explicit examples) and every array contains at least one item. Composed schemas collapse
     * to a single variant. `includeWriteOnly` and depth/cycle guards still apply.
     */
    val fullExample: Boolean = SchemaExampleValueGeneratorOptions.DEFAULT_FULL_EXAMPLE,
) {
    init {
        require(maxExampleDepth > 0) { "maxExampleDepth must be positive, was $maxExampleDepth" }
        require(providers.isNotEmpty()) { "providers list must not be empty" }

        require(providers.all { it.isNotBlank() }) { "providers list must not contain blank ids" }

        val duplicates = providers.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        require(duplicates.isEmpty()) { "providers list must not contain duplicates: $duplicates" }

        require(uuid.template.contains("%s")) { "uuid.template must contain '%s'" }
        require(email.template.contains("%s")) { "email.template must contain '%s'" }
        require(dateTime.timeSuffixTemplate.contains("%s")) { "dateTime.timeSuffixTemplate must contain '%s'" }
        require(plainString.validChars.isNotEmpty()) { "plainString.validChars must not be empty" }

        // Fail fast on invalid date formats
        try {
            LocalDate.parse(date.startDate)
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException(
                "date.startDate must be ISO-8601 format (YYYY-MM-DD): '${date.startDate}'",
                e,
            )
        }
        try {
            LocalDate.parse(dateTime.startDate)
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("dateTime.startDate must be ISO-8601 format (YYYY-MM-DD): '${dateTime.startDate}'", e)
        }
    }

    public companion object {
        /** Default provider order (format-first). */
        public val DEFAULT_PROVIDER_ORDER: List<String> = listOf(
            "enum",
            "const",
            "uuid",
            "email",
            "date",
            "date-time",
            "plain-string",
            "number",
            "boolean",
        )

        /**
         * All known core provider IDs.
         *
         * Note: the provider list is open-world. Unknown ids can be contributed by modules.
         */
        public val KNOWN_PROVIDERS: Set<String> = DEFAULT_PROVIDER_ORDER.toSet()

        private val log = LoggerFactory.getLogger(ExampleValueSettings::class.java)

        /**
         * Builds [ExampleValueSettings] from a map-based configuration.
         *
         * @param default default values used for missing entries
         * @throws ConfigurationException if a field has an invalid type
         */
        public fun fromMap(map: Map<String, Any?>, default: ExampleValueSettings = ExampleValueSettings()): ExampleValueSettings {
            val mutableMap = map.toMutableMap()

            val providers = ConfigExtractors.extractStringList("providers", mutableMap)
                ?: default.providers

            val maxExampleDepth = ConfigExtractors.extractInteger("maxExampleDepth", mutableMap)
                ?: default.maxExampleDepth

            val includeOptionalExampleProperties = extractBoolean("includeOptionalExampleProperties", mutableMap)
                ?: default.includeOptionalExampleProperties
            val includeWriteOnly = extractBoolean("includeWriteOnly", mutableMap)
                ?: default.includeWriteOnly
            val useSchemaExampleFallback = extractBoolean("useSchemaExampleFallback", mutableMap)
                ?: default.useSchemaExampleFallback
            val fullExample = extractBoolean("fullExample", mutableMap)
                ?: default.fullExample

            val uuidMap = extractStringAnyNullableMap("uuid", mutableMap) ?: emptyMap()
            val emailMap = extractStringAnyNullableMap("email", mutableMap) ?: emptyMap()
            val dateMap = extractStringAnyNullableMap("date", mutableMap) ?: emptyMap()
            val dateTimeMap = extractStringAnyNullableMap("dateTime", mutableMap) ?: emptyMap()
            val plainStringMap = extractStringAnyNullableMap("plainString", mutableMap) ?: emptyMap()

            val result = ExampleValueSettings(
                providers = providers,
                maxExampleDepth = maxExampleDepth,
                includeOptionalExampleProperties = includeOptionalExampleProperties,
                includeWriteOnly = includeWriteOnly,
                useSchemaExampleFallback = useSchemaExampleFallback,
                fullExample = fullExample,
                uuid = UuidProviderSettings.fromMap(uuidMap, default.uuid),
                email = EmailProviderSettings.fromMap(emailMap, default.email),
                date = DateProviderSettings.fromMap(dateMap, default.date),
                dateTime = DateTimeProviderSettings.fromMap(dateTimeMap, default.dateTime),
                plainString = PlainStringProviderSettings.fromMap(plainStringMap, default.plainString),
            )

            warnUnusedKeys(
                log = log,
                scope = "testGenerationSettings.exampleValues",
                mutableMap = mutableMap,
            )

            return result
        }
    }
}

/**
 * Configuration for UUID value generation.
 *
 * @property template template string for UUID generation
 */
public data class UuidProviderSettings(
    val template: String = UuidValueProvider.DEFAULT_UUID_TEMPLATE,
) {
    public companion object {
        /**
         * Builds [UuidProviderSettings] from a map-based configuration.
         *
         * @param map raw configuration map
         * @param default fallback values for missing entries
         */
        public fun fromMap(map: Map<String, Any?>, default: UuidProviderSettings): UuidProviderSettings {
            val mutableMap = map.toMutableMap()
            val template = extractStringOrDefault("template", mutableMap, default.template)

            warnUnusedKeys(
                log = LoggerFactory.getLogger(UuidProviderSettings::class.java),
                scope = "testGenerationSettings.exampleValues.uuid",
                mutableMap = mutableMap,
            )

            return UuidProviderSettings(template = template)
        }
    }
}

/**
 * Configuration for email value generation.
 *
 * @property template template string for email generation
 */
public data class EmailProviderSettings(
    val template: String = EmailValueProvider.DEFAULT_EMAIL_TEMPLATE,
) {
    public companion object {
        /**
         * Builds [EmailProviderSettings] from a map-based configuration.
         *
         * @param map raw configuration map
         * @param default fallback values for missing entries
         */
        public fun fromMap(map: Map<String, Any?>, default: EmailProviderSettings): EmailProviderSettings {
            val mutableMap = map.toMutableMap()
            val template = extractStringOrDefault("template", mutableMap, default.template)

            warnUnusedKeys(
                log = LoggerFactory.getLogger(EmailProviderSettings::class.java),
                scope = "testGenerationSettings.exampleValues.email",
                mutableMap = mutableMap,
            )

            return EmailProviderSettings(template = template)
        }
    }
}

/**
 * Configuration for date value generation.
 *
 * @property startDate start date string for date generation (format: yyyy-MM-dd)
 */
public data class DateProviderSettings(
    val startDate: String = DateValueProvider.DEFAULT_START_DATE_STRING,
) {
    public companion object {
        /**
         * Builds [DateProviderSettings] from a map-based configuration.
         *
         * @param map raw configuration map
         * @param default fallback values for missing entries
         */
        public fun fromMap(map: Map<String, Any?>, default: DateProviderSettings): DateProviderSettings {
            val mutableMap = map.toMutableMap()
            val startDate = extractStringOrDefault("startDate", mutableMap, default.startDate)

            warnUnusedKeys(
                log = LoggerFactory.getLogger(DateProviderSettings::class.java),
                scope = "testGenerationSettings.exampleValues.date",
                mutableMap = mutableMap,
            )

            return DateProviderSettings(startDate = startDate)
        }
    }
}

/**
 * Configuration for date-time value generation.
 *
 * @property startDate start date string (format: yyyy-MM-dd)
 * @property timeSuffixTemplate template for the time portion suffix
 */
public data class DateTimeProviderSettings(
    val startDate: String = DateTimeValueProvider.DEFAULT_START_DATE_STRING,
    val timeSuffixTemplate: String = DateTimeValueProvider.DEFAULT_TIME_SUFFIX_TEMPLATE,
) {
    public companion object {
        /**
         * Builds [DateTimeProviderSettings] from a map-based configuration.
         *
         * @param map raw configuration map
         * @param default fallback values for missing entries
         */
        public fun fromMap(map: Map<String, Any?>, default: DateTimeProviderSettings): DateTimeProviderSettings {
            val mutableMap = map.toMutableMap()
            val startDate = extractStringOrDefault("startDate", mutableMap, default.startDate)
            val timeSuffixTemplate = extractStringOrDefault(
                "timeSuffixTemplate",
                mutableMap,
                default.timeSuffixTemplate,
            )

            warnUnusedKeys(
                log = LoggerFactory.getLogger(DateTimeProviderSettings::class.java),
                scope = "testGenerationSettings.exampleValues.dateTime",
                mutableMap = mutableMap,
            )

            return DateTimeProviderSettings(
                startDate = startDate,
                timeSuffixTemplate = timeSuffixTemplate,
            )
        }
    }
}

/**
 * Configuration for plain string value generation.
 *
 * @property validChars characters allowed in generated strings
 */
public data class PlainStringProviderSettings(
    val validChars: String = PlainStringValueProvider.DEFAULT_VALID_CHARS_STRING,
) {
    public companion object {
        /**
         * Builds [PlainStringProviderSettings] from a map-based configuration.
         *
         * @param map raw configuration map
         * @param default fallback values for missing entries
         */
        public fun fromMap(map: Map<String, Any?>, default: PlainStringProviderSettings): PlainStringProviderSettings {
            val mutableMap = map.toMutableMap()
            val validChars = extractStringOrDefault("validChars", mutableMap, default.validChars)

            warnUnusedKeys(
                log = LoggerFactory.getLogger(PlainStringProviderSettings::class.java),
                scope = "testGenerationSettings.exampleValues.plainString",
                mutableMap = mutableMap,
            )

            return PlainStringProviderSettings(validChars = validChars)
        }
    }
}

private fun extractStringOrDefault(field: String, map: MutableMap<String, Any?>, default: String): String {
    val raw = map.remove(field) ?: return default
    if (raw !is String) {
        throw ConfigurationException(
            field = field,
            expected = "String",
            actual = raw::class.qualifiedName ?: "unknown",
        )
    }
    return raw
}

private fun warnUnusedKeys(
    log: org.slf4j.Logger,
    scope: String,
    mutableMap: Map<String, Any?>,
) {
    if (mutableMap.isEmpty()) return
    log.warn(
        "Unused configuration entries in {}:\n {}",
        scope,
        mutableMap.entries.sortedBy { it.key }.joinToString("\n "),
    )
}
