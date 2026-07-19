package art.galushko.openapi.testgen.generator.template

import kotlin.text.iterator

private const val ESCAPE_BUFFER_PADDING = 16
private const val UNICODE_ESCAPE_LENGTH = 4
private const val HEX_RADIX = 16

/**
 * Escapes [value] for embedding in a double-quoted string literal that must compile as both Java
 * and Kotlin source. Built-in template sets exist for both languages and custom templates carry no
 * language metadata, so the output has to be valid in either.
 *
 * Language-portability constraints:
 * - `$` becomes `\u0024`: a literal escape in Kotlin (blocks string-template interpolation) and a
 *   pre-lexing unicode escape in Java (harmless, yields `$`).
 * - Form feed falls into the generic control-character branch (`\u000c`); `\f` is not a valid
 *   Kotlin escape.
 * - `/` stays unescaped: `\/` is a JSON-only escape and illegal in both Java and Kotlin literals.
 */
internal fun escapeStringLiteral(value: String): String {
    val sb = StringBuilder(value.length + ESCAPE_BUFFER_PADDING)
    for (ch in value) {
        when (ch) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\b' -> sb.append("\\b")
            '$' -> sb.append("\\u0024")
            '\u2028' -> sb.append("\\u2028")
            '\u2029' -> sb.append("\\u2029")
            else -> if (ch < ' ') {
                sb.append("\\u").append(ch.code.toString(HEX_RADIX).padStart(UNICODE_ESCAPE_LENGTH, '0'))
            } else {
                sb.append(ch)
            }
        }
    }
    return sb.toString()
}

internal val escapeString: (Any) -> Any = escapeStringFun@{ s ->
    if (s !is String) return@escapeStringFun s
    escapeStringLiteral(s)
}

