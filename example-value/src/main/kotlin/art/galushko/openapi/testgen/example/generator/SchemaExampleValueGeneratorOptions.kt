package art.galushko.openapi.testgen.example.generator

/**
 * Configuration options for [SchemaExampleValueGenerator].
 *
 * Controls the behavior of example value generation from OpenAPI schemas.
 *
 * Java callers: start from [REQUEST_DEFAULTS] or [RESPONSE_DEFAULTS] and adjust via the
 * `with*` methods instead of the positional constructor:
 *
 * ```java
 * var options = SchemaExampleValueGeneratorOptions.RESPONSE_DEFAULTS.withFullExample(true);
 * ```
 *
 * Note: when a generator is used through the `ResponseExampleExtractor(SchemaExampleValueGenerator)`
 * constructor, response extraction applies [RESPONSE_DEFAULTS] for the include/fallback flags and only
 * honors [maxExampleDepth] and [fullExample] from the configured options.
 *
 * @property maxExampleDepth Maximum depth for recursive schema traversal (default: 50)
 * @property includeOptionalExampleProperties When true, includes optional properties that have explicit examples
 * @property includeWriteOnly When false, excludes writeOnly properties (appropriate for response examples)
 * @property useSchemaExampleFallback When true, falls back to schema.examples and schema.default if no example found
 * @property fullExample When true, generates a complete example: every declared property (required and optional,
 *   regardless of explicit examples) is populated, and every array contains at least one item. For composed schemas
 *   (`oneOf`/`anyOf`), a single variant is produced. `includeWriteOnly` and depth/cycle guards still apply.
 */
public data class SchemaExampleValueGeneratorOptions(
    val maxExampleDepth: Int = DEFAULT_MAX_EXAMPLE_DEPTH,
    val includeOptionalExampleProperties: Boolean = DEFAULT_INCLUDE_OPTIONAL_EXAMPLES,
    val includeWriteOnly: Boolean = DEFAULT_INCLUDE_WRITE_ONLY,
    val useSchemaExampleFallback: Boolean = DEFAULT_USE_SCHEMA_EXAMPLE_FALLBACK,
    val fullExample: Boolean = DEFAULT_FULL_EXAMPLE,
) {
    /** Returns a copy with [maxExampleDepth] replaced. Java-friendly alternative to `copy`. */
    public fun withMaxExampleDepth(value: Int): SchemaExampleValueGeneratorOptions = copy(maxExampleDepth = value)

    /** Returns a copy with [includeOptionalExampleProperties] replaced. Java-friendly alternative to `copy`. */
    public fun withIncludeOptionalExampleProperties(value: Boolean): SchemaExampleValueGeneratorOptions =
        copy(includeOptionalExampleProperties = value)

    /** Returns a copy with [includeWriteOnly] replaced. Java-friendly alternative to `copy`. */
    public fun withIncludeWriteOnly(value: Boolean): SchemaExampleValueGeneratorOptions = copy(includeWriteOnly = value)

    /** Returns a copy with [useSchemaExampleFallback] replaced. Java-friendly alternative to `copy`. */
    public fun withUseSchemaExampleFallback(value: Boolean): SchemaExampleValueGeneratorOptions =
        copy(useSchemaExampleFallback = value)

    /** Returns a copy with [fullExample] replaced. Java-friendly alternative to `copy`. */
    public fun withFullExample(value: Boolean): SchemaExampleValueGeneratorOptions = copy(fullExample = value)

    public companion object {
        public const val DEFAULT_MAX_EXAMPLE_DEPTH: Int = 50
        public const val DEFAULT_INCLUDE_OPTIONAL_EXAMPLES: Boolean = false
        public const val DEFAULT_INCLUDE_WRITE_ONLY: Boolean = true
        public const val DEFAULT_USE_SCHEMA_EXAMPLE_FALLBACK: Boolean = false
        public const val DEFAULT_FULL_EXAMPLE: Boolean = false

        /**
         * Default options for request body generation.
         * Generates only required properties and includes writeOnly fields.
         */
        @JvmField
        public val REQUEST_DEFAULTS: SchemaExampleValueGeneratorOptions = SchemaExampleValueGeneratorOptions()

        /**
         * Default options for response body generation.
         * Includes optional properties with examples, excludes writeOnly fields,
         * and falls back to schema examples/defaults.
         */
        @JvmField
        public val RESPONSE_DEFAULTS: SchemaExampleValueGeneratorOptions = SchemaExampleValueGeneratorOptions(
            includeOptionalExampleProperties = true,
            includeWriteOnly = false,
            useSchemaExampleFallback = true,
        )
    }
}
