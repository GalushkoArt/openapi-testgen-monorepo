package art.galushko.openapi.testgen.model

/**
 * Test case model describing request details, expectations, and provenance.
 *
 * Designed to preserve JSON shape and equality/hash semantics across generators.
 *
 * @property name Human-readable name of the test case.
 * @property method HTTP method (usually uppercase, e.g., GET, POST).
 * @property path Endpoint relative path (e.g., "/pets/{id}").
 * @property queryParams Query parameters as a map from name to value.
 * @property pathParams Path parameters as a map from name to value.
 * @property headers HTTP headers as ordered name-value pairs; order is preserved.
 * @property cookie Cookies as ordered name-value pairs.
 * @property securityValues Security material derived from OpenAPI security requirements.
 * @property body Request body as any present object, or null if absent.
 * @property requestBodyMediaType Request body media type selected from OpenAPI content, if applicable.
 * @property expectedBody Expected response body example, when available from the OpenAPI spec.
 * @property responseBodyMediaType Response media type used to populate expectedBody, if applicable.
 * @property needToComplete Whether this generated case requires manual completion.
 * @property expectedStatusCode Expected HTTP status code; 0 means unspecified.
 * @property rule Fully qualified rule class name that generated this case, if any.
 */
public data class TestCase(
    val name: String,
    val method: String,
    val path: String,
    val queryParams: Map<String, Any> = emptyMap(),
    val pathParams: Map<String, Any> = emptyMap(),
    val headers: List<KeyValuePair<String, Any>> = emptyList(),
    val cookie: List<KeyValuePair<String, Any>> = emptyList(),
    val securityValues: SecurityValues = SecurityValues(),
    val body: Any? = null,
    val requestBodyMediaType: String? = null,
    val expectedBody: Any? = null,
    val responseBodyMediaType: String? = null,
    val needToComplete: Boolean = false,
    val expectedStatusCode: Int = 0,
    val rule: String? = null,
)

/**
 * Represents security-related values used in a request.
 *
 * Values are separated from regular params/headers to keep provenance and allow
 * auth rules to reason about security material independently.
 *
 * @property queryParams Query parameters as a map of names to values.
 * @property headers HTTP headers represented as a list of ordered key-value pairs.
 * @property cookie Cookies represented as a list of ordered key-value pairs.
 * @property other Additional security-related attributes for extensions.
 *   Known keys:
 *   - `authorizationScopes`: List of OAuth2/OpenID scope metadata when present.
 *     Each entry contains: `name` (scheme name), `type` ("oauth2" or "openidconnect"),
 *     and `scopes` (list of scope strings, may be empty).
 */
public data class SecurityValues(
    val queryParams: Map<String, Any> = emptyMap(),
    val headers: List<KeyValuePair<String, Any>> = emptyList(),
    val cookie: List<KeyValuePair<String, Any>> = emptyList(),
    val other: Map<String, Any> = emptyMap(),
)
