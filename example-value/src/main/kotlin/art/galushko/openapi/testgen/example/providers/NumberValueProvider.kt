package art.galushko.openapi.testgen.example.providers

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isNumber
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Provides number values for numeric schemas.
 *
 * Generates varied numbers based on minimum, maximum, and multipleOf constraints.
 */
public class NumberValueProvider : SchemaValueProvider {
    override fun provide(schema: Schema<*>, variationIndex: Int): Any? {
        if (!isNumber(schema)) {
            return null
        }
        return getGenericExampleNumberValue(schema, variationIndex)
    }

    private fun getGenericExampleNumberValue(schema: Schema<*>, variationIndex: Int): BigDecimal {
        val base = schema.minimum ?: BigDecimal.ONE
        val step = schema.multipleOf ?: BigDecimal.ONE
        val candidate = base.add(step.multiply(BigDecimal(variationIndex)))
        return if (schema.maximum != null && candidate > schema.maximum) {
            base.add(step.multiply(BigDecimal(variationIndex % getMaxVariations(schema))))
        } else {
            candidate
        }
    }

    private fun getMaxVariations(schema: Schema<*>): Int {
        val min = schema.minimum ?: BigDecimal.ONE
        val max = schema.maximum ?: return Int.MAX_VALUE
        val step = schema.multipleOf ?: BigDecimal.ONE
        return max.subtract(min).divide(step, 0, RoundingMode.DOWN).toInt() + 1
    }
}
