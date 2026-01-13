package art.galushko.openapi.testgen.distribution

import art.galushko.openapi.testgen.config.GeneratorConfig
import art.galushko.openapi.testgen.config.ModuleSettingsExtractor
import art.galushko.openapi.testgen.config.TestGenerationEngine
import art.galushko.openapi.testgen.config.TestGenerationModule
import art.galushko.openapi.testgen.config.TestGenerationSettings
import art.galushko.openapi.testgen.config.TestGeneratorExecutionOptionsFactory
import art.galushko.openapi.testgen.config.TestGeneratorOverrides
import art.galushko.openapi.testgen.pattern.support.PatternModuleSettingsExtractor
import art.galushko.openapi.testgen.pattern.value.PatternGenerationOptions

/**
 * Main entry point for test generation execution.
 *
 * Encapsulates the common execution flow shared between CLI and Gradle plugin:
 * 1. Create execution options by merging config and overrides
 * 2. Extract module settings (e.g., pattern generation options)
 * 3. Create modules with extracted settings
 * 4. Generate test report via [TestGenerationEngine]
 * 5. Write test artifacts if successful or `alwaysWriteTests` is enabled
 *
 * Use [Builder] for custom configuration or [withDefaults] for standard distribution setup.
 */
public class TestGenerationRunner private constructor(
    private val reporter: TestGenerationReporter,
    private val moduleExtractors: List<ModuleSettingsExtractor>,
    private val defaultSettings: TestGenerationSettings,
    private val moduleFactory: ModuleFactory,
) {

    /**
     * Executes test generation with the provided configuration and overrides.
     *
     * @param config declarative configuration from YAML file (may be null)
     * @param overrides environment-specific overrides from CLI/Gradle
     * @return [TestGenerationResult.Success] if tests were generated successfully,
     *         [TestGenerationResult.Failure] if generation failed
     *
     * Artifacts are written only when generation succeeds, or when
     * `alwaysWriteTests` is enabled in execution options.
     */
    public fun execute(
        config: GeneratorConfig?,
        overrides: TestGeneratorOverrides,
    ): TestGenerationResult {
        val executionOptions = TestGeneratorExecutionOptionsFactory.fromConfig(
            config = config,
            overrides = overrides,
            moduleExtractors = moduleExtractors,
            defaultTestGenerationSettings = defaultSettings,
        )

        // Build modules dynamically using module factory (allows pattern options injection)
        val resolvedModules = moduleFactory.createModules(executionOptions)

        val artifactGenerator = TestGenerationEngine.createArtifactGenerator(executionOptions, modules = resolvedModules)
        val generationReport = TestGenerationEngine.generateReport(executionOptions, modules = resolvedModules)

        val formattedReport = reporter.formatReport(generationReport)
        reporter.logInfo(formattedReport)

        return if (generationReport.errors.isEmpty() || executionOptions.alwaysWriteTests) {
            artifactGenerator.generateTests(generationReport.successfulSuites)
            reporter.logInfo("OpenAPI tests generated")
            TestGenerationResult.Success(
                report = generationReport,
                testsWritten = true,
            )
        } else {
            val message = "Test Generation failed due to errors. Consider using --always-write-test to force writing tests"
            TestGenerationResult.Failure(
                report = generationReport,
                message = message,
            )
        }
    }

    /**
     * Factory interface for creating modules with resolved settings.
     */
    public fun interface ModuleFactory {
        public fun createModules(
            options: art.galushko.openapi.testgen.config.TestGeneratorExecutionOptions,
        ): List<TestGenerationModule>
    }

    /**
     * Builder for constructing [TestGenerationRunner] with custom configuration.
     */
    public class Builder {
        private var reporter: TestGenerationReporter? = null
        private var modules: MutableList<TestGenerationModule> = mutableListOf()
        private var moduleExtractors: MutableList<ModuleSettingsExtractor> = mutableListOf()
        private var defaultSettings: TestGenerationSettings? = null
        private var moduleFactory: ModuleFactory? = null

        /**
         * Sets the reporter for output and logging.
         */
        public fun reporter(reporter: TestGenerationReporter): Builder = apply {
            this.reporter = reporter
        }

        /**
         * Sets the complete list of modules.
         */
        public fun modules(modules: List<TestGenerationModule>): Builder = apply {
            this.modules = modules.toMutableList()
        }

        /**
         * Adds a single module to the list.
         */
        public fun addModule(module: TestGenerationModule): Builder = apply {
            this.modules.add(module)
        }

        /**
         * Sets the complete list of module extractors.
         */
        public fun moduleExtractors(extractors: List<ModuleSettingsExtractor>): Builder = apply {
            this.moduleExtractors = extractors.toMutableList()
        }

        /**
         * Adds a single module extractor.
         */
        public fun addModuleExtractor(extractor: ModuleSettingsExtractor): Builder = apply {
            this.moduleExtractors.add(extractor)
        }

        /**
         * Sets the default test generation settings.
         */
        public fun defaultSettings(settings: TestGenerationSettings): Builder = apply {
            this.defaultSettings = settings
        }

        /**
         * Sets a custom module factory for dynamic module creation.
         *
         * This allows creating modules with settings extracted from execution options.
         */
        public fun moduleFactory(factory: ModuleFactory): Builder = apply {
            this.moduleFactory = factory
        }

        /**
         * Builds the [TestGenerationRunner] instance.
         *
         * @throws IllegalStateException if reporter is not set
         */
        public fun build(): TestGenerationRunner {
            val reporter = this.reporter
                ?: throw IllegalStateException("Reporter must be set")
            val settings = defaultSettings ?: DistributionDefaults.settings()

            // If no module factory is provided, use the static modules list
            val factory = moduleFactory ?: ModuleFactory { modules }

            return TestGenerationRunner(
                reporter = reporter,
                moduleExtractors = moduleExtractors,
                defaultSettings = settings,
                moduleFactory = factory,
            )
        }
    }

    public companion object {
        /**
         * Creates a new [Builder] for constructing a [TestGenerationRunner].
         */
        public fun builder(): Builder = Builder()

        /**
         * Creates a [TestGenerationRunner] with all distribution defaults.
         *
         * This is the simplest way to create a runner for standard use cases.
         * Uses:
         * - [DistributionDefaults.extractors] for module extractors
         * - [DistributionDefaults.settings] for default settings
         * - Dynamic module factory that creates modules with extracted pattern options
         *
         * @param reporter the reporter for output and logging
         * @return configured [TestGenerationRunner] ready for execution
         */
        public fun withDefaults(reporter: TestGenerationReporter): TestGenerationRunner {
            return builder()
                .reporter(reporter)
                .moduleExtractors(DistributionDefaults.extractors())
                .defaultSettings(DistributionDefaults.settings())
                .moduleFactory { options ->
                    val patternOptions = options.moduleSettings
                        .get<PatternGenerationOptions>(PatternModuleSettingsExtractor.SETTINGS_KEY)
                        ?: PatternGenerationOptions()
                    DistributionDefaults.modules(patternOptions)
                }
                .build()
        }
    }
}
