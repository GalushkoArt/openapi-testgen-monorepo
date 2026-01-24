package art.galushko.openapi.testgen.generator.writer

/**
 * Options for the TestSuite JSON/YAML generator.
 */
internal data class TestSuiteWriterOptions(
    val outputFileName: String,
    val writeMode: WriteMode = WriteMode.MERGE,
    val preventOverwriteSuites: Boolean = false,
    val preventOverwriteCases: Boolean = true,
    val protectedTestCaseFields: Set<String> = emptySet(),
    val format: OutputFormat = OutputFormat.JSON,
    val indent: String = "    ",
    val outputMode: OutputMode = OutputMode.SINGLE_FILE,
    val fileNamePrefix: String = "",
)

internal enum class WriteMode {
    MERGE,
    OVERWRITE,
}

internal enum class OutputFormat {
    JSON,
    YAML,
}

/**
 * Output mode for test suite files.
 */
internal enum class OutputMode {
    /** Aggregate all suites into a single file (keyed by operationName). */
    SINGLE_FILE,

    /** Write one file per suite (prefix + operationName + extension). */
    MULTIPLE_FILES,
}

@Suppress("ThrowsCount", "CyclomaticComplexMethod")
internal fun transformAndValidateWriterOptions(map: Map<String, Any?>): TestSuiteWriterOptions {
    val outputMode = when (val m = map["outputMode"]) {
        is OutputMode -> m
        is String? -> m?.let { runCatching { OutputMode.valueOf(it.uppercase()) }.getOrElse { OutputMode.SINGLE_FILE } }
            ?: OutputMode.SINGLE_FILE
        else -> OutputMode.SINGLE_FILE
    }

    val outputFileName = map["outputFileName"] as? String
    if (outputMode == OutputMode.SINGLE_FILE) {
        require(!outputFileName.isNullOrBlank()) {
            "generatorOptions['outputFileName'] is required for SINGLE_FILE mode but was not provided."
        }
    }

    val writeMode = when (val modeValue = map["writeMode"]) {
        is WriteMode -> modeValue
        is String? -> modeValue?.let { runCatching { WriteMode.valueOf(it) }.getOrElse { WriteMode.MERGE } } ?: WriteMode.MERGE
        else -> throw IllegalArgumentException("Invalid 'writeMode' option: '$modeValue'. Expected enum name or string.")
    }

    val preventOverwriteSuites = when (val v = map["preventOverwriteSuites"]) {
        is Boolean -> v
        is String? -> v?.toBooleanStrictOrNull() ?: false
        else -> throw IllegalArgumentException("Invalid 'preventOverwriteSuites' option: '$v'. Expected boolean or string.")
    }

    val preventOverwriteCases = when (val v = map["preventOverwriteCases"]) {
        is Boolean -> v
        is String? -> v?.toBooleanStrictOrNull() ?: true
        else -> throw IllegalArgumentException("Invalid 'preventOverwriteCases' option: '$v'. Expected boolean or string.")
    }

    val protectedTestCaseFields: Set<String> = when (val raw = map["protectedTestCaseFields"]) {
        is Collection<*> -> raw.filterIsInstance<String>().flatMap { it.split(',') }.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        is String? -> raw?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
        else -> throw IllegalArgumentException("Invalid 'protectedTestCaseFields' option: '$raw'. Expected list or comma-separated string.")
    }

    val format = when (val f = map["format"]) {
        is OutputFormat -> f
        is String? -> f?.let { runCatching { OutputFormat.valueOf(it.uppercase()) } }?.getOrElse { OutputFormat.JSON } ?: OutputFormat.JSON
        else -> OutputFormat.JSON
    }

    val indent = map["indent"] as? String ?: "    "

    val fileNamePrefix = map["fileNamePrefix"] as? String ?: ""

    return TestSuiteWriterOptions(
        outputFileName = outputFileName ?: "",
        writeMode = writeMode,
        preventOverwriteSuites = preventOverwriteSuites,
        preventOverwriteCases = preventOverwriteCases,
        protectedTestCaseFields = protectedTestCaseFields,
        format = format,
        indent = indent,
        outputMode = outputMode,
        fileNamePrefix = fileNamePrefix,
    )
}
