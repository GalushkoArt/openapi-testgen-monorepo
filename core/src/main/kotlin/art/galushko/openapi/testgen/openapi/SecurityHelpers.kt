package art.galushko.openapi.testgen.openapi

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.SecurityValues
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.with
import art.galushko.openapi.testgen.openapi.SecurityHelpers.isAuthSchemeWithScope
import art.galushko.openapi.testgen.spi.SecuritySchemeToScope
import art.galushko.openapi.testgen.testdata.extractExpectedResponseExample
import art.galushko.openapi.testgen.util.Consts.UNAUTHORIZED_CODE
import art.galushko.openapi.testgen.util.addOrReplace
import art.galushko.openapi.testgen.util.remove
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY

/**
 * Helpers for working with OpenAPI security requirements.
 */
@Suppress("TooManyFunctions")
internal object SecurityHelpers {
    internal const val AUTHORIZATION_HEADER: String = "authorization"

    /**
     * Returns operation-level security or falls back to global `OpenAPI.security`.
     *
     * @param operation the operation whose security requirements are inspected
     * @param openAPI the OpenAPI document to fall back to when the operation has none
     * @return a list of security requirements; empty if none are defined
     */
    @JvmStatic
    public fun getSecurityRequirements(operation: Operation, openAPI: OpenAPI): List<SecurityRequirement> {
        operation.security?.let { return it }
        openAPI.security?.let { return it }
        return emptyList()
    }

    /**
     * Retrieves and maps security requirement schemas for a specific operation from the OpenAPI specification.
     *
     * This method processes the security requirements of an operation or falls back to global security
     * requirements defined in the OpenAPI specification if the operation's security is not defined.
     * It converts these requirements into a list of security schemes paired with their respective scopes.
     *
     * @param operation the OpenAPI operation object containing specific security requirements
     * @param openAPI the OpenAPI specification that defines global security requirements and security components
     * @return a list of lists, where each inner list contains SecuritySchemeToScope instances representing
     *         a mapping of security schemes to their associated scopes
     */
    @JvmStatic
    public fun getSecurityRequirementSchemas(operation: Operation, openAPI: OpenAPI): List<List<SecuritySchemeToScope>> {
        val security = operation.security ?: openAPI.security ?: return emptyList()
        return getSecurityRequirementSchemas(security, openAPI)
    }

    /**
     * Retrieves and maps security requirement schemas from the provided OpenAPI specification.
     *
     * This method processes security requirements and converts them into a list of security schemes
     * paired with their corresponding scopes, validating the existence of each security scheme
     * in the OpenAPI components. If any unknown security schemes are detected, an exception is thrown.
     *
     * @param security a list of SecurityRequirement objects, where each represents a security requirement with
     *                 associated schemes and their scopes
     * @param openAPI the OpenAPI specification that defines security components and schemas
     * @return a list of lists, where each inner list contains SecuritySchemeToScope instances,
     *         representing a mapping of security schemes to their associated scopes
     * @throws IllegalArgumentException if any security schemes referenced in the security requirements list
     *                                  are not defined in the OpenAPI specification
     */
    @JvmStatic
    public fun getSecurityRequirementSchemas(security: List<SecurityRequirement>, openAPI: OpenAPI): List<List<SecuritySchemeToScope>> {
        val unknownSecSchemas = security.flatMap { it.keys.filter { key -> openAPI.components.securitySchemes[key] == null } }.distinct()
        require(unknownSecSchemas.isEmpty()) { "Unknown security schemes: $unknownSecSchemas" }
        return security.map { it.entries.map { entry -> SecuritySchemeToScope(openAPI.components.securitySchemes[entry.key]!!, entry.key, entry.value) } }
    }

    /**
     * Reduces duplication in lists of security schemes by grouping and filtering based on a scheme type.
     *
     * Each list of security schemes in the input is processed to remove duplicate authorization
     * headers, ensuring that schemes of type other than `APIKEY` are treated as identical
     * for deduplication purposes. For schemes of type `APIKEY`, duplicates are removed by their name.
     *
     * @param input A list of lists containing `SecuritySchemeToScope` objects. Each nested list
     * represents a group of security schemes with associated scopes.
     * @return A list of lists where duplicate `SecuritySchemeToScope` entries are removed
     * based on defined criteria.
     */
    @JvmStatic
    public fun reduceAuthorizationHeaderSecuritySchemesDuplication(input: List<List<SecuritySchemeToScope>>): List<List<SecuritySchemeToScope>> =
        input.map { it.distinctBy { (scheme, _) -> if (scheme.type != APIKEY) AUTHORIZATION_HEADER else scheme.name + scheme.`in` } }
            .distinctBy { it.map { (scheme, _) -> if (scheme.type != APIKEY) AUTHORIZATION_HEADER else scheme.name + scheme.`in` }.distinct() }

    /**
     * Generates a string describing the security requirements for a test case based on the provided security schemes.
     *
     * @param providedSecurityRequirement a list of SecuritySchemeToScope objects describing the security schemes and their associated scopes
     * @return a human-readable string representing the security requirements, such as API key names or "Authorization header"
     */
    @JvmStatic
    public fun describeSecurityRequirements(providedSecurityRequirement: List<SecuritySchemeToScope>): String =
        providedSecurityRequirement.joinToString(" and ") { if (it.scheme.type == APIKEY) "${it.scheme.name} API key" else "Authorization header" }

    /**
     * Resolves API key security schemes referenced by requirements that contain exactly one entry.
     *
     * @param requirements security requirements to inspect
     * @param openAPI OpenAPI document used to resolve requirement names to schemes
     * @return list of API key security schemes
     */
    @JvmStatic
    public fun findSingleApiKeyRequirementSchemes(
        requirements: List<SecurityRequirement>,
        openAPI: OpenAPI,
    ): List<SecurityScheme> {
        return requirements
            .asSequence()
            .filter { it.size == 1 }
            .mapNotNull { req ->
                val key = req.keys.first()
                openAPI.components.securitySchemes[key]
            }
            .filter { it.type == APIKEY }
            .toList()
    }

    /**
     * Creates a deep copy of the TestCase from the given context without any security-sensitive values.
     * This excludes query parameters, headers, and cookies that exist within the securityValues
     * property of the given TestCase. The returned TestCase will still maintain its overall structure
     * but will have an empty SecurityValues object.
     *
     * @param context The test generation context containing the original valid test case and associated specification data.
     * @return a new TestCase object with security-sensitive values removed.
     */
    @JvmStatic
    public fun testCaseWithoutSecurityValues(context: TestGenerationContext): TestCase {
        val testCase = context.validCase
        val queryParams = testCase.queryParams.filter { (key, _) -> !testCase.securityValues.queryParams.keys.contains(key) }
        val headers = testCase.headers.filter { (key, _) -> !testCase.securityValues.headers.any { it.key == key } }
        val cookie = testCase.cookie.filter { (key, _) -> !testCase.securityValues.cookie.any { it.key == key } }
        return testCase.copy(
            expectedStatusCode = UNAUTHORIZED_CODE,
            expectedBody = context.schemaExampleValueGenerator.extractExpectedResponseExample(context, UNAUTHORIZED_CODE),
            queryParams = queryParams,
            headers = headers,
            cookie = cookie,
            securityValues = SecurityValues(),
        )
    }

    /**
     * Modifies an existing test case by removing certain property values related to specific security schemes
     * for security purposes. The function ensures the test conforms to unauthorized status (401) by updating
     * the expected response and removing the associated security scheme credentials (e.g., query, cookie, headers).
     *
     * @param context The test generation context containing the original valid test case and associated specification data.
     * @param securitySchemesToRemove A list of security schemes and their associated scopes, which determine the properties to be removed from the test case.
     * @return A new, modified instance of the test case with security-related property values removed and an updated expected unauthorized status.
     */
    @JvmStatic
    public fun removePropertyValuesForSecurityFromTestCase(context: TestGenerationContext, securitySchemesToRemove: List<SecuritySchemeToScope>): TestCase {
        val apiKeyRequirements = securitySchemesToRemove.filter { isApiKeySecurity(it) }
        val headers = apiKeyRequirements.filter { it.scheme.`in` == SecurityScheme.In.HEADER }.map { it.scheme.name }.toMutableList()
        val cookies = apiKeyRequirements.filter { it.scheme.`in` == SecurityScheme.In.COOKIE }.map { it.scheme.name }
        val queries = apiKeyRequirements.filter { it.scheme.`in` == SecurityScheme.In.QUERY }.map { it.scheme.name }
        securitySchemesToRemove.filter { !isApiKeySecurity(it) }.takeIf { it.isNotEmpty() }?.let { headers.add(AUTHORIZATION_HEADER) }
        val validCase = context.validCase
        return validCase.copy(
            expectedStatusCode = UNAUTHORIZED_CODE,
            expectedBody = context.schemaExampleValueGenerator.extractExpectedResponseExample(context, UNAUTHORIZED_CODE),
            headers = validCase.headers.remove(headers, true),
            cookie = validCase.cookie.remove(cookies),
            queryParams = validCase.queryParams.remove(queries),
            securityValues = validCase.securityValues.copy(
                headers = validCase.securityValues.headers.remove(headers, true),
                cookie = validCase.securityValues.cookie.remove(cookies),
                queryParams = validCase.securityValues.queryParams.remove(queries),
            ),
        )
    }

    /**
     * Applies the provided security requirements to the specified test case, updating its headers,
     * cookies, query parameters, and security metadata based on the given security schemes and
     * scope mappings.
     *
     * @param testCase The test case to which the security requirements will be applied.
     * @param requirements A list of security scheme and scope mappings that detail the security
     *                     configurations to be applied.
     * @param apiKeyValueProvider A function that takes a `SecurityScheme` and returns the associated
     *                            API key value to be included in the test case.
     * @param authHeaderValueProvider A function that takes a list of `SecuritySchemeToScope` objects
     *                                and returns the value to be assigned to the authorization header.
     * @return A new `TestCase` instance with the updated security configurations applied.
     */
    @JvmStatic
    public fun applySecurityRequirementToTestCase(
        testCase: TestCase,
        requirements: List<SecuritySchemeToScope>,
        apiKeyValueProvider: (SecuritySchemeToScope) -> String,
        authHeaderValueProvider: (List<SecuritySchemeToScope>) -> String,
    ): TestCase {
        val apiKeyRequirements = requirements.filter { isApiKeySecurity(it) }
        val headers = apiKeyRequirements.filter { it.scheme.`in` == SecurityScheme.In.HEADER }.map { requirement ->
            requirement.scheme.name with apiKeyValueProvider(requirement)
        }.toMutableList()
        val cookies = apiKeyRequirements.filter { it.scheme.`in` == SecurityScheme.In.COOKIE }.map { requirement ->
            requirement.scheme.name with apiKeyValueProvider(requirement)
        }
        val queries = apiKeyRequirements.filter { it.scheme.`in` == SecurityScheme.In.QUERY }.map { requirement ->
            requirement.scheme.name to apiKeyValueProvider(requirement)
        }
        requirements.filter { !isApiKeySecurity(it) }.takeIf { it.isNotEmpty() }?.let { headers.add(AUTHORIZATION_HEADER with authHeaderValueProvider(it)) }
        return testCase.copy(
            headers = testCase.headers.addOrReplace(headers, true),
            cookie = testCase.cookie.addOrReplace(cookies),
            queryParams = testCase.queryParams.addOrReplace(queries),
            securityValues = testCase.securityValues.copy(
                headers = testCase.securityValues.headers.addOrReplace(headers, true),
                cookie = testCase.securityValues.cookie.addOrReplace(cookies),
                queryParams = testCase.securityValues.queryParams.addOrReplace(queries),
            )
        )
    }

    /**
     * Removes duplicate security requirement sets by processing and deduplicating the security schemes
     * defined in the OpenAPI specification for a given test generation context. This ensures that
     * security schemes are reduced to their unique representations for further processing.
     *
     * @param context The test generation context containing the operation, OpenAPI specification,
     *                and related data used to extract and process security requirements.
     * @return A deduplicated list of security requirement sets, where each set contains a list of
     *         `SecuritySchemeToScope` instances that represent the security schemes and their scopes.
     */
    @JvmStatic
    public fun dedupedRequirementSets(context: TestGenerationContext): List<List<SecuritySchemeToScope>> =
        reduceAuthorizationHeaderSecuritySchemesDuplication(
            getSecurityRequirementSchemas(context.operation, context.openAPI)
        )

    /**
     * Determines whether the given security scheme is of the type API key.
     *
     * @param security The `SecuritySchemeToScope` object containing the security scheme to be checked.
     * @return `true` if the security scheme's type is `APIKEY`, otherwise `false`.
     */
    @JvmStatic
    public fun isApiKeySecurity(security: SecuritySchemeToScope): Boolean = security.scheme.type == APIKEY

    /**
     * Determines if there are any security requirements defined for an operation within the OpenAPI specification.
     *
     * This method checks if security schemes exist either on the operation or at a global level in the OpenAPI specification.
     * The result indicates whether any security constraints are present for the operation being processed.
     *
     * @param context the test generation context that encapsulates the operation, OpenAPI specification, and other test-related data
     * @return true if any security requirements are defined; false otherwise
     */
    @JvmStatic
    public fun hasAnySecurity(context: TestGenerationContext): Boolean =
        getSecurityRequirementSchemas(context.operation, context.openAPI).isNotEmpty()

    /**
     * Determines if any security scheme in the provided list has an associated scope.
     *
     * @see isAuthSchemeWithScope
     *
     * @param schemas A list of `SecuritySchemeToScope` objects representing the security schemes and their
     *                associated scopes. Each object defines a specific security scheme and the list of
     *                permissions or access levels it provides.
     * @return `true` if at least one security scheme in the provided list contains scopes and matches the
     *         criteria for scoped auth schemes; `false` otherwise.
     */
    @JvmStatic
    public fun hasScopedAuthScheme(schemas: List<SecuritySchemeToScope>): Boolean =
        schemas.any(::isAuthSchemeWithScope)

    /**
     * Determines whether a given security scheme is an authentication scheme requiring scopes.
     * This is applicable for OAuth2 and OpenID Connect security schemes with non-empty scopes.
     *
     * @param schemeToScope The security scheme and its associated scopes.
     * @return `true` if the scheme is of type OAUTH2 or OPENIDCONNECT and has non-empty scopes; otherwise, `false`.
     */
    @JvmStatic
    public fun isAuthSchemeWithScope(schemeToScope: SecuritySchemeToScope): Boolean {
        return(schemeToScope.scheme.type == SecurityScheme.Type.OAUTH2 || schemeToScope.scheme.type == SecurityScheme.Type.OPENIDCONNECT) &&
            schemeToScope.scopes.isNotEmpty()
    }
}


