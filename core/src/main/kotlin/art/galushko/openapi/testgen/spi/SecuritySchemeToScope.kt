package art.galushko.openapi.testgen.spi

import io.swagger.v3.oas.models.security.SecurityScheme

/**
 * Represents the relationship between a security scheme and its associated scopes.
 *
 * Used by auth rules and security value providers to describe which scheme (and scopes) should be applied.
 */
public data class SecuritySchemeToScope(
    val scheme: SecurityScheme,
    val name: String,
    val scopes: List<String>,
)


