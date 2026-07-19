package art.galushko.openapi.testgen.example.generator

import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Epic("Example Value Generation")
@Feature("Generator Options")
@DisplayName("SchemaExampleValueGeneratorOptions")
class SchemaExampleValueGeneratorOptionsTest {

    @Test
    @DisplayName("REQUEST_DEFAULTS should equal the all-defaults options")
    fun requestDefaultsShouldEqualAllDefaults() {
        assertThat(SchemaExampleValueGeneratorOptions.REQUEST_DEFAULTS).isEqualTo(
            SchemaExampleValueGeneratorOptions(
                maxExampleDepth = 50,
                includeOptionalExampleProperties = false,
                includeWriteOnly = true,
                useSchemaExampleFallback = false,
                fullExample = false,
            )
        )
    }

    @Test
    @DisplayName("RESPONSE_DEFAULTS should enable optional examples and schema fallback, exclude writeOnly")
    fun responseDefaultsShouldMatchResponseProfile() {
        assertThat(SchemaExampleValueGeneratorOptions.RESPONSE_DEFAULTS).isEqualTo(
            SchemaExampleValueGeneratorOptions(
                maxExampleDepth = 50,
                includeOptionalExampleProperties = true,
                includeWriteOnly = false,
                useSchemaExampleFallback = true,
                fullExample = false,
            )
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("witherProvider")
    @DisplayName("withers should replace exactly one property")
    fun withersShouldReplaceExactlyOneProperty(
        scenario: String,
        actual: SchemaExampleValueGeneratorOptions,
        expected: SchemaExampleValueGeneratorOptions,
    ) {
        assertThat(actual).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        fun witherProvider(): Stream<Arguments> {
            val base = SchemaExampleValueGeneratorOptions.REQUEST_DEFAULTS
            return Stream.of(
                Arguments.of(
                    "withMaxExampleDepth",
                    base.withMaxExampleDepth(7),
                    base.copy(maxExampleDepth = 7),
                ),
                Arguments.of(
                    "withIncludeOptionalExampleProperties",
                    base.withIncludeOptionalExampleProperties(true),
                    base.copy(includeOptionalExampleProperties = true),
                ),
                Arguments.of(
                    "withIncludeWriteOnly",
                    base.withIncludeWriteOnly(false),
                    base.copy(includeWriteOnly = false),
                ),
                Arguments.of(
                    "withUseSchemaExampleFallback",
                    base.withUseSchemaExampleFallback(true),
                    base.copy(useSchemaExampleFallback = true),
                ),
                Arguments.of(
                    "withFullExample",
                    base.withFullExample(true),
                    base.copy(fullExample = true),
                ),
            )
        }
    }
}
