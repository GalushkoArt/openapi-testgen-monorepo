package art.galushko.openapi.testgen.generator

import art.galushko.openapi.testgen.generator.writer.TestSuiteWriterGeneratorFactory

/**
 * Factory for creating built-in artifact generator factories.
 *
 * Provides explicit, deterministic list of all built-in generators without reflection.
 * Generators are sorted by ID for deterministic ordering.
 */
public object BuiltInGenerators {

    /**
     * Creates all built-in artifact generator factories.
     *
     * @return sorted list of all built-in [ArtifactGeneratorFactory] implementations
     */
    public fun all(): List<ArtifactGeneratorFactory> = listOf(
        TestSuiteWriterGeneratorFactory,
    ).sortedBy { it.id }
}
