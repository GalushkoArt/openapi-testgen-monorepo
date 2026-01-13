package art.galushko.openapi.testgen.providers

import art.galushko.openapi.testgen.rules.ManualRuleRegistry
import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.rules.auth.AllSecurityMissedAuthValidationRule
import art.galushko.openapi.testgen.rules.auth.InvalidSecurityValuesAuthValidationRule
import art.galushko.openapi.testgen.spi.AuthValidationRule
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test


@Feature("Auth Test Case Generation")
class AuthTestCaseProviderForOperationTest : TestProviderTest() {
    private val provider =
        AuthTestCaseProviderForOperation(ManualRuleRegistry().getRules(AuthValidationRule::class.java, emptySet()))

    @Test
    @DisplayName("Provider should return empty list when no security is defined")
    @Description("Verifies that the provider returns an empty list when neither operation nor OpenAPI has security definitions")
    fun provideTestCasesShouldReturnEmptyListWhenNoSecurityIsDefined() {
        // Arrange
        val operation = step("Create operation without security") {
            Operation()
        }
        val openAPI = step("Create OpenAPI without security") {
            OpenAPI()
        }
        val validCase = step("Create valid test case") { createBasicTestCase() }

        // Act
        val outcome = step("Call provideTestCases") {
            provider.provideTestCases(operation, createTestContext(validCase, operation, openAPI))
        }

        // Assert
        assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
        assertThat((outcome as Outcome.Success).value)
            .`as`("Test cases list should be empty when no security is defined").isEmpty()
    }

    @Test
    @DisplayName("Provider should apply all rules when operation has security")
    @Description("Verifies that the provider applies all auth rules when the operation has security definitions")
    fun provideTestCasesShouldApplyAllRulesWhenOperationHasSecurity() {
        // Arrange
        val openAPI = step("Create OpenAPI with API Key security scheme") {
            val securityScheme = SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .`in`(SecurityScheme.In.HEADER)
                .name("X-API-Key")
            val components = Components().addSecuritySchemes("api_key", securityScheme)
            OpenAPI().components(components)
        }
        val operation = step("Create operation with security requirement") {
            val securityRequirement = SecurityRequirement().addList("api_key")
            Operation().security(listOf(securityRequirement))
        }
        val validCase = step("Create valid test case") { createBasicTestCase() }

        // Act
        val outcome = step("Call provideTestCases") {
            provider.provideTestCases(operation, createTestContext(validCase, operation, openAPI))
        }

        // Assert
        val testCases = (outcome as Outcome.Success).value
        assertSoftly { softly ->
            softly.assertThat(testCases.map {
                it.rule
            }.distinct().count())
                .`as`("Test cases should be generated from multiple rules")
                .isEqualTo(2)

            softly.assertThat(testCases.map {
                it.rule
            }).`as`("Test cases should include those from MissingApiKeyAuthRule")
                .anyMatch {
                    it == AllSecurityMissedAuthValidationRule::class.java.name
                }

            softly.assertThat(testCases.map {
                it.rule
            }).`as`("Test cases should include those from InvalidApiKeyAuthRule")
                .anyMatch {
                    it == InvalidSecurityValuesAuthValidationRule::class.java.name
                }
        }
    }

    @Test
    @DisplayName("Provider should apply all rules when only OpenAPI has security")
    @Description("Verifies that the provider applies all auth rules when only the OpenAPI has global security definitions")
    fun provideTestCasesShouldApplyAllRulesWhenOnlyOpenAPIHasSecurity() {
        // Arrange
        val openAPI = step("Create OpenAPI with global API Key security") {
            val securityScheme = SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .`in`(SecurityScheme.In.HEADER)
                .name("X-API-Key")
            val components = Components().addSecuritySchemes("api_key", securityScheme)
            val securityRequirement = SecurityRequirement().addList("api_key")
            OpenAPI()
                .components(components)
                .security(listOf(securityRequirement))
        }
        val operation = step("Create operation without security") {
            Operation()
        }
        val validCase = step("Create valid test case") { createBasicTestCase() }

        // Act
        val outcome = step("Call provideTestCases") {
            provider.provideTestCases(operation, createTestContext(validCase, operation, openAPI))
        }

        // Assert
        val testCases = (outcome as Outcome.Success).value
        assertSoftly { softly ->
            softly.assertThat(testCases.map {
                it.rule
            }.distinct().count())
                .`as`("Test cases should be generated from multiple rules")
                .isEqualTo(2)

            softly.assertThat(testCases.map {
                it.rule
            }).`as`("Test cases should include those from MissingApiKeyAuthRule")
                .anyMatch {
                    it == AllSecurityMissedAuthValidationRule::class.java.name
                }

            softly.assertThat(testCases.map {
                it.rule
            }).`as`("Test cases should include those from InvalidApiKeyAuthRule")
                .anyMatch {
                    it == InvalidSecurityValuesAuthValidationRule::class.java.name
                }
        }
    }
}
