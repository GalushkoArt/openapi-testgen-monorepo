package art.galushko.openapi.testgen.plugin

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/**
 * User-configurable extension for the OpenAPI Test Generator Gradle plugin.
 */
@Suppress("MagicNumber")
public open class TestGeneratorExtension @Inject constructor(
    objects: ObjectFactory,
) {
    /** Optional path to YAML config with all options. */
    @get:Input
    public val configFile: Property<String> = objects.property(String::class.java)

    /** Path to the OpenAPI specification file (relative to project root or absolute). */
    @get:Input
    public val specFile: Property<String> = objects.property(String::class.java)

    /** Directory where generated tests will be written. */
    @get:Input
    public val outputDir: DirectoryProperty = objects.directoryProperty()

    /** Test framework to target (e.g., "template"). */
    @get:Input
    public val generator: Property<String> = objects.property(String::class.java).convention("")

    /** Options passed to the underlying generator. */
    @get:Input
    public val generatorOptions: MapProperty<String, Any?> = objects.mapProperty(String::class.java, Any::class.java)

    /** Type-safe test generation settings. */
    @get:Nested
    public val testGenerationSettings: TestGenerationSettingsExtension = objects.newInstance(TestGenerationSettingsExtension::class.java)

    /** Configure test generation settings using type-safe DSL. */
    public fun testGenerationSettings(configure: TestGenerationSettingsExtension.() -> Unit) {
        configure(testGenerationSettings)
    }

    /** If true, generation task will only run when invoked manually (no implicit wiring). */
    @get:Input
    public val manualOnly: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    /** Flag to force writing tests even if there are errors on generation. */
    @get:Input
    public val alwaysWriteTests: Property<Boolean> = objects.property(Boolean::class.java).convention(null)

    /** Log level for generator logs (SLF4J backend). */
    @get:Input
    @get:Optional
    public val logLevel: Property<String> = objects.property(String::class.java).convention(null)
}
