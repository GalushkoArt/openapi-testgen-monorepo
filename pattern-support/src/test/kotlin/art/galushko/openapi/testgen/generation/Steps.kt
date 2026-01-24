package art.galushko.openapi.testgen.generation

import art.galushko.openapi.testgen.model.SecurityValues
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGenerator
import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGeneratorFactory
import art.galushko.openapi.testgen.example.openapi.SchemaMerger
import art.galushko.openapi.testgen.example.response.ResponseExampleExtractor
import art.galushko.openapi.testgen.example.util.CombinationBudget
import art.galushko.openapi.testgen.testdata.BasicTestDataProvider
import art.galushko.openapi.testgen.testdata.SecurityValueProvider
import io.qameta.allure.Allure
import io.qameta.allure.Step
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema

/**
 * Creates a minimal valid [TestCase] for tests.
 */
@Suppress("LongParameterList")
@Step("Create valid test case")
fun createBasicTestCase(
    name: String = "Test Valid Case",
    method: String = "GET",
    path: String = "/api/resource",
    expectedStatusCode: Int = 200,
    queryParams: Map<String, Any> = emptyMap(),
    pathParams: Map<String, Any> = emptyMap(),
    headers: List<art.galushko.openapi.testgen.model.KeyValuePair<String, Any>> = emptyList(),
    cookie: List<art.galushko.openapi.testgen.model.KeyValuePair<String, Any>> = emptyList(),
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
 * Note: This is a lightweight test-only implementation that does not model traversal state,
 * because the pattern rule tests only require access to OpenAPI and schema attributes.
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
    responseExampleExtractor: ResponseExampleExtractor = ResponseExampleExtractor(schemaExampleValueGenerator),
    schemaMerger: SchemaMerger = SchemaMerger(),
    maxDepth: Int = 50,
    combinationBudget: CombinationBudget? = null,
): TestGenerationContext = TestOnlyTestGenerationContext(
    openAPI = openAPI,
    operation = operation,
    validCase = validCase,
    basicTestData = basicTestData,
    securityValueProvider = securityValueProvider,
    schemaExampleValueGenerator = schemaExampleValueGenerator,
    responseExampleExtractor = responseExampleExtractor,
    schemaMerger = schemaMerger,
    maxDepth = maxDepth,
    combinationBudget = combinationBudget,
)

private data class TestOnlyTestGenerationContext(
    override val openAPI: OpenAPI,
    override val operation: Operation,
    override val validCase: TestCase,
    override val basicTestData: BasicTestDataProvider,
    override val securityValueProvider: SecurityValueProvider,
    override val schemaExampleValueGenerator: SchemaExampleValueGenerator,
    override val responseExampleExtractor: ResponseExampleExtractor,
    override val schemaMerger: SchemaMerger,
    override val maxDepth: Int,
    override val combinationBudget: CombinationBudget?,
) : TestGenerationContext {
    override val visitedSchemaRefs: Set<String> = emptySet()
    override val depth: Int = 0
    override val schemaPath: List<String> = emptyList()

    override fun checkSkip(schema: Schema<*>): art.galushko.openapi.testgen.openapi.SkipReason? = null

    override fun withVisitedSchema(schema: Schema<*>, name: String): TestGenerationContext = this
}

/**
 * Wraps a block into an Allure step.
 */
fun <T> step(name: String, action: () -> T): T {
    Allure.step(name)
    return action()
}


