package art.galushko.openapi.testgen.providers.body

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetRequestBodyFromRef
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.spi.SchemaValidationRule
import art.galushko.openapi.testgen.spi.TestCaseProvider
import art.galushko.openapi.testgen.testdata.extractExpectedResponseExample
import art.galushko.openapi.testgen.util.Consts.BAD_REQUEST_CODE
import art.galushko.openapi.testgen.util.Consts.supportedMediaTypes
import art.galushko.openapi.testgen.util.buildRequestBodyContext
import art.galushko.openapi.testgen.util.runProviderSafely
import io.swagger.v3.oas.models.parameters.RequestBody

/**
 * Generates negative test cases by applying [SchemaValidationRule]s to a request body's schema.
 *
 * Inputs: request body content for supported media types and [TestGenerationContext].
 * Output: list of [TestCase]s with invalid body values and `expectedStatusCode` 400.
 * Constraints: uses the first supported media type from `Consts.supportedMediaTypes`; skips when content is empty.
 * Determinism: preserves schema-combination order from `SchemaMerger` and rule order from wiring.
 * Settings: rule list is filtered via `TestGenerationSettings.ignoreSchemaValidationRules`; combinations are limited by
 * `maxSchemaCombinations` and `maxMergedSchemaDepth`, while example generation follows `exampleValues`.
 *
 * @param rules the rules to apply to the resolved body schema
 */
internal class RequestBodySchemaValidationTestProvider(private val rules: List<SchemaValidationRule>) : TestCaseProvider<RequestBody> {
    public fun isApplicable(requestBody: RequestBody): Boolean {
        return requestBody.content != null && supportedMediaTypes.any { requestBody.content.containsKey(it) }
    }

    override fun provideTestCases(
        spec: RequestBody,
        context: TestGenerationContext,
    ): Outcome<List<TestCase>> = runProviderSafely(this, buildRequestBodyContext(context, spec)) {
        processSpec(spec, context)
    }

    public fun processSpec(spec: RequestBody, context: TestGenerationContext): List<TestCase> {
        val deref = tryGetRequestBodyFromRef(spec, context.openAPI)
        if (!isApplicable(deref)) return emptyList()
        val schema = supportedMediaTypes.asSequence()
            .mapNotNull { mediaType -> deref.content?.get(mediaType)?.schema }
            .map { s -> tryGetSchemaFromRef(s, context.openAPI) }
            .firstOrNull() ?: return emptyList()
        return context.schemaMerger.getSchemaFlatCombinations(
            schema,
            1,
            context.visitedSchemaRefs.toMutableSet(),
            context.combinationBudget,
        ) {
            tryGetSchemaFromRef(it, context.openAPI)
        }.flatMap { flat ->
            rules.asSequence().flatMap { rule ->
                rule.apply(flat, context)
                    .map { invalidValue ->
                        context.validCase.copy(
                            name = "Incorrect Request Body: ${invalidValue.buildDescription()}",
                            rule = rule::class.java.name,
                            body = invalidValue.value,
                            expectedStatusCode = BAD_REQUEST_CODE,
                            expectedBody = context.schemaExampleValueGenerator.extractExpectedResponseExample(context, BAD_REQUEST_CODE),
                        )
                    }.toList()
            }
        }
    }
}


