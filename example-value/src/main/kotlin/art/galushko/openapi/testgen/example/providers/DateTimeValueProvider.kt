package art.galushko.openapi.testgen.example.providers

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isString
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import art.galushko.openapi.testgen.example.util.FormatConsts
import io.swagger.v3.oas.models.media.Schema
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Provides date-time values for string schemas with format "date-time".
 *
 * Generates varied date-times based on variationIndex, incrementing days from start date.
 */
public class DateTimeValueProvider(
    public val timeSuffixTemplate: String = DEFAULT_TIME_SUFFIX_TEMPLATE,
    public val startDateString: String = DEFAULT_START_DATE_STRING
) : SchemaValueProvider {
    override fun provide(schema: Schema<*>, variationIndex: Int): Any? {
        if (!isString(schema) || schema.format != FormatConsts.DATE_TIME) {
            return null
        }
        val date = LocalDate.parse(startDateString)
            .plusDays(variationIndex.toLong())
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        return timeSuffixTemplate.format(date)
    }

    public companion object {
        /** RFC 3339 date-time suffix template. */
        public const val DEFAULT_TIME_SUFFIX_TEMPLATE: String = "%sT17:32:28Z"

        /** RFC 3339 date format string. */
        public const val DEFAULT_START_DATE_STRING: String = "2025-05-05"
    }
}
