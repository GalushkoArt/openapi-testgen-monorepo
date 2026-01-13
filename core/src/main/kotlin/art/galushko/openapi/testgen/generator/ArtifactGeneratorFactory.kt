package art.galushko.openapi.testgen.generator

import art.galushko.openapi.testgen.spi.ArtifactGenerator
import java.io.File

/**
 * Factory for creating [ArtifactGenerator] instances.
 *
 * Implementations register with [ArtifactGeneratorRegistry] and are selected by [id].
 * Factories are responsible for validating options and reporting configuration errors.
 */
public interface ArtifactGeneratorFactory {
    /**
     * Unique identifier for this generator type.
     */
    public val id: String

    /**
     * Human-readable description of what this generator produces.
     */
    public val description: String

    /**
     * Creates a new generator instance.
     *
     * @param outputDir directory where artifacts will be written
     * @param options generator-specific options
     * @return configured generator instance
     */
    public fun create(outputDir: File, options: Map<String, Any?>): ArtifactGenerator
}


