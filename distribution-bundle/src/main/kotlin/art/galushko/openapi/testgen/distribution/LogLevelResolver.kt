package art.galushko.openapi.testgen.distribution

import art.galushko.openapi.testgen.config.GeneratorConfig
import art.galushko.openapi.testgen.config.TestGeneratorOverrides

/**
 * Resolves the effective generator log level from configuration and overrides.
 *
 * Shared by the CLI and the Gradle plugin so the precedence rule (overrides win over config)
 * and the validation message stay identical in every entry point.
 */
public object LogLevelResolver {

    /** Log levels accepted by [resolve], in severity order. */
    public val allowedLevels: Set<String> =
        linkedSetOf("ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF")

    /**
     * Returns the normalized (trimmed, uppercased) log level, or null when neither
     * [overrides] nor [config] specifies one.
     *
     * @throws IllegalArgumentException if the specified level is not one of [allowedLevels]
     */
    public fun resolve(config: GeneratorConfig?, overrides: TestGeneratorOverrides): String? {
        val level = (overrides.logLevel ?: config?.logLevel)?.trim()?.uppercase() ?: return null
        require(level in allowedLevels) {
            "Invalid log level '$level'. Expected one of ${allowedLevels.joinToString(", ")}."
        }
        return level
    }
}
