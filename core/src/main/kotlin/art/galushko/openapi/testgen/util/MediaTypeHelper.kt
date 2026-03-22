package art.galushko.openapi.testgen.util

import art.galushko.openapi.testgen.example.util.APPLICATION_JSON
import art.galushko.openapi.testgen.example.util.APPLICATION_JSON_SUFFIX
import art.galushko.openapi.testgen.example.util.APPLICATION_JWT
import art.galushko.openapi.testgen.example.util.APPLICATION_JWT_SUFFIX
import art.galushko.openapi.testgen.example.util.APPLICATION_XML
import art.galushko.openapi.testgen.example.util.APPLICATION_XML_SUFFIX
import art.galushko.openapi.testgen.example.util.APPLICATION_XWWW_FORM_URLENCODED
import art.galushko.openapi.testgen.example.util.APPLICATION_YAML
import art.galushko.openapi.testgen.example.util.PLAIN_XML
import art.galushko.openapi.testgen.example.util.TEXT_JSON
import art.galushko.openapi.testgen.example.util.normalizeMediaTypeName

private val SUPPORTED_EXACT_TYPES = setOf(
    APPLICATION_JSON,
    TEXT_JSON,
    APPLICATION_JWT,
    APPLICATION_XML,
    APPLICATION_YAML,
    PLAIN_XML,
    APPLICATION_XWWW_FORM_URLENCODED,
)

private val SUPPORTED_SUFFIXES = setOf(
    APPLICATION_JSON_SUFFIX,
    APPLICATION_JWT_SUFFIX,
    APPLICATION_XML_SUFFIX
)

public fun isMediaTypeSupported(mediaType: String): Boolean {
    val normalized = normalizeMediaTypeName(mediaType)

    if (normalized in SUPPORTED_EXACT_TYPES) return true

    return SUPPORTED_SUFFIXES.any { normalized.endsWith(it) }
}
