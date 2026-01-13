package art.galushko.openapi.testgen.example.generator

/**
 * Configuration options for [SchemaExampleValueGenerator].
 *
 * Controls the behavior of example value generation from OpenAPI schemas.
 *
 * @property maxExampleDepth Maximum depth for recursive schema traversal (default: 50)
 */
public data class SchemaExampleValueGeneratorOptions(
    val maxExampleDepth: Int = DEFAULT_MAX_EXAMPLE_DEPTH,
) {
    public companion object {
        public const val DEFAULT_MAX_EXAMPLE_DEPTH: Int = 50
    }
}
