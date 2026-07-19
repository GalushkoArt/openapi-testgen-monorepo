package art.galushko.openapi.testgen.generator.template

import art.galushko.openapi.testgen.generator.writer.AtomicFileWriter
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.spi.ArtifactGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.Mustache
import com.github.mustachejava.reflect.ReflectionObjectHandler
import org.slf4j.LoggerFactory
import java.io.File
import java.io.StringReader
import java.io.StringWriter

/**
 * Generic template-based artifact generator that can generate code for different frameworks and languages
 * using Mustache templates. This implementation is framework-agnostic and relies on configurable templates
 * to define the output format and structure.
 *
 * Template variables commonly used by built-in templates:
 * - `classSuffix` for class naming
 * - `methodPrefix`, `methodSuffix` for method naming
 */
internal class TemplateArtifactGenerator(
    private val outputDir: File,
    optionMap: Map<String, Any?>,
) : ArtifactGenerator {

    private val jsonMapper = ObjectMapper()
    private val log = LoggerFactory.getLogger(TemplateArtifactGenerator::class.java)
    private val templateCache = mutableMapOf<String, Mustache>()
    private val options: TemplateArtifactGeneratorOptions = transformAndValidateTemplateOptions(optionMap)

    /**
     * Generates artifacts for the provided TestSuite using the configured templates.
     * The output format and structure are determined by the template set and framework configuration.
     *
     * @param testSuite suite to generate artifacts for
     */
    override fun generateTests(testSuite: TestSuite) {
        log.info("Generating artifacts for {} test cases using template set '{}'",
            testSuite.testCases.size, options.templateSet)

        val className = generateClassName(testSuite)
        val classContext = createClassTemplateContext(testSuite, className)

        val classTemplate = loadTemplate(options.resolveClassTemplatePath())

        val classWriter = StringWriter()
        classTemplate.execute(classWriter, classContext).close()

        val generatedClass = classWriter.toString()
        val outputFileName = options.resolveOutputFileName(className)
        val outputFile = File(outputDir, outputFileName)

        // Handle write mode
        if (options.writeMode == WriteMode.SKIP_IF_EXISTS && outputFile.exists()) {
            log.info("Skipping generation of {} as file already exists", outputFile.absolutePath)
            return
        }

        AtomicFileWriter.write(outputFile) { tmpFile -> tmpFile.writeText(generatedClass) }
        log.info("Generated {} artifact: {}", options.templateSet, className)
    }

    private fun generateClassName(testSuite: TestSuite): String {
        val baseName = testSuite.operationName ?: extractNameFromPath(testSuite.path)
        return camelCaseName(baseName, true) + getTemplateDefinedSuffix()
    }

    private fun extractNameFromPath(path: String): String {
        return path.trim('/')
            .split('/')
            .filter { it.isNotBlank() && !it.contains('{') } // Remove path parameters
            .joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
            .ifEmpty { "ApiTest" }
    }

    private fun getTemplateDefinedSuffix(): String {
        return options.templateVariables["classSuffix"] as? String ?: "Test"
    }

    private fun createClassTemplateContext(testSuite: TestSuite, className: String): GenericClassTemplateContext {
        val methodContexts = testSuite.testCases.map { testCase ->
            createMethodTemplateContext(testCase)
        }

        return GenericClassTemplateContext(
            className = className,
            operationName = testSuite.operationName ?: "UnknownOperation",
            operationPath = testSuite.path,
            methods = methodContexts,
            customVariables = options.templateVariables,
            fileHeaderComment = options.fileHeaderComment,
        )
    }

    private fun createMethodTemplateContext(testCase: TestCase): GenericMethodTemplateContext {
        val methodName = generateMethodName(testCase)
        val defaultJsonMethods = setOf("POST", "PUT", "PATCH")
        val requestBodyMediaType = testCase.requestBodyMediaType ?: if (testCase.method.uppercase() in defaultJsonMethods) {
            "application/json"
        } else {
            null
        }
        val requestRendering = testCase.body?.let {
            createRequestBodyRendering(it, requestBodyMediaType, jsonMapper)
        }
        val responseAssertion = createResponseAssertionPlan(
            body = testCase.expectedBody,
            mediaType = testCase.responseBodyMediaType,
            jsonMapper = jsonMapper,
        )

        val queryParams = testCase.queryParams.flatMap { param ->
            if (param.value is List<*>) (param.value as List<*>).map {
                GenericParamContext(
                    param.key,
                    serializeParameterValue(it ?: "", jsonMapper)
                )
            } else listOf(GenericParamContext(param.key, serializeParameterValue(param.value, jsonMapper)))
        }
        val notes = buildList {
            if (testCase.needToComplete || requestRendering?.manualComment != null || responseAssertion.manualComment != null) {
                add("TODO: Review this generated case before relying on it as a fully automated test.")
            }
        }

        return GenericMethodTemplateContext(
            methodName = methodName,
            testCaseName = testCase.name,
            description = testCase.name,
            httpMethod = testCase.method.lowercase(),
            path = testCase.path,
            expectedStatusCode = testCase.expectedStatusCode,
            headers = testCase.headers.map { GenericParamContext(it.key, serializeParameterValue(it.value, jsonMapper)) },
            pathParams = testCase.pathParams.map { (key, value) -> GenericParamContext(key, serializeParameterValue(value, jsonMapper)) },
            queryParams = queryParams,
            cookies = testCase.cookie.map { GenericParamContext(it.key, serializeParameterValue(it.value, jsonMapper)) },
            requestBody = requestRendering?.body,
            requestBodyMediaType = requestBodyMediaType,
            expectedResponseBody = responseAssertion.body,
            responseBodyMediaType = testCase.responseBodyMediaType,
            assertJsonResponseBody = responseAssertion.assertJson,
            requestBodyTodoComment = requestRendering?.manualComment,
            responseAssertionTodoComment = responseAssertion.manualComment,
            needToComplete = testCase.needToComplete,
            notes = notes,
            customVariables = options.templateVariables,
        )
    }

    private fun generateMethodName(testCase: TestCase): String {
        val baseName = camelCaseName(testCase.name, false)
        // Let templates define method naming through templateVariables
        val methodPrefix = options.templateVariables["methodPrefix"] as? String ?: ""
        val methodSuffix = options.templateVariables["methodSuffix"] as? String ?: ""
        return methodPrefix + baseName + methodSuffix
    }

    private fun loadTemplate(templatePath: String): Mustache {
        return templateCache.getOrPut(templatePath) {
            // Try to load from a custom directory first
            val customTemplateFile = options.customTemplateDir?.let { customDir ->
                File(customDir, templatePath).takeIf { it.exists() }
                    ?: throw IllegalStateException("Custom template not found: $customDir/$templatePath")
            }
            try {
                if (customTemplateFile != null) {
                    log.debug("Loading custom template from: {}", customTemplateFile.absolutePath)
                    val mf = DefaultMustacheFactory(File(options.customTemplateDir)).apply {
                        objectHandler = ReflectionObjectHandler()
                    }
                    mf.compile(templatePath)
                } else {
                    // Load from the classpath
                    val resourceStream = requireNotNull(this::class.java.classLoader.getResourceAsStream(templatePath)) {
                        "Template not found: $templatePath"
                    }

                    val templateContent = resourceStream.bufferedReader().use { it.readText() }
                    DefaultMustacheFactory().compile(StringReader(templateContent), templatePath)
                }
            } catch (e: Exception) {
                throw IllegalStateException("Failed to load template: $templatePath", e)
            }
        }
    }
}

@Suppress("ComplexCondition")
private fun camelCaseName(input: String?, capitalizeFirst: Boolean): String {
    if (input.isNullOrEmpty()) return "test"
    val result = StringBuilder()
    var capitalizeNext = capitalizeFirst

    for (c in input.toCharArray()) {
        if (!c.isLetterOrDigit()) {
            capitalizeNext = true
        } else if (capitalizeNext) {
            result.append(c.uppercaseChar())
            capitalizeNext = false
        } else {
            result.append(c)
        }
    }

    if (!capitalizeFirst && result.isNotEmpty()) {
        result.setCharAt(0, result[0].lowercaseChar())
    }

    return result.toString()
}

private data class RequestBodyRendering(
    val body: GenericBodyContext,
    val manualComment: String? = null,
)

private data class ResponseAssertionPlan(
    val body: GenericBodyContext?,
    val assertJson: Boolean,
    val manualComment: String? = null,
)

private fun createRequestBodyRendering(
    body: Any,
    mediaType: String?,
    jsonMapper: ObjectMapper,
): RequestBodyRendering {
    if (isJsonLikeMediaType(mediaType)) {
        return RequestBodyRendering(
            body = GenericBodyContext(rawBody = jsonMapper.writeValueAsString(body), body = body),
        )
    }

    return when (body) {
        is String, is Number, is Boolean, is Char -> RequestBodyRendering(
            body = GenericBodyContext(rawBody = body.toString(), body = body),
        )

        else -> RequestBodyRendering(
            body = GenericBodyContext(rawBody = "", body = body),
            manualComment = "TODO: Manual request body serialization required for media type '${mediaType ?: "unknown"}'. " +
                "Replace the placeholder body before using this test.",
        )
    }
}

private fun createResponseAssertionPlan(
    body: Any?,
    mediaType: String?,
    jsonMapper: ObjectMapper,
): ResponseAssertionPlan {
    if (body == null) {
        return ResponseAssertionPlan(body = null, assertJson = false)
    }

    if (isJsonLikeMediaType(mediaType)) {
        return ResponseAssertionPlan(
            body = GenericBodyContext(rawBody = jsonMapper.writeValueAsString(body), body = body),
            assertJson = true,
        )
    }

    val previewBody = when (body) {
        is String, is Number, is Boolean, is Char -> body.toString()
        else -> jsonMapper.writeValueAsString(body)
    }

    return ResponseAssertionPlan(
        body = GenericBodyContext(rawBody = previewBody, body = body),
        assertJson = false,
        manualComment = "TODO: Manual response assertion required for media type '${mediaType ?: "unknown"}'. Expected body preview is provided below.",
    )
}

private fun serializeParameterValue(value: Any, jsonMapper: ObjectMapper): String {
    return when (value) {
        is String -> value
        is Number, is Boolean, is Char -> value.toString()
        else -> jsonMapper.writeValueAsString(value)
    }
}

private fun isJsonLikeMediaType(mediaType: String?): Boolean {
    val normalized = mediaType
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase()

    return normalized == null ||
        normalized == "application/json" ||
        normalized == "text/json" ||
        normalized.endsWith("+json")
}
