package art.galushko.openapi.testgen.example.providers

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isString
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import io.swagger.v3.oas.models.media.Schema

/**
 * Provides plain string values for string schemas without specific format.
 *
 * Generates varied strings using base-62 encoding based on variationIndex.
 */
public class PlainStringValueProvider(
    public val validCharsString: String = DEFAULT_VALID_CHARS_STRING
) : SchemaValueProvider {
    override fun provide(schema: Schema<*>, variationIndex: Int): Any? {
        if (!isString(schema)) {
            return null
        }
        return getVariedPlainString(schema, variationIndex)
    }

    private fun getVariedPlainString(schema: Schema<*>, variationIndex: Int): String {
        val minLength = schema.minLength ?: 0
        val baseLength = schema.maxLength ?: (minLength + 1)
        val base = validCharsString.length
        // Convert variationIndex to base-62 representation, padded with 'a'
        val result = CharArray(baseLength) { validCharsString[0] }

        var remaining = variationIndex
        var position = baseLength - 1

        while (remaining > 0 && position >= 0) {
            result[position] = validCharsString[remaining % base]
            remaining /= base
            position--
        }
        return String(result)
    }

    public companion object {
        public const val DEFAULT_VALID_CHARS_STRING: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    }
}
