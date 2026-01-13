package art.galushko.openapi.testgen.plugin

import art.galushko.openapi.testgen.config.GeneratorConfig
import art.galushko.openapi.testgen.config.GeneratorConfigLoader
import art.galushko.openapi.testgen.config.TestGeneratorOverrides
import art.galushko.openapi.testgen.distribution.TestGenerationResult
import art.galushko.openapi.testgen.distribution.TestGenerationRunner
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import java.nio.file.Files
import javax.inject.Inject

public abstract class OpenApiTestGeneratorTask @Inject constructor(
    objects: ObjectFactory,
) : DefaultTask() {

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val configFile: Property<String>

    @get:Input
    @get:Optional
    public abstract val specFile: Property<String>

    @get:OutputDirectory
    @get:Optional
    public abstract val outputDir: DirectoryProperty

    @get:Input
    @get:Optional
    public abstract val generator: Property<String>

    @get:Input
    @get:Optional
    public abstract val generatorOptions: MapProperty<String, Any?>

    @get:Nested
    public val testGenerationSettings: TestGenerationSettingsExtension =
        objects.newInstance(TestGenerationSettingsExtension::class.java)

    /** Configure test generation settings using type-safe DSL. */
    public fun testGenerationSettings(configure: TestGenerationSettingsExtension.() -> Unit) {
        configure(testGenerationSettings)
    }

    @get:Input
    @get:Optional
    public abstract val alwaysWriteTests: Property<Boolean>

    /**
     * Log level for generator logs (SLF4J backend).
     *
     * Note: This does not affect Gradle's own logging level. It is intended to control
     * the generator's SLF4J output when a backend (e.g. Logback) is present.
     */
    @get:Input
    @get:Optional
    public abstract val logLevel: Property<String>

    @TaskAction
    public fun generate() {
        val config = loadConfig()
        val overrides = buildOverrides(config)

        configureLogging(config, overrides)

        val runner = TestGenerationRunner.withDefaults(
            reporter = GradleReporter(project.logger),
        )

        when (val result = runner.execute(config, overrides)) {
            is TestGenerationResult.Success -> {
                // Success already logged by runner
            }
            is TestGenerationResult.Failure -> {
                throw IllegalStateException(result.message)
            }
        }
    }

    private fun configureLogging(
        config: GeneratorConfig?,
        overrides: TestGeneratorOverrides,
    ) {
        val level = (overrides.logLevel ?: config?.logLevel)?.trim()?.uppercase() ?: return
        val desiredLevel = parseLogbackLevel(level)

        val context = LoggerFactory.getLogger(ROOT_LOGGER_NAME) as? Logger
        context?.level = desiredLevel
    }

    private fun parseLogbackLevel(value: String): Level {
        return try {
            Level.valueOf(value)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid logLevel '$value'. Expected one of ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF.",
                e,
            )
        }
    }

    private fun buildOverrides(config: GeneratorConfig?): TestGeneratorOverrides {
        val outputPath = outputDir.orNull?.asFile?.toPath()
            ?: config?.outputDir?.let { project.layout.projectDirectory.file(it).asFile.toPath() }
        return TestGeneratorOverrides(
            logLevel = logLevel.orNull,
            specFile = (specFile.orNull ?: config?.specFile)?.let { resolveSpecFile(it) },
            outputDir = outputPath,
            generatorId = generator.orNull,
            generatorOptions = generatorOptions.getOrElse(emptyMap()),
            testGenerationSettings = testGenerationSettings.buildTestGenerationSettingsMap(),
            alwaysWriteTests = alwaysWriteTests.orNull,
        )
    }

    private fun resolveSpecFile(raw: String): String {
        var effectiveInputSpec: String = raw
        val specPath = project.layout.projectDirectory.file(effectiveInputSpec).asFile.toPath()
        if (Files.exists(specPath)) {
            effectiveInputSpec = specPath.toAbsolutePath().toString()
        }
        project.logger.lifecycle("Parsing OpenAPI spec: {}", effectiveInputSpec)
        return effectiveInputSpec
    }

    private fun loadConfig(): GeneratorConfig? = configFile.orNull
        ?.let { project.layout.projectDirectory.file(it).asFile.toPath() }
        ?.let { GeneratorConfigLoader.load(it) }
}
