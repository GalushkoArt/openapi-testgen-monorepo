package art.galushko.openapi.testgen.example.providers

import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import io.swagger.v3.oas.models.media.Schema

/**
 * Provides constant values defined in schema.
 *
 * Returns the const value if present, null otherwise.
 */
public class ConstValueProvider : SchemaValueProvider {
    override fun provide(schema: Schema<*>, variationIndex: Int): Any? {
        return schema.const
    }
}
