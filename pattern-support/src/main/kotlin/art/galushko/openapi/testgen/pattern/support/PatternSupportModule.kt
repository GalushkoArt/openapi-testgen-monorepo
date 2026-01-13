package art.galushko.openapi.testgen.pattern.support

import art.galushko.openapi.testgen.config.TestGenerationModule
import art.galushko.openapi.testgen.config.TestGeneratorExecutionOptions
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import art.galushko.openapi.testgen.pattern.value.PatternGenerationOptions
import art.galushko.openapi.testgen.pattern.value.PatternValueGenerator
import art.galushko.openapi.testgen.pattern.value.PatternValueProvider
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule

/**
 * Optional pattern support module backed by regexp-gen.
 *
 * Contributes:
 * - Schema example provider id "pattern" that generates values matching `schema.pattern`
 * - Negative schema rule [InvalidPatternSchemaValidationRule] that generates non-matching values
 *
 * Configuration is provided via [PatternModuleSettingsExtractor] (settings key `patternGeneration`).
 *
 * The module is intentionally explicit: embedder code (CLI / Gradle plugin) must pass it to
 * [art.galushko.openapi.testgen.config.TestGenerationEngine] to enable pattern-aware behavior.
 */
public class PatternSupportModule(
    patternGenerationOptions: PatternGenerationOptions = PatternGenerationOptions(),
) : TestGenerationModule {
    override val id: String = "pattern-support"
    private val generator = PatternValueGenerator(patternGenerationOptions)

    override fun schemaValueProviders(options: TestGeneratorExecutionOptions): Map<String, SchemaValueProvider> {
        return mapOf(
            "pattern" to PatternValueProvider(generator),
        )
    }

    override fun extraSimpleSchemaRules(options: TestGeneratorExecutionOptions): List<SimpleSchemaValidationRule> {
        return listOf(
            InvalidPatternSchemaValidationRule(generator),
        )
    }
}
