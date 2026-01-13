package art.galushko.openapi.testgen.rules.auth

import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.model.SecurityValues
import art.galushko.openapi.testgen.model.with
import art.galushko.openapi.testgen.openapi.SecurityHelpers.AUTHORIZATION_HEADER
import art.galushko.openapi.testgen.rules.auth.fixtures.API_KEY_SCHEME_NAME
import art.galushko.openapi.testgen.rules.auth.fixtures.openAPIWithApiKeyScheme
import art.galushko.openapi.testgen.rules.auth.fixtures.openAPIWithApiKeySchemeAndGlobalSecurity
import art.galushko.openapi.testgen.rules.auth.fixtures.openAPIWithOAuthScheme
import art.galushko.openapi.testgen.rules.auth.fixtures.operationWithSecurity
import art.galushko.openapi.testgen.rules.auth.fixtures.validCaseFor
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
@Feature("All security missed Auth Rule")
class AllSecurityMissedAuthValidationRuleTest {
    private val rule = AllSecurityMissedAuthValidationRule()

    fun securityRequirementsProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Operation with API Key security requirement",
            operationWithSecurity(API_KEY_SCHEME_NAME),
            openAPIWithApiKeyScheme(API_KEY_SCHEME_NAME, SecurityScheme.In.HEADER),
            true
        ),
        Arguments.of(
            "Operation without security requirement, but OpenAPI with security requirement",
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
        Arguments.of(
            "Operation and OpenAPI without security requirements",
            Operation(),
            OpenAPI(),
            false
        ),
        Arguments.of(
            "Operation with empty security array overriding global security",
            Operation().security(listOf()),
            openAPIWithApiKeySchemeAndGlobalSecurity(API_KEY_SCHEME_NAME, SecurityScheme.In.HEADER),
            false
        )
    )

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("securityRequirementsProvider")
    @DisplayName("decide should return true when security requirement exists")
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
    @DisplayName("apply should return test case with missing security")
    @Description("Verifies that the apply method returns a test case with the security removed")
    fun applyShouldReturnTestCaseWithMissingApiKey(place: SecurityScheme.In) {
        val openAPI = step("Create OpenAPI with API Key security scheme") {
            openAPIWithApiKeyScheme(API_KEY_SCHEME_NAME, place)
        }

        val operation = step("Create operation with security requirement") {
            Operation().security(listOf(SecurityRequirement().addList(API_KEY_SCHEME_NAME)))
        }

        val validCase = validCaseFor(place)

        val testCases = step("Call apply") { rule.apply(createTestContext(validCase, operation, openAPI)).toList() }

        assertThat(testCases).hasSize(1).first().isEqualTo(
            validCase.copy(
                cookie = emptyList(),
                headers = emptyList(),
                queryParams = emptyMap(),
                securityValues = SecurityValues(),
                rule = AllSecurityMissedAuthValidationRule::class.java.name,
                name = "No security values provided",
                expectedStatusCode = 401,
            )
        )
    }

    @Test
    @DisplayName("apply should handle oauth2 with OpenId Connect schemes by removing Authorization header")
    @Description("Verifies that for oauth2 with OpenId Connect schemes the rule produces cases without Authorization header and proper naming")
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
                        Components()
                            .addSecuritySchemes("openIdConnect", SecurityScheme().type(SecurityScheme.Type.OPENIDCONNECT))
                            .addSecuritySchemes("oauth", SecurityScheme().type(SecurityScheme.Type.OAUTH2))
                    )
                )
            ).toList()
        }

        assertThat(testCases).hasSize(1).first().isEqualTo(
            validCase.copy(
                cookie = emptyList(),
                headers = emptyList(),
                queryParams = emptyMap(),
                securityValues = SecurityValues(),
                rule = AllSecurityMissedAuthValidationRule::class.java.name,
                name = "No security values provided",
                expectedStatusCode = 401,
            )
        )
    }
}
