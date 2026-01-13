package art.galushko.openapi.testgen.testdata

import art.galushko.openapi.testgen.spi.SecuritySchemeToScope
import io.swagger.v3.oas.models.security.SecurityScheme

/**
 * Provides valid security values for API key and authorization schemes.
 *
 * Inputs: a map of scheme name to credential value.
 * Output: deterministic values (configured or placeholders) used by [ValidCaseBuilder] and auth rules.
 *
 * @param validSecurityValues a map where the key represents the security scheme name and
 *                           the value is the corresponding credential or parameter
 */
public class SecurityValueProvider(
    private val validSecurityValues: Map<String, String> = emptyMap()
) {
    /**
     * Retrieves a valid API key value for the given security scheme.
     *
     * @param scheme the security scheme for which the valid API key value is needed
     * @return the valid API key value as a string if available; otherwise, a placeholder string
     */
    public fun getValidApiKeyValue(scheme: SecuritySchemeToScope): String =
        validSecurityValues[scheme.name] ?: "<valid_${scheme.name}_api_key_placeholder>"

    /**
     * Generates the value of the Authorization header based on the provided scheme requirements.
     *
     * - If an HTTP scheme is present, uses the configured value or a placeholder for that scheme.
     * - Otherwise, builds a deterministic placeholder from OAuth2/OpenID Connect schemes,
     *   sorted by scheme name and rendered as `<scheme:[scope1,scope2]>`.
     *
     * @param authorizeHeaderRequirements list of security scheme requirements and scopes
     * @return authorization header value to use in a valid test case
     * @throws IllegalArgumentException if the provided list of requirements is empty
     */
    public fun getAuthorizationSchemaValue(authorizeHeaderRequirements: List<SecuritySchemeToScope>): String {
        require(authorizeHeaderRequirements.isNotEmpty()) {
            "Authorization schemes should not be empty"
        }
        val httpAuthorizationRequirement = authorizeHeaderRequirements.find { it.scheme.type == SecurityScheme.Type.HTTP }
        if (httpAuthorizationRequirement != null) {
            val securitySchemaName = httpAuthorizationRequirement.name
            val headerValue = validSecurityValues[securitySchemaName]
            return headerValue ?: "<valid_${securitySchemaName}_placeholder>"
        } else {
            return authorizeHeaderRequirements
                .filter { it.scheme.type == SecurityScheme.Type.OAUTH2 || it.scheme.type == SecurityScheme.Type.OPENIDCONNECT }
                .sortedBy { it.name }.joinToString(prefix = "<", separator = "&", postfix = ">") {
                    "${it.name}:[${it.scopes.joinToString(",")}]"
                }
        }
    }
}


