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
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Emits test suites as JSON/YAML files.
 *
 * Supports two output modes:
 * - [OutputMode.SINGLE_FILE]: Aggregates all suites into one file keyed by operationName.
 * - [OutputMode.MULTIPLE_FILES]: Writes one file per suite immediately (prefix + operationName + extension).
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
 * // Preferred: Use the registry/factory
 * val registry = ArtifactGeneratorRegistry()
 * val writer = registry.create("test-suite-writer", outputDir, options)
 * testSuites.forEach { writer.generateTests(it) }
 *
 * // Direct instantiation (for testing or when factory not available)
 * val writer = TestSuiteWriter(outputDir, options)
 * testSuites.forEach { writer.generateTests(it) }
 * // Do not reuse 'writer' for a new generation run
 * ```
 *
 * ### Incorrect Usage
 *
 * ```kotlin
 * // BAD: Reusing writer across generation runs causes state leakage
 * val writer = TestSuiteWriter(outputDir, options)
 * generation1.forEach { writer.generateTests(it) }
 * generation2.forEach { writer.generateTests(it) }  // State from generation1 leaks here!
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
 * @see TestSuiteWriterGeneratorFactory factory for creating instances
 * @see art.galushko.openapi.testgen.generator.ArtifactGeneratorRegistry registry-based creation with discovery
 */
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
            loadExisting()
        }
    }

    override fun generateTests(testSuite: TestSuite) {
        val suiteName = testSuite.operationName
        if (suiteName.isNullOrBlank()) {
            log.warn(
                "TestSuite has no operationName; skipping: path={}, method={}",
                testSuite.path, testSuite.method
            )
            return
        }

        when (options.writeMode) {
            WriteMode.OVERWRITE -> {
                putSuite(suiteName, testSuite)
            }

            WriteMode.MERGE -> {
                mergeSuite(suiteName, testSuite, allowOverwriteSuite = !options.preventOverwriteSuites)
            }
        }

        when (options.outputMode) {
            OutputMode.SINGLE_FILE -> writeAggregatedFile()
            OutputMode.MULTIPLE_FILES -> writeSuiteToFile(suiteName, suites[suiteName])
        }
    }

    // --- init helpers

    private fun prepareOutputDir() {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            log.warn("Failed to create output directory: {}", outputDir.absolutePath)
        }
    }

    private fun loadExisting() {
        when (options.outputMode) {
            OutputMode.SINGLE_FILE -> loadExistingAggregatedFile()
            OutputMode.MULTIPLE_FILES -> loadExistingMultipleFiles()
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
        if (existing == null || allowOverwriteSuite) {
            // Replace the whole suite, keep cases sorted for stability
            putSuite(name, incoming)
            return
        }

        // Merge into existing
        val mergedCases = mergeCases(existing.testCases, incoming.testCases)
        val merged = existing.copy(
            path = incoming.path,
            method = incoming.method,
            operationName = incoming.operationName ?: existing.operationName,
            testCases = mergedCases
        )
        suites[name] = merged
    }

    // --- case merge

    private fun mergeCases(existingCases: List<TestCase>, incomingCases: List<TestCase>): List<TestCase> {
        if (incomingCases.isEmpty()) return existingCases

        // index by name for O(1) replacement; preserve order
        val indexByName = mutableMapOf<String, Int>()
        existingCases.forEachIndexed { idx, c -> indexByName[c.name] = idx }

        val result = existingCases.toMutableList()

        for (inc in incomingCases) {
            val idx = indexByName[inc.name]
            if (idx == null) {
                // New identity -> append and track
                result.add(inc)
                indexByName[inc.name] = result.lastIndex
            } else {
                // Existing identity -> replace or merge-in-place
                val replacement = if (options.preventOverwriteCases) {
                    mergeCasePreservingFields(result[idx], inc)
                } else {
                    inc
                }
                result[idx] = replacement
            }
        }

        val shouldSort = !options.preventOverwriteCases
        return if (shouldSort) result.sortedBy { it.name } else result
    }

    private fun mergeCasePreservingFields(existing: TestCase, incoming: TestCase): TestCase {
        if (options.protectedTestCaseFields.isEmpty()) return existing

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
            expectedBody = pick("expectedBody", existing.expectedBody, incoming.expectedBody),
            needToComplete = pick("needToComplete", existing.needToComplete, incoming.needToComplete),
            expectedStatusCode = pick("expectedStatusCode", existing.expectedStatusCode, incoming.expectedStatusCode),
            rule = pick("rule", existing.rule, incoming.rule),
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

    private fun writeSuiteToFile(operationName: String, suite: TestSuite?) {
        requireNotNull(suite) { "Suite for operation '$operationName' not found" }
        val sanitizedName = sanitizeFileName(operationName)
        val extension = options.format.name.lowercase()
        val fileName = "${options.fileNamePrefix}$sanitizedName.$extension"
        val outFile = File(outputDir, fileName)

        writeAtomically(outFile, suite)
        log.debug("Wrote suite {} to {}", operationName, outFile.absolutePath)
    }

    private fun writeAtomically(outFile: File, content: Any) {
        if (!outFile.parentFile.exists()) {
            outFile.parentFile.mkdirs()
        }
        val tmpFile = File(outFile.parentFile, outFile.name + ".tmp")

        try {
            writer().writeValue(tmpFile, content)
            Files.move(
                tmpFile.toPath(),
                outFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: Exception) {
            log.error("Failed to write {} to {}", options.format.name.lowercase(), outFile.absolutePath, e)
            runCatching { tmpFile.delete() }
        }
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
}
