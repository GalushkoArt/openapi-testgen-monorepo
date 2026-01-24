import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Extension for configuring testgen library conventions.
 */
abstract class TestgenQualityExtension {
    /**
     * Minimum code coverage percentage required by Kover.
     * Defaults to 0 (no minimum).
     */
    abstract val koverMinCoverage: Property<Int>

    abstract val koverDisabledForTestTasks: ListProperty<String>
}
