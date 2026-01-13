package art.galushko.openapi.testgen.example.providers

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isBoolean
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import io.swagger.v3.oas.models.media.Schema

/**
 * Provides boolean values for boolean schemas.
 *
 * Alternates between true and false based on variationIndex.
 */
public class BooleanValueProvider : SchemaValueProvider {
    override fun provide(schema: Schema<*>, variationIndex: Int): Any? {
        if (!isBoolean(schema)) {
            return null
        }
        return variationIndex % 2 == 0
    }
}
