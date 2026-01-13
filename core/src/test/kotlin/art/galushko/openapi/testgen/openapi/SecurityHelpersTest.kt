package art.galushko.openapi.testgen.openapi

import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.model.SecurityValues
import art.galushko.openapi.testgen.model.with
import art.galushko.openapi.testgen.openapi.SecurityHelpers.getSecurityRequirementSchemas
import art.galushko.openapi.testgen.openapi.SecurityHelpers.getSecurityRequirements
import art.galushko.openapi.testgen.openapi.SecurityHelpers.reduceAuthorizationHeaderSecuritySchemesDuplication
import art.galushko.openapi.testgen.openapi.SecurityHelpers.removePropertyValuesForSecurityFromTestCase
import art.galushko.openapi.testgen.openapi.SecurityHelpers.testCaseWithoutSecurityValues
import art.galushko.openapi.testgen.spi.SecuritySchemeToScope
import art.galushko.openapi.testgen.util.getSubsetsOfValues
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Epic("Test utils validation")
@Feature("Validation of Security Helpers")
class SecurityHelpersTest {

    @Test
    @DisplayName("getSecurityRequirements returns operation.security when present")
    fun getSecurityRequirements_returnsOperationSecurity() {
        val operation = Operation().security(listOf(SecurityRequirement().addList("op")))
        val openAPI = OpenAPI().security(listOf(SecurityRequirement().addList("root")))

        val result = step("Act: resolve security requirements") { getSecurityRequirements(operation, openAPI) }
        assertThat(result).hasSize(1)
    }

    @Test
    @DisplayName("getSecurityRequirements falls back to OpenAPI.security when operation.security is null")
    fun getSecurityRequirements_fallsBackToRoot() {
        val openAPI = OpenAPI().security(listOf(SecurityRequirement().addList("root")))

        val result = step("Act: fallback to root security") { getSecurityRequirements(Operation(), openAPI) }
        assertThat(result).hasSize(1)
    }

    @Test
    @DisplayName("getSecurityRequirements returns empty when neither operation nor root define security")
    fun getSecurityRequirements_returnsEmptyWhenNone() {
        val result = step("Act: no security defined") { getSecurityRequirements(Operation(), OpenAPI()) }
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("getSecurityRequirementSchemas(security, openAPI) maps names to schemes")
    fun getSecurityRequirementSchemas_mapsNamesToSchemes() {
        val apiKey = SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.HEADER).name("X-API-Key")
        val bearer = SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
        val openAPI = OpenAPI().components(Components().addSecuritySchemes("api_key", apiKey).addSecuritySchemes("bearer", bearer))
        val security = listOf(SecurityRequirement().addList("api_key").addList("bearer"))

        val mapped = step("Act: map known schemes") { getSecurityRequirementSchemas(security, openAPI) }
        assertThat(mapped).hasSize(1)
        assertThat(mapped.first().map { it.name }).containsExactlyInAnyOrder("api_key", "bearer")
    }

    @Test
    @DisplayName("getSecurityRequirementSchemas(security, openAPI) throws for unknown schemes")
    fun getSecurityRequirementSchemas_throwsOnUnknown() {
        // Ensure securitySchemes map is non-null to exercise unknown-scheme path instead of NPE
        val openAPI = OpenAPI().components(Components().addSecuritySchemes("dummy", SecurityScheme()))
        val unknownSecurity = listOf(SecurityRequirement().addList("unknown"))

        assertThatThrownBy { getSecurityRequirementSchemas(unknownSecurity, openAPI) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unknown security schemes")
    }

    @Test
    @DisplayName("getSecurityRequirementSchemas(operation, openAPI) returns empty when none defined")
    fun getSecurityRequirementSchemas_operationVariant_empty() {
        val result = step("Act: no op or root security") { getSecurityRequirementSchemas(Operation(), OpenAPI()) }
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("getSecurityRequirementSchemas(operation, openAPI) resolves operation-level entries")
    fun getSecurityRequirementSchemas_operationVariant_maps() {
        val apiKey = SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.HEADER).name("X-API-Key")
        val openAPI = OpenAPI().components(Components().addSecuritySchemes("api_key", apiKey))
        val operation = Operation().security(listOf(SecurityRequirement().addList("api_key")))

        val mapped = step("Act: map op security") { getSecurityRequirementSchemas(operation, openAPI) }
        assertThat(mapped).hasSize(1)
        assertThat(mapped.first().map { it.name }).containsExactly("api_key")
    }

    @Test
    @DisplayName("reduceAuthorizationHeaderSecuritySchemesDuplication dedups identical groups by treating non-APIKEY as Authorization")
    fun reduceAuthorizationHeaderSecuritySchemesDuplication_dedupsAuthorizationGroups() {
        val apiKey = SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.HEADER).name("X-API-Key")
        val oauth = SecurityScheme().type(SecurityScheme.Type.OAUTH2)
        val bearer = SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
        val group = listOf(
            SecuritySchemeToScope(apiKey, "api_key", emptyList()),
            SecuritySchemeToScope(oauth, "oauth", emptyList()),
            SecuritySchemeToScope(bearer, "bearer", emptyList())
        )
        val input = listOf(group, group)

        val reduced = step("Act: reduce duplicates") { reduceAuthorizationHeaderSecuritySchemesDuplication(input) }
        assertThat(reduced).hasSize(1)
        assertThat(reduced.first().map { it.name }).containsExactlyInAnyOrder("api_key", "oauth")
    }

    @Test
    @DisplayName("reduceAuthorizationHeaderSecuritySchemesDuplication considers API key name in distinctness")
    fun reduceAuthorizationHeaderSecuritySchemesDuplication_distinctByApiKeyName() {
        val k1 = SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.HEADER).name("X-API-Key")
        val k2 = SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.HEADER).name("X-OTHER")
        val oauth = SecurityScheme().type(SecurityScheme.Type.OAUTH2)
        val group1 = listOf(SecuritySchemeToScope(k1, "api_key", emptyList()), SecuritySchemeToScope(oauth, "oauth", emptyList()))
        val group2 = listOf(SecuritySchemeToScope(k1, "api_key", emptyList()), SecuritySchemeToScope(oauth, "oauth", emptyList()))
        val group3 = listOf(SecuritySchemeToScope(k2, "api_key2", emptyList()), SecuritySchemeToScope(oauth, "oauth", emptyList()))

        val reduced = step("Act: reduce with different API key names") { reduceAuthorizationHeaderSecuritySchemesDuplication(listOf(group1, group2, group3)) }
        assertThat(reduced).hasSize(2)
    }

    @Test
    @DisplayName("getSubsetsOfValues returns all non-empty subsets in stable order for two security elements")
    fun securitySchemeCombination_subsetsStableOrder_twoElements() {
        val e1 = SecuritySchemeToScope(
            SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.HEADER).name("X-API-Key"),
            "api_key",
            emptyList(),
        )
        val e2 = SecuritySchemeToScope(
            SecurityScheme().type(SecurityScheme.Type.OAUTH2),
            "oauth",
            emptyList(),
        )

        val subsets = step("Act: generate subsets") { getSubsetsOfValues(listOf(e1, e2), includeCompleteSet = true) }
        assertThat(subsets).hasSize(3)
        assertThat(subsets[0]).containsExactly(e1)
        assertThat(subsets[1]).containsExactly(e2)
        assertThat(subsets[2]).containsExactly(e1, e2)
    }

    @Test
    @DisplayName("testCaseName formats API key only")
    fun describeSecurityRequirements_apiKeyOnly() {
        val apiKey = SecuritySchemeToScope(
            SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.HEADER).name("X-API-Key"),
            "api_key",
            emptyList(),
        )
        assertThat(SecurityHelpers.describeSecurityRequirements(listOf(apiKey))).isEqualTo("X-API-Key API key")
    }

    @Test
    @DisplayName("testCaseName formats Authorization header for non-APIKEY")
    fun describeSecurityRequirements_authorizationOnly() {
        val oauth = SecuritySchemeToScope(SecurityScheme().type(SecurityScheme.Type.OAUTH2), "oauth", emptyList())
        assertThat(SecurityHelpers.describeSecurityRequirements(listOf(oauth))).isEqualTo("Authorization header")
    }

    @Test
    @DisplayName("testCaseName formats mixed API key and Authorization header")
    fun describeSecurityRequirements_mixed() {
        val apiKey = SecuritySchemeToScope(
            SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.HEADER).name("X-API-Key"),
            "api_key",
            emptyList(),
        )
        val oauth = SecuritySchemeToScope(
            SecurityScheme().type(SecurityScheme.Type.OAUTH2),
            "oauth",
            emptyList(),
        )
        assertThat(SecurityHelpers.describeSecurityRequirements(listOf(apiKey, oauth))).isEqualTo("X-API-Key API key and Authorization header")
    }

    @Test
    @DisplayName("testCaseWithoutSecurityValues sets 401 and removes only security-bearing entries")
    fun testCaseWithoutSecurityValues_removesSecurityAndSets401() {
        val case = createBasicTestCase(
            headers = listOf("authorization" with "Bearer valid", "X-Other" with "v"),
            cookie = listOf("X-API-Key" with "valid"),
            queryParams = mapOf("X-API-Key" to "valid", "other" to "v"),
            securityValues = SecurityValues(
                headers = listOf("authorization" with "Bearer valid"),
                cookie = listOf("X-API-Key" with "valid"),
                queryParams = mapOf("X-API-Key" to "valid")
            )
        )
        val result = step("Act: strip security values") { testCaseWithoutSecurityValues(createTestContext(case, Operation(), OpenAPI())) }

        assertSoftly { softly ->
            softly.assertThat(result.expectedStatusCode).isEqualTo(401)
            softly.assertThat(result.headers.map { it.key.lowercase() }).doesNotContain("authorization").contains("x-other")
            softly.assertThat(result.cookie.map { it.key }).doesNotContain("X-API-Key")
            softly.assertThat(result.queryParams.keys).doesNotContain("X-API-Key").contains("other")
            softly.assertThat(result.securityValues.headers).isEmpty()
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("placementsProvider")
    @DisplayName("removePropertyValuesForSecurityFromTestCase removes values by placement")
    fun removePropertyValuesForSecurityFromTestCase_removesByPlacement(
        @Suppress("UNUSED_PARAMETER") scenario: String,
        toRemove: SecuritySchemeToScope,
        removedHeaderLowercase: String?,
        removedCookieKey: String?,
        removedQueryKey: String?,
    ) {
        val base = createBasicTestCase(
            headers = listOf("authorization" with "Bearer valid", "X-API-Key" with "valid", "X-Other" with "v"),
            cookie = listOf("X-API-Key" with "valid"),
            queryParams = mapOf("X-API-Key" to "valid", "other" to "v"),
            securityValues = SecurityValues(
                headers = listOf("authorization" with "Bearer valid", "X-API-Key" with "valid"),
                cookie = listOf("X-API-Key" with "valid"),
                queryParams = mapOf("X-API-Key" to "valid")
            )
        )
        val ctx = createTestContext(base, Operation(), OpenAPI())

        val result = step("Act: remove property values for selected scheme") {
            removePropertyValuesForSecurityFromTestCase(ctx, listOf(toRemove))
        }

        assertThat(result.expectedStatusCode).isEqualTo(401)
        removedHeaderLowercase?.let { header ->
            assertThat(result.headers.map { it.key.lowercase() }).doesNotContain(header)
            assertThat(result.securityValues.headers.map { it.key.lowercase() }).doesNotContain(header)
        }
        removedCookieKey?.let { key ->
            assertThat(result.cookie.map { it.key }).doesNotContain(key)
            assertThat(result.securityValues.cookie.map { it.key }).doesNotContain(key)
        }
        removedQueryKey?.let { key ->
            assertThat(result.queryParams.keys).doesNotContain(key)
            assertThat(result.securityValues.queryParams.keys).doesNotContain(key)
        }

        // Unaffected values remain
        assertThat(result.headers.map { it.key.lowercase() }).contains("x-other")
        assertThat(result.queryParams.keys).contains("other")
    }

    companion object {
        @JvmStatic
        fun placementsProvider(): Stream<Arguments> {
            val queryKey = SecuritySchemeToScope(
                SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.QUERY).name("X-API-Key"),
                "api_key",
                emptyList()
            )
            val cookieKey = SecuritySchemeToScope(
                SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.COOKIE).name("X-API-Key"),
                "api_key",
                emptyList()
            )
            val headerKey = SecuritySchemeToScope(
                SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.HEADER).name("X-API-Key"),
                "api_key",
                emptyList()
            )
            val bearer = SecuritySchemeToScope(
                SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer"),
                "bearer",
                emptyList()
            )
            return Stream.of(
                Arguments.of("Query API key is removed from query and securityValues", queryKey, null, null, "X-API-Key"),
                Arguments.of("Cookie API key is removed from cookies and securityValues", cookieKey, null, "X-API-Key", null),
                Arguments.of("Header API key is removed from headers and securityValues", headerKey, "x-api-key", null, null),
                Arguments.of("Bearer removes Authorization header (null in)", bearer, "authorization", null, null),
            )
        }
    }

    @Test
    @DisplayName("findSingleApiKeyRequirementSchemes returns only APIKEY schemes from single-entry requirements")
    fun findSingleApiKeyRequirementSchemes_returnsApiKeys() {
        val apiKey1 = SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.HEADER).name("X-API-Key")
        val apiKey2 = SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.QUERY).name("X-API-Key-2")
        val bearer = SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
        val components = Components()
            .addSecuritySchemes("k1", apiKey1)
            .addSecuritySchemes("k2", apiKey2)
            .addSecuritySchemes("bearer", bearer)
        val openAPI = OpenAPI().components(components)

        val requirements = listOf(
            SecurityRequirement().addList("k1"),
            SecurityRequirement().addList("bearer"),
            SecurityRequirement().addList("k2"),
        )

        val result = step("Act: extract API key schemes") { SecurityHelpers.findSingleApiKeyRequirementSchemes(requirements, openAPI) }
        assertThat(result.map { it.name }).containsExactly("X-API-Key", "X-API-Key-2")
    }

    @Test
    @DisplayName("findSingleApiKeyRequirementSchemes ignores multi-entry requirements and non-APIKEY types")
    fun findSingleApiKeyRequirementSchemes_ignoresMultiAndNonApiKey() {
        val apiKey = SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.HEADER).name("X-API-Key")
        val bearer = SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
        val components = Components()
            .addSecuritySchemes("api_key", apiKey)
            .addSecuritySchemes("bearer", bearer)
        val openAPI = OpenAPI().components(components)

        val requirements = listOf(
            SecurityRequirement().addList("api_key").addList("bearer"), // multi-entry -> ignore
            SecurityRequirement().addList("bearer"), // non-APIKEY -> ignore
        )

        val result = step("Act: filter invalid requirements") { SecurityHelpers.findSingleApiKeyRequirementSchemes(requirements, openAPI) }
        assertThat(result).isEmpty()
    }
}


