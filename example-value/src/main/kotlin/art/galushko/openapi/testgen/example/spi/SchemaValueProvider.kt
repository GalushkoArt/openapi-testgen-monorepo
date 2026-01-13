package art.galushko.openapi.testgen.example.spi

import io.swagger.v3.oas.models.media.Schema

/**
 * Provides example values for OpenAPI schemas.
 *
 * Implementations should check whether they support the given schema and return an
 * appropriate value, or null when not applicable.
 *
 * Providers are composed in a chain where the first non-null result wins. Providers
 * should be deterministic and side-effect free.
 */
public fun interface SchemaValueProvider {
    /**
     * Attempts to provide an example value for the given schema.
     *
     * @param schema the OpenAPI schema to generate a value for
     * @param variationIndex index used to generate varied values for uniqueness
     * @return example value matching the schema, or null if this provider cannot handle the schema
     */
    public fun provide(schema: Schema<*>, variationIndex: Int): Any?
}
