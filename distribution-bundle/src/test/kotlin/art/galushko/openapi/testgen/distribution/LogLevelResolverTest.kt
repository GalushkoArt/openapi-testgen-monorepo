package art.galushko.openapi.testgen.distribution

import art.galushko.openapi.testgen.config.GeneratorConfig
import art.galushko.openapi.testgen.config.TestGeneratorOverrides
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@DisplayName("LogLevelResolver")
class LogLevelResolverTest {

    companion object {
        @JvmStatic
        fun resolutionProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("override wins over config", "debug", "ERROR", "DEBUG"),
            Arguments.of("config used when override is absent", null, "warn", "WARN"),
            Arguments.of("null when neither is set", null, null, null),
            Arguments.of("value is trimmed and uppercased", "  info  ", null, "INFO"),
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("resolutionProvider")
    @DisplayName("should resolve the effective level with override-beats-config precedence")
    fun shouldResolveEffectiveLevel(
        scenario: String,
        overrideLevel: String?,
        configLevel: String?,
        expected: String?,
    ) {
        val config = configLevel?.let { GeneratorConfig(logLevel = it) }
        val overrides = TestGeneratorOverrides(logLevel = overrideLevel)

        assertThat(LogLevelResolver.resolve(config, overrides)).isEqualTo(expected)
    }

    @Test
    @DisplayName("should reject an unknown level with a single well-formed message")
    fun shouldRejectUnknownLevel() {
        assertThatThrownBy {
            LogLevelResolver.resolve(null, TestGeneratorOverrides(logLevel = "bogus"))
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Invalid log level 'BOGUS'. Expected one of ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF.")
    }

    @Test
    @DisplayName("should expose the allowed levels in severity order")
    fun shouldExposeAllowedLevels() {
        assertThat(LogLevelResolver.allowedLevels)
            .containsExactly("ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF")
    }
}
