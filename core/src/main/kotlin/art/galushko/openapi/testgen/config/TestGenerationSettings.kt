package art.galushko.openapi.testgen.config

import art.galushko.openapi.testgen.config.IgnoreConfigValidator.validateIgnoreConfig
import art.galushko.openapi.testgen.example.config.ConfigExtractors
import art.galushko.openapi.testgen.example.config.ExampleValueSettings
import art.galushko.openapi.testgen.example.openapi.SchemaMergerOptions.Companion.DEFAULT_MAX_MERGED_SCHEMA_DEPTH
import art.galushko.openapi.testgen.generation.TestGeneratorConfigurer.DEFAULT_MAX_ERRORS
import art.galushko.openapi.testgen.generation.TestGeneratorConfigurer.DEFAULT_MAX_SCHEMA_COMBINATIONS
import art.galushko.openapi.testgen.generation.TestGeneratorConfigurer.DEFAULT_MAX_SCHEMA_DEPTH
import art.galushko.openapi.testgen.generation.TestGeneratorConfigurer.DEFAULT_MAX_TEST_CASES_PER_OPERATION
import art.galushko.openapi.testgen.model.error.ErrorMode
import org.slf4j.LoggerFactory

/**
 * Typed settings for test case generation behavior.
 *
 * Inputs: these settings are typically resolved from YAML config and CLI/Gradle overrides via
 * [TestGeneratorExecutionOptionsFactory.fromConfig].
 * Validation: numeric budgets must be positive and ignore config must be structurally valid.
 * Determinism: ordering-sensitive settings (ignore lists, provider order) are preserved as provided.
 *
 * @property ignoreTestCases map of ignore rules keyed by path/method/test case
 * @property ignoreSchemaValidationRules set of schema rule class names to ignore
 * @property ignoreAuthValidationRules set of auth rule class names to ignore
 * @property maxSchemaDepth maximum depth of schema validation
 * @property overrideBasicTestData map of [art.galushko.openapi.testgen.testdata.BasicTestDataProvider] method names to override values
 * @property maxMergedSchemaDepth maximum depth used when merging composed schemas (allOf/anyOf/oneOf)
 * @property maxSchemaCombinations maximum schema combinations per parameter/property
 * @property maxTestCasesPerOperation maximum test cases generated per operation
 * @property validSecurityValues a map of security scheme names to their valid values
 * @property errorMode error mode for test generation
 * @property includeValidCase include the baseline valid case (2xx) in output suites
 * @property maxErrors maximum number of errors to collect before test generation fails
 * @property exampleValues configuration for schema-derived example value generation
 */
public data class TestGenerationSettings(
    val ignoreTestCases: Map<String, Any> = emptyMap(),
    val ignoreSchemaValidationRules: Set<String> = emptySet(),
    val ignoreAuthValidationRules: Set<String> = emptySet(),
    val maxSchemaDepth: Int = DEFAULT_MAX_SCHEMA_DEPTH,
    val overrideBasicTestData: Map<String, String> = emptyMap(),
    val maxMergedSchemaDepth: Int = DEFAULT_MAX_MERGED_SCHEMA_DEPTH,
    val maxSchemaCombinations: Int = DEFAULT_MAX_SCHEMA_COMBINATIONS,
    val maxTestCasesPerOperation: Int = DEFAULT_MAX_TEST_CASES_PER_OPERATION,
    val validSecurityValues: Map<String, String> = emptyMap(),
    val errorMode: ErrorMode = ErrorMode.COLLECT_ALL,
    val includeValidCase: Boolean = false,
    val maxErrors: Int = DEFAULT_MAX_ERRORS,
    val exampleValues: ExampleValueSettings = ExampleValueSettings(),
) {

    init {
        require(maxSchemaDepth > 0) {
            "maxSchemaDepth must be positive, was $maxSchemaDepth"
        }
        require(maxMergedSchemaDepth > 0) {
            "maxMergedSchemaDepth must be positive, was $maxMergedSchemaDepth"
        }
        require(maxSchemaCombinations > 0) {
            "maxSchemaCombinations must be positive, was $maxSchemaCombinations"
        }
        require(maxTestCasesPerOperation > 0) {
            "maxTestCasesPerOperation must be positive, was $maxTestCasesPerOperation"
        }
        require(maxErrors > 0) {
            "maxErrors must be positive, was $maxErrors"
        }
        validateIgnoreConfig(ignoreTestCases)
    }

    public companion object {
        private val log = LoggerFactory.getLogger(TestGenerationSettings::class.java)
        private val ignoreTestCasesName = TestGenerationSettings::ignoreTestCases.name
        private val overrideBasicTestDataName = TestGenerationSettings::overrideBasicTestData.name
        private val validSecurityValuesName = TestGenerationSettings::validSecurityValues.name
        private val ignoreSchemaValidationRulesName = TestGenerationSettings::ignoreSchemaValidationRules.name
        private val ignoreAuthValidationRulesName = TestGenerationSettings::ignoreAuthValidationRules.name
        private val maxSchemaDepthName = TestGenerationSettings::maxSchemaDepth.name
        private val maxMergedSchemaDepthName = TestGenerationSettings::maxMergedSchemaDepth.name
        private val maxSchemaCombinationsName = TestGenerationSettings::maxSchemaCombinations.name
        private val maxTestCasesPerOperationName = TestGenerationSettings::maxTestCasesPerOperation.name
        private val maxErrorsName = TestGenerationSettings::maxErrors.name
        private val errorModeName = TestGenerationSettings::errorMode.name
        private val includeValidCaseName = TestGenerationSettings::includeValidCase.name
        private val exampleValuesName = TestGenerationSettings::exampleValues.name

        /**
         * Builds [TestGenerationSettings] from a map-based configuration.
         *
         * @param default default values used for missing entries
         * @throws art.galushko.openapi.testgen.example.config.ConfigurationException if a field has an invalid type
         * @throws IllegalArgumentException if validation fails (e.g., non-positive budgets)
         */
        @Suppress("CyclomaticComplexMethod")
        public fun fromMap(map: Map<String, Any?>, default: TestGenerationSettings = TestGenerationSettings()): TestGenerationSettings {
            val mutableMap = map.toMutableMap()

            val exampleValues = ConfigExtractors.extractStringAnyNullableMap(exampleValuesName, mutableMap)
                ?.let { ExampleValueSettings.fromMap(it, default.exampleValues) }
                ?: default.exampleValues

            val result = TestGenerationSettings(
                ignoreTestCases = ConfigExtractors.extractStringAnyMap(
                    ignoreTestCasesName, mutableMap
                ) ?: default.ignoreTestCases,
                overrideBasicTestData = ConfigExtractors.extractStringStringMap(
                    overrideBasicTestDataName, mutableMap
                ) ?: default.overrideBasicTestData,
                validSecurityValues = ConfigExtractors.extractStringStringMap(
                    validSecurityValuesName, mutableMap
                ) ?: default.validSecurityValues,
                ignoreSchemaValidationRules = ConfigExtractors.extractStringSet(
                    ignoreSchemaValidationRulesName, mutableMap
                ) ?: default.ignoreSchemaValidationRules,
                ignoreAuthValidationRules = ConfigExtractors.extractStringSet(
                    ignoreAuthValidationRulesName, mutableMap
                ) ?: default.ignoreAuthValidationRules,
                maxSchemaDepth = ConfigExtractors.extractInteger(
                    maxSchemaDepthName, mutableMap
                ) ?: default.maxSchemaDepth,
                maxMergedSchemaDepth = ConfigExtractors.extractInteger(
                    maxMergedSchemaDepthName, mutableMap
                ) ?: default.maxMergedSchemaDepth,
                maxSchemaCombinations = ConfigExtractors.extractInteger(
                    maxSchemaCombinationsName, mutableMap
                ) ?: default.maxSchemaCombinations,
                maxTestCasesPerOperation = ConfigExtractors.extractInteger(
                    maxTestCasesPerOperationName, mutableMap
                ) ?: default.maxTestCasesPerOperation,
                maxErrors = ConfigExtractors.extractInteger(
                    maxErrorsName, mutableMap
                ) ?: default.maxErrors,
                errorMode = ConfigExtractors.extractEnum(
                    errorModeName, mutableMap
                ) ?: default.errorMode,
                includeValidCase = ConfigExtractors.extractBoolean(
                    includeValidCaseName, mutableMap
                ) ?: default.includeValidCase,
                exampleValues = exampleValues,
            )

            // Warn about unused keys
            if (mutableMap.isNotEmpty()) {
                log.warn(
                    "Unused configuration entries in testGenerationSettings:\n {}",
                    mutableMap.entries.sortedBy { it.key }.joinToString("\n ")
                )
            }

            return result
        }
    }
}
