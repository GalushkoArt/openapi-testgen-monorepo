package art.galushko.openapi.testgen.rules.auth

import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.model.SecurityValues
import art.galushko.openapi.testgen.model.with
import art.galushko.openapi.testgen.openapi.SecurityHelpers.AUTHORIZATION_HEADER
import art.galushko.openapi.testgen.rules.auth.fixtures.API_KEY_HEADER_NAME
import art.galushko.openapi.testgen.rules.auth.fixtures.apiKeyScheme
import art.galushko.openapi.testgen.rules.auth.fixtures.copyWithHeaders
import art.galushko.openapi.testgen.rules.auth.fixtures.oauthScheme
import art.galushko.openapi.testgen.rules.auth.fixtures.oauthSchemeWithScopes
import art.galushko.openapi.testgen.rules.auth.fixtures.openApiWithGlobalSecurity
import art.galushko.openapi.testgen.rules.auth.fixtures.openApiWithSchemes
import art.galushko.openapi.testgen.rules.auth.fixtures.openIdScheme
import art.galushko.openapi.testgen.rules.auth.fixtures.securityRequirementOf
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.security.SecurityScheme
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Epic("Auth Rules")
@Feature("Insufficient scopes Auth Rule")
@Suppress("LongMethod")
class InsufficientScopesAuthValidationRuleTest {
    private val rule = InsufficientScopesAuthValidationRule()

    fun decideProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Operation with OAuth2 scopes",
            Operation().security(listOf(securityRequirementOf("oauth" to listOf("read")))),
            openApiWithSchemes(mapOf("oauth" to oauthSchemeWithScopes("read"))),
            true,
        ),
        Arguments.of(
            "Operation without scoped requirements but OpenAPI has global scoped security",
            Operation(),
            openApiWithSchemes(mapOf("oauth" to oauthSchemeWithScopes("manage")))
                .security(listOf(securityRequirementOf("oauth" to listOf("manage")))),
            true,
        ),
        Arguments.of(
            "Operation with OAuth2 scheme but empty scopes",
            Operation().security(listOf(securityRequirementOf("oauth" to emptyList()))),
            openApiWithSchemes(mapOf("oauth" to oauthScheme())),
            false,
        ),
        Arguments.of(
            "Operation with API key only",
            Operation().security(listOf(securityRequirementOf("api_key" to emptyList()))),
            openApiWithSchemes(mapOf("api_key" to apiKeyScheme(SecurityScheme.In.HEADER))),
            false,
        ),
        Arguments.of(
            "Operation with empty security array overriding global scoped security",
            Operation().security(listOf()),
            openApiWithGlobalSecurity("oauth", oauthSchemeWithScopes("read")),
            false,
        ),
        Arguments.of(
            "Operation with 2 OR requirements - two with scopes",
            Operation().security(
                listOf(
                    securityRequirementOf("api_key" to emptyList()),
                    securityRequirementOf("openid" to listOf("profile", "email"))
                )
            ),
            openApiWithSchemes(
                mapOf(
                    "api_key" to apiKeyScheme(SecurityScheme.In.HEADER),
                    "openid" to openIdScheme()
                )
            ),
            true,
        ),
    )

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("decideProvider")
    @DisplayName("decide should return true only when scoped auth schemes exist")
    @Description("Validates that rule activation aligns with presence of scoped security requirements")
    fun decideShouldRespectScopedSecurity(
        scenario: String,
        operation: Operation,
        openAPI: OpenAPI,
        expected: Boolean,
    ) {
        val result = step("Call decide") { rule.decide(createTestContext(createBasicTestCase(), operation, openAPI)) }
        assertThat(result).`as`("decide should return %s for %s", expected, scenario).isEqualTo(expected)
    }

    @Test
    @DisplayName("apply should generate all missing scope combinations with forbidden expectations")
    @Description("Ensures negative cases enumerate every subset of missing scopes while preserving security placement")
    fun applyShouldGenerateMissingScopeCombinations() {
        val apiKey = apiKeyScheme(SecurityScheme.In.HEADER)
        val oauth = oauthSchemeWithScopes("read", "write")
        val openid = openIdScheme()

        val openAPI = step("Compose OpenAPI with scoped OAuth2 and OpenID schemes") {
            openApiWithSchemes(
                mapOf(
                    "api_key" to apiKey,
                    "oauth" to oauth,
                    "openid" to openid,
                )
            )
        }

        val operation = step("Create operation with combined security requirement") {
            Operation().security(
                listOf(
                    securityRequirementOf(
                        "api_key" to emptyList(),
                        "oauth" to listOf("read", "write"),
                        "openid" to listOf("email"),
                    )
                )
            )
        }

        val validAuthorizationValue = "<oauth:[read,write]&openid:[email]>"
        val validApiKeyValue = "<valid_api_key_api_key_placeholder>"

        val validCase = step("Create valid test case with populated security values") {
            val apiKeyHeader = API_KEY_HEADER_NAME with validApiKeyValue
            val authorizationHeader = AUTHORIZATION_HEADER with validAuthorizationValue
            createBasicTestCase(
                headers = listOf(apiKeyHeader, authorizationHeader),
                securityValues = SecurityValues(headers = listOf(apiKeyHeader, authorizationHeader)),
            )
        }

        val testCases = step("Call apply") {
            rule.apply(createTestContext(validCase, operation, openAPI)).toList()
        }

        val expectedApiKey = API_KEY_HEADER_NAME with validApiKeyValue

        val basicTestCase = createBasicTestCase(
            expectedStatusCode = 403,
            rule = InsufficientScopesAuthValidationRule::class.java.name,
        )
        val expectedCases = listOf(
            basicTestCase.copyWithHeaders(
                "Missing [email] scopes of openid in security",
                listOf(expectedApiKey, AUTHORIZATION_HEADER with "<oauth:[read,write]&openid:[]>"),
            ),
            basicTestCase.copyWithHeaders(
                "Missing [write] scopes of oauth and [email] scopes of openid in security",
                listOf(expectedApiKey, AUTHORIZATION_HEADER with "<oauth:[read]&openid:[]>"),
            ),
            basicTestCase.copyWithHeaders(
                "Missing [read] scopes of oauth and [email] scopes of openid in security",
                listOf(expectedApiKey, AUTHORIZATION_HEADER with "<oauth:[write]&openid:[]>"),
            ),
            basicTestCase.copyWithHeaders(
                "Missing [read,write] scopes of oauth and [email] scopes of openid in security",
                listOf(expectedApiKey, AUTHORIZATION_HEADER with "<oauth:[]&openid:[]>"),
            ),
            basicTestCase.copyWithHeaders(
                "Missing [read,write] scopes of oauth in security",
                listOf(expectedApiKey, AUTHORIZATION_HEADER with "<oauth:[]&openid:[email]>"),
            ),
            basicTestCase.copyWithHeaders(
                "Missing [write] scopes of oauth in security",
                listOf(expectedApiKey, AUTHORIZATION_HEADER with "<oauth:[read]&openid:[email]>"),
            ),
            basicTestCase.copyWithHeaders(
                "Missing [read] scopes of oauth in security",
                listOf(expectedApiKey, AUTHORIZATION_HEADER with "<oauth:[write]&openid:[email]>"),
            ),
        )

        assertThat(testCases).containsExactlyInAnyOrderElementsOf(expectedCases)
    }

    @Test
    @DisplayName("apply should return empty sequence when no scoped schemes exist")
    @Description("Ensures rule skips generation when requirements lack scopes")
    fun applyShouldReturnEmptyWithoutScopedSchemes() {
        val apiKey = apiKeyScheme(SecurityScheme.In.HEADER)
        val openAPI = openApiWithSchemes(mapOf("api_key" to apiKey))
        val operation = Operation().security(listOf(securityRequirementOf("api_key" to emptyList())))

        val testCases = step("Call apply without scoped schemes") {
            rule.apply(createTestContext(createBasicTestCase(), operation, openAPI)).toList()
        }

        assertThat(testCases).isEmpty()
    }

    @Test
    @DisplayName("apply should generate missing scope combinations for single OAuth2 scheme")
    @Description("Verifies test generation for a simple OAuth2-only requirement without other schemes")
    fun applyShouldGenerateMissingScopeCombinationsForSingleOAuth2() {
        val oauth = oauthSchemeWithScopes("read", "write")
        val openAPI = step("Create OpenAPI with single OAuth2 scheme") {
            openApiWithSchemes(mapOf("oauth" to oauth))
        }

        val operation = step("Create operation with OAuth2 requirement") {
            Operation().security(listOf(securityRequirementOf("oauth" to listOf("read", "write"))))
        }

        val validAuthorization = "<oauth:[read,write]>"
        val validCase = step("Create valid test case with OAuth2 authorization") {
            createBasicTestCase(
                headers = listOf(AUTHORIZATION_HEADER with validAuthorization),
                securityValues = SecurityValues(headers = listOf(AUTHORIZATION_HEADER with validAuthorization)),
            )
        }

        val testCases = step("Call apply for single OAuth2 scheme") {
            rule.apply(createTestContext(validCase, operation, openAPI)).toList()
        }

        // Should generate: [], [read], [write] = 3 subsets (excluding complete set [read, write])
        assertThat(testCases).hasSize(3)

        val expectedTestCaseNames = setOf(
            "Missing [read,write] scopes of oauth in security",
            "Missing [write] scopes of oauth in security",
            "Missing [read] scopes of oauth in security",
        )

        assertThat(testCases.map { it.name }).containsExactlyInAnyOrderElementsOf(expectedTestCaseNames)

        testCases.forEach { testCase ->
            assertSoftly { softly ->
                softly.assertThat(testCase.expectedStatusCode).isEqualTo(403)
                softly.assertThat(testCase.rule).isEqualTo(InsufficientScopesAuthValidationRule::class.java.name)
                softly.assertThat(testCase.cookie).isEmpty()
                softly.assertThat(testCase.queryParams).isEmpty()
            }
        }
    }

    @Test
    @DisplayName("apply should generate missing scope combinations for single OpenID Connect scheme")
    @Description("Verifies test generation for OpenID Connect with scopes independent of OAuth2")
    fun applyShouldGenerateMissingScopeCombinationsForSingleOpenIDConnect() {
        val openid = openIdScheme()
        val openAPI = step("Create OpenAPI with single OpenID Connect scheme") {
            openApiWithSchemes(mapOf("openid" to openid))
        }

        val operation = step("Create operation with OpenID Connect requirement with scopes") {
            Operation().security(listOf(securityRequirementOf("openid" to listOf("profile", "email"))))
        }

        val validAuthorization = "<openid:[profile,email]>"
        val validCase = step("Create valid test case with OpenID Connect authorization") {
            createBasicTestCase(
                headers = listOf(AUTHORIZATION_HEADER with validAuthorization),
                securityValues = SecurityValues(headers = listOf(AUTHORIZATION_HEADER with validAuthorization)),
            )
        }

        val testCases = step("Call apply for single OpenID Connect scheme") {
            rule.apply(createTestContext(validCase, operation, openAPI)).toList()
        }

        // Should generate: [], [profile], [email] = 3 subsets (excluding complete set [profile, email])
        assertThat(testCases).hasSize(3)

        val expectedTestCaseNames = setOf(
            "Missing [email,profile] scopes of openid in security",
            "Missing [profile] scopes of openid in security",
            "Missing [email] scopes of openid in security",
        )

        assertThat(testCases.map { it.name }).containsExactlyInAnyOrderElementsOf(expectedTestCaseNames)

        testCases.forEach { testCase ->
            assertSoftly { softly ->
                softly.assertThat(testCase.expectedStatusCode).isEqualTo(403)
                softly.assertThat(testCase.rule).isEqualTo(InsufficientScopesAuthValidationRule::class.java.name)
                softly.assertThat(testCase.headers).hasSize(1)
                softly.assertThat(testCase.cookie).isEmpty()
                softly.assertThat(testCase.queryParams).isEmpty()
            }
        }
    }

    @Test
    @DisplayName("apply should generate missing scope tests for each OR alternative with scopes")
    @Description("Validates that scope combinations are generated for multiple OR alternatives independently")
    fun applyShouldHandleTripleOrRequirementsWithScopes() {
        val apiKey = apiKeyScheme(SecurityScheme.In.HEADER)
        val oauth = oauthSchemeWithScopes("read", "write")
        val openid = openIdScheme()

        val openAPI = step("Create OpenAPI with 3 OR requirements") {
            openApiWithSchemes(
                mapOf(
                    "api_key" to apiKey,
                    "oauth" to oauth,
                    "openid" to openid
                )
            )
        }

        val operation = step("Create operation with 3 OR alternatives") {
            Operation().security(
                listOf(
                    securityRequirementOf("api_key" to emptyList()),
                    securityRequirementOf("oauth" to listOf("read", "write")),
                    securityRequirementOf("openid" to listOf("profile", "email"))
                )
            )
        }

        val validCase = createBasicTestCase()

        val testCases = step("Call apply") {
            rule.apply(createTestContext(validCase, operation, openAPI)).toList()
        }

        // Should generate scope combinations for oauth and openid
        // OAuth: [], [read], [write] = 3 tests
        // OpenID: [], [profile], [email] = 3 tests
        // Total: exactly 6 tests
        assertSoftly { softly ->
            softly.assertThat(testCases).hasSize(6)

            // All should expect 403 Forbidden with correct rule name
            softly.assertThat(testCases).allMatch { it.expectedStatusCode == 403 }
            softly.assertThat(testCases).allMatch { it.rule == InsufficientScopesAuthValidationRule::class.java.name }

            // Verify exact oauth scope test names
            val expectedNames = setOf(
                "Missing [read,write] scopes of oauth in security",
                "Missing [write] scopes of oauth in security",
                "Missing [read] scopes of oauth in security",
                "Missing [email,profile] scopes of openid in security",
                "Missing [email] scopes of openid in security",
                "Missing [profile] scopes of openid in security",
            )
            softly.assertThat(testCases.map { it.name }).containsExactlyInAnyOrderElementsOf(expectedNames)
        }
    }

    @Test
    @DisplayName("apply should generate exactly one test case for single scope edge case")
    @Description("Verifies that when a requirement has only one scope, exactly one test with empty scopes is generated")
    fun applyShouldGenerateExactlyOneTestForSingleScope() {
        val oauth = oauthSchemeWithScopes("admin")
        val openAPI = step("Create OpenAPI with OAuth2 scheme having single scope") {
            openApiWithSchemes(mapOf("oauth" to oauth))
        }

        val operation = step("Create operation with single scope requirement") {
            Operation().security(listOf(securityRequirementOf("oauth" to listOf("admin"))))
        }

        val validAuthorization = "<oauth:[admin]>"
        val validCase = step("Create valid test case") {
            createBasicTestCase(
                headers = listOf(AUTHORIZATION_HEADER with validAuthorization),
                securityValues = SecurityValues(headers = listOf(AUTHORIZATION_HEADER with validAuthorization)),
            )
        }

        val testCases = step("Call apply") {
            rule.apply(createTestContext(validCase, operation, openAPI)).toList()
        }

        // Should generate exactly 1 subset: [] (empty set, missing the admin scope)
        assertThat(testCases).hasSize(1)
        val testCase = testCases.single()

        assertSoftly { softly ->
            softly.assertThat(testCase.name).isEqualTo("Missing [admin] scopes of oauth in security")
            softly.assertThat(testCase.expectedStatusCode).isEqualTo(403)
            softly.assertThat(testCase.rule).isEqualTo(InsufficientScopesAuthValidationRule::class.java.name)
        }
    }
}


