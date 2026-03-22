package art.galushko.openapi.testgen.providers.parameter

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetParametersFromRef
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.model.with
import art.galushko.openapi.testgen.spi.SchemaValidationRule
import art.galushko.openapi.testgen.spi.TestCaseProvider
import art.galushko.openapi.testgen.testdata.extractExpectedResponseExampleWithMediaType
import art.galushko.openapi.testgen.util.Consts.BAD_REQUEST_CODE
import art.galushko.openapi.testgen.util.addOrReplace
import art.galushko.openapi.testgen.util.buildParameterContext
import art.galushko.openapi.testgen.util.exceptions.UnsupportedParameterType
import art.galushko.openapi.testgen.util.resolveParameterSchema
import art.galushko.openapi.testgen.util.runProviderSafely
import io.swagger.v3.oas.models.parameters.CookieParameter
import io.swagger.v3.oas.models.parameters.HeaderParameter
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.PathParameter
import io.swagger.v3.oas.models.parameters.QueryParameter
import org.slf4j.LoggerFactory

/**
 * Generates negative test cases by applying [SchemaValidationRule]s to parameter schemas.
 *
 * Inputs: parameter (dereferenced) and [TestGenerationContext] with a valid baseline.
 * Output: list of [TestCase]s with invalid parameter values and `expectedStatusCode` 400.
 * Constraints: supports query/path/header/cookie parameters; unsupported parameter types raise
 * [UnsupportedParameterType] and are handled by `runProviderSafely`. Parameter schema resolution
 * prefers `schema`, then falls back to `content`; when both exist, `schema` is used and a warning is logged.
 * Determinism: preserves schema-combination order from `SchemaMerger` and rule order from wiring.
 * Settings: rule list is filtered via `TestGenerationSettings.ignoreSchemaValidationRules`; combinations are limited by
 * `maxSchemaCombinations` and `maxMergedSchemaDepth`, while example generation follows `exampleValues`.
 *
 * @param rules the rules to apply against the resolved parameter schema
 */
internal class ParameterSchemaValidationTestProvider(private val rules: List<SchemaValidationRule>) : TestCaseProvider<Parameter> {
    private val log = LoggerFactory.getLogger(ParameterSchemaValidationTestProvider::class.java)

    override fun provideTestCases(
        spec: Parameter,
        context: TestGenerationContext,
    ): Outcome<List<TestCase>> = runProviderSafely(this, buildParameterContext(context, spec)) {
        processSpec(spec, context)
    }

    public fun processSpec(spec: Parameter, context: TestGenerationContext): List<TestCase> {
        val deref = tryGetParametersFromRef(spec, context.openAPI)
        val resolved = resolveParameterSchema(deref, context.openAPI)
        if (resolved.hasBothSchemaAndContent) {
            log.warn(
                "Parameter '{}' in '{}' defines both schema and content. Content is ignored and schema is used.",
                deref.name,
                deref.`in`,
            )
        }
        val schema = resolved.schema ?: return emptyList()
        return context.schemaMerger.getSchemaFlatCombinations(schema, 1, context.visitedSchemaRefs.toMutableSet(), context.combinationBudget) {
            tryGetSchemaFromRef(it, context.openAPI)
        }.flatMap { flat ->
            rules.asSequence().flatMap { rule ->
                rule.apply(flat, context).flatMap { invalidValue ->
                    val validCase = context.validCase
                    val expectedResponse = context.responseExampleExtractor.extractExpectedResponseExampleWithMediaType(context, BAD_REQUEST_CODE)
                    val common = validCase.copy(
                        rule = rule::class.java.name,
                        expectedStatusCode = BAD_REQUEST_CODE,
                        expectedBody = expectedResponse.body,
                        responseBodyMediaType = expectedResponse.mediaType,
                    )
                    val updated = when (deref) {
                        is QueryParameter -> common.copy(
                            name = "Invalid Query ${deref.name} parameter: ${invalidValue.buildDescription()}",
                            queryParams = validCase.queryParams.addOrReplace(deref.name to invalidValue.value)
                        )

                        is PathParameter -> common.copy(
                            name = "Invalid Path ${deref.name} parameter: ${invalidValue.buildDescription()}",
                            pathParams = validCase.pathParams.addOrReplace(deref.name to invalidValue.value)
                        )

                        is HeaderParameter -> common.copy(
                            name = "Invalid Header ${deref.name} parameter: ${invalidValue.buildDescription()}",
                            headers = validCase.headers.addOrReplace(deref.name with invalidValue.value, true)
                        )

                        is CookieParameter -> common.copy(
                            name = "Invalid Cookie ${deref.name} parameter: ${invalidValue.buildDescription()}",
                            cookie = validCase.cookie.addOrReplace(deref.name with invalidValue.value)
                        )

                        else -> throw UnsupportedParameterType(deref)
                    }
                    listOf(updated).asSequence()
                }
            }.toList()
        }
    }
}


