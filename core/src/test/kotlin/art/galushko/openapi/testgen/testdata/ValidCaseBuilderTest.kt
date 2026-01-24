package art.galushko.openapi.testgen.testdata

import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.model.SecurityValues
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.model.with
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.CookieParameter
import io.swagger.v3.oas.models.parameters.HeaderParameter
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.PathParameter
import io.swagger.v3.oas.models.parameters.QueryParameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Suppress("SameParameterValue")
@Epic("Test Data Generation")
@Feature("Valid Case Builder")
class ValidCaseBuilderTest {

    @Suppress("LongMethod")
    fun validCaseProvider(): Stream<ValidCaseScenario> = Stream.of(
        validCaseScenario(
            name = "Simple GET operation with required path parameter",
            path = "/pets/{petId}",
            method = "get",
            operation = operationWithResponse("200").addParametersItem(pathParameter("petId", "test-id")),
            expected = expectedCase("get", "/pets/{petId}", 200)
                .copy(pathParams = mapOf("petId" to "test-id")),
        ),
        validCaseScenario(
            name = "Operation with required query parameter",
            path = "/pets",
            method = "get",
            operation = operationWithResponse("200").addParametersItem(requiredQueryParameter("limit", "10")),
            expected = expectedCase("get", "/pets", 200)
                .copy(queryParams = mapOf("limit" to "10")),
        ),
        validCaseScenario(
            name = "Operation with required header parameter",
            path = "/pets/headers",
            method = "get",
            operation = operationWithResponse("200").addParametersItem(requiredHeaderParameter("X-API-Key", "test-key")),
            expected = expectedCase("get", "/pets/headers", 200)
                .copy(headers = listOf("X-API-Key" with "test-key")),
        ),
        validCaseScenario(
            name = "Operation with required cookie parameter",
            path = "/pets/cookies",
            method = "get",
            operation = operationWithResponse("200").addParametersItem(requiredCookieParameter("session", "test-session")),
            expected = expectedCase("get", "/pets/cookies", 200)
                .copy(cookie = listOf("session" with "test-session")),
        ),
        validCaseScenario(
            name = "Operation with request body",
            path = "/pets",
            method = "post",
            operation = operationWithResponse("201")
                .requestBody(jsonRequestBody(mapOf("name" to "test-pet", "type" to "dog"))),
            expected = expectedCase("post", "/pets", 201)
                .copy(body = mapOf("name" to "test-pet", "type" to "dog")),
        )
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("validCaseProvider")
    @DisplayName("generateValidCase should create correct test cases for different operations")
    @Description("Verifies that the generateValidCase method creates test cases with correct parameters based on the operation")
    fun generateValidCaseShouldCreateCorrectTestCases(scenario: ValidCaseScenario) {
        val result = generateValidCase(scenario)
        step("Verify test case properties") { assertThat(result).isEqualTo(scenario.expected) }
    }

    @Suppress("LongMethod")
    fun apiKeyAuthenticationTestCasesProvider(): Stream<ValidCaseScenario> = Stream.of(
        validCaseScenario(
            name = "No security requirements",
            path = "/test",
            method = "get",
            operation = operationWithResponse("200"),
            expected = expectedCase("get", "/test", 200),
        ),
        validCaseScenario(
            name = "API key required in Open API spec in query parameter",
            path = "/test",
            method = "get",
            operation = operationWithResponse("200"),
            openAPI = createOpenAPIWithSecurity("api_key", createApiKeySecurityScheme(SecurityScheme.In.QUERY, "api_key"))
                .security(securityRequirements("api_key")),
            expected = expectedCase("get", "/test", 200).copy(
                queryParams = mapOf("api_key" to VALID_API_KEY_PLACEHOLDER),
                securityValues = SecurityValues(queryParams = mapOf("api_key" to VALID_API_KEY_PLACEHOLDER)),
            ),
        ),
        validCaseScenario(
            name = "API key requirements overridden for operation",
            path = "/test",
            method = "get",
            operation = operationWithResponse("200").security(listOf()),
            openAPI = createOpenAPIWithSecurity("api_key", createApiKeySecurityScheme(SecurityScheme.In.QUERY, "api_key"))
                .security(securityRequirements("api_key")),
            expected = expectedCase("get", "/test", 200),
        ),
        validCaseScenario(
            name = "API key in query parameter",
            path = "/test",
            method = "get",
            operation = operationWithResponse("200").security(securityRequirements("api_key")),
            openAPI = createOpenAPIWithSecurity("api_key", createApiKeySecurityScheme(SecurityScheme.In.QUERY, "api_key")),
            expected = expectedCase("get", "/test", 200).copy(
                queryParams = mapOf("api_key" to VALID_API_KEY_PLACEHOLDER),
                securityValues = SecurityValues(queryParams = mapOf("api_key" to VALID_API_KEY_PLACEHOLDER)),
            ),
        ),
        validCaseScenario(
            name = "API key in header",
            path = "/test",
            method = "get",
            operation = operationWithResponse("200").security(securityRequirements("api_key")),
            openAPI = createOpenAPIWithSecurity("api_key", createApiKeySecurityScheme(SecurityScheme.In.HEADER, "X-API-Key")),
            expected = expectedCase("get", "/test", 200).copy(
                headers = listOf("X-API-Key" with VALID_API_KEY_PLACEHOLDER),
                securityValues = SecurityValues(headers = listOf("X-API-Key" with VALID_API_KEY_PLACEHOLDER)),
            ),
        ),
        validCaseScenario(
            name = "API key in cookie",
            path = "/test",
            method = "get",
            operation = operationWithResponse("200").security(securityRequirements("api_key")),
            openAPI = createOpenAPIWithSecurity("api_key", createApiKeySecurityScheme(SecurityScheme.In.COOKIE, "api_key")),
            expected = expectedCase("get", "/test", 200).copy(
                cookie = listOf("api_key" with VALID_API_KEY_PLACEHOLDER),
                securityValues = SecurityValues(cookie = listOf("api_key" with VALID_API_KEY_PLACEHOLDER)),
            ),
        )
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("apiKeyAuthenticationTestCasesProvider")
    @DisplayName("generateValidCase should add API key to the correct location")
    @Description("Verifies that the generateValidCase method adds the API key to the correct location based on the security scheme")
    fun addApiKeyAuthenticationIfRequiredShouldAddApiKeyToCorrectLocationSecurityItems(scenario: ValidCaseScenario) {
        val result = generateValidCase(scenario)
        step("Verify API key was added to the correct location") { assertThat(result).isEqualTo(scenario.expected) }
    }

    @Test
    @DisplayName("generateValidCase should return Failure for unsupported security scheme location")
    @Description("Verifies that the generateValidCase method returns Failure when the security scheme location is unsupported")
    fun addRequiredSecurityItemsShouldReturnFailureForUnsupportedLocation() {
        val operation = Operation()
            .security(listOf(SecurityRequirement().addList("api_key")))
            .responses(ApiResponses().addApiResponse("200", ApiResponse().description("OK")))
        val openAPI = createOpenAPIWithSecurity("api_key", createApiKeySecurityScheme(null, "api_key"))
        val builder = ValidCaseBuilder("/test", "get", operation, openAPI)

        val outcome = builder.generateValidCase()
        assertThat(outcome).isInstanceOf(Outcome.Failure::class.java)
        val failure = outcome as Outcome.Failure
        assertThat(failure.errors).hasSize(1)
        assertThat(failure.errors[0].message).contains("Unsupported API-KEY security scheme type apiKey")
    }

    @Test
    @DisplayName("Should create defensive copies of collections")
    @Description("Verifies that TestCase collections are immutable copies, preventing accidental mutation")
    fun shouldCreateDefensiveCopiesOfCollections() {
        // Arrange
        val operation = operationWithResponse("200")
            .addParametersItem(requiredQueryParameter("testParam", "value"))
            .addParametersItem(requiredCookieParameter("testCookieParam", "cookie-value"))
            .addParametersItem(requiredHeaderParameter("X-Test-Header", "header-value"))

        val testCase = generateValidCase("/test", "get", operation, createOpenAPIWithoutSecurity())

        // Assert - Verify collections are immutable or mutations don't affect the test case
        step("Verify defensive copies protect against mutation") {
            // Attempt to cast to mutable types and mutate
            val queryParams = testCase.queryParams
            val headers = testCase.headers
            val cookies = testCase.cookie

            // Try to mutate - these should either throw UnsupportedOperationException
            // or be no-ops because they're immutable
            assertThatThrownBy {
                (queryParams as? MutableMap<String, Any>)?.put("injected", "value")
                    ?: throw UnsupportedOperationException("Map is immutable")
            }.isInstanceOf(UnsupportedOperationException::class.java)

            assertThatThrownBy {
                (headers as? MutableList)?.add("injected" with "value")
                    ?: throw UnsupportedOperationException("List is immutable")
            }.isInstanceOf(UnsupportedOperationException::class.java)

            assertThatThrownBy {
                (cookies as? MutableList)?.add("injected" with "value")
                    ?: throw UnsupportedOperationException("List is immutable")
            }.isInstanceOf(UnsupportedOperationException::class.java)

            // Verify original values unchanged
            assertThat(testCase.queryParams).containsExactlyEntriesOf(mapOf("testParam" to "value"))
            assertThat(testCase.headers).containsExactly("X-Test-Header" with "header-value")
            assertThat(testCase.cookie).containsExactly("testCookieParam" with "cookie-value")
        }
    }

    @Test
    @DisplayName("Provider copy operations should work with immutable collections")
    @Description("Verifies that TestCase.copy() works correctly with immutable collections")
    fun providerCopyShouldWorkWithImmutableCollections() {
        // Arrange
        val originalCase = step("Create original test case") {
            TestCase(
                name = "original",
                method = "GET",
                path = "/test",
                queryParams = mapOf("key" to "value"),
                headers = listOf("header" with "value"),
                cookie = listOf("cookie" with "value"),
                expectedStatusCode = 200
            )
        }

        // Act - Providers frequently use .copy() with modifications
        val derivedCase = step("Create derived test case using copy()") {
            originalCase.copy(
                name = "derived",
                queryParams = originalCase.queryParams + ("newKey" to "newValue"),
                headers = originalCase.headers + ("newHeader" with "newValue"),
                cookie = originalCase.cookie + ("newCookie" with "newValue"),
            )
        }

        // Assert - Original should be unchanged
        step("Verify original case unchanged") {
            assertThat(originalCase.queryParams).isEqualTo(mapOf("key" to "value"))
            assertThat(originalCase.queryParams).doesNotContainKey("newKey")
            assertThat(originalCase.headers).hasSize(1)
            assertThat(originalCase.headers).containsExactly("header" with "value")
            assertThat(originalCase.cookie).hasSize(1)
            assertThat(originalCase.cookie).containsExactly("cookie" with "value")
        }

        step("Verify derived case has new values") {
            assertThat(derivedCase.queryParams).containsKey("newKey")
            assertThat(derivedCase.queryParams).isEqualTo(mapOf("key" to "value", "newKey" to "newValue"))
            assertThat(derivedCase.headers).hasSize(2)
            assertThat(derivedCase.headers).contains("newHeader" with "newValue")
            assertThat(derivedCase.cookie).hasSize(2)
            assertThat(derivedCase.cookie).contains("newCookie" with "newValue")
        }
    }

    @Test
    @DisplayName("Test case collections should be independent copies")
    @Description("Verifies that TestCase receives defensive copies that are independent from builder state")
    fun testCaseCollectionsShouldBeIndependentCopies() {
        // Arrange
        val operation = operationWithResponse("200")
            .addParametersItem(requiredQueryParameter("param", "value"))
            .addParametersItem(requiredCookieParameter("cookie", "cookie-value"))
            .addParametersItem(requiredHeaderParameter("X-Header", "header"))
        val case1 = generateValidCase("/test", "get", operation, createOpenAPIWithoutSecurity())
        val case2 = generateValidCase("/test", "get", operation, createOpenAPIWithoutSecurity())

        // Assert - Collections should be distinct instances (defensive copies)
        step("Verify collection instances are different") {
            assertThat(case1.queryParams).isNotSameAs(case2.queryParams)
            assertThat(case1.headers).isNotSameAs(case2.headers)
            assertThat(case1.cookie).isNotSameAs(case2.cookie)
        }

        // Content should be equal though
        step("Verify collection contents are equal") {
            assertThat(case1.queryParams).isEqualTo(case2.queryParams)
            assertThat(case1.headers).isEqualTo(case2.headers)
            assertThat(case1.cookie).isEqualTo(case2.cookie)
        }
    }

    @Test
    @DisplayName("generateValidCase should populate authorizationScopes in securityValues.other for OAuth2")
    @Description("Verifies that OAuth2 security schemes populate the authorizationScopes metadata in securityValues.other")
    fun generateValidCaseShouldPopulateAuthorizationScopesForOAuth2() {
        val operation = operationWithResponse("200").security(
            listOf(SecurityRequirement().addList("oauth", listOf("read", "write")))
        )
        val openAPI = createOpenAPIWithSecurity(
            "oauth",
            SecurityScheme().type(SecurityScheme.Type.OAUTH2)
        )

        val testCase = generateValidCase("/test", "get", operation, openAPI)

        step("Verify authorizationScopes is populated in securityValues.other") {
            assertThat(testCase.securityValues.other).containsKey("authorizationScopes")
            @Suppress("UNCHECKED_CAST")
            val scopes = testCase.securityValues.other["authorizationScopes"] as List<Map<String, Any>>
            assertThat(scopes).hasSize(1)
            assertThat(scopes[0]).isEqualTo(
                mapOf(
                    "name" to "oauth",
                    "type" to "oauth2",
                    "scopes" to listOf("read", "write")
                )
            )
        }
    }

    @Test
    @DisplayName("generateValidCase should not populate authorizationScopes for API key only")
    @Description("Verifies that API key-only security does not populate authorizationScopes in securityValues.other")
    fun generateValidCaseShouldNotPopulateAuthorizationScopesForApiKeyOnly() {
        val operation = operationWithResponse("200").security(securityRequirements("api_key"))
        val openAPI = createOpenAPIWithSecurity("api_key", createApiKeySecurityScheme(SecurityScheme.In.HEADER, "X-API-Key"))

        val testCase = generateValidCase("/test", "get", operation, openAPI)

        step("Verify authorizationScopes is not populated") {
            assertThat(testCase.securityValues.other).doesNotContainKey("authorizationScopes")
        }
    }

    @Test
    @DisplayName("Parallel test generation should be thread-safe")
    @Description("Verify that ValidCaseBuilder with SecurityValueProvider is thread-safe during parallel execution")
    fun parallelGenerationShouldBeThreadSafe() {
        // Given: 100 operations with different IDs
        val operations = (1..100).map { i ->
            Operation().apply {
                operationId = "op_$i"
                responses = ApiResponses().apply { addApiResponse("200", ApiResponse()) }
            }
        }

        // When: Generate test cases in parallel with different security configs
        val outcomes = operations.parallelStream()
            .map { operation ->
                val provider = SecurityValueProvider(mapOf("ApiKey" to "secret_${operation.operationId}"))
                val queryParam = QueryParameter()
                    .required(true)
                    .name(operation.operationId)
                    .schema(StringSchema().example("${operation.operationId}-value"))
                val openApi = createOpenAPIWithSecurity("ApiKey", createApiKeySecurityScheme(SecurityScheme.In.COOKIE, "api_key"))
                    .security(securityRequirements("ApiKey"))
                ValidCaseBuilder("/test", "get", operation.addParametersItem(queryParam), openApi, provider).generateValidCase()
            }
            .toList()

        // Then: Each test case should have its own independent security configuration
        assertThat(outcomes).hasSize(100)
        val results = outcomes.map { (it as Outcome.Success).value }
        results.forEach { testCase ->
            assertThat(testCase).isNotNull
            assertThat(testCase.name).isEqualTo(VALID_CASE_NAME)
            assertThat(testCase.method).isEqualTo("GET")
            val queryParams = testCase.queryParams
            assertThat(queryParams).hasSize(1)
            val (name, value) = queryParams.entries.first()
            assertThat(value).isEqualTo("$name-value")
            assertThat(testCase.cookie).hasSize(1).first().isEqualTo("api_key" with "secret_$name")
        }
    }

    @Nested
    @DisplayName("Success Status Code Resolution")
    inner class SuccessStatusCodeResolutionTest {
        fun successStatusCodeProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("exact 200", listOf("200"), 200),
            Arguments.of("exact 201", listOf("201"), 201),
            Arguments.of("prefer lower numeric code", listOf("201", "200"), 200),
            Arguments.of("prefer numeric over 2XX range", listOf("2XX", "201"), 201),
            Arguments.of("2XX range fallback", listOf("2XX"), 200),
            Arguments.of("lowercase 2xx range", listOf("2xx"), 200),
            Arguments.of("default fallback", listOf("default"), 200),
            Arguments.of("prefer 2XX range over default", listOf("default", "2XX"), 200),
            Arguments.of("ignore non-2xx numeric codes", listOf("400", "201", "500"), 201),
            Arguments.of("complex: numeric preferred over range and default", listOf("400", "2XX", "default", "204"), 204),
        )

        @Suppress("UNUSED_PARAMETER") // scenario is used in test name via {0}
        @ParameterizedTest(name = "{0}")
        @MethodSource("successStatusCodeProvider")
        @DisplayName("should resolve success status code correctly")
        fun shouldResolveSuccessStatusCodeCorrectly(scenario: String, responseCodes: List<String>, expected: Int) {
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    responseCodes.forEach { code ->
                        addApiResponse(code, ApiResponse().description("Response $code"))
                    }
                }
            }
            val builder = ValidCaseBuilder("/test", "get", operation, createOpenAPIWithoutSecurity())

            val outcome = builder.generateValidCase()

            assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
            val testCase = (outcome as Outcome.Success).value
            assertThat(testCase.expectedStatusCode).isEqualTo(expected)
        }

        @Test
        @DisplayName("should throw when no success response defined")
        fun shouldThrowWhenNoSuccessResponseDefined() {
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("400", ApiResponse().description("Bad Request"))
                    addApiResponse("500", ApiResponse().description("Server Error"))
                }
            }
            val builder = ValidCaseBuilder("/test", "get", operation, createOpenAPIWithoutSecurity())

            val outcome = builder.generateValidCase()

            assertThat(outcome).isInstanceOf(Outcome.Failure::class.java)
            val failure = outcome as Outcome.Failure
            assertThat(failure.errors).hasSize(1)
            assertThat(failure.errors[0].message).contains("Success status code not found")
        }
    }

    @Nested
    @DisplayName("Expected body population")
    inner class ExpectedBodyPopulationTest {

        @Test
        @DisplayName("should populate expectedBody from response example")
        fun shouldPopulateExpectedBodyFromResponseExample() {
            val responseExample = mapOf("id" to 1, "name" to "test")
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse(
                        "200",
                        ApiResponse()
                            .description("OK")
                            .content(
                                Content().addMediaType(
                                    "application/json",
                                    MediaType().example(responseExample)
                                )
                            )
                    )
                }
            }
            val builder = ValidCaseBuilder("/test", "get", operation, createOpenAPIWithoutSecurity())

            val outcome = builder.generateValidCase()

            assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
            val testCase = (outcome as Outcome.Success).value
            assertThat(testCase.expectedBody).isEqualTo(responseExample)
        }

        @Test
        @DisplayName("should return null expectedBody when no response example defined")
        fun shouldReturnNullExpectedBodyWhenNoExample() {
            val operation = operationWithResponse("200")
            val builder = ValidCaseBuilder("/test", "get", operation, createOpenAPIWithoutSecurity())

            val outcome = builder.generateValidCase()

            assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
            val testCase = (outcome as Outcome.Success).value
            assertThat(testCase.expectedBody).isNull()
        }

        @Test
        @DisplayName("should populate expectedBody from schema-derived example")
        fun shouldPopulateExpectedBodyFromSchemaDerivedExample() {
            val schema = ObjectSchema().apply {
                addProperty("status", StringSchema().example("ok"))
                required = listOf("status")
            }
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse(
                        "200",
                        ApiResponse()
                            .description("OK")
                            .content(Content().addMediaType("application/json", MediaType().schema(schema)))
                    )
                }
            }
            val builder = ValidCaseBuilder("/test", "get", operation, createOpenAPIWithoutSecurity())

            val outcome = builder.generateValidCase()

            assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
            val testCase = (outcome as Outcome.Success).value
            assertThat(testCase.expectedBody).isEqualTo(mapOf("status" to "ok"))
        }

        @Test
        @DisplayName("should use correct status code for expectedBody extraction")
        fun shouldUseCorrectStatusCodeForExpectedBody() {
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse(
                        "201",
                        ApiResponse()
                            .description("Created")
                            .content(
                                Content().addMediaType(
                                    "application/json",
                                    MediaType().example(mapOf("created" to true))
                                )
                            )
                    )
                    addApiResponse(
                        "200",
                        ApiResponse()
                            .description("OK")
                            .content(
                                Content().addMediaType(
                                    "application/json",
                                    MediaType().example(mapOf("ok" to true))
                                )
                            )
                    )
                }
            }
            val builder = ValidCaseBuilder("/test", "post", operation, createOpenAPIWithoutSecurity())

            val outcome = builder.generateValidCase()

            assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
            val testCase = (outcome as Outcome.Success).value
            // Should use 200 (minimum 2xx) not 201
            assertThat(testCase.expectedStatusCode).isEqualTo(200)
            assertThat(testCase.expectedBody).isEqualTo(mapOf("ok" to true))
        }
    }

    data class ValidCaseScenario(
        val name: String,
        val path: String,
        val method: String,
        val operation: Operation,
        val openAPI: OpenAPI,
        val expected: TestCase,
    ) {
        override fun toString(): String = name
    }

    @Suppress("LongParameterList")
    private fun validCaseScenario(
        name: String,
        path: String,
        method: String,
        operation: Operation,
        expected: TestCase,
        openAPI: OpenAPI = createOpenAPIWithoutSecurity(),
    ): ValidCaseScenario = ValidCaseScenario(name, path, method, operation, openAPI, expected)

    private fun expectedCase(method: String, path: String, statusCode: Int): TestCase =
        TestCase(
            name = VALID_CASE_NAME,
            method = method.uppercase(),
            path = path,
            expectedStatusCode = statusCode,
        )

    private fun generateValidCase(scenario: ValidCaseScenario): TestCase =
        generateValidCase(scenario.path, scenario.method, scenario.operation, scenario.openAPI)

    private fun generateValidCase(path: String, method: String, operation: Operation, openAPI: OpenAPI): TestCase {
        val builder = step("Create ValidCaseBuilder") { ValidCaseBuilder(path, method, operation, openAPI) }
        val outcome = step("Call generateValidCase") { builder.generateValidCase() }
        step("Verify outcome is success") { assertThat(outcome).isInstanceOf(Outcome.Success::class.java) }
        return (outcome as Outcome.Success).value
    }

    private fun pathParameter(name: String, example: String): Parameter =
        PathParameter().name(name).required(true).schema(StringSchema().example(example))

    private fun requiredQueryParameter(name: String, value: String): Parameter =
        QueryParameter().name(name).required(true).schema(StringSchema().example(value))

    private fun requiredHeaderParameter(name: String, example: String): Parameter =
        HeaderParameter().name(name).required(true).schema(StringSchema().example(example))

    private fun requiredCookieParameter(name: String, example: String): Parameter =
        CookieParameter().name(name).required(true).schema(StringSchema().example(example))

    private fun operationWithResponse(code: String): Operation {
        val responses = ApiResponses().addApiResponse(code, ApiResponse().description("Created"))
        return Operation().responses(responses)
    }

    private fun jsonRequestBody(jsonWithExamples: Map<String, String>): RequestBody {
        val properties = jsonWithExamples.entries.associate {
            it.key to StringSchema().example(it.value)
        }
        val schema = ObjectSchema().properties(properties).required(jsonWithExamples.keys.toList())
        return RequestBody().required(true).content(Content().addMediaType("application/json", MediaType().schema(schema)))
    }

    private fun createOpenAPIWithoutSecurity(): OpenAPI = OpenAPI().paths(Paths())

    private fun createOpenAPIWithSecurity(name: String, securityScheme: SecurityScheme): OpenAPI =
        OpenAPI().components(Components().addSecuritySchemes(name, securityScheme)).paths(Paths())

    private fun createApiKeySecurityScheme(place: SecurityScheme.In?, paramName: String): SecurityScheme =
        SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(place).name(paramName)

    private fun securityRequirements(vararg requirements: String): List<SecurityRequirement> =
        requirements.map { requirement -> SecurityRequirement().addList(requirement) }

    companion object {
        private const val VALID_CASE_NAME = "Test Valid Case"
        private const val VALID_API_KEY_PLACEHOLDER = "<valid_api_key_api_key_placeholder>"
    }
}
