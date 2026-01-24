package art.galushko.openapi.testgen.plugin

import art.galushko.openapi.testgen.model.error.ErrorMode
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/**
 * Gradle-native typed configuration block for test generation settings.
 *
 * Provides IDE autocomplete and compile-time validation for all test generation settings.
 */
public open class TestGenerationSettingsExtension @Inject constructor(
    objects: ObjectFactory,
) {
    /** Map of paths to HTTP methods to include. Empty means include all (default). */
    @get:Input
    @get:Optional
    public val includeOperations: MapProperty<String, Any> = objects.mapProperty(String::class.java, Any::class.java)

    /** Map of operation patterns to ignore during test generation. */
    @get:Input
    @get:Optional
    public val ignoreTestCases: MapProperty<String, Any> = objects.mapProperty(String::class.java, Any::class.java)

    /** Set of schema validation rule class names to ignore. */
    @get:Input
    @get:Optional
    public val ignoreSchemaValidationRules: SetProperty<String> = objects.setProperty(String::class.java)

    /** Set of auth validation rule class names to ignore. */
    @get:Input
    @get:Optional
    public val ignoreAuthValidationRules: SetProperty<String> = objects.setProperty(String::class.java)

    /** Maximum depth for recursive schema traversal. */
    @get:Input
    @get:Optional
    public val maxSchemaDepth: Property<Int> = objects.property(Int::class.java)

    /** Override values for basic test data generation. */
    @get:Input
    @get:Optional
    public val overrideBasicTestData: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java)

    /** Maximum number of schema combinations to generate. */
    @get:Input
    @get:Optional
    public val maxSchemaCombinations: Property<Int> = objects.property(Int::class.java)

    /** Maximum depth used when merging composed schemas (allOf/anyOf/oneOf). */
    @get:Input
    @get:Optional
    public val maxMergedSchemaDepth: Property<Int> = objects.property(Int::class.java)

    /** Maximum number of test cases per operation. */
    @get:Input
    @get:Optional
    public val maxTestCasesPerOperation: Property<Int> = objects.property(Int::class.java)

    /** Valid security values for authenticated requests (e.g., API keys, tokens). */
    @get:Input
    @get:Optional
    public val validSecurityValues: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java)

    /** Error handling mode: FAIL_FAST or COLLECT_ALL. */
    @get:Input
    @get:Optional
    public val errorMode: Property<ErrorMode> = objects.property(ErrorMode::class.java)

    /** Include the baseline valid case (2xx status) in generated test suites. */
    @get:Input
    @get:Optional
    public val includeValidCase: Property<Boolean> = objects.property(Boolean::class.java)

    /** Maximum number of errors to collect before stopping. */
    @get:Input
    @get:Optional
    public val maxErrors: Property<Int> = objects.property(Int::class.java)

    /** Schema-derived example value generation settings (raw map, passed to core as-is). */
    @get:Input
    @get:Optional
    public val exampleValues: MapProperty<String, Any> = objects.mapProperty(String::class.java, Any::class.java)

    /** Pattern generation options used by regexp-gen for pattern-based value generation. */
    @get:Input
    @get:Optional
    public val patternGeneration: MapProperty<String, Any> = objects.mapProperty(String::class.java, Any::class.java)

    public fun buildTestGenerationSettingsMap(): Map<String, Any?> {
        val settings = mutableMapOf<String, Any?>()

        fun add(key: String, provider: Provider<*>) {
            val value = provider.orNull
            val isValid = value != null &&
                (value !is Map<*, *> || value.isNotEmpty()) &&
                (value !is Collection<*> || value.isNotEmpty())

            if (isValid) {
                settings[key] = value
            }
        }

        add("includeOperations", includeOperations)
        add("validSecurityValues", validSecurityValues)
        add("errorMode", errorMode)
        add("includeValidCase", includeValidCase)
        add("maxErrors", maxErrors)
        add("maxSchemaDepth", maxSchemaDepth)
        add("ignoreTestCases", ignoreTestCases)
        add("ignoreSchemaValidationRules", ignoreSchemaValidationRules)
        add("ignoreAuthValidationRules", ignoreAuthValidationRules)
        add("overrideBasicTestData", overrideBasicTestData)
        add("maxSchemaCombinations", maxSchemaCombinations)
        add("maxMergedSchemaDepth", maxMergedSchemaDepth)
        add("maxTestCasesPerOperation", maxTestCasesPerOperation)
        add("exampleValues", exampleValues)
        add("patternGeneration", patternGeneration)

        return settings
    }
}
