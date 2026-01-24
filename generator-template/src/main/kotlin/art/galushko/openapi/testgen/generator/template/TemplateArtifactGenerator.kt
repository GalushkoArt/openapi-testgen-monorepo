package art.galushko.openapi.testgen.generator.template

import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.spi.ArtifactGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.Mustache
import com.github.mustachejava.reflect.ReflectionObjectHandler
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
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

    private val mapper = ObjectMapper()
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

        try {
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(generatedClass)
            log.info("Generated {} artifact: {}", options.templateSet, className)
        } catch (e: IOException) {
            log.error("Failed to write artifact: {}.{}", options.templateSet, className, e)
        }
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

        val queryParams = testCase.queryParams.flatMap { param ->
            if (param.value is List<*>) (param.value as List<*>).map {
                GenericParamContext(
                    param.key,
                    it.toString()
                )
            } else listOf(GenericParamContext(param.key, param.value.toString()))
        }

        return GenericMethodTemplateContext(
            methodName = methodName,
            testCaseName = testCase.name,
            description = testCase.name,
            httpMethod = testCase.method.lowercase(),
            path = testCase.path,
            expectedStatusCode = testCase.expectedStatusCode,
            headers = testCase.headers.map { GenericParamContext(it.key, it.value.toString()) },
            pathParams = testCase.pathParams.map { (key, value) -> GenericParamContext(key, value.toString()) },
            queryParams = queryParams,
            cookies = testCase.cookie.map { GenericParamContext(it.key, it.value.toString()) },
            requestBody = testCase.body?.let { createGenericBodyContext(it) },
            expectedResponseBody = testCase.expectedBody?.let { createGenericBodyContext(it) },
            needToComplete = testCase.needToComplete,
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

    private fun createGenericBodyContext(body: Any): GenericBodyContext {
        val jsonBody = try {
            mapper.writeValueAsString(body)
        } catch (e: Exception) {
            log.warn("Failed to serialize request body to JSON", e)
            "{}"
        }

        return GenericBodyContext(
            rawBody = jsonBody,
            body = body
        )
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
