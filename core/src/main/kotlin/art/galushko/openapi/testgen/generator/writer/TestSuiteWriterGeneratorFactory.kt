package art.galushko.openapi.testgen.generator.writer

import art.galushko.openapi.testgen.generator.ArtifactGeneratorFactory
import art.galushko.openapi.testgen.generator.GeneratorIds
import art.galushko.openapi.testgen.generator.writer.TestSuiteWriterGeneratorFactory.create
import art.galushko.openapi.testgen.spi.ArtifactGenerator
import java.io.File

/**
 * Factory for creating [TestSuiteWriter] instances.
 *
 * Each call to [create] returns a fresh instance with no accumulated state.
 * This ensures clean lifecycle management - create a new writer for each generation run.
 *
 * ```kotlin
 * val factory = TestSuiteWriterGeneratorFactory
 * val writer = factory.create(outputDir, options)
 * testSuites.forEach { writer.generateTests(it) }
 * // For a new generation run, call create() again
 * ```
 *
 * @see TestSuiteWriter for lifecycle and thread-safety details
 */
internal object TestSuiteWriterGeneratorFactory : ArtifactGeneratorFactory {
    override val id: String = GeneratorIds.TEST_SUITE_WRITER
    override val description: String = "Writes test suites to JSON/YAML files"

    override fun create(outputDir: File, options: Map<String, Any?>): ArtifactGenerator =
        TestSuiteWriter(outputDir, options)
}


