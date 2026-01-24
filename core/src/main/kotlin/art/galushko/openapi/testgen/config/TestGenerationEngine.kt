package art.galushko.openapi.testgen.config

import art.galushko.openapi.testgen.filtering.IgnoreConfigHandler
import art.galushko.openapi.testgen.filtering.IncludeOperationsHandler
import art.galushko.openapi.testgen.generation.TestGenerationProcessor
import art.galushko.openapi.testgen.generation.TestGeneratorConfigurer.createTestGenerationProcessor
import art.galushko.openapi.testgen.generation.TestGeneratorConfigurer.createTestSuiteGenerator
import art.galushko.openapi.testgen.generator.ArtifactGeneratorFactory
import art.galushko.openapi.testgen.generator.ArtifactGeneratorRegistry
import art.galushko.openapi.testgen.model.error.GenerationReport
import art.galushko.openapi.testgen.model.error.ErrorHandlingConfig
import art.galushko.openapi.testgen.openapi.OpenApiSpecParser
import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGeneratorFactory
import art.galushko.openapi.testgen.example.openapi.SchemaMerger
import art.galushko.openapi.testgen.example.openapi.SchemaMergerOptions
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import art.galushko.openapi.testgen.rules.ManualRuleRegistry
import art.galushko.openapi.testgen.spi.ArtifactGenerator
import io.swagger.v3.oas.models.OpenAPI

/**
 * Shared facade for wiring core test generation components from [TestGeneratorExecutionOptions].
 *
 * This entry point is used by CLI, Gradle plugin, and embedded usage to avoid duplicating
 * orchestration logic (parsing, wiring, execution).
 *
 * Determinism: module ordering is stable (sorted by id, then class name), and provider/rule ordering
 * is delegated to [art.galushko.openapi.testgen.generation.TestGeneratorConfigurer] and [ManualRuleRegistry].
 */
public object TestGenerationEngine {

    /**
     * Generates a [GenerationReport] from [TestGeneratorExecutionOptions] without exposing orchestration internals.
     *
     * Inputs: resolved execution options and optional feature modules.
     * Output: a [GenerationReport] capturing successes, partials, and failures.
     * Errors: throws on invalid module configuration or OpenAPI parse failures.
     */
    public fun generateReport(
        options: TestGeneratorExecutionOptions,
        modules: List<TestGenerationModule> = emptyList(),
    ): GenerationReport {
        val resolvedModules = sortAndValidateModules(modules)
        val openApi = parseOpenApi(options)
        val processor = createProcessor(options, resolvedModules)
        val errorHandlingConfig = createErrorHandlingConfig(options)
        return processor.generateReport(openApi, errorHandlingConfig)
    }

    /**
     * Parses OpenAPI specification according to [TestGeneratorExecutionOptions].
     */
    internal fun parseOpenApi(options: TestGeneratorExecutionOptions): OpenAPI =
        OpenApiSpecParser.parseOpenApi(options.specFile)

    /**
     * Creates [TestGenerationProcessor] configured according to [TestGeneratorExecutionOptions].
     */
    internal fun createProcessor(
        options: TestGeneratorExecutionOptions,
        modules: List<TestGenerationModule> = emptyList(),
    ): TestGenerationProcessor {
        val resolvedModules = sortAndValidateModules(modules)

        val schemaMerger = SchemaMerger(SchemaMergerOptions(options.testGenerationSettings.maxMergedSchemaDepth))
        val schemaValueProviders = mergeSchemaValueProviders(options, resolvedModules)
        val schemaExampleValueGenerator = SchemaExampleValueGeneratorFactory(
            extraProviders = schemaValueProviders,
            schemaMerger = schemaMerger,
        ).create(options.testGenerationSettings.exampleValues)

        val extraSimpleSchemaRules = resolvedModules.flatMap { it.extraSimpleSchemaRules(options) }
        val extraAuthRules = resolvedModules.flatMap { it.extraAuthRules(options) }
        val ruleRegistry = ManualRuleRegistry(
            extraSimpleSchemaRules = extraSimpleSchemaRules,
            extraAuthRules = extraAuthRules,
        )

        val testSuiteGenerator = createTestSuiteGenerator(
            testGenerationSettings = options.testGenerationSettings,
            schemaExampleValueGenerator = schemaExampleValueGenerator,
            schemaMerger = schemaMerger,
            ruleRegistry = ruleRegistry,
        )
        val includeOperationsHandler = IncludeOperationsHandler(options.testGenerationSettings.includeOperations)
        val ignoreConfigHandler = IgnoreConfigHandler(options.testGenerationSettings.ignoreTestCases)

        return createTestGenerationProcessor(testSuiteGenerator, includeOperationsHandler, ignoreConfigHandler)
    }

    /**
     * Creates [ArtifactGenerator] for the configured generator id and options.
     *
     * Inputs: resolved options and optional feature modules contributing generator factories.
     * Output: configured [ArtifactGenerator] instance.
     * Errors: throws if generator id is unknown or duplicate factories are provided by modules.
     */
    public fun createArtifactGenerator(
        options: TestGeneratorExecutionOptions,
        modules: List<TestGenerationModule> = emptyList(),
    ): ArtifactGenerator {
        val resolvedModules = sortAndValidateModules(modules)
        val extraFactories = mergeArtifactGeneratorFactories(resolvedModules)
        val registry = ArtifactGeneratorRegistry(extraFactories)
        return registry.create(
            generatorId = options.generatorId,
            outputDir = options.outputDir.toFile(),
            options = options.generatorOptions,
        )
    }

    /**
     * Builds [ErrorHandlingConfig] from [TestGeneratorExecutionOptions].
     */
    internal fun createErrorHandlingConfig(options: TestGeneratorExecutionOptions): ErrorHandlingConfig =
        ErrorHandlingConfig(
            mode = options.testGenerationSettings.errorMode,
            maxErrors = options.testGenerationSettings.maxErrors,
        )

    internal fun sortAndValidateModules(modules: List<TestGenerationModule>): List<TestGenerationModule> {
        if (modules.isEmpty()) return emptyList()

        val blankIds = modules.filter { it.id.isBlank() }
        require(blankIds.isEmpty()) {
            "TestGenerationModule.id must not be blank for: ${blankIds.joinToString { it::class.java.name }}"
        }

        val duplicateIds = modules.groupBy { it.id }.filter { it.value.size > 1 }
        require(duplicateIds.isEmpty()) {
            val details = duplicateIds.entries
                .sortedBy { it.key }
                .joinToString { (id, mods) ->
                    "'$id' -> ${mods.joinToString { it::class.java.name }}"
                }
            "Duplicate TestGenerationModule ids: $details"
        }

        return modules.sortedWith(compareBy<TestGenerationModule> { it.id }.thenBy { it::class.java.name })
    }

    private fun mergeSchemaValueProviders(
        options: TestGeneratorExecutionOptions,
        modules: List<TestGenerationModule>,
    ): Map<String, SchemaValueProvider> {
        if (modules.isEmpty()) return emptyMap()

        val result = LinkedHashMap<String, SchemaValueProvider>()
        val owners = LinkedHashMap<String, TestGenerationModule>()

        for (module in modules) {
            for ((id, provider) in module.schemaValueProviders(options)) {
                require(id.isNotBlank()) {
                    "SchemaValueProvider id must not be blank (module='${module.id}', provider=${provider::class.java.name})"
                }
                val existingOwner = owners[id]
                if (existingOwner != null) {
                    throw IllegalArgumentException(
                        "Schema value provider '$id' is contributed by multiple modules: " +
                            "'${existingOwner.id}' (${existingOwner::class.java.name}) and " +
                            "'${module.id}' (${module::class.java.name})"
                    )
                }
                owners[id] = module
                result[id] = provider
            }
        }

        return result.toMap()
    }

    private fun mergeArtifactGeneratorFactories(modules: List<TestGenerationModule>): List<ArtifactGeneratorFactory> {
        if (modules.isEmpty()) return emptyList()

        val factoriesById = LinkedHashMap<String, Pair<TestGenerationModule, ArtifactGeneratorFactory>>()

        for (module in modules) {
            for (factory in module.artifactGeneratorFactories()) {
                val existing = factoriesById[factory.id]
                if (existing != null) {
                    throw IllegalArgumentException(
                        "Artifact generator factory '${factory.id}' is contributed by multiple modules: " +
                            "'${existing.first.id}' (${existing.second::class.java.name}) and " +
                            "'${module.id}' (${factory::class.java.name})"
                    )
                }
                factoriesById[factory.id] = module to factory
            }
        }

        return factoriesById.values.map { it.second }.sortedBy { it.id }
    }
}
