package art.galushko.openapi.testgen.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

/**
 * Fully resolved options for a single test generation execution.
 *
 * Combines values from configuration files and environment-specific sources (CLI / Gradle).
 * Side effects: [TestGeneratorExecutionOptionsFactory.fromConfig] ensures [outputDir] exists.
 */
public data class TestGeneratorExecutionOptions(
    val specFile: String,
    val outputDir: Path,
    val generatorId: String,
    val generatorOptions: Map<String, Any?>,
    val testGenerationSettings: TestGenerationSettings,
    val alwaysWriteTests: Boolean,
    val moduleSettings: ModuleSettings = ModuleSettings.EMPTY,
    val parserSettings: ParserSettings = ParserSettings(),
)

/**
 * Generic overrides that can be merged with [GeneratorConfig]
 * to produce [TestGeneratorExecutionOptions].
 *
 * Inputs: typically populated from CLI arguments or Gradle DSL.
 * Precedence: override values take priority over config values.
 */
public data class TestGeneratorOverrides(
    val logLevel: String? = null,
    val specFile: String? = null,
    val outputDir: Path? = null,
    val generatorId: String? = null,
    val generatorOptions: Map<String, Any?> = emptyMap(),
    val testGenerationSettings: Map<String, Any?> = emptyMap(),
    val alwaysWriteTests: Boolean? = null,
    val parserSettings: Map<String, Any?> = emptyMap(),
)

/**
 * Factory that merges declarative [GeneratorConfig] with environment-specific [TestGeneratorOverrides]
 * into fully resolved [TestGeneratorExecutionOptions].
 *
 * Merge semantics:
 * - Overrides win over config values when provided.
 * - Nested maps are deep-merged; lists/arrays are replaced.
 * - Map/non-map mismatches prefer the override value.
 *
 * Determinism: module extractors are sorted by [ModuleSettingsExtractor.settingsKey].
 */
public object TestGeneratorExecutionOptionsFactory {

    private val logger = LoggerFactory.getLogger(TestGeneratorExecutionOptionsFactory::class.java)

    /**
     * Creates [TestGeneratorExecutionOptions] by merging config and overrides.
     *
     * @param config Declarative configuration from YAML file (may be null)
     * @param overrides Environment-specific overrides from CLI/Gradle
     * @param moduleExtractors Module settings extractors, processed before core settings parsing.
     *   Extractors are sorted by [ModuleSettingsExtractor.settingsKey] for deterministic order.
     * @param defaultTestGenerationSettings fallback settings when config and overrides do not provide a value
     */
    public fun fromConfig(
        config: GeneratorConfig?,
        overrides: TestGeneratorOverrides = TestGeneratorOverrides(),
        moduleExtractors: List<ModuleSettingsExtractor> = emptyList(),
        defaultTestGenerationSettings: TestGenerationSettings = TestGenerationSettings(),
    ): TestGeneratorExecutionOptions {
        val specFile = resolveSpecFile(config, overrides)
        val outputDir = resolveOutputDir(config, overrides)

        val generatorId: String =
            overrides.generatorId
                ?: config?.generator
                ?: error("Generator must be provided either via overrides or config")

        val generatorOptions = ConfigMerger.merge(
            base = config?.generatorOptions ?: emptyMap(),
            overrides = overrides.generatorOptions,
            rootField = "generatorOptions",
        )

        // Merge testGenerationSettings from config and overrides
        val mergedSettings = ConfigMerger.merge(
            base = config?.testGenerationSettings ?: emptyMap(),
            overrides = overrides.testGenerationSettings,
            rootField = "testGenerationSettings",
        ).toMutableMap()

        // Extract module settings (removes keys from mergedSettings)
        val moduleSettingsMap = extractModuleSettings(mergedSettings, moduleExtractors)

        // Parse remaining settings as core settings
        val testGenerationSettings = TestGenerationSettings.fromMap(mergedSettings, defaultTestGenerationSettings)

        val alwaysWriteTests: Boolean =
            overrides.alwaysWriteTests ?: config?.alwaysWriteTests ?: false

        // Merge parserSettings from config and overrides
        val mergedParserSettings = ConfigMerger.merge(
            base = config?.parserSettings ?: emptyMap(),
            overrides = overrides.parserSettings,
            rootField = "parserSettings",
        )
        val parserSettings = ParserSettings.fromMap(mergedParserSettings)

        val result = TestGeneratorExecutionOptions(
            specFile = specFile,
            outputDir = outputDir,
            generatorId = generatorId,
            generatorOptions = generatorOptions,
            testGenerationSettings = testGenerationSettings,
            alwaysWriteTests = alwaysWriteTests,
            moduleSettings = ModuleSettings(moduleSettingsMap),
            parserSettings = parserSettings,
        )

        if (logger.isDebugEnabled) {
            logger.debug("Resolved TestGeneratorExecutionOptions:\n{}", prettyPrint(result))
        }

        return result
    }

    private fun extractModuleSettings(
        mergedSettings: MutableMap<String, Any?>,
        moduleExtractors: List<ModuleSettingsExtractor>,
    ): Map<String, Any> {
        val moduleSettingsMap = linkedMapOf<String, Any>()
        for (extractor in moduleExtractors.sortedBy { it.settingsKey }) {
            val raw = mergedSettings.remove(extractor.settingsKey)
            moduleSettingsMap[extractor.settingsKey] = extractor.parse(raw)
        }
        return moduleSettingsMap
    }

    private fun resolveSpecFile(
        config: GeneratorConfig?,
        overrides: TestGeneratorOverrides,
    ): String {
        val fromOverrides = overrides.specFile
        return fromOverrides
            ?: config?.specFile
            ?: error("specFile must be configured via overrides or config file")
    }

    private fun resolveOutputDir(
        config: GeneratorConfig?,
        overrides: TestGeneratorOverrides,
    ): Path {
        val path: Path? =
            overrides.outputDir
                ?: config?.outputDir?.let { Path.of(it) }

        requireNotNull(path) { "outputDir must be configured via overrides or config file" }
        Files.createDirectories(path)
        return path
    }

    /**
     * Pretty prints an object to YAML format. Falls back to toString with formatting if YAML conversion fails.
     */
    internal fun prettyPrint(obj: Any): String =
        try {
            val yamlMapper = ObjectMapper(
                YAMLFactory()
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            ).registerKotlinModule()
            yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
        } catch (e: Exception) {
            logger.debug("Failed to serialize object to YAML, falling back to toString: {}", e.message)
            obj.toString().replace(", ", "\n  ")
        }
}


