package art.galushko.openapi.testgen.generator.template

import kotlin.text.iterator

private const val ESCAPE_BUFFER_PADDING = 16
private const val UNICODE_ESCAPE_LENGTH = 4
private const val HEX_RADIX = 16

internal val escapeString: (Object) -> Object = escapeStringFun@{ s ->
    if (s !is String) return@escapeStringFun s
    val sb = StringBuilder(s.length + ESCAPE_BUFFER_PADDING)
    for (ch in s) {
        when (ch) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\b' -> sb.append("\\b")
            '\u000C' -> sb.append("\\f")
            '\u2028' -> sb.append("\\u2028")
            '\u2029' -> sb.append("\\u2029")
            '/' -> sb.append("\\/")
            else -> if (ch < ' ') {
                sb.append("\\u").append(ch.code.toString(HEX_RADIX).padStart(UNICODE_ESCAPE_LENGTH, '0'))
            } else sb.append(ch)
        }
    }
    sb.toString() as Object
}

