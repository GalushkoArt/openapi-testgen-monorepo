package art.galushko.openapi.testgen.example.util

import io.swagger.v3.oas.models.media.Content

public const val APPLICATION_JSON: String = "application/json"
public const val TEXT_JSON: String = "text/json"
public const val APPLICATION_JWT: String = "application/jwt"
public const val APPLICATION_XML: String = "application/xml"
public const val APPLICATION_YAML: String = "application/yaml"
public const val PLAIN_XML: String = "text/xml"
public const val APPLICATION_XWWW_FORM_URLENCODED: String = "application/x-www-form-urlencoded"
public const val APPLICATION_JSON_SUFFIX: String = "+json"
public const val APPLICATION_JWT_SUFFIX: String = "+jwt"
public const val APPLICATION_XML_SUFFIX: String = "+xml"

/**
 * Normalizes a media type by removing parameters (charset, etc.) and converting to lowercase.
 */
public fun normalizeMediaTypeName(mediaType: String): String =
    mediaType.substringBefore(';').trim().lowercase()

/**
 * Provides deterministic ordering of media types example extraction.
 *
 * Media types are ordered by priority to ensure JSON-like formats are preferred:
 * 1. `application/json` (exact match)
 * 2. `text/json` (exact match)
 * 3. `*+json` suffixes (e.g., `application/hal+json`)
 * 4. `application/jwt` (exact match)
 * 5. `*+jwt` suffixes (e.g., `application/secevent+jwt`)
 * 6. `application/xml` (exact match)
 * 7. `text/xml` (exact match)
 * 8. `*+xml` suffixes (e.g., `application/atom+xml`)
 * 9. `application/yaml` (exact match)
 * 10. `application/x-www-form-urlencoded` (exact match)
 * 11. All other media types (alphabetically)
 */
public object MediaTypePrioritizer {
    private const val PRIORITY_JSON_EXACT = 0
    private const val PRIORITY_JSON_TEXT = 1
    private const val PRIORITY_JSON_SUFFIX = 2
    private const val PRIORITY_JWT_EXACT = 3
    private const val PRIORITY_JWT_SUFFIX = 4
    private const val PRIORITY_XML_EXACT = 5
    private const val PRIORITY_XML_TEXT = 6
    private const val PRIORITY_XML_SUFFIX = 7
    private const val PRIORITY_YAML_EXACT = 8
    private const val PRIORITY_XWWW_FORM_URLENCODED = 9
    private const val PRIORITY_OTHER = 10

    /**
     * Returns media type keys ordered by priority for example extraction.
     *
     * @param content the OpenAPI Content object containing media types
     * @return list of media type keys in priority order
     */
    public fun orderedMediaTypeKeys(content: Content): List<String> =
        content.keys.sortedWith(
            compareBy<String> { priority(it) }
                .thenBy { normalizeMediaTypeName(it) }
        )

    public fun isExpectedStructuredSchema(mediaType: String): Boolean = priority(mediaType) != PRIORITY_OTHER

    /**
     * Returns the priority value for a media type.
     * Lower values indicate higher priority.
     */
    private fun priority(mediaType: String): Int {
        val normalized = normalizeMediaTypeName(mediaType)
        return when {
            normalized == APPLICATION_JSON -> PRIORITY_JSON_EXACT
            normalized == TEXT_JSON -> PRIORITY_JSON_TEXT
            normalized.endsWith(APPLICATION_JSON_SUFFIX) -> PRIORITY_JSON_SUFFIX
            normalized == APPLICATION_JWT -> PRIORITY_JWT_EXACT
            normalized.endsWith(APPLICATION_JWT_SUFFIX) -> PRIORITY_JWT_SUFFIX
            normalized == APPLICATION_XML -> PRIORITY_XML_EXACT
            normalized == PLAIN_XML -> PRIORITY_XML_TEXT
            normalized.endsWith(APPLICATION_XML_SUFFIX) -> PRIORITY_XML_SUFFIX
            normalized == APPLICATION_YAML -> PRIORITY_YAML_EXACT
            normalized == APPLICATION_XWWW_FORM_URLENCODED -> PRIORITY_XWWW_FORM_URLENCODED
            else -> PRIORITY_OTHER
        }
    }
}
