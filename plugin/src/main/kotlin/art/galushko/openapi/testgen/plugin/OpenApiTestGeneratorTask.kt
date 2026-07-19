package art.galushko.openapi.testgen.plugin

import art.galushko.openapi.testgen.config.GeneratorConfig
import art.galushko.openapi.testgen.config.TestGeneratorOverrides
import art.galushko.openapi.testgen.distribution.TestGenerationExecution
import art.galushko.openapi.testgen.distribution.TestGenerationResult
import art.galushko.openapi.testgen.distribution.TestGenerationRunner
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject

/**
 * Not cacheable: the default generator modes read the existing output directory (test-suite-writer
 * `MERGE`/`preventOverwriteCases`, template `SKIP_IF_EXISTS`), so output depends on prior output
 * state — a build-cache restore would replace user-preserved edits instead of merging them.
 * Remote spec URIs and `customTemplateDir` contents are also tracked by string value only.
 */
@DisableCachingByDefault(
    because = "generator merge/skip-if-exists modes read existing outputs; caching would replace user-preserved edits",
)
public abstract class OpenApiTestGeneratorTask @Inject constructor(
    objects: ObjectFactory,
) : DefaultTask() {

    init {
        val projectDirectory = layout.projectDirectory
        val loadedConfig = configFile.map { file ->
            TestGenerationExecution.loadConfig(file.asFile.toPath())
        }
        outputDir.convention(
            loadedConfig.map { config ->
                val configuredOutput = requireNotNull(config?.outputDir) {
                    "outputDir must be configured via overrides or config file"
                }
                projectDirectory.dir(configuredOutput)
            },
        )
        val effectiveSpecFile = specFile.orElse(
            loadedConfig.map { config ->
                config?.specFile.orEmpty()
            },
        ).orElse("")

        trackedSpecFiles.from(
            effectiveSpecFile.map { raw ->
                resolveLocalSpecFile(raw, projectDirectory)?.let(::listOf).orEmpty()
            },
        )
    }

    /** YAML config file with generator options. */
    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val configFile: RegularFileProperty

    /**
     * Path or URI of the OpenAPI 3.x or Swagger 2.0 specification. Kept as a string because
     * remote URIs are supported; local files are additionally content-tracked via
     * [trackedSpecFiles].
     */
    @get:Input
    @get:Optional
    public abstract val specFile: Property<String>

    /**
     * Content-tracked local spec files backing the effective spec setting. The effective value is
     * read from [specFile] first, then from [configFile], so spec edits invalidate up-to-date and
     * build-cache checks in both DSL and config-file-only builds. Empty for remote URIs.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val trackedSpecFiles: ConfigurableFileCollection

    @get:OutputDirectory
    @get:Optional
    public abstract val outputDir: DirectoryProperty

    @get:Input
    @get:Optional
    public abstract val generator: Property<String>

    @get:Input
    @get:Optional
    public abstract val generatorOptions: MapProperty<String, Any>

    @get:Nested
    public val testGenerationSettings: TestGenerationSettingsExtension =
        objects.newInstance(TestGenerationSettingsExtension::class.java)

    /** Configure test generation settings using type-safe DSL. */
    public fun testGenerationSettings(configure: TestGenerationSettingsExtension.() -> Unit) {
        configure(testGenerationSettings)
    }

    @get:Nested
    public val parserSettings: ParserSettingsExtension =
        objects.newInstance(ParserSettingsExtension::class.java)

    /** Configure parser settings using type-safe DSL. */
    public fun parserSettings(configure: ParserSettingsExtension.() -> Unit) {
        configure(parserSettings)
    }

    @get:Input
    @get:Optional
    public abstract val alwaysWriteTests: Property<Boolean>

    /**
     * Log level for generator logs.
     *
     * Deprecated: inside the Gradle daemon SLF4J is bound to Gradle's own logging backend, so
     * this property has no effect; use Gradle's `--info`/`--debug` instead. The value is still
     * validated and an invalid level fails the task.
     */
    @get:Input
    @get:Optional
    public abstract val logLevel: Property<String>

    @get:Inject
    protected abstract val layout: ProjectLayout

    @TaskAction
    public fun generate() {
        val config = TestGenerationExecution.loadConfig(configFile.orNull?.asFile?.toPath())
        val overrides = buildOverrides(config)

        if (logLevel.isPresent) {
            logger.warn(
                "openApiTestGenerator.logLevel has no effect inside Gradle; " +
                    "use --info/--debug or configure Gradle logging instead."
            )
        }

        val runner = TestGenerationRunner.withDefaults(
            reporter = GradleReporter(logger),
        )

        when (val result = TestGenerationExecution.run(runner, config, overrides)) {
            is TestGenerationResult.Success -> {
                // Success already logged by runner
            }
            is TestGenerationResult.Failure -> {
                throw IllegalStateException(result.message)
            }
        }
    }

    private fun buildOverrides(config: GeneratorConfig?): TestGeneratorOverrides {
        val outputPath = outputDir.orNull?.asFile?.toPath()
            ?: config?.outputDir?.let { layout.projectDirectory.file(it).asFile.toPath() }
        return TestGeneratorOverrides(
            logLevel = logLevel.orNull,
            specFile = (specFile.orNull ?: config?.specFile)?.let { resolveSpecFile(it) },
            outputDir = outputPath,
            // The extension's convention is "" (needed for source-set auto-wiring); treat it as
            // unset so a generator declared in the YAML config file is not shadowed.
            generatorId = generator.orNull?.takeIf { it.isNotBlank() },
            generatorOptions = generatorOptions.getOrElse(emptyMap()),
            testGenerationSettings = testGenerationSettings.buildTestGenerationSettingsMap(),
            alwaysWriteTests = alwaysWriteTests.orNull,
            parserSettings = parserSettings.buildParserSettingsMap(),
        )
    }

    private fun resolveSpecFile(raw: String): String {
        var effectiveInputSpec: String = raw
        val specPath = layout.projectDirectory.file(effectiveInputSpec).asFile.toPath()
        if (Files.exists(specPath)) {
            effectiveInputSpec = specPath.toAbsolutePath().toString()
        }
        logger.lifecycle("Parsing OpenAPI/Swagger spec: {}", effectiveInputSpec)
        return effectiveInputSpec
    }
}

private fun resolveLocalSpecFile(raw: String, projectDirectory: Directory): File? {
    if (raw.isBlank()) return null

    val uri = runCatching { URI(raw) }.getOrNull()
    val candidate = when {
        uri?.scheme == "file" -> runCatching { Path.of(uri).toFile() }.getOrNull()
        uri?.scheme != null && uri.scheme.length > 1 -> null
        else -> projectDirectory.file(raw).asFile
    }
    return candidate?.takeIf { it.isFile }
}
