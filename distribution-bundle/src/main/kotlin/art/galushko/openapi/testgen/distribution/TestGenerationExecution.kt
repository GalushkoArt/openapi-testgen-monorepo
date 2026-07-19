package art.galushko.openapi.testgen.distribution

import art.galushko.openapi.testgen.config.GeneratorConfig
import art.galushko.openapi.testgen.config.GeneratorConfigLoader
import art.galushko.openapi.testgen.config.TestGeneratorOverrides
import java.nio.file.Path

/**
 * Shared CLI/Gradle execution flow: load the optional config file, resolve and apply the
 * effective log level, then execute test generation.
 *
 * Callers load the config first (they typically need it to build their [TestGeneratorOverrides]),
 * then hand both to [run]:
 *
 * ```kotlin
 * val config = TestGenerationExecution.loadConfig(configPath)
 * val overrides = buildOverrides(config)
 * val result = TestGenerationExecution.run(runner, config, overrides)
 * ```
 */
public object TestGenerationExecution {

    /**
     * Loads the YAML generator config, or returns null when [configFile] is null.
     *
     * @throws IllegalArgumentException if the file is missing or unreadable
     */
    public fun loadConfig(configFile: Path?): GeneratorConfig? = configFile?.let(GeneratorConfigLoader::load)

    /**
     * Resolves the effective log level via [LogLevelResolver], applies it through
     * [applyLogLevel], and executes [runner].
     *
     * @param runner configured runner to execute
     * @param config declarative configuration, typically from [loadConfig] (may be null)
     * @param overrides environment-specific overrides built by the caller
     * @param applyLogLevel invoked with the resolved log level (if any) before execution;
     *        the CLI uses it to set the Logback root logger, the Gradle plugin does not
     *        apply it (Gradle owns the logging backend inside the daemon)
     */
    public fun run(
        runner: TestGenerationRunner,
        config: GeneratorConfig?,
        overrides: TestGeneratorOverrides,
        applyLogLevel: (String) -> Unit = {},
    ): TestGenerationResult {
        LogLevelResolver.resolve(config, overrides)?.let(applyLogLevel)
        return runner.execute(config, overrides)
    }
}
