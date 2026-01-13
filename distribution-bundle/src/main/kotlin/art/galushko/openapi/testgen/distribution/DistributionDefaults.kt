package art.galushko.openapi.testgen.distribution

import art.galushko.openapi.testgen.config.ModuleSettingsExtractor
import art.galushko.openapi.testgen.example.config.ExampleValueSettings
import art.galushko.openapi.testgen.config.TestGenerationModule
import art.galushko.openapi.testgen.config.TestGenerationSettings
import art.galushko.openapi.testgen.generator.template.TemplateGeneratorModule
import art.galushko.openapi.testgen.pattern.support.PatternModuleSettingsExtractor
import art.galushko.openapi.testgen.pattern.support.PatternSupportModule
import art.galushko.openapi.testgen.pattern.value.PatternGenerationOptions

/**
 * Factory for standard distribution configuration.
 *
 * Provides defaults for modules, extractors, and settings that ensure
 * CLI and plugin use consistent "distribution-ready" configuration.
 */
public object DistributionDefaults {

    /**
     * Returns the default list of test generation modules.
     *
     * Includes:
     * - [TemplateGeneratorModule] - Mustache template-based code generation
     * - [PatternSupportModule] - Pattern-based example generation and validation rules
     *
     * @param patternOptions options for pattern generation (defaults to [PatternGenerationOptions])
     * @return list of modules in registration order
     */
    public fun modules(
        patternOptions: PatternGenerationOptions = PatternGenerationOptions(),
    ): List<TestGenerationModule> = listOf(
        TemplateGeneratorModule,
        PatternSupportModule(patternOptions),
    )

    /**
     * Returns the default list of module settings extractors.
     *
     * Includes:
     * - [PatternModuleSettingsExtractor] - Extracts pattern generation settings
     *
     * @return list of extractors sorted by settings key for deterministic processing
     */
    public fun extractors(): List<ModuleSettingsExtractor> = listOf(
        PatternModuleSettingsExtractor,
    ).sortedBy { it.settingsKey }

    /**
     * Returns the default test generation settings with pattern provider enabled.
     *
     * Inserts the "pattern" provider before "plain-string" in the provider order
     * to enable pattern-based example generation. If "plain-string" is not present,
     * the provider is appended to the end.
     *
     * @return default settings with pattern provider in the provider order
     */
    public fun settings(): TestGenerationSettings {
        val providers = ExampleValueSettings.DEFAULT_PROVIDER_ORDER.toMutableList()
        if ("pattern" !in providers) {
            val insertAt = providers.indexOf("plain-string").takeIf { it >= 0 } ?: providers.size
            providers.add(insertAt, "pattern")
        }
        return TestGenerationSettings(
            exampleValues = ExampleValueSettings(providers = providers),
        )
    }
}
