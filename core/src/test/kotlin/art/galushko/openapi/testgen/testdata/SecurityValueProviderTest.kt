package art.galushko.openapi.testgen.testdata

import art.galushko.openapi.testgen.spi.SecuritySchemeToScope
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.security.SecurityScheme
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Epic("Test Data Generation")
@Feature("Security Value Provider")
class SecurityValueProviderTest {

    @Test
    @DisplayName("getValidApiKeyValue should return configured value when scheme name exists")
    @Description("Verifies that getValidApiKeyValue returns the configured value from the map when the scheme name is found")
    fun getValidApiKeyValueShouldReturnConfiguredValue() {
        // Given: Provider with configured API key
        val securityValues = mapOf("API-Key" to "secret-key-123")
        val provider = SecurityValueProvider(securityValues)
        val scheme = SecuritySchemeToScope(
            name = "API-Key",
            scheme = SecurityScheme().type(SecurityScheme.Type.APIKEY).name("X-API-Key"),
            scopes = emptyList()
        )

        // When: Get API key value
        val result = provider.getValidApiKeyValue(scheme)

        // Then: Returns configured value
        Assertions.assertThat(result).isEqualTo("secret-key-123")
    }

    @Test
    @DisplayName("getValidApiKeyValue should return placeholder when scheme name not found")
    @Description("Verifies that getValidApiKeyValue returns a placeholder when the scheme name is not in the configured map")
    fun getValidApiKeyValueShouldReturnPlaceholder() {
        // Given: Provider with different API key
        val securityValues = mapOf("X-Other-Key" to "other-secret")
        val provider = SecurityValueProvider(securityValues)
        val scheme = SecuritySchemeToScope(
            name = "API-Key",
            scheme = SecurityScheme().type(SecurityScheme.Type.APIKEY).name("X-API-Key"),
            scopes = emptyList()
        )

        // When: Get API key value
        val result = provider.getValidApiKeyValue(scheme)

        // Then: Returns placeholder
        Assertions.assertThat(result).isEqualTo("<valid_API-Key_api_key_placeholder>")
    }

    @Test
    @DisplayName("getValidApiKeyValue should return placeholder when map is empty")
    @Description("Verifies that getValidApiKeyValue returns a placeholder when the provider is initialized with an empty map")
    fun getValidApiKeyValueShouldReturnPlaceholderWhenMapEmpty() {
        // Given: Provider with empty map
        val provider = SecurityValueProvider()
        val scheme = SecuritySchemeToScope(
            name = "API-Key",
            scheme = SecurityScheme().type(SecurityScheme.Type.APIKEY).name("X-API-Key"),
            scopes = emptyList()
        )

        // When: Get API key value
        val result = provider.getValidApiKeyValue(scheme)

        // Then: Returns placeholder
        Assertions.assertThat(result).isEqualTo("<valid_API-Key_api_key_placeholder>")
    }

    @Test
    @DisplayName("getAuthorizationSchemaValue should return configured value for HTTP scheme")
    @Description("Verifies that getAuthorizationSchemaValue returns the configured value when HTTP scheme is found")
    fun getAuthorizationSchemaValueShouldReturnConfiguredValueForHttp() {
        // Given: Provider with configured HTTP auth
        val securityValues = mapOf("bearer_auth" to "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")
        val provider = SecurityValueProvider(securityValues)
        val requirements = listOf(
            SecuritySchemeToScope(
                name = "bearer_auth",
                scheme = SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer"),
                scopes = emptyList()
            )
        )

        // When: Get authorization schema value
        val result = provider.getAuthorizationSchemaValue(requirements)

        // Then: Returns configured value
        Assertions.assertThat(result).isEqualTo("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")
    }

    @Test
    @DisplayName("getAuthorizationSchemaValue should return placeholder for HTTP scheme when not configured")
    @Description("Verifies that getAuthorizationSchemaValue returns a placeholder when HTTP scheme value is not in the map")
    fun getAuthorizationSchemaValueShouldReturnPlaceholderForHttp() {
        // Given: Provider with empty map
        val provider = SecurityValueProvider()
        val requirements = listOf(
            SecuritySchemeToScope(
                name = "bearer_auth",
                scheme = SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer"),
                scopes = emptyList()
            )
        )

        // When: Get authorization schema value
        val result = provider.getAuthorizationSchemaValue(requirements)

        // Then: Returns placeholder
        Assertions.assertThat(result).isEqualTo("<valid_bearer_auth_placeholder>")
    }

    @Test
    @DisplayName("getAuthorizationSchemaValue should format OAuth2 scheme with scopes")
    @Description("Verifies that getAuthorizationSchemaValue formats OAuth2 scheme with scopes correctly")
    fun getAuthorizationSchemaValueShouldFormatOAuth2WithScopes() {
        // Given: Provider with OAuth2 requirements
        val provider = SecurityValueProvider()
        val requirements = listOf(
            SecuritySchemeToScope(
                name = "oauth2",
                scheme = SecurityScheme().type(SecurityScheme.Type.OAUTH2),
                scopes = listOf("read:users", "write:users")
            )
        )

        // When: Get authorization schema value
        val result = provider.getAuthorizationSchemaValue(requirements)

        // Then: Returns formatted OAuth2 string
        Assertions.assertThat(result).isEqualTo("<oauth2:[read:users,write:users]>")
    }

    @Test
    @DisplayName("getAuthorizationSchemaValue should format OpenID Connect scheme with scopes")
    @Description("Verifies that getAuthorizationSchemaValue formats OpenID Connect scheme with scopes correctly")
    fun getAuthorizationSchemaValueShouldFormatOpenIdConnectWithScopes() {
        // Given: Provider with OpenID Connect requirements
        val provider = SecurityValueProvider()
        val requirements = listOf(
            SecuritySchemeToScope(
                name = "openid",
                scheme = SecurityScheme().type(SecurityScheme.Type.OPENIDCONNECT),
                scopes = listOf("openid", "profile", "email")
            )
        )

        // When: Get authorization schema value
        val result = provider.getAuthorizationSchemaValue(requirements)

        // Then: Returns formatted OpenID Connect string
        Assertions.assertThat(result).isEqualTo("<openid:[openid,profile,email]>")
    }

    @Test
    @DisplayName("getAuthorizationSchemaValue should format multiple OAuth2/OpenID schemes sorted by name")
    @Description("Verifies that getAuthorizationSchemaValue formats multiple schemes sorted alphabetically by name")
    fun getAuthorizationSchemaValueShouldFormatMultipleSchemsSorted() {
        // Given: Provider with multiple OAuth2/OpenID requirements
        val provider = SecurityValueProvider()
        val requirements = listOf(
            SecuritySchemeToScope(
                name = "oauth2_github",
                scheme = SecurityScheme().type(SecurityScheme.Type.OAUTH2),
                scopes = listOf("repo", "user")
            ),
            SecuritySchemeToScope(
                name = "oauth2_google",
                scheme = SecurityScheme().type(SecurityScheme.Type.OAUTH2),
                scopes = listOf("email")
            ),
            SecuritySchemeToScope(
                name = "openid",
                scheme = SecurityScheme().type(SecurityScheme.Type.OPENIDCONNECT),
                scopes = listOf("openid", "profile")
            )
        )

        // When: Get authorization schema value
        val result = provider.getAuthorizationSchemaValue(requirements)

        // Then: Returns formatted string sorted by name
        Assertions.assertThat(result).isEqualTo("<oauth2_github:[repo,user]&oauth2_google:[email]&openid:[openid,profile]>")
    }

    @Test
    @DisplayName("getAuthorizationSchemaValue should handle empty scopes list")
    @Description("Verifies that getAuthorizationSchemaValue handles OAuth2 scheme with empty scopes")
    fun getAuthorizationSchemaValueShouldHandleEmptyScopes() {
        // Given: Provider with OAuth2 scheme with no scopes
        val provider = SecurityValueProvider()
        val requirements = listOf(
            SecuritySchemeToScope(
                name = "oauth2",
                scheme = SecurityScheme().type(SecurityScheme.Type.OAUTH2),
                scopes = emptyList()
            )
        )

        // When: Get authorization schema value
        val result = provider.getAuthorizationSchemaValue(requirements)

        // Then: Returns formatted string with empty scopes
        Assertions.assertThat(result).isEqualTo("<oauth2:[]>")
    }

    @Test
    @DisplayName("getAuthorizationSchemaValue should throw IllegalArgumentException for empty list")
    @Description("Verifies that getAuthorizationSchemaValue returns '<>' when given an empty requirements list")
    fun getAuthorizationSchemaValueShouldReturnEmptyPlaceholderForEmptyList() {
        // Given: Provider with empty requirements
        val provider = SecurityValueProvider()
        val requirements = emptyList<SecuritySchemeToScope>()

        // When: Get authorization schema value
        val exception = assertThrows<IllegalArgumentException> { provider.getAuthorizationSchemaValue(requirements) }

        // Then: Returns empty placeholder
        Assertions.assertThat(exception.message).isEqualTo("Authorization schemes should not be empty")
    }

    @Test
    @DisplayName("getAuthorizationSchemaValue should prioritize HTTP scheme over OAuth2")
    @Description("Verifies that getAuthorizationSchemaValue returns HTTP value even when OAuth2 schemes are present")
    fun getAuthorizationSchemaValueShouldPrioritizeHttpOverOAuth2() {
        // Given: Provider with both HTTP and OAuth2 requirements
        val securityValues = mapOf("bearer_auth" to "Bearer token123")
        val provider = SecurityValueProvider(securityValues)
        val requirements = listOf(
            SecuritySchemeToScope(
                name = "bearer_auth",
                scheme = SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer"),
                scopes = emptyList()
            ),
            SecuritySchemeToScope(
                name = "oauth2",
                scheme = SecurityScheme().type(SecurityScheme.Type.OAUTH2),
                scopes = listOf("read", "write")
            )
        )

        // When: Get authorization schema value
        val result = provider.getAuthorizationSchemaValue(requirements)

        // Then: Returns HTTP value (not OAuth2 formatted string)
        Assertions.assertThat(result).isEqualTo("Bearer token123")
    }
}
