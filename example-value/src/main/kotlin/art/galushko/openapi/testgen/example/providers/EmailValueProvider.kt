package art.galushko.openapi.testgen.example.providers

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isString
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import art.galushko.openapi.testgen.example.util.FormatConsts
import io.swagger.v3.oas.models.media.Schema

/**
 * Provides email values for string schemas with format "email".
 *
 * Generates varied emails based on variationIndex.
 */
public class EmailValueProvider(
    public val emailTemplate: String = DEFAULT_EMAIL_TEMPLATE
) : SchemaValueProvider {
    override fun provide(schema: Schema<*>, variationIndex: Int): Any? {
        if (!isString(schema) || schema.format != FormatConsts.EMAIL) {
            return null
        }
        return emailTemplate.format(variationIndex)
    }

    public companion object {
        /** RFC 5322 email template. */
        public const val DEFAULT_EMAIL_TEMPLATE: String = "test%s@example.com"
    }
}
