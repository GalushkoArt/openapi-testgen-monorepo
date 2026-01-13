package art.galushko.openapi.testgen.generator.template

/**
 * Options to configure the template-based artifact generation behavior.
 * This is a generic configuration that can be used with different frameworks and languages
 * through customizable Mustache templates.
 *
 * Placeholders supported in patterns:
 * - `{{templateSet}}` in template paths
 * - `{{className}}` and `{{outputFileExtension}}` in output file names
 */
internal data class TemplateArtifactGeneratorOptions(
    // Template configuration
    val templateSet: String = "restassured-java", // Template set identifier (e.g., "restassured-java", "restassured-kotlin")
    val classTemplatePath: String = "templates/{{templateSet}}/class.mustache",
    val customTemplateDir: String? = null, // Optional filesystem directory for custom templates

    // Template variables - generic key-value pairs that can be used in templates
    val templateVariables: Map<String, Any?> = emptyMap(),

    // Output configuration
    val outputFileExtension: String = "java", // File extension for generated files
    val outputFileNamePattern: String = "{{className}}.{{outputFileExtension}}", // Pattern for output file names
    val writeMode: WriteMode = WriteMode.OVERWRITE,

    // Content customization
    val fileHeaderComment: String? = null,
) {
    init {
        require(templateSet.isNotBlank()) { "templateSet cannot be blank" }
        require(outputFileExtension.isNotBlank()) { "outputFileExtension cannot be blank" }
        require(outputFileNamePattern.isNotBlank()) { "outputFileNamePattern cannot be blank" }

        // Validate template paths contain template set placeholder or are absolute
        require(classTemplatePath.isNotBlank()) { "classTemplatePath cannot be blank" }
    }

    // Helper methods for template processing
    fun resolveClassTemplatePath(): String {
        return if (classTemplatePath.contains("{{templateSet}}")) {
            classTemplatePath.replace("{{templateSet}}", templateSet)
        } else {
            classTemplatePath
        }
    }

    fun resolveOutputFileName(className: String): String {
        return outputFileNamePattern
            .replace("{{className}}", className)
            .replace("{{outputFileExtension}}", outputFileExtension)
    }
}

internal enum class WriteMode {
    OVERWRITE,
    SKIP_IF_EXISTS,
}

@Suppress("CyclomaticComplexMethod")
internal fun transformAndValidateTemplateOptions(map: Map<String, Any?>): TemplateArtifactGeneratorOptions {
    val templateSet = map["templateSet"] as? String ?: map["classTemplatePath"]?.let { "custom" } ?: "restassured-java"
    val customTemplateDir = map["customTemplateDir"] as? String

    val classTemplatePath = map["classTemplatePath"] as? String ?: "templates/{{templateSet}}/class.mustache"

    val templateVariables = when (val tv = map["templateVariables"]) {
        is Map<*, *> -> tv.entries.associate { (k, v) -> k.toString() to v }
        null -> emptyMap()
        else -> throw IllegalArgumentException("Invalid 'templateVariables' option: '$tv'. Expected map.")
    }

    val outputFileExtension = map["outputFileExtension"] as? String ?: when {
        templateSet.contains("kotlin", true) -> "kt"
        templateSet.contains("java", true) -> "java"
        else -> throw IllegalArgumentException("Cannot identify file extension. Please specify 'outputFileExtension' option in generatorOptions")
    }
    val outputFileNamePattern = map["outputFileNamePattern"] as? String ?: "{{className}}.{{outputFileExtension}}"

    val writeMode = try {
        (map["writeMode"] as? String)?.let { WriteMode.valueOf(it) } ?: WriteMode.OVERWRITE
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException(
            "Invalid 'writeMode' option: '${map["writeMode"]}'. Supported values: " +
                WriteMode.entries.joinToString { it.name },
            e
        )
    }

    val fileHeaderComment = map["fileHeaderComment"] as? String

    val options = TemplateArtifactGeneratorOptions(
        templateSet = templateSet,
        classTemplatePath = classTemplatePath,
        customTemplateDir = customTemplateDir,
        templateVariables = templateVariables,
        outputFileExtension = outputFileExtension,
        outputFileNamePattern = outputFileNamePattern,
        writeMode = writeMode,
        fileHeaderComment = fileHeaderComment,
    )

    return options
}
