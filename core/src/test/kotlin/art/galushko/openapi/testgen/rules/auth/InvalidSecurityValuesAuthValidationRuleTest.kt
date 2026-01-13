package art.galushko.openapi.testgen.rules.auth

import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.model.SecurityValues
import art.galushko.openapi.testgen.model.with
import art.galushko.openapi.testgen.openapi.SecurityHelpers.AUTHORIZATION_HEADER
import art.galushko.openapi.testgen.rules.auth.fixtures.API_KEY_HEADER_NAME
import art.galushko.openapi.testgen.rules.auth.fixtures.API_KEY_SCHEME_NAME
import art.galushko.openapi.testgen.rules.auth.fixtures.apiKeyScheme
import art.galushko.openapi.testgen.rules.auth.fixtures.oauthSchemeWithScopes
import art.galushko.openapi.testgen.rules.auth.fixtures.openAPIWithApiKeyScheme
import art.galushko.openapi.testgen.rules.auth.fixtures.openAPIWithApiKeySchemeAndGlobalSecurity
import art.galushko.openapi.testgen.rules.auth.fixtures.openAPIWithOAuthScheme
import art.galushko.openapi.testgen.rules.auth.fixtures.openApiWithSchemes
import art.galushko.openapi.testgen.rules.auth.fixtures.operationWithSecurity
import art.galushko.openapi.testgen.rules.auth.fixtures.validCaseFor
import art.galushko.openapi.testgen.testdata.BasicTestDataProvider
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Epic("Auth Rules")
@Feature("Invalid security value Auth Rule")
@Suppress("LongMethod")
class InvalidSecurityValuesAuthValidationRuleTest {
    private val rule = InvalidSecurityValuesAuthValidationRule()

    fun securityRequirementsProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Operation with security requirement",
            operationWithSecurity(API_KEY_SCHEME_NAME),
            openAPIWithApiKeyScheme(API_KEY_SCHEME_NAME, SecurityScheme.In.HEADER),
            true
        ),
        Arguments.of(
            "Operation without security requirement, but OpenAPI with non-empty security requirement",
            Operation(),
            openAPIWithApiKeySchemeAndGlobalSecurity(API_KEY_SCHEME_NAME, SecurityScheme.In.HEADER),
            true
        ),
        Arguments.of(
            "Operation with non-API Key security requirement",
            operationWithSecurity("oauth"),
            openAPIWithOAuthScheme(),
            true
        ),
        Arguments.of("Operation and OpenAPI without security requirements", Operation(), OpenAPI(), false),
        Arguments.of(
            "Operation with empty security array overriding global security",
            Operation().security(listOf()),
            openAPIWithApiKeySchemeAndGlobalSecurity(API_KEY_SCHEME_NAME, SecurityScheme.In.HEADER),
            false
        )
    )

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("securityRequirementsProvider")
    @DisplayName("decide should return true when security security requirement exists")
    @Description("Verifies that the decide method returns true when there is a security requirement")
    fun decideShouldReturnTrueWhenApiKeySecurityRequirementExists(
        scenario: String,
        operation: Operation,
        openAPI: OpenAPI,
        expected: Boolean,
    ) {
        val result = step("Call decide") { rule.decide(createTestContext(createBasicTestCase(), operation, openAPI)) }
        assertThat(result).`as`("decide should return %s for the given security requirements", expected)
            .isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(SecurityScheme.In::class)
    @DisplayName("apply should return test case with invalid security")
    @Description("Verifies that the apply method returns a test case with an invalid security")
    fun applyShouldReturnTestCaseWithInvalidApiKey(place: SecurityScheme.In) {
        val openAPI = step("Create OpenAPI with security security scheme") {
            openAPIWithApiKeyScheme(API_KEY_SCHEME_NAME, place)
        }

        val operation = step("Create operation with security requirement") {
            Operation().security(listOf(SecurityRequirement().addList(API_KEY_SCHEME_NAME)))
        }

        val validCase = validCaseFor(place)

        val testCases = step("Call apply") { rule.apply(createTestContext(validCase, operation, openAPI)).toList() }

        assertSoftly { softly ->
            softly.assertThat(testCases).hasSize(1)
            val testCase = testCases.single()
            softly.assertThat(testCase.rule).isEqualTo(InvalidSecurityValuesAuthValidationRule::class.java.name)
            softly.assertThat(testCase.expectedStatusCode).isEqualTo(401)
            softly.assertThat(testCase.name).isEqualTo("Invalid $API_KEY_HEADER_NAME API key security")
            when (place) {
                SecurityScheme.In.QUERY -> {
                    softly.assertThat(testCase.queryParams).containsEntry(API_KEY_HEADER_NAME, BasicTestDataProvider().invalidApiKey())
                    softly.assertThat(testCase.securityValues.queryParams).containsEntry(API_KEY_HEADER_NAME, BasicTestDataProvider().invalidApiKey())
                }

                SecurityScheme.In.COOKIE -> {
                    softly.assertThat(testCase.cookie).contains(API_KEY_HEADER_NAME with BasicTestDataProvider().invalidApiKey())
                    softly.assertThat(testCase.securityValues.cookie).contains(API_KEY_HEADER_NAME with BasicTestDataProvider().invalidApiKey())
                }

                SecurityScheme.In.HEADER -> {
                    softly.assertThat(testCase.headers).contains(API_KEY_HEADER_NAME with BasicTestDataProvider().invalidApiKey())
                    softly.assertThat(testCase.securityValues.headers).contains(API_KEY_HEADER_NAME with BasicTestDataProvider().invalidApiKey())
                }
            }
        }
    }

    fun nonApiSecurityProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "OAuth2 security",
            openApiWithSchemes(mapOf("oauth" to SecurityScheme().type(SecurityScheme.Type.OAUTH2))),
            Operation().security(listOf(SecurityRequirement().addList("oauth")))
        ),
        Arguments.of(
            "OpenIdConnect security",
            openApiWithSchemes(mapOf("openIdConnect" to SecurityScheme().type(SecurityScheme.Type.OPENIDCONNECT))),
            Operation().security(listOf(SecurityRequirement().addList("openIdConnect")))
        ),
        Arguments.of(
            "HTTP Bearer security",
            openApiWithSchemes(mapOf("bearer" to SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer"))),
            Operation().security(listOf(SecurityRequirement().addList("bearer")))
        )
    )

    @Suppress("UnusedParameter")
    @ParameterizedTest(name = "{index}: invalid non-API key security sets Authorization header ({0})")
    @MethodSource("nonApiSecurityProvider")
    @DisplayName("apply should handle non-APIKEY schemes by setting invalid Authorization header")
    @Description("Verifies that for non-APIKEY schemes the rule produces cases with invalid Authorization header and proper naming")
    fun applyShouldHandleNonApiKeySchemes(description: String, openAPI: OpenAPI, operation: Operation) {
        val validCase = createBasicTestCase(headers = listOf(AUTHORIZATION_HEADER with "Bearer valid"))

        val testCases = step("Call apply") { rule.apply(createTestContext(validCase, operation, openAPI)).toList() }

        assertSoftly { softly ->
            softly.assertThat(testCases).hasSize(1)
            val first = testCases.first()
            softly.assertThat(first.rule).isEqualTo(InvalidSecurityValuesAuthValidationRule::class.java.name)
            softly.assertThat(first.name).contains("Invalid").contains("Authorization header")
            softly.assertThat(first.expectedStatusCode).isEqualTo(401)
            softly.assertThat(first.headers).contains(AUTHORIZATION_HEADER with BasicTestDataProvider().invalidAuthorizationHeader())
            softly.assertThat(first.securityValues.headers).contains(AUTHORIZATION_HEADER with BasicTestDataProvider().invalidAuthorizationHeader())
        }
    }

    @Test
    @DisplayName("apply should handle oauth2 with OpenId Connect schemes by setting invalid Authorization header")
    @Description("Verifies that for oauth2 with OpenId Connect schemes the rule produces cases with invalid Authorization header and proper naming")
    fun applyShouldHandleOauthWithOpenIdSchemes() {
        val validCase = createBasicTestCase(
            headers = listOf(AUTHORIZATION_HEADER with "Bearer valid"),
            securityValues = SecurityValues(headers = listOf(AUTHORIZATION_HEADER with "Bearer valid")),
        )

        val testCases = step("Call apply") {
            rule.apply(
                createTestContext(
                    validCase,
                    Operation().security(listOf(SecurityRequirement().addList("openIdConnect").addList("oauth"))),
                    OpenAPI().components(
                        io.swagger.v3.oas.models.Components()
                            .addSecuritySchemes("openIdConnect", SecurityScheme().type(SecurityScheme.Type.OPENIDCONNECT))
                            .addSecuritySchemes("oauth", SecurityScheme().type(SecurityScheme.Type.OAUTH2))
                    )
                )
            ).toList()
        }

        assertSoftly { softly ->
            softly.assertThat(testCases).hasSize(1)
            val first = testCases.first()
            softly.assertThat(first.rule).isEqualTo(InvalidSecurityValuesAuthValidationRule::class.java.name)
            softly.assertThat(first.name).contains("Invalid").contains("Authorization header")
            softly.assertThat(first.expectedStatusCode).isEqualTo(401)
            softly.assertThat(first.headers).contains(AUTHORIZATION_HEADER with BasicTestDataProvider().invalidAuthorizationHeader())
            softly.assertThat(first.securityValues.headers).contains(AUTHORIZATION_HEADER with BasicTestDataProvider().invalidAuthorizationHeader())
        }
    }

    @Test
    @DisplayName("apply should handle HTTP Basic authentication with invalid credentials")
    @Description("Verifies that for HTTP Basic scheme the rule produces cases with invalid Authorization header")
    fun applyShouldHandleHttpBasicScheme() {
        val openAPI = openApiWithSchemes(mapOf("basic" to SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
        val operation = Operation().security(listOf(SecurityRequirement().addList("basic")))
        val validCase = createBasicTestCase(headers = listOf(AUTHORIZATION_HEADER with "Basic dXNlcjpwYXNz"))

        val testCases = step("Call apply") { rule.apply(createTestContext(validCase, operation, openAPI)).toList() }

        assertSoftly { softly ->
            softly.assertThat(testCases).hasSize(1)
            val testCase = testCases.first()
            softly.assertThat(testCase.rule).isEqualTo(InvalidSecurityValuesAuthValidationRule::class.java.name)
            softly.assertThat(testCase.name).contains("Invalid").contains("Authorization header")
            softly.assertThat(testCase.expectedStatusCode).isEqualTo(401)
            softly.assertThat(testCase.headers).contains(AUTHORIZATION_HEADER with BasicTestDataProvider().invalidAuthorizationHeader())
            softly.assertThat(testCase.securityValues.headers).contains(AUTHORIZATION_HEADER with BasicTestDataProvider().invalidAuthorizationHeader())
        }
    }

    @Test
    @DisplayName("apply should handle 3 OR requirements generating tests for each alternative")
    @Description("Validates that multiple OR alternatives each produce independent invalid test cases")
    fun applyShouldHandleTripleOrRequirements() {
        val apiKey = apiKeyScheme(SecurityScheme.In.HEADER)
        val oauth = oauthSchemeWithScopes("read")
        val basic = SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")

        val openAPI = step("Create OpenAPI with 3 OR security requirements") {
            openApiWithSchemes(mapOf(
                "api_key" to apiKey,
                "oauth" to oauth,
                "basic" to basic
            ))
        }

        val operation = step("Create operation with 3 OR requirements") {
            Operation().security(listOf(
                SecurityRequirement().addList("api_key"),
                SecurityRequirement().addList("oauth"),
                SecurityRequirement().addList("basic")
            ))
        }

        val validCase = createBasicTestCase(
            headers = listOf(API_KEY_HEADER_NAME with "valid-key")
        )

        val testCases = step("Call apply") {
            rule.apply(createTestContext(validCase, operation, openAPI)).toList()
        }

        // Should generate tests for each OR alternative
        // After deduplication: api_key (header), Authorization (oauth + basic consolidated)
        // 2 distinct requirement groups × 1 subset each = exactly 2 test cases
        assertSoftly { softly ->
            softly.assertThat(testCases).hasSize(2)

            // All test cases should have correct rule and expect 401 Unauthorized
            softly.assertThat(testCases).allMatch { it.rule == InvalidSecurityValuesAuthValidationRule::class.java.name }
            softly.assertThat(testCases).allMatch { it.expectedStatusCode == 401 }

            // Verify exact test case names
            val expectedNames = setOf(
                "Invalid X-API-Key API key security",
                "Invalid Authorization header security"
            )
            softly.assertThat(testCases.map { it.name }).containsExactlyInAnyOrderElementsOf(expectedNames)

            // Verify api_key test case has invalid API key in header
            val apiKeyTest = testCases.first { it.name.contains("X-API-Key") }
            softly.assertThat(apiKeyTest.headers).contains(API_KEY_HEADER_NAME with BasicTestDataProvider().invalidApiKey())
            softly.assertThat(apiKeyTest.securityValues.headers).contains(API_KEY_HEADER_NAME with BasicTestDataProvider().invalidApiKey())

            // Verify Authorization header test case has invalid bearer token
            val authTest = testCases.first { it.name.contains("Authorization header") }
            softly.assertThat(authTest.headers).contains(AUTHORIZATION_HEADER with BasicTestDataProvider().invalidAuthorizationHeader())
            softly.assertThat(authTest.securityValues.headers).contains(AUTHORIZATION_HEADER with BasicTestDataProvider().invalidAuthorizationHeader())
        }
    }

    @Test
    @DisplayName("apply should generate subsets for multi-scheme requirement and populate values (size>2)")
    @Description("Generates all non-empty subsets for API Key (+placement) and Authorization header, with correct names and placements")
    fun applyGeneratesAllSubsetsSize3() {
        val openAPI = step("Create OpenAPI with API Key and Bearer security schemes") {
            OpenAPI().components(
                io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes(
                        API_KEY_SCHEME_NAME,
                        SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.HEADER).name(API_KEY_HEADER_NAME)
                    )
                    .addSecuritySchemes(
                        "JSESSIONID",
                        SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.COOKIE).name("JSESSIONID")
                    )
                    .addSecuritySchemes(
                        "bearer", SecurityScheme().type(SecurityScheme.Type.HTTP)
                    )
            )
        }

        val operation = step("Create operation with combined requirement (api_key + bearer)") {
            Operation().security(listOf(SecurityRequirement().addList("api_key").addList("bearer").addList("JSESSIONID")))
        }

        val apiKey = API_KEY_HEADER_NAME with "<valid_X-API-Key_api_key_placeholder>"
        val invalidApiKey = "X-API-Key" with "some_really_invalid_api_key"
        val bearer = AUTHORIZATION_HEADER with "<valid_bearer_placeholder>"
        val invalidBearer = "authorization" with "bearer some_really_invalid_authorization_header"
        val session = "JSESSIONID" with "<valid_JSESSIONID_api_key_placeholder>"
        val invalidSession = "JSESSIONID" with "some_really_invalid_api_key"
        val validCase = step("Create valid case with API Key, session, and Authorization values present") {
            createBasicTestCase(
                headers = listOf(apiKey, bearer),
                cookie = listOf(session),
                securityValues = SecurityValues(headers = listOf(apiKey, bearer), cookie = listOf(session))
            )
        }

        val testCases = step("Call apply") { rule.apply(createTestContext(validCase, operation, openAPI)).toList() }

        val base = createBasicTestCase(rule = InvalidSecurityValuesAuthValidationRule::class.java.name, expectedStatusCode = 401)
        val expected = listOf(
            base.copy(
                name = "Invalid X-API-Key API key security",
                securityValues = SecurityValues(headers = listOf(bearer, invalidApiKey), cookie = listOf(session)),
                headers = listOf(bearer, invalidApiKey),
                cookie = listOf(session),
            ),
            base.copy(
                name = "Invalid JSESSIONID API key security",
                securityValues = SecurityValues(headers = listOf(apiKey, bearer), cookie = listOf(invalidSession)),
                headers = listOf(apiKey, bearer),
                cookie = listOf(invalidSession),
            ),
            base.copy(
                name = "Invalid Authorization header security",
                securityValues = SecurityValues(headers = listOf(apiKey, invalidBearer), cookie = listOf(session)),
                headers = listOf(apiKey, invalidBearer),
                cookie = listOf(session),
            ),
            base.copy(
                name = "Invalid X-API-Key API key and Authorization header security",
                securityValues = SecurityValues(headers = listOf(invalidApiKey, invalidBearer), cookie = listOf(session)),
                cookie = listOf(session),
                headers = listOf(invalidApiKey, invalidBearer),
            ),
            base.copy(
                name = "Invalid Authorization header and JSESSIONID API key security",
                securityValues = SecurityValues(headers = listOf(apiKey, invalidBearer), cookie = listOf(invalidSession)),
                headers = listOf(apiKey, invalidBearer),
                cookie = listOf(invalidSession),
            ),
            base.copy(
                name = "Invalid X-API-Key API key and JSESSIONID API key security",
                securityValues = SecurityValues(headers = listOf(bearer, invalidApiKey), cookie = listOf(invalidSession)),
                headers = listOf(bearer, invalidApiKey),
                cookie = listOf(invalidSession),
            ),
            base.copy(
                name = "Invalid X-API-Key API key and Authorization header and JSESSIONID API key security",
                securityValues = SecurityValues(headers = listOf(invalidApiKey, invalidBearer), cookie = listOf(invalidSession)),
                headers = listOf(invalidApiKey, invalidBearer),
                cookie = listOf(invalidSession),
            ),
        )
        assertThat(testCases).`as`("Test cases stream should contain expected test cases").containsExactlyInAnyOrderElementsOf(expected)
    }
}
