package art.galushko.openapi.testgen.pattern.value

import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema

/**
 * Provides values matching schema pattern constraints.
 *
 * Applies to string schemas with a non-null `pattern`. Delegates to [PatternValueGenerator]
 * and respects `minLength` / `maxLength` when generating values.
 */
public class PatternValueProvider(
    private val patternValueGenerator: PatternValueGenerator = PatternValueGenerator(),
) : SchemaValueProvider {
    override fun provide(schema: Schema<*>, variationIndex: Int): Any? {
        if (!isStringSchema(schema) || schema.pattern == null) {
            return null
        }
        return patternValueGenerator.generateValidValue(
            pattern = schema.pattern,
            minLength = schema.minLength,
            maxLength = schema.maxLength,
            variationIndex = variationIndex,
        )
    }

    private fun isStringSchema(schema: Schema<*>): Boolean {
        if (schema is StringSchema) return true
        val types = schema.types
        if (types != null && types.contains("string")) return true
        return schema.type == "string"
    }
}


