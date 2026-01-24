package art.galushko.openapi.testgen.rules.auth.fixtures

import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.model.KeyValuePair
import art.galushko.openapi.testgen.model.SecurityValues
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.with
import art.galushko.openapi.testgen.openapi.SecurityHelpers
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme

const val API_KEY_SCHEME_NAME: String = "api_key"
const val API_KEY_HEADER_NAME: String = "X-API-Key"

fun apiKeyScheme(apiKeyIn: SecurityScheme.In, name: String = API_KEY_HEADER_NAME): SecurityScheme =
    SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(apiKeyIn).name(name)

fun bearerScheme(): SecurityScheme =
    SecurityScheme().type(SecurityScheme.Type.HTTP)

fun oauthScheme(): SecurityScheme =
    SecurityScheme().type(SecurityScheme.Type.OAUTH2)

fun oauthSchemeWithScopes(vararg scopes: String): SecurityScheme {
    if (scopes.isEmpty()) return oauthScheme()
    val flows = OAuthFlows().clientCredentials(
        OAuthFlow()
            .tokenUrl("https://example.com/token")
            .scopes(Scopes().apply { putAll(scopes.associateWith { "$it scope" }) })
    )
    return oauthScheme().flows(flows)
}

fun openIdScheme(openIdConnectUrl: String = "https://example.com/.well-known/openid-configuration"): SecurityScheme =
    SecurityScheme().type(SecurityScheme.Type.OPENIDCONNECT).openIdConnectUrl(openIdConnectUrl)

fun securityRequirementOf(vararg entries: Pair<String, List<String>>): SecurityRequirement {
    val requirement = SecurityRequirement()
    entries.forEach { (name, scopes) -> requirement.addList(name, scopes) }
    return requirement
}

fun operationWithSecurity(name: String): Operation =
    Operation().security(listOf(SecurityRequirement().addList(name)))

fun openApiWithSchemes(securitySchemes: Map<String, SecurityScheme>): OpenAPI =
    OpenAPI().components(Components().securitySchemes(securitySchemes))

fun openApiWithGlobalSecurity(
    schemeName: String,
    scheme: SecurityScheme,
    extra: Map<String, SecurityScheme> = emptyMap(),
): OpenAPI {
    val components = Components().addSecuritySchemes(schemeName, scheme)
    extra.forEach { (k, v) -> components.addSecuritySchemes(k, v) }
    val securityRequirement = SecurityRequirement().addList(schemeName)
    return OpenAPI().components(components).security(listOf(securityRequirement))
}

fun openAPIWithApiKeyScheme(schemeName: String, place: SecurityScheme.In): OpenAPI {
    val securityScheme = SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(place).name(API_KEY_HEADER_NAME)
    return OpenAPI().components(Components().addSecuritySchemes(schemeName, securityScheme))
}

fun openAPIWithApiKeySchemeAndGlobalSecurity(schemeName: String, place: SecurityScheme.In): OpenAPI {
    val openAPI = openAPIWithApiKeyScheme(schemeName, place)
    val securityRequirement = SecurityRequirement().addList(schemeName)
    return openAPI.security(listOf(securityRequirement))
}

fun openAPIWithOAuthScheme(): OpenAPI =
    OpenAPI().components(Components().addSecuritySchemes("oauth", oauthScheme()))

fun validCaseFor(
    place: SecurityScheme.In,
    includeBearer: Boolean = false,
    apiKeyName: String = API_KEY_HEADER_NAME,
) = when (place) {
    SecurityScheme.In.HEADER -> {
        val headers = buildList {
            add(apiKeyName with "valid-api-key")
            if (includeBearer) add(SecurityHelpers.AUTHORIZATION_HEADER with "Bearer valid")
        }
        createBasicTestCase(headers = headers, securityValues = SecurityValues(headers = headers))
    }

    SecurityScheme.In.COOKIE -> {
        val cookie = listOf(apiKeyName with "valid-api-key")
        val headers = if (includeBearer) listOf(SecurityHelpers.AUTHORIZATION_HEADER with "Bearer valid") else emptyList()
        createBasicTestCase(
            headers = headers,
            cookie = cookie,
            securityValues = SecurityValues(headers = headers, cookie = cookie)
        )
    }

    SecurityScheme.In.QUERY -> {
        val query = mapOf(apiKeyName to "valid-api-key")
        val headers = if (includeBearer) listOf(SecurityHelpers.AUTHORIZATION_HEADER with "Bearer valid") else emptyList()
        createBasicTestCase(
            headers = headers,
            queryParams = query,
            securityValues = SecurityValues(headers = headers, queryParams = query)
        )
    }
}

fun TestCase.copyWithHeaders(
    name: String,
    headers: List<KeyValuePair<String, String>>,
    authorizationScopes: List<Map<String, Any>>? = null,
): TestCase = copy(
    name = name,
    headers = headers,
    securityValues = securityValues.copy(
        headers = headers,
        other = authorizationScopes?.let { mapOf(SecurityHelpers.AUTHORIZATION_SCOPES_KEY to it) } ?: emptyMap(),
    )
)
