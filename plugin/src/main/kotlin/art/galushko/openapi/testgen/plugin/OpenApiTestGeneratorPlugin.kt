package art.galushko.openapi.testgen.plugin

import art.galushko.openapi.testgen.distribution.TestGenerationExecution
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

public class OpenApiTestGeneratorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "openApiTestGenerator",
            TestGeneratorExtension::class.java,
            project.objects,
        )

        val testGenerationTask = registerGenerateTask(project, extension)
        wireGeneratedOutputsIntoTestSourceSets(project, extension, testGenerationTask)
    }

    private fun registerGenerateTask(
        project: Project,
        extension: TestGeneratorExtension,
    ): TaskProvider<OpenApiTestGeneratorTask> {
        // Captured at configuration time; Directory is safe to hold in providers under the
        // configuration cache (unlike Project).
        val projectDir: Directory = project.layout.projectDirectory

        return project.tasks.register(
            "generateOpenApiTests",
            OpenApiTestGeneratorTask::class.java,
        ) { task ->
            task.group = "verification"
            task.description = "Generates tests from an OpenAPI or Swagger specification"

            task.configFile.set(extension.configFile.map { projectDir.file(it) })
            task.specFile.set(extension.specFile)
            task.outputDir.set(
                extension.outputDir.orElse(
                    task.configFile.map { configFile ->
                        val configuredOutput = requireNotNull(
                            TestGenerationExecution.loadConfig(configFile.asFile.toPath())?.outputDir,
                        ) {
                            "outputDir must be configured via overrides or config file"
                        }
                        projectDir.dir(configuredOutput)
                    },
                ),
            )
            task.generator.set(extension.generator)
            task.generatorOptions.set(extension.generatorOptions)
            task.alwaysWriteTests.set(extension.alwaysWriteTests)
            @Suppress("DEPRECATION")
            task.logLevel.set(extension.logLevel)

            wireTestGenerationSettings(task.testGenerationSettings, extension.testGenerationSettings)
            wireParserSettings(task.parserSettings, extension.parserSettings)
        }
    }

    /**
     * Auto-wiring decisions are providers resolved lazily at task-graph time (no afterEvaluate).
     * Directory values come from the extension's outputDir (no task producer, so they can be
     * queried at configuration time); the producing-task dependency is attached separately via
     * `builtBy`, only when auto-running is enabled (non-manual mode).
     */
    private fun wireGeneratedOutputsIntoTestSourceSets(
        project: Project,
        extension: TestGeneratorExtension,
        testGenerationTask: TaskProvider<OpenApiTestGeneratorTask>,
    ) {
        val providers = project.providers
        val autoWiring = extension.generator.zip(extension.manualOnly) { generator, manualOnly ->
            generator to manualOnly
        }

        fun outputDirWhen(condition: (generatorId: String, manualOnly: Boolean) -> Boolean): Provider<List<Directory>> =
            autoWiring.flatMap { (generatorId, manualOnly) ->
                if (condition(generatorId, manualOnly)) {
                    extension.outputDir.map { listOf(it) }
                } else {
                    providers.provider { emptyList() }
                }
            }

        fun generateTaskWhen(condition: (generatorId: String, manualOnly: Boolean) -> Boolean): Provider<List<Any>> =
            autoWiring.map { (generatorId, manualOnly) ->
                if (condition(generatorId, manualOnly)) listOf(testGenerationTask) else emptyList()
            }

        val generatedTestSources = project.files(
            outputDirWhen { generatorId, _ -> generatorId == TEMPLATE_GENERATOR_ID }
        ).builtBy(
            generateTaskWhen { generatorId, manualOnly -> generatorId == TEMPLATE_GENERATOR_ID && !manualOnly }
        )

        val generatedTestResources = project.files(
            outputDirWhen { generatorId, manualOnly -> generatorId == TEST_SUITE_WRITER_GENERATOR_ID && !manualOnly }
        ).builtBy(
            generateTaskWhen { generatorId, manualOnly -> generatorId == TEST_SUITE_WRITER_GENERATOR_ID && !manualOnly }
        )

        project.plugins.withId("java") { _ ->
            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            sourceSets.named("test") { testSet ->
                testSet.java.srcDir(generatedTestSources)
            }
            project.tasks.named(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME) {
                it.mustRunAfter(testGenerationTask)
            }
            project.tasks.named(JavaPlugin.PROCESS_TEST_RESOURCES_TASK_NAME, ProcessResources::class.java) {
                it.from(generatedTestResources)
                it.mustRunAfter(testGenerationTask)
            }
        }
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            // No SAM lambdas with Kotlin-plugin parameter types here: their synthetic method
            // signatures would break Gradle's class decoration when the Kotlin plugin (a
            // compileOnly dependency) is absent from the consumer's classpath.
            val kotlinExt = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
            kotlinExt.sourceSets.getByName("test").kotlin.srcDir(generatedTestSources)
            project.tasks.named("compileTestKotlin") {
                it.mustRunAfter(testGenerationTask)
            }
        }
    }

    /**
     * Live-links every task setting to its extension counterpart with `set(sourceProperty)`.
     *
     * Wiring is explicit (no reflection) so it stays configuration-cache friendly. When adding a
     * property to [TestGenerationSettingsExtension], extend this method and
     * [TestGenerationSettingsExtension.buildTestGenerationSettingsMap]; a drift-guard test
     * enumerates the extension's properties to catch omissions.
     */
    private fun wireTestGenerationSettings(
        target: TestGenerationSettingsExtension,
        source: TestGenerationSettingsExtension,
    ) {
        target.includeOperations.set(source.includeOperations)
        target.ignoreTestCases.set(source.ignoreTestCases)
        target.ignoreSchemaValidationRules.set(source.ignoreSchemaValidationRules)
        target.ignoreAuthValidationRules.set(source.ignoreAuthValidationRules)
        target.maxSchemaDepth.set(source.maxSchemaDepth)
        target.overrideBasicTestData.set(source.overrideBasicTestData)
        target.maxSchemaCombinations.set(source.maxSchemaCombinations)
        target.maxMergedSchemaDepth.set(source.maxMergedSchemaDepth)
        target.maxTestCasesPerOperation.set(source.maxTestCasesPerOperation)
        target.validSecurityValues.set(source.validSecurityValues)
        target.errorMode.set(source.errorMode)
        target.includeValidCase.set(source.includeValidCase)
        target.maxErrors.set(source.maxErrors)
        target.exampleValues.set(source.exampleValues)
        target.patternGeneration.set(source.patternGeneration)
    }

    private fun wireParserSettings(
        target: ParserSettingsExtension,
        source: ParserSettingsExtension,
    ) {
        target.yamlCodePointLimit.set(source.yamlCodePointLimit)
        target.yamlMaxAliasesForCollections.set(source.yamlMaxAliasesForCollections)
        target.yamlAllowRecursiveKeys.set(source.yamlAllowRecursiveKeys)
        target.yamlNestingDepthLimit.set(source.yamlNestingDepthLimit)
    }

    private companion object {
        private const val TEMPLATE_GENERATOR_ID = "template"
        private const val TEST_SUITE_WRITER_GENERATOR_ID = "test-suite-writer"
    }
}
