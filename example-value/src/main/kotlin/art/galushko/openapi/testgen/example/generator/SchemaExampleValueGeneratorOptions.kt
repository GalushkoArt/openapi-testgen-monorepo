package art.galushko.openapi.testgen.example.generator

/**
 * Configuration options for [SchemaExampleValueGenerator].
 *
 * Controls the behavior of example value generation from OpenAPI schemas.
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
        public val REQUEST_DEFAULTS: SchemaExampleValueGeneratorOptions = SchemaExampleValueGeneratorOptions()

        /**
         * Default options for response body generation.
         * Includes optional properties with examples, excludes writeOnly fields,
         * and falls back to schema examples/defaults.
         */
        public val RESPONSE_DEFAULTS: SchemaExampleValueGeneratorOptions = SchemaExampleValueGeneratorOptions(
            includeOptionalExampleProperties = true,
            includeWriteOnly = false,
            useSchemaExampleFallback = true,
        )
    }
}
