package art.galushko.openapi.testgen.generation

import art.galushko.openapi.testgen.generation.TestGeneratorConfigurer.DEFAULT_MAX_SCHEMA_COMBINATIONS
import art.galushko.openapi.testgen.generation.TestGeneratorConfigurer.DEFAULT_MAX_SCHEMA_DEPTH
import art.galushko.openapi.testgen.generation.budget.TestCaseBudgetValidator
import art.galushko.openapi.testgen.generation.orchestration.OutcomeAggregator
import art.galushko.openapi.testgen.generation.orchestration.ProviderOrchestrator
import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.model.error.ErrorContext
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGenerator
import art.galushko.openapi.testgen.example.openapi.SchemaMerger
import art.galushko.openapi.testgen.example.util.CombinationBudget
import art.galushko.openapi.testgen.testdata.BasicTestDataProvider
import art.galushko.openapi.testgen.testdata.SecurityValueProvider
import art.galushko.openapi.testgen.testdata.ValidCaseBuilder
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

internal data class DefaultTestSuiteGeneratorComponents(
    val providerOrchestrator: ProviderOrchestrator,
    val outcomeAggregator: OutcomeAggregator,
    val budgetValidator: TestCaseBudgetValidator,
    val securityValueProvider: SecurityValueProvider,
    val basicTestDataProvider: BasicTestDataProvider,
    val schemaExampleValueGenerator: SchemaExampleValueGenerator,
    val schemaMerger: SchemaMerger,
)

/**
 * Default implementation of [TestSuiteGenerator].
 *
 * This class orchestrates test suite generation by delegating to focused components:
 * - [ValidCaseBuilder] for creating baseline test cases
 * - [ProviderOrchestrator] for executing test case providers
 * - [OutcomeAggregator] for aggregating provider results
 * - [TestCaseBudgetValidator] for enforcing test case limits
 *
 * @param components pre-wired components used by this generator (wired by [TestGeneratorConfigurer])
 * @param maxSchemaDepth maximum depth for schema validation traversal
 * @param maxSchemaCombinations maximum schema combinations per parameter/property (prevents combinatorial explosion)
 * @param includeValidCase include baseline valid case in output suites when enabled
 */
internal class DefaultTestSuiteGenerator(
    private val components: DefaultTestSuiteGeneratorComponents,
    private val maxSchemaDepth: Int = DEFAULT_MAX_SCHEMA_DEPTH,
    private val maxSchemaCombinations: Int = DEFAULT_MAX_SCHEMA_COMBINATIONS,
    private val includeValidCase: Boolean = false,
) : TestSuiteGenerator {

    override fun generateTestSuite(openAPI: OpenAPI, path: String, method: String, operation: Operation): Outcome<TestSuite> {
        val operationName = operation.operationId ?: "${method.lowercase()} $path"

        val operationContext = ErrorContext.Operation(
            path = path,
            method = method,
            operationId = operation.operationId,
        )
        val combinationBudget = CombinationBudget(
            limit = maxSchemaCombinations,
            errorContext = operationContext,
        )

        // 1. Build valid case baseline
        val validCaseBuilder = ValidCaseBuilder(path, method, operation, openAPI, components.securityValueProvider, components.schemaExampleValueGenerator)
        val validTest = when (val validTestOutcome = validCaseBuilder.generateValidCase()) {
            is Outcome.Success -> validTestOutcome.value
            is Outcome.Failure -> return Outcome.Failure(validTestOutcome.errors)
            is Outcome.PartialSuccess -> return Outcome.Failure(validTestOutcome.errors)
        }

        // 2. Create context for providers
        val context: TestGenerationContext = DefaultTestGenerationContext(
            openAPI = openAPI,
            operation = operation,
            validCase = validTest,
            basicTestData = components.basicTestDataProvider,
            securityValueProvider = components.securityValueProvider,
            schemaExampleValueGenerator = components.schemaExampleValueGenerator,
            schemaMerger = components.schemaMerger,
            maxDepth = maxSchemaDepth,
            combinationBudget = combinationBudget,
        )

        // 3. Execute providers and aggregate results
        val aggregated = components.outcomeAggregator.createEmptyResult()
        for (providerResult in components.providerOrchestrator.executeProviders(operation, context)) {
            components.outcomeAggregator.addProviderResult(aggregated, providerResult)
            components.budgetValidator.validate(aggregated.testCases, operationContext)
        }

        if (includeValidCase) {
            components.budgetValidator.validate(listOf(validTest) + aggregated.testCases, operationContext)
        }

        // 4. Build final outcome
        return components.outcomeAggregator.buildOutcome(
            path = path,
            method = method,
            operationName = operationName,
            aggregated = aggregated,
            validCase = if (includeValidCase) validTest else null,
        )
    }
}
