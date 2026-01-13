package art.galushko.openapi.testgen.generator.template

import art.galushko.openapi.testgen.config.TestGenerationModule
import art.galushko.openapi.testgen.generator.ArtifactGeneratorFactory

/**
 * Optional Mustache template generator module.
 *
 * Contributes a single [ArtifactGeneratorFactory] for generator id "template" ([art.galushko.openapi.testgen.generator.GeneratorIds.TEMPLATE]).
 *
 * This module is intentionally explicit: embedder code (CLI / Gradle plugin) must pass it to
 * [art.galushko.openapi.testgen.config.TestGenerationEngine] to enable the template generator.
 */
public object TemplateGeneratorModule : TestGenerationModule {
    override val id: String = "template"

    override fun artifactGeneratorFactories(): List<ArtifactGeneratorFactory> = listOf(
        TemplateArtifactGeneratorFactory,
    )
}


