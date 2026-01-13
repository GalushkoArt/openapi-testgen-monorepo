package art.galushko.openapi.testgen.generator.template

import art.galushko.openapi.testgen.generator.ArtifactGeneratorFactory
import art.galushko.openapi.testgen.generator.GeneratorIds
import art.galushko.openapi.testgen.spi.ArtifactGenerator
import java.io.File

/**
 * Factory for [TemplateArtifactGenerator].
 */
internal object TemplateArtifactGeneratorFactory : ArtifactGeneratorFactory {
    override val id: String = GeneratorIds.TEMPLATE
    override val description: String = "Generates test code using Mustache templates"

    override fun create(outputDir: File, options: Map<String, Any?>): ArtifactGenerator =
        TemplateArtifactGenerator(outputDir, options)
}


