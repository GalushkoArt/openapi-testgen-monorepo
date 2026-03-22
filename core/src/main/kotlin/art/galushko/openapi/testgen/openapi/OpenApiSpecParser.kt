package art.galushko.openapi.testgen.openapi

import art.galushko.openapi.testgen.config.ParserSettings
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.parser.util.DeserializationUtils
import org.slf4j.LoggerFactory

/**
 * Utility for parsing OpenAPI specifications.
 *
 * Parsing is delegated to swagger-parser; support for OpenAPI 3.1 is best-effort
 * and depends on the parser version.
 */
internal object OpenApiSpecParser {
    private val log = LoggerFactory.getLogger(OpenApiSpecParser::class.java)
    private val parserOptionsLock = Any()

    /**
     * Parses an OpenAPI specification file.
     *
     * @param inputSpec URL/path to the OpenAPI specification file
     * @param parserSettings Parser settings to apply for this parse call
     * @param parseOptions Parse options to use (default: fully resolve schemas)
     * @return Parsed OpenAPI model
     *
     * Thread safety: parser settings are applied and parsing is executed under a lock because
     * swagger-parser uses global static options for SnakeYAML.
     * @throws IllegalArgumentException if parsing fails or a file is invalid
     */
    public fun parseOpenApi(
        inputSpec: String,
        parserSettings: ParserSettings = ParserSettings(),
        parseOptions: ParseOptions = ParseOptions().apply {
            isResolveFully = true
            // Keep combinators intact and let SchemaMerger handle them; swagger-parser combinator resolution is lossy and flattens composed schemas.
            isResolveCombinators = false
            isResolveRequestBody = true
            isResolveResponses = true
        },
    ): OpenAPI {
        return withConfiguredParserOptions(parserSettings) {
            parseOpenApiUnsafe(inputSpec, parseOptions)
        }
    }

    internal fun <T> withConfiguredParserOptions(
        parserSettings: ParserSettings,
        action: () -> T,
    ): T {
        synchronized(parserOptionsLock) {
            val options = DeserializationUtils.getOptions()
            val snapshot = ParserOptionsSnapshot.from(options)

            try {
                configureParserOptions(options, parserSettings)
                return action()
            } finally {
                restoreParserOptions(options, snapshot)
            }
        }
    }

    private fun parseOpenApiUnsafe(
        inputSpec: String,
        parseOptions: ParseOptions,
    ): OpenAPI {
        val parser = OpenAPIV3Parser()
        val parsed = parser.readLocation(inputSpec, null, parseOptions)

        val messages = parsed.messages
        if (messages != null && messages.isNotEmpty()) {
            log.warn("Errors on parsing OpenAPI spec:\n{}", messages.joinToString("\n"))
        }

        return requireNotNull(parsed.openAPI) { "Parsed OpenAPI model is null" }
    }

    private fun configureParserOptions(
        options: DeserializationUtils.Options,
        settings: ParserSettings,
    ) {
        val defaultOptions = DeserializationUtils.Options()

        val codePointLimit = settings.yamlCodePointLimit ?: defaultOptions.maxYamlCodePoints
        val maxAliases = settings.yamlMaxAliasesForCollections ?: defaultOptions.maxYamlAliasesForCollections
        val allowRecursiveKeys = settings.yamlAllowRecursiveKeys ?: defaultOptions.isYamlAllowRecursiveKeys
        val nestingDepthLimit = settings.yamlNestingDepthLimit ?: defaultOptions.maxYamlDepth

        options.maxYamlCodePoints = codePointLimit
        options.maxYamlAliasesForCollections = maxAliases
        options.isYamlAllowRecursiveKeys = allowRecursiveKeys
        options.maxYamlDepth = nestingDepthLimit

        log.debug(
            "Configured parser options: maxYamlCodePoints={}, maxYamlAliasesForCollections={}, yamlAllowRecursiveKeys={}, maxYamlDepth={}",
            codePointLimit,
            maxAliases,
            allowRecursiveKeys,
            nestingDepthLimit,
        )
    }

    private fun restoreParserOptions(
        options: DeserializationUtils.Options,
        snapshot: ParserOptionsSnapshot,
    ) {
        options.maxYamlDepth = snapshot.maxYamlDepth
        options.maxYamlReferences = snapshot.maxYamlReferences
        options.isValidateYamlInput = snapshot.validateYamlInput
        options.isYamlCycleCheck = snapshot.yamlCycleCheck
        options.maxYamlCodePoints = snapshot.maxYamlCodePoints
        options.maxYamlAliasesForCollections = snapshot.maxYamlAliasesForCollections
        options.isYamlAllowRecursiveKeys = snapshot.yamlAllowRecursiveKeys
    }

    private data class ParserOptionsSnapshot(
        val maxYamlDepth: Int?,
        val maxYamlReferences: Long?,
        val validateYamlInput: Boolean,
        val yamlCycleCheck: Boolean,
        val maxYamlCodePoints: Int?,
        val maxYamlAliasesForCollections: Int?,
        val yamlAllowRecursiveKeys: Boolean,
    ) {
        companion object {
            fun from(options: DeserializationUtils.Options): ParserOptionsSnapshot =
                ParserOptionsSnapshot(
                    maxYamlDepth = options.maxYamlDepth,
                    maxYamlReferences = options.maxYamlReferences,
                    validateYamlInput = options.isValidateYamlInput,
                    yamlCycleCheck = options.isYamlCycleCheck,
                    maxYamlCodePoints = options.maxYamlCodePoints,
                    maxYamlAliasesForCollections = options.maxYamlAliasesForCollections,
                    yamlAllowRecursiveKeys = options.isYamlAllowRecursiveKeys,
                )
        }
    }
}

