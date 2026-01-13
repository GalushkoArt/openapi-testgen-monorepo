package art.galushko.openapi.testgen.pattern.support

import art.galushko.openapi.testgen.config.ModuleSettingsExtractor
import art.galushko.openapi.testgen.example.config.ConfigurationException
import art.galushko.openapi.testgen.pattern.value.PatternGenerationOptions

/**
 * Extracts configuration entries owned by the pattern support module from `testGenerationSettings`.
 *
 * Settings key: [SETTINGS_KEY] (`patternGeneration`).
 *
 * Implements [ModuleSettingsExtractor] for use with
 * [art.galushko.openapi.testgen.config.TestGeneratorExecutionOptionsFactory.fromConfig].
 */
public object PatternModuleSettingsExtractor : ModuleSettingsExtractor {
    public const val SETTINGS_KEY: String = "patternGeneration"

    override val settingsKey: String = SETTINGS_KEY

    override fun parse(raw: Any?): PatternGenerationOptions = when (raw) {
        null -> PatternGenerationOptions()
        is Map<*, *> -> PatternGenerationOptions.fromMap(
            raw.toStringAnyNullableMap(scope = "testGenerationSettings.$settingsKey")
        )
        else -> throw ConfigurationException(
            field = "testGenerationSettings.$settingsKey",
            expected = "Map<String, Any?>",
            actual = raw::class.qualifiedName ?: "unknown",
        )
    }

    private fun Map<*, *>.toStringAnyNullableMap(scope: String): Map<String, Any?> {
        return entries.associate { (key, value) ->
            val stringKey = key as? String
                ?: throw ConfigurationException(
                    field = scope,
                    expected = "Map<String, Any?> (string keys)",
                    actual = "non-string key: ${key?.let { it::class.qualifiedName } ?: "null"}",
                )
            stringKey to value
        }
    }
}
