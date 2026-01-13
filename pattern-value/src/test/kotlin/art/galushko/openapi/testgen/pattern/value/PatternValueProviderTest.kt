package art.galushko.openapi.testgen.pattern.value

import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@Epic("Test Data Generation")
@Feature("Pattern Value Provider")
class PatternValueProviderTest {

    private val provider = PatternValueProvider()

    @Test
    @DisplayName("should return null for non-string schemas")
    fun shouldReturnNullForNonStringSchemas() {
        val result = provider.provide(IntegerSchema(), variationIndex = 0)
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("should return null for string schema without pattern")
    fun shouldReturnNullForStringSchemaWithoutPattern() {
        val result = provider.provide(StringSchema(), variationIndex = 0)
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("should generate value matching schema.pattern")
    fun shouldGenerateValueMatchingPattern() {
        val schema = StringSchema().pattern("^[A-Z]{3}$")

        val result = provider.provide(schema, variationIndex = 0)

        assertThat(result).isInstanceOf(String::class.java)
        assertThat(Regex(schema.pattern).matches(result as String)).isTrue()
    }

    @Test
    @DisplayName("should treat Schema.type='string' as a string schema")
    fun shouldTreatSchemaTypeStringAsStringSchema() {
        val schema = Schema<Any>().apply {
            type = "string"
            pattern = "^[A-Z]{3}$"
        }

        val result = provider.provide(schema, variationIndex = 0)

        assertThat(result).isInstanceOf(String::class.java)
        assertThat(Regex(schema.pattern).matches(result as String)).isTrue()
    }

    @Test
    @DisplayName("should treat Schema.types containing 'string' as a string schema")
    fun shouldTreatSchemaTypesContainingStringAsStringSchema() {
        val schema = Schema<Any>().apply {
            types = setOf("string")
            pattern = "^[A-Z]{3}$"
        }

        val result = provider.provide(schema, variationIndex = 0)

        assertThat(result).isInstanceOf(String::class.java)
        assertThat(Regex(schema.pattern).matches(result as String)).isTrue()
    }
}


