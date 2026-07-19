package art.galushko.openapi.testgen.generator.writer

import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.spi.ArtifactGenerator
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Emits test suites as JSON/YAML files.
 *
 * Supports two output modes:
 * - [OutputMode.SINGLE_FILE]: Aggregates all suites into one file keyed by operationName.
 * - [OutputMode.MULTIPLE_FILES]: Writes one file per suite (prefix + operationName + extension).
 *
 * ## Lifecycle
 *
 * This generator maintains internal state to support suite aggregation and merge operations.
 * **Create a new instance for each generation run.** Do not reuse instances across multiple
 * independent generation sessions, as state from previous runs will leak into subsequent ones.
 *
 * Use [TestSuiteWriterGeneratorFactory] or [art.galushko.openapi.testgen.generator.ArtifactGeneratorRegistry] to create instances:
 *
 * ```kotlin
 * // Preferred: Use the batch method via the registry/factory
 * val registry = ArtifactGeneratorRegistry()
 * val writer = registry.create("test-suite-writer", outputDir, options)
 * writer.generateTests(testSuites)
 *
 * // Direct instantiation (for testing or when factory not available)
 * val writer = TestSuiteWriter(outputDir, options)
 * writer.generateTests(testSuites)
 * // Do not reuse 'writer' for a new generation run
 * ```
 *
 * ### Incorrect Usage
 *
 * ```kotlin
 * // BAD: Reusing writer across generation runs causes state leakage
 * val writer = TestSuiteWriter(outputDir, options)
 * writer.generateTests(generation1)
 * writer.generateTests(generation2)  // State from generation1 leaks here!
 * ```
 *
 * ## Thread Safety
 *
 * This class is **NOT thread-safe**. Do not call [generateTests] concurrently from multiple threads.
 * If concurrent generation is required, create separate instances per thread.
 *
 * ## State Management
 *
 * Internal state serves two purposes:
 * - **SINGLE_FILE mode**: Suites are aggregated in memory and written together.
 * - **MERGE mode**: Existing file content is loaded on construction and merged with incoming suites.
 *
 * ## Performance
 *
 * The batch method [generateTests(List)] is optimized for both output modes:
 * - **SINGLE_FILE**: All suites are processed in memory first, then the aggregated file is written **once**
 *   (instead of rewriting after each suite).
 * - **MULTIPLE_FILES**: All suites are processed sequentially, then individual files are written
 *   in **parallel** via `parallelStream()`.
 *
 * CLI and Gradle plugin use the batch entry point automatically.
 *
 * @see TestSuiteWriterGeneratorFactory factory for creating instances
 * @see art.galushko.openapi.testgen.generator.ArtifactGeneratorRegistry registry-based creation with discovery
 */
@Suppress("TooManyFunctions")
internal class TestSuiteWriter(
    private val outputDir: File,
    optionMap: Map<String, Any?>,
) : ArtifactGenerator {

    private val log = LoggerFactory.getLogger(TestSuiteWriter::class.java)
    private val suites: LinkedHashMap<String, TestSuite> = linkedMapOf()
    private val options: TestSuiteWriterOptions = transformAndValidateWriterOptions(optionMap)
    private val mapper: ObjectMapper = ObjectMapper(
        when (options.format) {
            OutputFormat.YAML -> YAMLFactory()
            OutputFormat.JSON -> JsonFactory()
        }
    ).registerModule(KotlinModule.Builder().build()).enable(SerializationFeature.INDENT_OUTPUT)

    init {
        prepareOutputDir()
        if (options.writeMode == WriteMode.MERGE) {
            when (options.outputMode) {
                OutputMode.SINGLE_FILE -> loadExistingAggregatedFile()
                OutputMode.MULTIPLE_FILES -> loadExistingMultipleFiles()
            }
        }
    }

    override fun generateTests(testSuite: TestSuite) {
        val suiteName = processSuite(testSuite) ?: return

        when (options.outputMode) {
            OutputMode.SINGLE_FILE -> writeAggregatedFile()
            OutputMode.MULTIPLE_FILES -> writeSuiteToFile(resolveWriteTargets(listOf(suiteName)).single())
        }
    }

    override fun generateTests(testSuites: List<TestSuite>) {
        if (testSuites.isEmpty()) return

        when (options.outputMode) {
            OutputMode.SINGLE_FILE -> {
                for (testSuite in testSuites) {
                    processSuite(testSuite)
                }
                writeAggregatedFile()
            }

            OutputMode.MULTIPLE_FILES -> {
                val suitesToWrite = mutableListOf<String>()
                for (testSuite in testSuites) {
                    val suiteName = processSuite(testSuite) ?: continue
                    suitesToWrite.add(suiteName)
                }
                resolveWriteTargets(suitesToWrite).parallelStream().forEach { target ->
                    writeSuiteToFile(target)
                }
            }
        }
    }

    private fun processSuite(testSuite: TestSuite): String? {
        val suiteName = testSuite.operationName
        if (suiteName.isNullOrBlank()) {
            log.warn(
                "TestSuite has no operationName; skipping: path={}, method={}",
                testSuite.path, testSuite.method
            )
            return null
        }

        ensureUniqueOutputPath(suiteName)

        when (options.writeMode) {
            WriteMode.OVERWRITE -> putSuite(suiteName, testSuite)
            WriteMode.MERGE -> mergeSuite(suiteName, testSuite, allowOverwriteSuite = !options.preventOverwriteSuites)
        }

        return suiteName
    }

    // --- init helpers

    private fun prepareOutputDir() {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            log.warn("Failed to create output directory: {}", outputDir.absolutePath)
        }
    }

    private fun loadExistingAggregatedFile() {
        val file = File(outputDir, options.outputFileName)
        if (!file.exists() || !file.isFile || !file.canRead()) return

        try {
            val fromFile: LinkedHashMap<String, TestSuite> = mapper.readValue(file)
            // Keep deterministic order by key
            fromFile.entries.sortedBy { it.key }.forEach { (k, v) -> suites[k] = v }
            log.info(
                "Loaded {} suites from existing {}",
                suites.size, file.absolutePath
            )
        } catch (e: Exception) {
            log.warn(
                "Failed to read existing {}; starting with empty aggregator",
                file.absolutePath, e
            )
        }
    }

    private fun loadExistingMultipleFiles() {
        val extension = ".${options.format.name.lowercase()}"
        val prefix = options.fileNamePrefix

        val matchingFiles = outputDir.listFiles { file ->
            file.isFile &&
                file.name.startsWith(prefix) &&
                file.name.endsWith(extension) &&
                file.canRead()
        } ?: return

        for (file in matchingFiles.sortedBy { it.name }) {
            try {
                val suite: TestSuite = mapper.readValue(file, TestSuite::class.java)
                val operationName = suite.operationName
                if (!operationName.isNullOrBlank()) {
                    suites[operationName] = suite
                }
            } catch (e: Exception) {
                log.warn("Failed to read existing suite from {}; skipping", file.absolutePath, e)
            }
        }

        if (suites.isNotEmpty()) {
            log.info("Loaded {} suites from existing files in {}", suites.size, outputDir.absolutePath)
        }
    }

    // --- suite merge

    private fun putSuite(name: String, incoming: TestSuite) {
        val sorted = incoming.copy(testCases = incoming.testCases.sortedBy { it.name })
        suites[name] = sorted
    }

    private fun mergeSuite(name: String, incoming: TestSuite, allowOverwriteSuite: Boolean) {
        val existing = suites[name]
        if (existing == null) {
            // No existing suite - just add, keep cases sorted for stability
            putSuite(name, incoming)
            return
        }

        if (!allowOverwriteSuite) {
            // Preserve existing suite as-is (no case changes)
            return
        }

        // Merge cases when suite overwrite is allowed
        val mergedCases = mergeCases(existing.testCases, incoming.testCases)
        suites[name] = incoming.copy(testCases = mergedCases)
    }

    // --- case merge

    private fun mergeCases(existingCases: List<TestCase>, incomingCases: List<TestCase>): List<TestCase> {
        if (incomingCases.isEmpty()) return existingCases

        // index by name for O(1) replacement; preserve order
        val indexByName = mutableMapOf<String, Int>()
        existingCases.forEachIndexed { idx, c -> indexByName[c.name] = idx }

        val result = existingCases.toMutableList()
        val overwriteCases = !options.preventOverwriteCases
        val preserveFields = overwriteCases && options.protectedTestCaseFields.isNotEmpty()

        for (inc in incomingCases) {
            val idx = indexByName[inc.name]
            if (idx == null) {
                // New identity -> append and track
                result.add(inc)
                indexByName[inc.name] = result.lastIndex
            } else if (overwriteCases) {
                // Existing identity -> replace or merge-in-place
                val replacement = if (preserveFields) {
                    mergeCasePreservingFields(result[idx], inc)
                } else {
                    inc
                }
                result[idx] = replacement
            }
        }

        return if (overwriteCases) result.sortedBy { it.name } else result
    }

    private fun mergeCasePreservingFields(existing: TestCase, incoming: TestCase): TestCase {
        if (options.protectedTestCaseFields.isEmpty()) return incoming

        fun <T> pick(field: String, cur: T, next: T): T =
            if (field in options.protectedTestCaseFields) cur else next

        return TestCase(
            name = pick("name", existing.name, incoming.name),
            method = pick("method", existing.method, incoming.method),
            path = pick("path", existing.path, incoming.path),
            queryParams = pick("queryParams", existing.queryParams, incoming.queryParams),
            pathParams = pick("pathParams", existing.pathParams, incoming.pathParams),
            headers = pick("headers", existing.headers, incoming.headers),
            cookie = pick("cookie", existing.cookie, incoming.cookie),
            body = pick("body", existing.body, incoming.body),
            requestBodyMediaType = pick("requestBodyMediaType", existing.requestBodyMediaType, incoming.requestBodyMediaType),
            expectedBody = pick("expectedBody", existing.expectedBody, incoming.expectedBody),
            responseBodyMediaType = pick("responseBodyMediaType", existing.responseBodyMediaType, incoming.responseBodyMediaType),
            needToComplete = pick("needToComplete", existing.needToComplete, incoming.needToComplete),
            expectedStatusCode = pick("expectedStatusCode", existing.expectedStatusCode, incoming.expectedStatusCode),
            rule = pick("rule", existing.rule, incoming.rule),
            securityValues = pick("securityValues", existing.securityValues, incoming.securityValues),
        )
    }

    // --- write

    private fun writer() =
        if (options.format == OutputFormat.JSON) {
            val pp = DefaultPrettyPrinter().apply {
                val indenter = DefaultIndenter(options.indent, DefaultIndenter.SYS_LF)
                indentObjectsWith(indenter)
                indentArraysWith(indenter)
            }
            mapper.writer(pp)
        } else {
            mapper.writer()
        }

    private fun writeAggregatedFile() {
        if (suites.isEmpty()) {
            log.info("No suites to write; skipping file output")
            return
        }

        // Deterministic suite order
        val ordered: LinkedHashMap<String, TestSuite> =
            suites.entries.sortedBy { it.key }.fold(linkedMapOf()) { acc, e ->
                acc[e.key] = e.value; acc
            }

        val outFile = File(outputDir, options.outputFileName)
        writeAtomically(outFile, ordered)
        log.info(
            "Wrote aggregated {} to {} ({} suites)",
            options.format.name.lowercase(), outFile.absolutePath, ordered.size
        )
    }

    private fun ensureUniqueOutputPath(operationName: String) {
        if (options.outputMode != OutputMode.MULTIPLE_FILES) {
            return
        }

        val targetPath = suiteOutputPathKey(operationName)
        val collidingOperationName = suites.keys.firstOrNull { existingOperationName ->
            existingOperationName != operationName && suiteOutputPathKey(existingOperationName) == targetPath
        }

        require(collidingOperationName == null) {
            "Multiple test suites resolve to the same output file '$targetPath': " +
                "'$collidingOperationName' and '$operationName'."
        }
    }

    private fun resolveWriteTargets(operationNames: Collection<String>): List<SuiteWriteTarget> {
        val targetsByPath = linkedMapOf<String, SuiteWriteTarget>()

        for (operationName in operationNames.distinct()) {
            val suite = requireNotNull(suites[operationName]) {
                "Suite for operation '$operationName' not found"
            }
            val outFile = resolveSuiteOutputFile(operationName)
            val pathKey = outFile.toPath().toAbsolutePath().normalize().toString()
            val existingTarget = targetsByPath.putIfAbsent(
                pathKey,
                SuiteWriteTarget(
                    operationName = operationName,
                    suite = suite,
                    outFile = outFile,
                ),
            )

            require(existingTarget == null || existingTarget.operationName == operationName) {
                "Multiple test suites resolve to the same output file '$pathKey': " +
                    "'${existingTarget?.operationName}' and '$operationName'."
            }
        }

        return targetsByPath.values.sortedBy { it.outFile.name }
    }

    private fun resolveSuiteOutputFile(operationName: String): File {
        val sanitizedName = sanitizeFileName(operationName)
        val extension = options.format.name.lowercase()
        val fileName = "${options.fileNamePrefix}$sanitizedName.$extension"
        return File(outputDir, fileName)
    }

    private fun suiteOutputPathKey(operationName: String): String =
        resolveSuiteOutputFile(operationName).toPath().toAbsolutePath().normalize().toString()

    private fun writeSuiteToFile(target: SuiteWriteTarget) {
        writeAtomically(target.outFile, target.suite)
        log.debug("Wrote suite {} to {}", target.operationName, target.outFile.absolutePath)
    }

    private fun writeAtomically(outFile: File, content: Any) {
        AtomicFileWriter.write(outFile) { tmpFile -> writer().writeValue(tmpFile, content) }
    }

    public companion object {
        private val UNSAFE_FILENAME_CHARS = Regex("[/\\\\:*?\"<>|\\s]+")

        /**
         * Sanitizes operationName for safe use as a filename.
         * Replaces unsafe characters with underscores.
         */
        public fun sanitizeFileName(name: String): String =
            name.replace(UNSAFE_FILENAME_CHARS, "_").trim('_')
    }

    private data class SuiteWriteTarget(
        val operationName: String,
        val suite: TestSuite,
        val outFile: File,
    )
}
