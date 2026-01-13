package art.galushko.openapi.testgen.config

import art.galushko.openapi.testgen.generator.ArtifactGeneratorFactory
import art.galushko.openapi.testgen.spi.AuthValidationRule
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule

/**
 * Explicit, reflection-free contract for contributing optional features to test generation.
 *
 * Modules are provided explicitly to [TestGenerationEngine] by CLI / Gradle plugin / embedding code.
 * This enables optional feature modules (e.g., pattern support, additional generators) without
 * reflection, ServiceLoader, or global mutable registries.
 *
 * Constraints: [id] must be non-blank and unique across modules; duplicates fail fast during wiring.
 * Determinism: modules are sorted by [id] and class name when applied.
 */
public interface TestGenerationModule {
    /**
     * Stable module id used for deterministic ordering and duplicate detection.
     */
    public val id: String

    /**
     * Contributes additional [ArtifactGeneratorFactory] implementations.
     */
    public fun artifactGeneratorFactories(): List<ArtifactGeneratorFactory> = emptyList()

    /**
     * Contributes additional schema example value providers keyed by provider id.
     *
     * Provider ids can be referenced from [art.galushko.openapi.testgen.example.config.ExampleValueSettings.providers].
     */
    public fun schemaValueProviders(options: TestGeneratorExecutionOptions): Map<String, SchemaValueProvider> = emptyMap()

    /**
     * Contributes additional schema validation rules.
     */
    public fun extraSimpleSchemaRules(options: TestGeneratorExecutionOptions): List<SimpleSchemaValidationRule> = emptyList()

    /**
     * Contributes additional auth validation rules.
     */
    public fun extraAuthRules(options: TestGeneratorExecutionOptions): List<AuthValidationRule> = emptyList()
}


