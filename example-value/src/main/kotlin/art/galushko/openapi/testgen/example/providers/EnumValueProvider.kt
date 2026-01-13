package art.galushko.openapi.testgen.example.providers

import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import io.swagger.v3.oas.models.media.Schema

/**
 * Provides values from schema enum definitions.
 *
 * Cycles through enum values based on variationIndex.
 */
public class EnumValueProvider : SchemaValueProvider {
    override fun provide(schema: Schema<*>, variationIndex: Int): Any? {
        val enumValues = schema.enum
        if (enumValues == null || enumValues.isEmpty()) {
            return null
        }
        val index = variationIndex % enumValues.size
        return enumValues[index]
    }
}
