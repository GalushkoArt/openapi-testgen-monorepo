package art.galushko.openapi.testgen.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import org.slf4j.LoggerFactory

/**
 * Utility for parsing OpenAPI specifications.
 *
 * Parsing is delegated to swagger-parser; support for OpenAPI 3.1 is best-effort
 * and depends on the parser version.
 */
internal object OpenApiSpecParser {
    private val log = LoggerFactory.getLogger(OpenApiSpecParser::class.java)

    /**
     * Parses an OpenAPI specification file.
     *
     * @param inputSpec URL/path to the OpenAPI specification file
     * @param parseOptions Parse options to use (default: fully resolve schemas)
     * @return Parsed OpenAPI model
     * @throws IllegalArgumentException if parsing fails or a file is invalid
     */
    public fun parseOpenApi(
        inputSpec: String,
        parseOptions: ParseOptions = ParseOptions().apply {
            isResolveFully = true
            isResolveCombinators = true
            isResolveRequestBody = true
            isResolveResponses = true
        },
    ): OpenAPI {
        val parser = OpenAPIV3Parser()

        val parsed = parser.readLocation(inputSpec, null, parseOptions)

        val messages = parsed.messages
        if (messages != null && messages.isNotEmpty()) {
            log.warn("Errors on parsing OpenAPI spec:\n{}", messages.joinToString("\n"))
        }

        return requireNotNull(parsed.openAPI) { "Parsed OpenAPI model is null" }
    }
}


