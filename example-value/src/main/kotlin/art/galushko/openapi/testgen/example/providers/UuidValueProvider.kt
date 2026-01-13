package art.galushko.openapi.testgen.example.providers

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isString
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import art.galushko.openapi.testgen.example.util.FormatConsts
import io.swagger.v3.oas.models.media.Schema

/**
 * Provides UUID values for string schemas with format "uuid".
 *
 * Generates varied UUIDs based on variationIndex.
 */
public class UuidValueProvider(
    public val uuidTemplate: String = DEFAULT_UUID_TEMPLATE,
) : SchemaValueProvider {
    override fun provide(schema: Schema<*>, variationIndex: Int): Any? {
        if (!isString(schema) || schema.format != FormatConsts.UUID) {
            return null
        }
        val paddedIndex = variationIndex.toString().padStart(PADDING_LENGTH, '0')
        return uuidTemplate.format(paddedIndex)
    }

    public companion object {
        private const val PADDING_LENGTH = 12

        /** RFC 9562 UUID template. */
        public const val DEFAULT_UUID_TEMPLATE: String = "d5a5495b-cbdc-4237-a66e-%s"
    }
}
