package art.galushko.openapi.testgen.rules.auth

import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.model.SecurityValues
import art.galushko.openapi.testgen.model.with
import art.galushko.openapi.testgen.openapi.SecurityHelpers.AUTHORIZATION_HEADER
import art.galushko.openapi.testgen.rules.auth.fixtures.apiKeyScheme
import art.galushko.openapi.testgen.rules.auth.fixtures.bearerScheme
import art.galushko.openapi.testgen.rules.auth.fixtures.oauthSchemeWithScopes
import art.galushko.openapi.testgen.rules.auth.fixtures.openApiWithGlobalSecurity
import art.galushko.openapi.testgen.rules.auth.fixtures.openApiWithSchemes
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Epic("Auth Rules")
@Feature("Missing security values Auth Rule")
@Suppress("LongMethod")
class MissingSecurityValuesAuthValidationRuleTest {
    private val rule = MissingSecurityValuesAuthValidationRule()

    fun decideProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Operation with API Key and OAuth security in a single requirement (size>1)",
            operationWithSecurity(requirement("api_key", "oauth")),
            openApiWithSchemes(
                mapOf(
                    "api_key" to apiKeyScheme(SecurityScheme.In.HEADER),
                    "oauth" to SecurityScheme().type(SecurityScheme.Type.OAUTH2)
                )
            ),
            true,
        ),
        Arguments.of(
            "Operation with API Key and OAuth security in a different requirements",
            operationWithSecurity(requirement("api_key"), requirement("oauth")),
            openApiWithSchemes(
                mapOf(
                    "api_key" to apiKeyScheme(SecurityScheme.In.HEADER),
                    "oauth" to SecurityScheme().type(SecurityScheme.Type.OAUTH2)
                )
            ),
            false,
        ),
        Arguments.of(
            "Operation with OAuth, OpenID, and basic security in a non-single authorization header requirement",
            operationWithSecurity(requirement("openId", "oauth", "basic")),
            openApiWithSchemes(
                mapOf(
                    "oauth" to SecurityScheme().type(SecurityScheme.Type.OAUTH2),
                    "openId" to SecurityScheme().type(SecurityScheme.Type.OPENIDCONNECT),
                    "basic" to SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")
                )
            ),
            false,
        ),
        Arguments.of(
            "Operation with only API Key security with different names and same `in` (size=2)",
            operationWithSecurity(requirement("api_key", "api_key_2")),
            openApiWithSchemes(
                mapOf(
                    "api_key" to apiKeyScheme(SecurityScheme.In.HEADER).name("api_key"),
                    "api_key_2" to apiKeyScheme(SecurityScheme.In.HEADER).name("api_key_2")
                )
            ),
            true,
        ),
        Arguments.of(
            "Operation with only API Key security with same names and same `in` (size=2)",
            operationWithSecurity(requirement("api_key", "api_key_2")),
            openApiWithSchemes(
                mapOf(
                    "api_key" to apiKeyScheme(SecurityScheme.In.HEADER).name("api_key"),
                    "api_key_2" to apiKeyScheme(SecurityScheme.In.HEADER).name("api_key")
                )
            ),
            false,
        ),
        Arguments.of(
            "Operation with only API Key security with same names and different `in` (size=2)",
            operationWithSecurity(requirement("api_key", "api_key_2")),
            openApiWithSchemes(
                mapOf(
                    "api_key" to apiKeyScheme(SecurityScheme.In.HEADER).name("api_key"),
                    "api_key_2" to apiKeyScheme(SecurityScheme.In.QUERY).name("api_key")
                )
            ),
            true,
        ),
        Arguments.of(
            "Operation with only API Key security (size=1)",
            operationWithSecurity(requirement("api_key")),
            openApiWithSchemes(mapOf("api_key" to apiKeyScheme(SecurityScheme.In.HEADER))),
            false,
        ),
        Arguments.of(
            "Operation with empty security array overriding global security",
            Operation().security(listOf()),
            openApiWithGlobalSecurity("api_key", apiKeyScheme(SecurityScheme.In.HEADER)),
            false,
        ),
        Arguments.of(
            "Operation with 2 OR requirements - different scheme types (should be false)",
            operationWithSecurity(
                requirement("oauth"),
                requirement("basic")
            ),
            openApiWithSchemes(
                mapOf(
                    "oauth" to oauthSchemeWithScopes("read", "write"),
                    "basic" to SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")
                )
            ),
            false,
        ),
        Arguments.of(
            "Operation with 3 OR requirements - one compound (should be true)",
            operationWithSecurity(
                requirement("api_key_plus_oauth", "oauth"),
                requirement("basic"),
                requirement("openid")
            ),
            openApiWithSchemes(
                mapOf(
                    "api_key_plus_oauth" to apiKeyScheme(SecurityScheme.In.QUERY),
                    "oauth" to SecurityScheme().type(SecurityScheme.Type.OAUTH2),
                    "basic" to SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic"),
                    "openid" to SecurityScheme().type(SecurityScheme.Type.OPENIDCONNECT)
                        .openIdConnectUrl("https://example.com/.well-known/openid-configuration")
                )
            ),
            true,
        ),
    )

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("decideProvider")
    @DisplayName("decide should return true only when requirement group has >1 scheme")
    @Description("Rule applies when at least one reduced requirement group contains more than one scheme")
    fun decideCoversMultiSchemeGroups(
        scenario: String,
        operation: Operation,
        openAPI: OpenAPI,
        expected: Boolean,
    ) {
        val result = step("Call decide") { rule.decide(createTestContext(createBasicTestCase(), operation, openAPI)) }
        assertThat(result).`as`("decide should return %s for: %s", expected, scenario).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(SecurityScheme.In::class)
    @DisplayName("apply should generate subsets for multi-scheme requirement and populate values")
    @Description("Generates all non-empty subsets for API Key (+placement) and Authorization header, with correct names and placements")
    fun applyGeneratesAllSubsets(apiKeyPlace: SecurityScheme.In) {
        val openAPI = step("Create OpenAPI with API Key and Bearer security schemes") {
            openApiWithSchemes(mapOf("api_key" to apiKeyScheme(apiKeyPlace), "bearer" to bearerScheme()))
        }

        val operation = step("Create operation with combined requirement (api_key + bearer)") {
            Operation().security(listOf(SecurityRequirement().addList("api_key").addList("bearer")))
        }

        val validCase = step("Create valid case with both API Key and Authorization values present") {
            when (apiKeyPlace) {
                SecurityScheme.In.HEADER -> {
                    val headers = listOf("X-API-Key" with "valid-api-key", AUTHORIZATION_HEADER with "Bearer valid")
                    createBasicTestCase(headers = headers, securityValues = SecurityValues(headers = headers))
                }

                SecurityScheme.In.COOKIE -> {
                    val cookie = listOf("X-API-Key" with "valid-api-key")
                    val headers = listOf(AUTHORIZATION_HEADER with "Bearer valid")
                    createBasicTestCase(cookie = cookie, headers = headers, securityValues = SecurityValues(cookie = cookie, headers = headers))
                }

                SecurityScheme.In.QUERY -> {
                    val query = mapOf("X-API-Key" to "valid-api-key")
                    val headers = listOf(AUTHORIZATION_HEADER with "Bearer valid")
                    createBasicTestCase(queryParams = query, headers = headers, securityValues = SecurityValues(queryParams = query, headers = headers))
                }
            }
        }

        val testCases = step("Call apply") { rule.apply(createTestContext(validCase, operation, openAPI)).toList() }

        val base = createBasicTestCase(rule = MissingSecurityValuesAuthValidationRule::class.java.name, expectedStatusCode = 401)
        val onlyAuth = base.copy(
            name = "Missing X-API-Key API key security",
            securityValues = SecurityValues(headers = listOf("authorization" with "<valid_bearer_placeholder>")),
            headers = listOf("authorization" with "<valid_bearer_placeholder>"),
        )
        val onlyApiKey = when (apiKeyPlace) {
            SecurityScheme.In.HEADER -> base.copy(
                name = "Missing Authorization header security",
                securityValues = SecurityValues(headers = listOf("X-API-Key" with "<valid_api_key_api_key_placeholder>")),
                headers = listOf("X-API-Key" with "<valid_api_key_api_key_placeholder>"),
            )

            SecurityScheme.In.COOKIE -> base.copy(
                name = "Missing Authorization header security",
                securityValues = SecurityValues(cookie = listOf("X-API-Key" with "<valid_api_key_api_key_placeholder>")),
                cookie = listOf("X-API-Key" with "<valid_api_key_api_key_placeholder>"),
            )

            SecurityScheme.In.QUERY -> base.copy(
                name = "Missing Authorization header security",
                securityValues = SecurityValues(queryParams = mapOf("X-API-Key" to "<valid_api_key_api_key_placeholder>")),
                queryParams = mapOf("X-API-Key" to "<valid_api_key_api_key_placeholder>"),
            )
        }
        assertThat(testCases).`as`("Test cases stream should contain 2 test cases").containsExactlyInAnyOrder(onlyAuth, onlyApiKey)
    }

    @Test
    @DisplayName("apply should generate subsets for multi-scheme requirement and populate values (size>2)")
    @Description("Generates all non-empty subsets for API Key (+placement) and Authorization header, with correct names and placements")
    fun applyGeneratesAllSubsetsSize3() {
        val openAPI = step("Create OpenAPI with API Key and Bearer security schemes") {
            OpenAPI().components(
                Components()
                    .addSecuritySchemes("api_key", SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.HEADER).name("X-API-Key"))
                    .addSecuritySchemes("JSESSIONID", SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.COOKIE).name("JSESSIONID"))
                    .addSecuritySchemes("bearer", SecurityScheme().type(SecurityScheme.Type.HTTP))
            )
        }

        val operation = step("Create operation with combined requirement (api_key + bearer)") {
            Operation().security(listOf(SecurityRequirement().addList("api_key").addList("bearer").addList("JSESSIONID")))
        }

        val apiKey = "X-API-Key" with "<valid_api_key_api_key_placeholder>"
        val bearer = "authorization" with "<valid_bearer_placeholder>"
        val session = "JSESSIONID" with "<valid_JSESSIONID_api_key_placeholder>"
        val validCase = step("Create valid case with API Key, session, and Authorization values present") {
            createBasicTestCase(
                headers = listOf(apiKey, bearer),
                cookie = listOf(session),
                securityValues = SecurityValues(headers = listOf(apiKey, bearer), cookie = listOf(session))
            )
        }

        val testCases = step("Call apply") { rule.apply(createTestContext(validCase, operation, openAPI)).toList() }

        val base = createBasicTestCase(rule = MissingSecurityValuesAuthValidationRule::class.java.name, expectedStatusCode = 401)
        val expected = listOf(
            base.copy(
                name = "Missing X-API-Key API key security",
                securityValues = SecurityValues(headers = listOf(bearer), cookie = listOf(session)),
                headers = listOf(bearer),
                cookie = listOf(session),
            ),
            base.copy(
                name = "Missing JSESSIONID API key security",
                securityValues = SecurityValues(headers = listOf(apiKey, bearer)),
                headers = listOf(apiKey, bearer),
            ),
            base.copy(
                name = "Missing Authorization header security",
                securityValues = SecurityValues(headers = listOf(apiKey), cookie = listOf(session)),
                headers = listOf(apiKey),
                cookie = listOf(session),
            ),
            base.copy(
                name = "Missing X-API-Key API key and Authorization header security",
                securityValues = SecurityValues(cookie = listOf(session)),
                cookie = listOf(session),
            ),
            base.copy(
                name = "Missing Authorization header and JSESSIONID API key security",
                securityValues = SecurityValues(headers = listOf(apiKey)),
                headers = listOf(apiKey),
            ),
            base.copy(
                name = "Missing X-API-Key API key and JSESSIONID API key security",
                securityValues = SecurityValues(headers = listOf(bearer)),
                headers = listOf(bearer),
            ),
        )
        assertThat(testCases).`as`("Test cases stream should contain expected test cases").containsExactlyInAnyOrderElementsOf(expected)
    }

    private fun operationWithSecurity(vararg req: SecurityRequirement): Operation {
        return Operation().security(req.toList())
    }

    private fun requirement(vararg names: String): SecurityRequirement {
        val req = SecurityRequirement()
        names.forEach { req.addList(it) }
        return req
    }
}


