package art.galushko.openapi.testgen.config

/**
 * Contract for extracting module-owned settings from `testGenerationSettings`.
 *
 * Implementations are provided to [TestGeneratorExecutionOptionsFactory.fromConfig] to extract
 * and parse module-specific settings before core settings validation. This ensures:
 * - Module settings are removed from the shared map before core parsing
 * - Each module owns its configuration parsing logic
 * - Factory orchestrates extraction in deterministic order (sorted by [settingsKey])
 */
public interface ModuleSettingsExtractor {
    /**
     * Key this extractor claims from `testGenerationSettings`.
     *
     * Must be unique across all extractors passed to the factory.
     */
    public val settingsKey: String

    /**
     * Parse raw value into typed settings object.
     *
     * @param raw The raw value from `testGenerationSettings` (may be null, Map, etc.)
     * @return Parsed settings object (type depends on module)
     * @throws art.galushko.openapi.testgen.example.config.ConfigurationException if the value has an invalid type or structure
     */
    public fun parse(raw: Any?): Any
}

/**
 * Type-safe wrapper for module settings storage.
 *
 * Provides type-safe access to extracted module settings via [get].
 */
@JvmInline
public value class ModuleSettings @PublishedApi internal constructor(
    @PublishedApi internal val data: Map<String, Any> = emptyMap(),
) {
    /**
     * Retrieves module settings by key with type casting.
     *
     * @param key The settings key (same as [ModuleSettingsExtractor.settingsKey])
     * @return The settings object cast to [T], or null if not present or wrong type
     */
    public inline fun <reified T> get(key: String): T? = data[key] as? T

    /**
     * Checks if settings for the given key exist.
     */
    public operator fun contains(key: String): Boolean = key in data

    public companion object {
        public val EMPTY: ModuleSettings = ModuleSettings(emptyMap())
    }
}
