package art.galushko.openapi.testgen.openapi

import art.galushko.openapi.testgen.config.ParserSettings
import com.fasterxml.jackson.databind.JsonNode
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.parameters.CookieParameter
import io.swagger.v3.oas.models.parameters.HeaderParameter
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.PathParameter
import io.swagger.v3.oas.models.parameters.QueryParameter
import io.swagger.v3.parser.converter.SwaggerConverter
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.parser.core.models.SwaggerParseResult
import io.swagger.v3.parser.util.DeserializationUtils
import org.slf4j.LoggerFactory
import io.swagger.parser.util.DeserializationUtils as V1DeserializationUtils
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

/**
 * Utility for parsing OpenAPI specifications.
 *
 * Parsing is delegated to swagger-parser; support for OpenAPI 3.1 is best-effort
 * and depends on the parser version.
 */
@Suppress("TooManyFunctions")
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
            val v1Options = V1DeserializationUtils.getOptions()
            val snapshot = ParserOptionsSnapshot.from(options)
            val v1Snapshot = V1ParserOptionsSnapshot.from(v1Options)

            try {
                configureParserOptions(options, parserSettings)
                configureV1ParserOptions(v1Options, parserSettings)
                return action()
            } finally {
                restoreParserOptions(options, snapshot)
                restoreV1ParserOptions(v1Options, v1Snapshot)
            }
        }
    }

    private fun parseOpenApiUnsafe(
        inputSpec: String,
        parseOptions: ParseOptions,
    ): OpenAPI {
        val detectedVersion = detectSpecVersion(inputSpec)
        require(detectedVersion.family != SpecVersionFamily.UNSUPPORTED_SWAGGER) {
            "Unsupported Swagger version '${detectedVersion.value}' in $inputSpec. Only Swagger 2.0 is supported."
        }

        val parsed = when (detectedVersion.family) {
            SpecVersionFamily.SWAGGER_2 -> SwaggerConverter().readLocation(inputSpec, null, parseOptions)
            else -> OpenAPIV3Parser().readLocation(inputSpec, null, parseOptions)
        }

        val messages = parsed.messages
        if (messages != null && messages.isNotEmpty()) {
            log.warn("Errors on parsing {} spec:\n{}", detectedVersion.displayName, messages.joinToString("\n"))
        }

        val openAPI = parsed.openAPI ?: throw parseFailure(inputSpec, detectedVersion, parsed)
        if (detectedVersion.family == SpecVersionFamily.SWAGGER_2) {
            normalizeSwagger2Model(openAPI)
        }
        return openAPI
    }

    private fun parseFailure(
        inputSpec: String,
        detectedVersion: DetectedSpecVersion,
        parsed: SwaggerParseResult,
    ): IllegalArgumentException {
        val messages = parsed.messages.orEmpty()
        val messageSuffix = if (messages.isEmpty()) {
            "Parser returned no diagnostic messages."
        } else {
            "Parser messages: ${messages.joinToString("; ")}"
        }
        return IllegalArgumentException(
            "Parsed ${detectedVersion.displayName} model is null for $inputSpec. $messageSuffix"
        )
    }

    private fun normalizeSwagger2Model(openAPI: OpenAPI) {
        if (openAPI.openapi.isNullOrBlank()) {
            openAPI.openapi = "3.0.3"
        }
        ensureComponents(openAPI)
        normalizeSwagger2Parameters(openAPI)
        mergePathLevelParametersIntoOperations(openAPI)
    }

    private fun ensureComponents(openAPI: OpenAPI): Components {
        val components = openAPI.components ?: Components().also { openAPI.components = it }
        if (components.schemas == null) components.schemas = linkedMapOf()
        if (components.parameters == null) components.parameters = linkedMapOf()
        if (components.requestBodies == null) components.requestBodies = linkedMapOf()
        if (components.responses == null) components.responses = linkedMapOf()
        if (components.examples == null) components.examples = linkedMapOf()
        if (components.securitySchemes == null) components.securitySchemes = linkedMapOf()
        return components
    }

    private fun normalizeSwagger2Parameters(openAPI: OpenAPI) {
        val components = openAPI.components
        components?.parameters = components.parameters?.mapValues { (_, parameter) ->
            normalizeParameterSubtype(parameter)
        }?.toMutableMap()

        openAPI.paths.orEmpty().values.forEach { pathItem ->
            pathItem.parameters = pathItem.parameters?.map(::normalizeParameterSubtype)
            pathItem.readOperationsMap().values.forEach { operation ->
                operation.parameters = operation.parameters?.map(::normalizeParameterSubtype)
            }
        }
    }

    private fun normalizeParameterSubtype(parameter: Parameter): Parameter {
        if (parameter.`$ref` != null) return parameter
        val target = when (parameter.`in`) {
            "query" -> QueryParameter()
            "path" -> PathParameter()
            "header" -> HeaderParameter()
            "cookie" -> CookieParameter()
            else -> return parameter
        }
        return copyParameter(parameter, target)
    }

    private fun copyParameter(source: Parameter, target: Parameter): Parameter {
        target.name = source.name
        target.`in` = source.`in`
        target.description = source.description
        target.required = source.required
        target.deprecated = source.deprecated
        target.allowEmptyValue = source.allowEmptyValue
        target.style = source.style
        target.explode = source.explode
        target.allowReserved = source.allowReserved
        target.schema = source.schema
        target.examples = source.examples
        target.example = source.example
        target.content = source.content
        target.`$ref` = source.`$ref`
        target.extensions = source.extensions
        return target
    }

    private fun mergePathLevelParametersIntoOperations(openAPI: OpenAPI) {
        openAPI.paths.orEmpty().values.forEach { pathItem ->
            val pathParameters = pathItem.parameters.orEmpty()
            if (pathParameters.isEmpty()) return@forEach

            pathItem.readOperationsMap().values.forEach { operation ->
                mergePathLevelParameters(pathParameters, operation, openAPI)
            }
        }
    }

    private fun mergePathLevelParameters(
        pathParameters: List<Parameter>,
        operation: Operation,
        openAPI: OpenAPI,
    ) {
        val operationParameters = operation.parameters.orEmpty()
        val operationKeys = operationParameters.map { parameterIdentity(it, openAPI) }.toSet()
        val inheritedParameters = pathParameters.filter { parameterIdentity(it, openAPI) !in operationKeys }
        if (inheritedParameters.isEmpty()) return

        operation.parameters = inheritedParameters + operationParameters
    }

    private fun parameterIdentity(parameter: Parameter, openAPI: OpenAPI): String {
        val resolved = parameter.`$ref`?.let { ref ->
            extractComponentRefKey(ref, "#/components/parameters/")?.let { key ->
                openAPI.components?.parameters?.get(key)
            }
        } ?: parameter

        val location = resolved.`in`
        val name = resolved.name
        return if (location != null && name != null) {
            "$location:$name"
        } else {
            parameter.`$ref` ?: "${parameter.`in`}:${parameter.name}"
        }
    }

    /**
     * Sniffs the version fields via swagger-parser's own deserializer so the configured SnakeYAML
     * limits ([ParserSettings]) apply to sniffing as well: a large spec that the user raised the
     * limits for must not fall back to [SpecVersionFamily.UNKNOWN] and get misrouted.
     * Callers run inside [withConfiguredParserOptions].
     */
    private fun detectSpecVersion(inputSpec: String): DetectedSpecVersion {
        val content = openSpecInput(inputSpec)?.use { stream ->
            runCatching { stream.readBytes().toString(Charsets.UTF_8) }.getOrNull()
        } ?: return DetectedSpecVersion(SpecVersionFamily.UNKNOWN, null)
        val root = runCatching { DeserializationUtils.deserializeIntoTree(content, inputSpec) }
            .onFailure { log.debug("Unable to sniff OpenAPI spec version for {}", inputSpec, it) }
            .getOrNull() ?: return DetectedSpecVersion(SpecVersionFamily.UNKNOWN, null)

        val swagger = root.textOrNull("swagger")
        if (swagger != null) {
            return if (swagger == "2.0") {
                DetectedSpecVersion(SpecVersionFamily.SWAGGER_2, swagger)
            } else {
                DetectedSpecVersion(SpecVersionFamily.UNSUPPORTED_SWAGGER, swagger)
            }
        }

        val openapi = root.textOrNull("openapi")
        if (openapi != null) {
            return DetectedSpecVersion(SpecVersionFamily.OPENAPI_3, openapi)
        }

        return DetectedSpecVersion(SpecVersionFamily.UNKNOWN, null)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun openSpecInput(inputSpec: String): InputStream? {
        return try {
            val uri = runCatching { URI(inputSpec) }.getOrNull()
            when {
                uri?.scheme == "file" -> Files.newInputStream(Path.of(uri))
                uri?.scheme == "http" || uri?.scheme == "https" -> uri.toURL().openStream()
                uri?.scheme != null && uri.scheme.length > 1 -> null
                else -> {
                    val path = Path.of(inputSpec)
                    if (Files.exists(path)) {
                        Files.newInputStream(path)
                    } else {
                        Thread.currentThread().contextClassLoader?.getResourceAsStream(inputSpec)
                            ?: OpenApiSpecParser::class.java.classLoader.getResourceAsStream(inputSpec)
                    }
                }
            }
        } catch (e: Exception) {
            log.debug("Unable to inspect OpenAPI spec version for {}", inputSpec, e)
            null
        }
    }

    /** Accepts numeric nodes as well: YAML resolves an unquoted `swagger: 2.0` to a double. */
    private fun JsonNode.textOrNull(fieldName: String): String? =
        get(fieldName)?.takeIf { it.isTextual || it.isNumber }?.asText()

    private fun extractComponentRefKey(ref: String, prefix: String): String? {
        if (!ref.startsWith(prefix)) return null
        return ref.removePrefix(prefix)
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

    /**
     * Mirrors [configureParserOptions] for the v1 parser statics used by the Swagger 2.0
     * conversion path (`SwaggerConverter` reads YAML through the v1
     * `io.swagger.parser.util.DeserializationUtils`, which has its own global options).
     */
    private fun configureV1ParserOptions(
        options: V1DeserializationUtils.Options,
        settings: ParserSettings,
    ) {
        val defaultOptions = V1DeserializationUtils.Options()

        options.maxYamlCodePoints = settings.yamlCodePointLimit ?: defaultOptions.maxYamlCodePoints
        options.maxYamlAliasesForCollections =
            settings.yamlMaxAliasesForCollections ?: defaultOptions.maxYamlAliasesForCollections
        options.isYamlAllowRecursiveKeys = settings.yamlAllowRecursiveKeys ?: defaultOptions.isYamlAllowRecursiveKeys
        options.maxYamlDepth = settings.yamlNestingDepthLimit ?: defaultOptions.maxYamlDepth
    }

    private fun restoreV1ParserOptions(
        options: V1DeserializationUtils.Options,
        snapshot: V1ParserOptionsSnapshot,
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

    private data class V1ParserOptionsSnapshot(
        val maxYamlDepth: Int?,
        val maxYamlReferences: Long?,
        val validateYamlInput: Boolean,
        val yamlCycleCheck: Boolean,
        val maxYamlCodePoints: Int?,
        val maxYamlAliasesForCollections: Int?,
        val yamlAllowRecursiveKeys: Boolean,
    ) {
        companion object {
            fun from(options: V1DeserializationUtils.Options): V1ParserOptionsSnapshot =
                V1ParserOptionsSnapshot(
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

    private data class DetectedSpecVersion(
        val family: SpecVersionFamily,
        val value: String?,
    ) {
        val displayName: String
            get() = when (family) {
                SpecVersionFamily.SWAGGER_2 -> "Swagger 2.0"
                SpecVersionFamily.OPENAPI_3 -> "OpenAPI ${value.orEmpty()}".trim()
                SpecVersionFamily.UNSUPPORTED_SWAGGER -> "Swagger ${value.orEmpty()}".trim()
                SpecVersionFamily.UNKNOWN -> "unknown OpenAPI/Swagger version"
            }
    }

    private enum class SpecVersionFamily {
        SWAGGER_2,
        OPENAPI_3,
        UNSUPPORTED_SWAGGER,
        UNKNOWN,
    }
}

