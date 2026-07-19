package art.galushko.openapi.testgen.generator.template

import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Epic("Generator")
@Feature("MustacheLambdas")
@DisplayName("escapeString Lambda Tests")
class MustacheLamdasTest {
    fun escapeStringProvider(): Stream<Arguments> = Stream.of(
        // Basic string - no escaping needed
        Arguments.of("Basic string unchanged", "hello", "hello"),

        // Empty string
        Arguments.of("Empty string unchanged", "", ""),

        // Backslash escaping
        Arguments.of("Backslash escaped", "a\\b", "a\\\\b"),

        // Double quote escaping
        Arguments.of("Double quote escaped", "\"quoted\"", "\\\"quoted\\\""),

        // Newline escaping
        Arguments.of("Newline escaped", "line1\nline2", "line1\\nline2"),

        // Carriage return escaping
        Arguments.of("Carriage return escaped", "a\rb", "a\\rb"),

        // Tab escaping
        Arguments.of("Tab escaped", "a\tb", "a\\tb"),

        // Backspace escaping
        Arguments.of("Backspace escaped", "a\bb", "a\\bb"),

        // Form feed escaping
        Arguments.of("Form feed escaped as unicode", "a\u000Cb", "a\\u000cb"),

        // Line separator U+2028
        Arguments.of("Line separator U+2028 escaped", "a\u2028b", "a\\u2028b"),

        // Paragraph separator U+2029
        Arguments.of("Paragraph separator U+2029 escaped", "a\u2029b", "a\\u2029b"),

        // Forward slash must stay unescaped (\/ is illegal in Java/Kotlin literals)
        Arguments.of("Forward slash left unescaped", "a/b", "a/b"),

        // URL with slashes stays a valid Java/Kotlin literal
        Arguments.of("URL not mangled", "https://example.com/a/b?q=1", "https://example.com/a/b?q=1"),

        // Dollar sign escaped as unicode (guards Kotlin string-template interpolation)
        Arguments.of("Dollar sign escaped", "price: $10", "price: \\u002410"),
        Arguments.of("Dollar before identifier escaped", "\${injected}", "\\u0024{injected}"),

        // Control character (U+0001)
        Arguments.of("Control character U+0001 escaped", "a\u0001b", "a\\u0001b"),

        // Control character (U+001F - last control char before space)
        Arguments.of("Control character U+001F escaped", "a\u001Fb", "a\\u001fb"),

        // Combined escapes
        Arguments.of(
            "Combined escapes",
            "\"hello\nworld\"",
            "\\\"hello\\nworld\\\""
        ),

        // Multiple special characters
        Arguments.of(
            "Multiple special characters",
            "path/to\\file\twith\nlines",
            "path/to\\\\file\\twith\\nlines"
        ),

        // Non-string input - Integer passthrough
        Arguments.of("Integer passthrough", 123, 123),

        // Non-string input - Boolean passthrough
        Arguments.of("Boolean passthrough", true, true),

        // Note: null input is not tested because escapeString expects non-null Object
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("escapeStringProvider")
    @DisplayName("should escape strings correctly for Java/Kotlin string literals")
    fun shouldEscapeStringsCorrectly(
        scenario: String,
        input: Any,
        expected: Any,
    ) {
        val result = escapeString(input)

        assertThat(result).isEqualTo(expected)
    }
}

