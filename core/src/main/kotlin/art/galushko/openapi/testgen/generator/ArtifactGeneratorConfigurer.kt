package art.galushko.openapi.testgen.generator

import art.galushko.openapi.testgen.spi.ArtifactGenerator
import java.io.File

public object ArtifactGeneratorConfigurer {

    /**
     * Creates an [ArtifactGenerator] instance for the given generator ID using built-in factories.
     *
     * Inputs: generator id, generator options, and output directory.
     * Output: configured [ArtifactGenerator].
     * Errors: throws if the generator id is unknown.
     */
    public fun createArtifactGenerator(
        generatorId: String,
        options: Map<String, Any?>,
        outputDir: File,
    ): ArtifactGenerator {
        val registry = ArtifactGeneratorRegistry()
        return registry.create(generatorId, outputDir, options)
    }
}
