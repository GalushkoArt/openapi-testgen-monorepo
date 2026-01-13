package art.galushko.openapi.testgen.generation

import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGenerator
import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGeneratorFactory
import art.galushko.openapi.testgen.example.openapi.SchemaMerger
import art.galushko.openapi.testgen.example.util.CombinationBudget
import art.galushko.openapi.testgen.model.KeyValuePair
import art.galushko.openapi.testgen.model.SecurityValues
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.testdata.BasicTestDataProvider
import art.galushko.openapi.testgen.testdata.SecurityValueProvider
import io.qameta.allure.Allure
import io.qameta.allure.Step
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation


/**
 * Creates a minimal valid [TestCase] for tests.
 *
 * @return a [TestCase] populated with defaults unless overridden
 */
@Suppress("LongParameterList")
@Step("Create valid test case")
fun createBasicTestCase(
    name: String = "Test Valid Case",
    method: String = "GET",
    path: String = "/api/resource",
    expectedStatusCode: Int = 200,
    cookie: List<KeyValuePair<String, Any>> = listOf(),
    headers: List<KeyValuePair<String, Any>> = listOf(),
    queryParams: Map<String, Any> = mapOf(),
    pathParams: Map<String, Any> = mapOf(),
    body: Any? = null,
    rule: String? = null,
    securityValues: SecurityValues = SecurityValues(),
): TestCase = TestCase(
    name = name,
    method = method,
    path = path,
    expectedStatusCode = expectedStatusCode,
    cookie = cookie,
    headers = headers,
    queryParams = queryParams,
    pathParams = pathParams,
    body = body,
    rule = rule,
    securityValues = securityValues,
)

/**
 * Creates a [TestGenerationContext] with sensible defaults for tests.
 *
 * This helper simplifies test setup by providing reasonable defaults for all
 * sub-contexts while allowing individual overrides.
 *
 * @param validCase valid baseline test case (defaults to [createBasicTestCase])
 * @param operation current operation being processed
 * @param openAPI OpenAPI document
 * @param basicTestData provider for basic test data values
 * @param securityValueProvider provider for security values
 * @param maxDepth maximum traversal depth
 * @param combinationBudget budget for schema combinations
 * @return configured [TestGenerationContext]
 */
@Suppress("LongParameterList")
@Step("Create test generation context")
fun createTestContext(
    validCase: TestCase = createBasicTestCase(),
    operation: Operation = Operation(),
    openAPI: OpenAPI = OpenAPI(),
    basicTestData: BasicTestDataProvider = BasicTestDataProvider(),
    securityValueProvider: SecurityValueProvider = SecurityValueProvider(),
    schemaExampleValueGenerator: SchemaExampleValueGenerator = SchemaExampleValueGeneratorFactory().create(),
    schemaMerger: SchemaMerger = SchemaMerger(),
    maxDepth: Int = 50,
    combinationBudget: CombinationBudget? = null,
): TestGenerationContext = DefaultTestGenerationContext(
    openAPI = openAPI,
    operation = operation,
    validCase = validCase,
    basicTestData = basicTestData,
    securityValueProvider = securityValueProvider,
    schemaExampleValueGenerator = schemaExampleValueGenerator,
    schemaMerger = schemaMerger,
    maxDepth = maxDepth,
    combinationBudget = combinationBudget,
)

/**
 * Wraps a block into an Allure step.
 */
fun <T> step(name: String, action: () -> T): T {
    Allure.step(name)
    return action()
}
