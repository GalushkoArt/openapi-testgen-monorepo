import org.gradle.api.provider.Property

/**
 * Extension for configuring testgen library conventions.
 */
abstract class TestgenLibraryExtension {
    abstract val platformPublishing: Property<com.vanniktech.maven.publish.Platform>
}
