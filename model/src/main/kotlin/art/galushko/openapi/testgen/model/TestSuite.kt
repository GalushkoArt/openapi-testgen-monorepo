package art.galushko.openapi.testgen.model


/**
 * Collection of test cases for a single API operation.
 *
 * @property path Endpoint relative path.
 * @property method HTTP method (usually uppercase, e.g., GET, POST).
 * @property operationName Operation id/name in the OpenAPI spec. Used as the stable suite key
 * in test-suite writer output; when null or blank, writers may skip the suite.
 * @property testCases Test cases associated with this operation.
 */
public data class TestSuite(
    val path: String,
    val method: String,
    val operationName: String? = null,
    val testCases: List<TestCase> = emptyList(),
)

