package art.galushko.openapi.testgen.plugin

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/**
 * Gradle-native typed configuration block for SnakeYAML parser settings.
 *
 * Provides IDE autocomplete and compile-time validation for parser settings.
 * All settings default to null (unset), which preserves SnakeYAML's built-in defaults.
 */
public open class ParserSettingsExtension @Inject constructor(
    objects: ObjectFactory,
) {
    /**
     * Maximum code points to parse. Increase for large YAML specs (>3MB).
     * Default: null (~3MB, SnakeYAML's built-in default)
     */
    @get:Input
    @get:Optional
    public val yamlCodePointLimit: Property<Int> = objects.property(Int::class.java)

    /**
     * Maximum YAML aliases for collections (DoS protection).
     * Default: null (50, SnakeYAML's built-in default)
     */
    @get:Input
    @get:Optional
    public val yamlMaxAliasesForCollections: Property<Int> = objects.property(Int::class.java)

    /**
     * Whether to allow recursive keys. Keep false to prevent infinite loops.
     * Default: null (false, SnakeYAML's built-in default)
     */
    @get:Input
    @get:Optional
    public val yamlAllowRecursiveKeys: Property<Boolean> = objects.property(Boolean::class.java)

    /**
     * Maximum nesting depth for YAML structures.
     * Default: null (50, SnakeYAML's built-in default)
     */
    @get:Input
    @get:Optional
    public val yamlNestingDepthLimit: Property<Int> = objects.property(Int::class.java)

    /**
     * Builds a map of parser settings for passing to the core engine.
     * Only includes settings that have been explicitly set.
     */
    public fun buildParserSettingsMap(): Map<String, Any?> {
        val settings = mutableMapOf<String, Any?>()

        fun add(key: String, provider: Provider<*>) {
            provider.orNull?.let { settings[key] = it }
        }

        add("yamlCodePointLimit", yamlCodePointLimit)
        add("yamlMaxAliasesForCollections", yamlMaxAliasesForCollections)
        add("yamlAllowRecursiveKeys", yamlAllowRecursiveKeys)
        add("yamlNestingDepthLimit", yamlNestingDepthLimit)

        return settings
    }
}
