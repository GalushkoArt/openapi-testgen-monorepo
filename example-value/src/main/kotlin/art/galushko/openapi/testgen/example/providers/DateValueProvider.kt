package art.galushko.openapi.testgen.example.providers

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isString
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import art.galushko.openapi.testgen.example.util.FormatConsts
import io.swagger.v3.oas.models.media.Schema
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Provides date values for string schemas with format "date".
 *
 * Generates varied dates based on variationIndex, incrementing days from start date.
 */
public class DateValueProvider(
    public val startDateString: String = DEFAULT_START_DATE_STRING
) : SchemaValueProvider {
    override fun provide(schema: Schema<*>, variationIndex: Int): Any? {
        if (!isString(schema) || schema.format != FormatConsts.DATE) {
            return null
        }
        return LocalDate.parse(startDateString)
            .plusDays(variationIndex.toLong())
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    public companion object {
        /** RFC 3339 date format string. */
        public const val DEFAULT_START_DATE_STRING: String = "2025-05-05"
    }
}
