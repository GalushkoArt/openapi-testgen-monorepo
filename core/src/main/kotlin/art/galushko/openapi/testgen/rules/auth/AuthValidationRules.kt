package art.galushko.openapi.testgen.rules.auth

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.openapi.SecurityHelpers
import art.galushko.openapi.testgen.openapi.SecurityHelpers.applySecurityRequirementToTestCase
import art.galushko.openapi.testgen.openapi.SecurityHelpers.dedupedRequirementSets
import art.galushko.openapi.testgen.openapi.SecurityHelpers.describeSecurityRequirements
import art.galushko.openapi.testgen.openapi.SecurityHelpers.getSecurityRequirementSchemas
import art.galushko.openapi.testgen.openapi.SecurityHelpers.hasAnySecurity
import art.galushko.openapi.testgen.openapi.SecurityHelpers.isAuthSchemeWithScope
import art.galushko.openapi.testgen.openapi.SecurityHelpers.testCaseWithoutSecurityValues
import art.galushko.openapi.testgen.spi.AuthValidationRule
import art.galushko.openapi.testgen.spi.SecuritySchemeToScope
import art.galushko.openapi.testgen.testdata.extractExpectedResponseExample
import art.galushko.openapi.testgen.util.Consts.FORBIDDEN_CODE
import art.galushko.openapi.testgen.util.Consts.UNAUTHORIZED_CODE
import art.galushko.openapi.testgen.util.getSubsetsOfValues

/**
 * Generates negative cases where a multi-scheme requirement is only partially satisfied.
 *
 * Inputs: security requirement sets and [TestGenerationContext].
 * Output: [TestCase]s missing one or more schemes with `expectedStatusCode` 401.
 * Constraints: applies only when a requirement set contains more than one scheme.
 * Determinism: preserves requirement and subset order from `dedupedRequirementSets`/`getSubsetsOfValues`.
 * Settings: uses `validSecurityValues` via `SecurityValueProvider` and response examples via `exampleValues`.
 */
internal class MissingSecurityValuesAuthValidationRule : AuthValidationRule {
    override fun getRuleName(): String = "Missing Security Values"

    override fun decide(context: TestGenerationContext): Boolean = dedupedRequirementSets(context).any { it.size > 1 }

    override fun apply(context: TestGenerationContext): Sequence<TestCase> {
        val basicTestCase = testCaseWithoutSecurityValues(context)
        return dedupedRequirementSets(context)
            .asSequence()
            .filter { it.size > 1 }
            .flatMap { securityRequirement ->
                getSubsetsOfValues(securityRequirement, includeCompleteSet = false).asSequence().map { providedSecurityRequirement ->
                    applySecurityRequirementToTestCase(
                        basicTestCase,
                        providedSecurityRequirement,
                        context.securityValueProvider::getValidApiKeyValue,
                        context.securityValueProvider::getAuthorizationSchemaValue,
                    ).copy(
                        name = "Missing ${describeSecurityRequirements(securityRequirement.minus(providedSecurityRequirement.toSet()))} security",
                        rule = MissingSecurityValuesAuthValidationRule::class.java.name,
                    )
                }
            }
    }
}

/**
 * Generates a negative case with all security values removed.
 *
 * Inputs: operation or global security requirements and [TestGenerationContext].
 * Output: single [TestCase] with `expectedStatusCode` 401 and no security values.
 * Constraints: applies only when any security requirement is defined.
 * Determinism: deterministic for identical context.
 * Settings: expected response examples follow `TestGenerationSettings.exampleValues`.
 */
internal class AllSecurityMissedAuthValidationRule : AuthValidationRule {
    override fun getRuleName(): String = "All Security Missed"

    override fun decide(context: TestGenerationContext): Boolean = hasAnySecurity(context)

    override fun apply(context: TestGenerationContext): Sequence<TestCase> {
        return listOf(
            testCaseWithoutSecurityValues(context).copy(
                name = "No security values provided",
                rule = AllSecurityMissedAuthValidationRule::class.java.name,
            )
        ).asSequence()
    }
}


/**
 * Generates negative cases by supplying invalid security values for required schemes.
 *
 * Inputs: security requirement sets and [TestGenerationContext].
 * Output: [TestCase]s with invalid values and `expectedStatusCode` 401.
 * Constraints: applies only when any security requirement is defined.
 * Determinism: preserves requirement and subset order from `getSubsetsOfValues`.
 * Settings: invalid values come from `TestGenerationSettings.overrideBasicTestData`;
 * response examples follow `TestGenerationSettings.exampleValues`.
 */
internal class InvalidSecurityValuesAuthValidationRule : AuthValidationRule {
    override fun getRuleName(): String = "Invalid Security Values"

    override fun decide(context: TestGenerationContext): Boolean = hasAnySecurity(context)

    override fun apply(context: TestGenerationContext): Sequence<TestCase> {
        return dedupedRequirementSets(context)
            .asSequence()
            .flatMap { securityRequirement ->
                getSubsetsOfValues(securityRequirement, includeCompleteSet = true).asSequence().map { providedSecurityRequirement ->
                    applySecurityRequirementToTestCase(
                        context.validCase,
                        providedSecurityRequirement,
                        { context.basicTestData.invalidApiKey() },
                        { context.basicTestData.invalidAuthorizationHeader() },
                    ).copy(
                        name = "Invalid ${describeSecurityRequirements(providedSecurityRequirement)} security",
                        rule = InvalidSecurityValuesAuthValidationRule::class.java.name,
                        expectedStatusCode = UNAUTHORIZED_CODE,
                        expectedBody = context.responseExampleExtractor.extractExpectedResponseExample(context, UNAUTHORIZED_CODE),
                    )
                }
            }
    }
}

/**
 * Generates negative cases with missing OAuth2/OpenID scopes (403 Forbidden).
 *
 * Inputs: scoped security requirement groups and [TestGenerationContext].
 * Output: [TestCase]s with reduced scope sets and `expectedStatusCode` 403.
 * Constraints: applies only when a requirement group contains scoped auth schemes.
 * Determinism: preserves requirement and subset order from `getSubsetsOfValues`.
 * Settings: uses `validSecurityValues` via `SecurityValueProvider`; response examples follow `exampleValues`.
 */
internal class InsufficientScopesAuthValidationRule : AuthValidationRule {
    override fun getRuleName(): String = "Insufficient Scopes"

    override fun decide(context: TestGenerationContext): Boolean =
        getSecurityRequirementSchemas(context.operation, context.openAPI).any(SecurityHelpers::hasScopedAuthScheme)

    override fun apply(context: TestGenerationContext): Sequence<TestCase> {
        return getSecurityRequirementSchemas(context.operation, context.openAPI)
            .asSequence()
            .filter(SecurityHelpers::hasScopedAuthScheme)
            .flatMap { group -> generateTestCasesForGroup(group, context) }
    }

    private fun generateTestCasesForGroup(
        group: List<SecuritySchemeToScope>,
        context: TestGenerationContext,
    ): Sequence<TestCase> {
        val basicTestCase = testCaseWithoutSecurityValues(context)
        return generateMissingScopeCombinations(group)
            .asSequence()
            .map { (name, requiredSecurityWithMissedScope) ->
                applySecurityRequirementToTestCase(
                    basicTestCase,
                    requiredSecurityWithMissedScope,
                    context.securityValueProvider::getValidApiKeyValue,
                    context.securityValueProvider::getAuthorizationSchemaValue,
                ).copy(
                    expectedStatusCode = FORBIDDEN_CODE,
                    expectedBody = context.responseExampleExtractor.extractExpectedResponseExample(context, FORBIDDEN_CODE),
                    rule = InsufficientScopesAuthValidationRule::class.java.name,
                    name = name,
                )
            }
    }

    /**
     * Generates all combinations of missing scope subsets for a given list of security schemes
     * with associated scopes. Each combination represents a potential test scenario where
     * a subset of the required scopes is omitted.
     *
     * @param scopedSchemes A list of `SecuritySchemeToScope` objects, representing the security
     *                      schemes and their associated scopes. Some schemes in the list
     *                      require scopes for authentication.
     * @return A list of pairs where each pair consists of:
     *         - A descriptive string indicating which scopes are missing.
     *         - A modified list of `SecuritySchemeToScope` objects reflecting the subsets of missing scopes.
     */
    private fun generateMissingScopeCombinations(
        scopedSchemes: List<SecuritySchemeToScope>,
    ): List<Pair<String, List<SecuritySchemeToScope>>> {
        val schemeToScope = scopedSchemes
            .filter(SecurityHelpers::isAuthSchemeWithScope)
            .flatMap { scheme ->
                scheme.scopes.map { scope -> scheme.name to scope }
            }
        val scopedSecuritiesMap = scopedSchemes.filter(SecurityHelpers::isAuthSchemeWithScope).associateBy({ it.name }) { it.scopes }
        val schemasWithoutScopes = scopedSchemes.map { if (isAuthSchemeWithScope(it)) it.copy(scopes = emptyList()) else it }
        return getSubsetsOfValues(schemeToScope, includeEmptySet = true).map {
            it.groupBy({ entry -> entry.first }) { entry -> entry.second }
        }.map { set ->
            missedScopeTestCaseName(scopedSecuritiesMap, set) to schemasWithoutScopes.toMutableList().also { list ->
                list.replaceAll {
                    set[it.name]?.let { scopes -> it.copy(scopes = scopes) } ?: it
                }
            }
        }
    }

    /**
     * Constructs a descriptive test case name indicating which scopes are missing for given security schemes.
     *
     * @param scopedSecurities A map where the key represents the name of a security scheme and the value is a list of its original scopes.
     * @param modifiedSecurities A map where the key represents the name of a security scheme and the value is a list of its scopes after modifications.
     * @return A string describing the missing scopes per security scheme.
     */
    private fun missedScopeTestCaseName(scopedSecurities: Map<String, List<String>>, modifiedSecurities: Map<String, List<String>>): String {
        val missedScopes = scopedSecurities.map { (name, scopes) -> name to (modifiedSecurities[name]?.let { scopes.minus(it.toSet()) } ?: scopes) }
            .filter { it.second.isNotEmpty() }
        return "Missing " + missedScopes.joinToString(" and ") {
            it.second.sorted().joinToString(prefix = "[", postfix = "]", separator = ",") + " scopes of " + it.first
        } + " in security"
    }
}

/**
 * Generates negative cases with invalid OAuth2/OpenID scopes (403 Forbidden).
 *
 * Inputs: scoped security requirement groups and [TestGenerationContext].
 * Output: [TestCase]s with invalid scope values and `expectedStatusCode` 403.
 * Constraints: applies only when scoped auth schemes are present.
 * Determinism: preserves requirement order from the OpenAPI spec.
 * Settings: invalid scope values come from `TestGenerationSettings.overrideBasicTestData`;
 * response examples follow `TestGenerationSettings.exampleValues`.
 */
internal class IncorrectScopesAuthValidationRule : AuthValidationRule {
    override fun getRuleName(): String = "Incorrect Scopes"

    override fun decide(context: TestGenerationContext): Boolean =
        getSecurityRequirementSchemas(context.operation, context.openAPI).any(SecurityHelpers::hasScopedAuthScheme)

    override fun apply(context: TestGenerationContext): Sequence<TestCase> {
        return getSecurityRequirementSchemas(context.operation, context.openAPI)
            .asSequence()
            .filter(SecurityHelpers::hasScopedAuthScheme)
            .flatMap { group -> generateTestCasesForGroup(group, context) }
    }

    private fun generateTestCasesForGroup(
        group: List<SecuritySchemeToScope>,
        context: TestGenerationContext,
    ): Sequence<TestCase> {
        return group.asSequence()
            .filter(SecurityHelpers::isAuthSchemeWithScope)
            .map { security ->
                val modifiableGroup = group.toMutableList()
                modifiableGroup.remove(security)
                modifiableGroup.add(security.copy(scopes = listOf(context.basicTestData.invalidSecurityScope())))
                applySecurityRequirementToTestCase(
                    context.validCase,
                    modifiableGroup,
                    context.securityValueProvider::getValidApiKeyValue,
                    context.securityValueProvider::getAuthorizationSchemaValue,
                ).copy(
                    expectedStatusCode = FORBIDDEN_CODE,
                    expectedBody = context.responseExampleExtractor.extractExpectedResponseExample(context, FORBIDDEN_CODE),
                    rule = IncorrectScopesAuthValidationRule::class.java.name,
                    name = "${security.name} security scheme has invalid scope",
                )
            }
    }
}
