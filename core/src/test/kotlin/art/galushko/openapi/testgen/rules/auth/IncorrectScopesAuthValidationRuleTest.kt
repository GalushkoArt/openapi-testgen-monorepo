package art.galushko.openapi.testgen.rules.auth

import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.model.SecurityValues
import art.galushko.openapi.testgen.model.with
import art.galushko.openapi.testgen.openapi.SecurityHelpers.AUTHORIZATION_HEADER
import art.galushko.openapi.testgen.rules.auth.fixtures.API_KEY_HEADER_NAME
import art.galushko.openapi.testgen.rules.auth.fixtures.apiKeyScheme
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
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Epic("Auth Rules")
@Feature("Incorrect scopes Auth Rule")
@Suppress("LongMethod")
class IncorrectScopesAuthValidationRuleTest {
    private val rule = IncorrectScopesAuthValidationRule()

    fun decideProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Operation with OAuth2 scopes",
            Operation().security(listOf(securityRequirementOf("oauth" to listOf("write")))),
            openApiWithSchemes(mapOf("oauth" to oauthSchemeWithScopes("write"))),
            true,
        ),
        Arguments.of(
            "Global security with scopes and operation without",
            Operation(),
            openApiWithSchemes(mapOf("oauth" to oauthSchemeWithScopes("read")))
                .security(listOf(securityRequirementOf("oauth" to listOf("read")))),
            true,
        ),
        Arguments.of(
            "Operation with OAuth2 scheme but no scopes",
            Operation().security(listOf(securityRequirementOf("oauth" to emptyList()))),
            openApiWithSchemes(mapOf("oauth" to oauthScheme())),
            false,
        ),
        Arguments.of(
            "Operation with API key only",
            Operation().security(listOf(SecurityRequirement().addList("api_key"))),
            openApiWithSchemes(mapOf("api_key" to apiKeyScheme(SecurityScheme.In.HEADER))),
            false,
        ),
        Arguments.of(
            "Operation with empty security array overriding global scoped security",
            Operation().security(listOf()),
            openApiWithGlobalSecurity("oauth", oauthSchemeWithScopes("read")),
            false,
        ),
    )

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("decideProvider")
    @DisplayName("decide should return true only when scoped schemes exist")
    @Description("Validates rule activation is driven by scoped authentication requirements")
    fun decideShouldMatchScopedSecurity(
        scenario: String,
        operation: Operation,
        openAPI: OpenAPI,
        expected: Boolean,
    ) {
        val result = step("Call decide") { rule.decide(createTestContext(createBasicTestCase(), operation, openAPI)) }
        assertThat(result).`as`("decide should be %s for %s", expected, scenario).isEqualTo(expected)
    }

    @Test
    @DisplayName("apply should inject invalid scope per scoped scheme")
    @Description("Ensures one negative case per scoped scheme with invalid scope while preserving other security values")
    fun applyShouldInjectInvalidScopesPerScheme() {
        val oauth = oauthSchemeWithScopes("read", "write")
        val openApi = openApiWithSchemes(mapOf("oauth" to oauth))

        val operation = Operation().security(listOf(securityRequirementOf("oauth" to listOf("read", "write"))))

        val validAuthorization = "<oauth:[read,write]>"
        val validCase = createBasicTestCase(
            headers = listOf(AUTHORIZATION_HEADER with validAuthorization),
            securityValues = SecurityValues(headers = listOf(AUTHORIZATION_HEADER with validAuthorization)),
        )

        val testCases = step("Call apply") { rule.apply(createTestContext(validCase, operation, openApi)).toList() }

        val expectedAuthorization = AUTHORIZATION_HEADER with "<oauth:[some_invalid_scope]>"
        assertThat(testCases).hasSize(1).first()
            .isEqualTo(
                validCase.copy(
                    expectedStatusCode = 403,
                    rule = IncorrectScopesAuthValidationRule::class.java.name,
                    name = "oauth security scheme has invalid scope",
                    securityValues = SecurityValues(headers = listOf(expectedAuthorization)),
                    headers = listOf(expectedAuthorization),
                )
            )
    }

    @Test
    @DisplayName("apply should keep API key values while mutating authorization scope")
    @Description("Verifies API key placements remain unchanged when invalid scope is applied to another scheme")
    fun applyShouldPreserveApiKeyValues() {
        val apiKey = apiKeyScheme(SecurityScheme.In.QUERY)
        val oauth = oauthSchemeWithScopes("email")

        val openAPI = openApiWithSchemes(mapOf("api_key" to apiKey, "oauth" to oauth))
        val operation = Operation().security(listOf(securityRequirementOf("api_key" to emptyList(), "oauth" to listOf("email"))))

        val validAuthorization = "<oauth:[some_invalid_scope]>"
        val validApiKeyValue = "<valid_api_key_api_key_placeholder>"
        val apiKeyEntry = API_KEY_HEADER_NAME to validApiKeyValue
        val authorizationPair = AUTHORIZATION_HEADER with validAuthorization
        val validCase = createBasicTestCase(
            headers = listOf(authorizationPair),
            queryParams = mapOf(apiKeyEntry),
            securityValues = SecurityValues(
                headers = listOf(authorizationPair),
                queryParams = mapOf(apiKeyEntry),
            ),
        )

        val testCases = step("Call apply for multi-scheme requirement") {
            rule.apply(createTestContext(validCase, operation, openAPI)).toList()
        }

        val expectedAuthorization = AUTHORIZATION_HEADER with "<oauth:[some_invalid_scope]>"
        assertThat(testCases).hasSize(1).first()
            .isEqualTo(
                validCase.copy(
                    expectedStatusCode = 403,
                    rule = IncorrectScopesAuthValidationRule::class.java.name,
                    name = "oauth security scheme has invalid scope",
                    queryParams = mapOf(apiKeyEntry),
                    securityValues = SecurityValues(headers = listOf(expectedAuthorization), queryParams = mapOf(apiKeyEntry)),
                    headers = listOf(expectedAuthorization),
                )
            )
    }

    @Test
    @DisplayName("apply should return empty when no scoped requirement exists")
    @Description("Ensures rule does not generate cases for operations without scoped schemes")
    fun applyShouldReturnEmptyWithoutScopes() {
        val apiKey = apiKeyScheme(SecurityScheme.In.HEADER)
        val openAPI = openApiWithSchemes(mapOf("api_key" to apiKey))
        val operation = Operation().security(listOf(securityRequirementOf("api_key" to emptyList())))

        val result = step("Call apply without scoped requirements") {
            rule.apply(createTestContext(createBasicTestCase(), operation, openAPI)).toList()
        }

        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("apply should generate test case per scoped scheme in multi-scheme requirement")
    @Description("Verifies one invalid scope test is generated for each scoped scheme when multiple schemes exist in one requirement")
    fun applyShouldGenerateTestPerScopedSchemeInMultiScheme() {
        val oauth = oauthSchemeWithScopes("read", "write")
        val openid = openIdScheme()
        val openAPI = openApiWithSchemes(mapOf("oauth" to oauth, "openid" to openid))

        val operation = Operation().security(
            listOf(securityRequirementOf("oauth" to listOf("read", "write"), "openid" to listOf("profile")))
        )

        val validAuthorization = "<oauth:[read,write]&openid:[profile]>"
        val validCase = createBasicTestCase(
            headers = listOf(AUTHORIZATION_HEADER with validAuthorization),
            securityValues = SecurityValues(headers = listOf(AUTHORIZATION_HEADER with validAuthorization)),
        )

        val testCases = step("Call apply for multi-scoped-scheme requirement") {
            rule.apply(createTestContext(validCase, operation, openAPI)).toList()
        }

        // Should generate 2 test cases: one for oauth with invalid scope, one for openid with invalid scope
        assertThat(testCases).hasSize(2).containsExactlyInAnyOrder(
            validCase.copy(
                expectedStatusCode = 403,
                rule = IncorrectScopesAuthValidationRule::class.java.name,
                name = "oauth security scheme has invalid scope",
                securityValues = SecurityValues(headers = listOf(AUTHORIZATION_HEADER with "<oauth:[some_invalid_scope]&openid:[profile]>")),
                headers = listOf(AUTHORIZATION_HEADER with "<oauth:[some_invalid_scope]&openid:[profile]>"),
            ),
            validCase.copy(
                expectedStatusCode = 403,
                rule = IncorrectScopesAuthValidationRule::class.java.name,
                name = "openid security scheme has invalid scope",
                securityValues = SecurityValues(headers = listOf(AUTHORIZATION_HEADER with "<oauth:[read,write]&openid:[some_invalid_scope]>")),
                headers = listOf(AUTHORIZATION_HEADER with "<oauth:[read,write]&openid:[some_invalid_scope]>"),
            ),
        )
    }

    @Test
    @DisplayName("apply should handle OpenID Connect with invalid scope")
    @Description("Verifies test generation for OpenID Connect scheme independent of OAuth2")
    fun applyShouldHandleOpenIDConnectWithInvalidScope() {
        val openid = openIdScheme()
        val openAPI = openApiWithSchemes(mapOf("openid" to openid))

        val operation = Operation().security(listOf(securityRequirementOf("openid" to listOf("email", "profile"))))

        val validAuthorization = "<openid:[email,profile]>"
        val validCase = createBasicTestCase(
            headers = listOf(AUTHORIZATION_HEADER with validAuthorization),
            securityValues = SecurityValues(headers = listOf(AUTHORIZATION_HEADER with validAuthorization)),
        )

        val testCases = step("Call apply for OpenID Connect") {
            rule.apply(createTestContext(validCase, operation, openAPI)).toList()
        }

        val expectedAuthorization = AUTHORIZATION_HEADER with "<openid:[some_invalid_scope]>"
        assertThat(testCases).hasSize(1).first()
            .isEqualTo(
                validCase.copy(
                    expectedStatusCode = 403,
                    rule = IncorrectScopesAuthValidationRule::class.java.name,
                    name = "openid security scheme has invalid scope",
                    securityValues = SecurityValues(headers = listOf(expectedAuthorization)),
                    headers = listOf(expectedAuthorization),
                )
            )
    }
}


