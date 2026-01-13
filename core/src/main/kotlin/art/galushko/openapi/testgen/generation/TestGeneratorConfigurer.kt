package art.galushko.openapi.testgen.generation

import art.galushko.openapi.testgen.config.TestGenerationSettings
import art.galushko.openapi.testgen.filtering.IgnoreConfigHandler
import art.galushko.openapi.testgen.generation.budget.TestCaseBudgetValidator
import art.galushko.openapi.testgen.generation.orchestration.OutcomeAggregator
import art.galushko.openapi.testgen.generation.orchestration.ProviderOrchestrator
import art.galushko.openapi.testgen.example.openapi.SchemaMerger
import art.galushko.openapi.testgen.providers.AuthTestCaseProviderForOperation
import art.galushko.openapi.testgen.providers.ParameterTestCaseProviderForOperation
import art.galushko.openapi.testgen.providers.RequestBodyTestCaseProviderForOperation
import art.galushko.openapi.testgen.providers.body.MissedRequiredRequestBodyTestProvider
import art.galushko.openapi.testgen.providers.body.RequestBodySchemaValidationTestProvider
import art.galushko.openapi.testgen.providers.parameter.MissedRequiredParameterTestProvider
import art.galushko.openapi.testgen.providers.parameter.ParameterSchemaValidationTestProvider
import art.galushko.openapi.testgen.rules.ManualRuleRegistry
import art.galushko.openapi.testgen.rules.composed.ArrayItemSchemaValidationRule
import art.galushko.openapi.testgen.rules.composed.ObjectItemSchemaValidationRule
import art.galushko.openapi.testgen.spi.AuthValidationRule
import art.galushko.openapi.testgen.spi.RuleContainer
import art.galushko.openapi.testgen.spi.RuleRegistry
import art.galushko.openapi.testgen.spi.SchemaValidationRule
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import art.galushko.openapi.testgen.spi.TestCaseProvider
import art.galushko.openapi.testgen.testdata.BasicTestDataProvider
import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGenerator
import art.galushko.openapi.testgen.testdata.SecurityValueProvider
import io.swagger.v3.oas.models.Operation

/** Configures and wires the components used by [TestGenerationProcessor]. */
internal object TestGeneratorConfigurer {
    public const val DEFAULT_MAX_SCHEMA_DEPTH: Int = 50
    public const val DEFAULT_MAX_SCHEMA_COMBINATIONS: Int = 100
    public const val DEFAULT_MAX_TEST_CASES_PER_OPERATION: Int = 1000
    public const val DEFAULT_MAX_ERRORS: Int = 100

    /**
     * Creates a [TestGenerationProcessor] instance.
     *
     * @param testSuiteGenerator configured [TestSuiteGenerator] instance
     * @param ignoreConfigHandler configured [IgnoreConfigHandler] instance
     * @return configured [TestGenerationProcessor]
     */
    public fun createTestGenerationProcessor(
        testSuiteGenerator: TestSuiteGenerator,
        ignoreConfigHandler: IgnoreConfigHandler,
    ): TestGenerationProcessor {
        return TestGenerationProcessor(testSuiteGenerator, ignoreConfigHandler)
    }

    /**
     * Builds a `TestSuiteGenerator` with all available providers and rules.
     *
     * Provider order is deterministic: auth -> parameters -> request body.
     *
     * @param testGenerationSettings test generation settings to use for configuration
     * @param schemaExampleValueGenerator example value generator to use for building baselines and schema-derived values
     * @return configured `TestSuiteGenerator`
     */
    public fun createTestSuiteGenerator(
        testGenerationSettings: TestGenerationSettings,
        schemaExampleValueGenerator: SchemaExampleValueGenerator,
        schemaMerger: SchemaMerger,
        ruleRegistry: RuleRegistry = ManualRuleRegistry(),
    ): TestSuiteGenerator {
        val schemaValidationRules = getSchemaValidationRules(testGenerationSettings.ignoreSchemaValidationRules, ruleRegistry)

        val parameterTestProviderForOperation = getParameterTestProvider(schemaValidationRules)
        val requestBodyTestProvider = getRequestBodyTestProvider(schemaValidationRules)
        val authHeaderTestCaseProvider = AuthTestCaseProviderForOperation(
            ruleRegistry.getRules(AuthValidationRule::class.java, testGenerationSettings.ignoreAuthValidationRules)
        )
        val providers: List<TestCaseProvider<Operation>> = listOf(
            authHeaderTestCaseProvider,
            parameterTestProviderForOperation,
            requestBodyTestProvider
        )

        val components = DefaultTestSuiteGeneratorComponents(
            providerOrchestrator = ProviderOrchestrator(providers),
            outcomeAggregator = OutcomeAggregator(),
            budgetValidator = TestCaseBudgetValidator(testGenerationSettings.maxTestCasesPerOperation),
            securityValueProvider = SecurityValueProvider(testGenerationSettings.validSecurityValues),
            basicTestDataProvider = BasicTestDataProvider(testGenerationSettings.overrideBasicTestData),
            schemaExampleValueGenerator = schemaExampleValueGenerator,
            schemaMerger = schemaMerger,
        )

        return DefaultTestSuiteGenerator(
            components = components,
            maxSchemaDepth = testGenerationSettings.maxSchemaDepth,
            maxSchemaCombinations = testGenerationSettings.maxSchemaCombinations,
            includeValidCase = testGenerationSettings.includeValidCase,
        )
    }

    /**
     * Creates a `ParameterTestProviderForOperation` configured with provided schema validation rules.
     *
     * @param schemaValidationRules rules to use for parameter validation
     * @return provider for parameter tests per operation
     */
    public fun getParameterTestProvider(schemaValidationRules: List<SchemaValidationRule>): ParameterTestCaseProviderForOperation {
        val missedRequiredParameterTestProvider = MissedRequiredParameterTestProvider()
        val parameterSchemaValidationTestProvider = ParameterSchemaValidationTestProvider(schemaValidationRules)
        return ParameterTestCaseProviderForOperation(
            listOf(
                missedRequiredParameterTestProvider,
                parameterSchemaValidationTestProvider
            )
        )
    }

    /**
     * Creates a `RequestBodyTestCaseProviderForOperation` configured with provided schema validation rules.
     *
     * @param schemaValidationRules rules to use for request body validation
     * @return provider for request body tests per operation
     */
    public fun getRequestBodyTestProvider(schemaValidationRules: List<SchemaValidationRule>): RequestBodyTestCaseProviderForOperation {
        val missedRequiredRequestBodyTestProvider = MissedRequiredRequestBodyTestProvider()
        val requestBodySchemaValidationTestProvider = RequestBodySchemaValidationTestProvider(schemaValidationRules)
        return RequestBodyTestCaseProviderForOperation(
            listOf(
                missedRequiredRequestBodyTestProvider,
                requestBodySchemaValidationTestProvider
            )
        )
    }

    /**
     * Configures all schema validation rules, including composed rules for arrays and objects.
     *
     * Composed rules (Array/Object) need access to all rules for recursive validation.
     * This is achieved by creating an anonymous [RuleContainer] backed by a mutable list,
     * then populating the list with all rules including the composed ones.
     *
     * @param ignoredSchemaRuleClasses set of schema rule class names to ignore
     * @param ruleRegistry registry for instantiating rules
     * @return ordered list of `SchemaValidationRule`
     */
    public fun getSchemaValidationRules(
        ignoredSchemaRuleClasses: Set<String> = emptySet(),
        ruleRegistry: RuleRegistry = ManualRuleRegistry(),
    ): List<SchemaValidationRule> {
        val arrayRuleName = ArrayItemSchemaValidationRule::class.java.name
        val objectRuleName = ObjectItemSchemaValidationRule::class.java.name

        val simpleIgnored = ignoredSchemaRuleClasses - setOf(arrayRuleName, objectRuleName)
        val simpleRules = ruleRegistry.getRules(SimpleSchemaValidationRule::class.java, simpleIgnored)

        // Mutable list that will hold all rules (simple + composed)
        val allRules: MutableList<SchemaValidationRule> = simpleRules.toMutableList()

        // Container backed by the list - composed rules reference this during apply()
        val container = object : RuleContainer {
            override fun getAllRules(): List<SchemaValidationRule> = allRules
        }

        // Add composed rules that reference the container
        if (arrayRuleName !in ignoredSchemaRuleClasses) {
            allRules.add(ArrayItemSchemaValidationRule(container))
        }
        if (objectRuleName !in ignoredSchemaRuleClasses) {
            allRules.add(ObjectItemSchemaValidationRule(container))
        }

        return allRules
    }
}

