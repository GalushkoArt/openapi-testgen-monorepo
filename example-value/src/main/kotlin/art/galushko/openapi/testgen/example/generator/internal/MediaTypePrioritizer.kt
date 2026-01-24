package art.galushko.openapi.testgen.example.generator.internal

import io.swagger.v3.oas.models.media.Content

/**
 * Provides deterministic ordering of media types for response example extraction.
 *
 * Media types are ordered by priority to ensure JSON-like formats are preferred:
 * 1. `application/json` (exact match)
 * 2. `*+json` suffixes (e.g., `application/hal+json`)
 * 3. `application/xml` (exact match)
 * 4. `*+xml` suffixes (e.g., `application/atom+xml`)
 * 5. All other media types (alphabetically)
 */
internal object MediaTypePrioritizer {
    private const val PRIORITY_JSON_EXACT = 0
    private const val PRIORITY_JSON_SUFFIX = 1
    private const val PRIORITY_XML_EXACT = 2
    private const val PRIORITY_XML_SUFFIX = 3
    private const val PRIORITY_OTHER = 4

    /**
     * Returns media type keys ordered by priority for example extraction.
     *
     * @param content the OpenAPI Content object containing media types
     * @return list of media type keys in priority order
     */
    fun orderedMediaTypeKeys(content: Content): List<String> =
        content.keys.sortedWith(
            compareBy<String> { priority(it) }
                .thenBy { normalize(it) }
                .thenBy { it.lowercase() }
        )

    fun isJsonLike(mediaType: String): Boolean {
        val normalized = normalize(mediaType)
        return normalized == "application/json" || normalized.endsWith("+json")
    }

    /**
     * Returns the priority value for a media type.
     * Lower values indicate higher priority.
     */
    private fun priority(mediaType: String): Int {
        val normalized = normalize(mediaType)
        return when {
            normalized == "application/json" -> PRIORITY_JSON_EXACT
            normalized.endsWith("+json") -> PRIORITY_JSON_SUFFIX
            normalized == "application/xml" -> PRIORITY_XML_EXACT
            normalized.endsWith("+xml") -> PRIORITY_XML_SUFFIX
            else -> PRIORITY_OTHER
        }
    }

    /**
     * Normalizes a media type by removing parameters (charset, etc.) and converting to lowercase.
     */
    private fun normalize(mediaType: String): String =
        mediaType.substringBefore(';').trim().lowercase()
}
