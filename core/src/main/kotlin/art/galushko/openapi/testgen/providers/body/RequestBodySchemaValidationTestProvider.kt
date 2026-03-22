package art.galushko.openapi.testgen.providers.body

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetRequestBodyFromRef
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.spi.SchemaValidationRule
import art.galushko.openapi.testgen.spi.TestCaseProvider
import art.galushko.openapi.testgen.testdata.extractExpectedResponseExampleWithMediaType
import art.galushko.openapi.testgen.util.Consts.BAD_REQUEST_CODE
import art.galushko.openapi.testgen.util.buildRequestBodyContext
import art.galushko.openapi.testgen.util.isMediaTypeSupported
import art.galushko.openapi.testgen.util.runProviderSafely
import io.swagger.v3.oas.models.parameters.RequestBody
import org.slf4j.LoggerFactory

/**
 * Generates negative test cases by applying [SchemaValidationRule]s to a request body's schema.
 *
 * Inputs: request body content for supported media types and [TestGenerationContext].
 * Output: list of [TestCase]s with invalid body values and `expectedStatusCode` 400.
 * Constraints: processes all supported media types (`application/json`, `text/json`, `application/jwt`, XML,
 * form-urlencoded, and `+json` / `+jwt` / `+xml` suffixes), logs unsupported ones as warn, and
 * skips when no supported content.
 * Determinism: preserves schema-combination order from `SchemaMerger` and rule order from wiring.
 * Settings: rule list is filtered via `TestGenerationSettings.ignoreSchemaValidationRules`; combinations are limited by
 * `maxSchemaCombinations` and `maxMergedSchemaDepth`, while example generation follows `exampleValues`.
 *
 * @param rules the rules to apply to the resolved body schema
 */
internal class RequestBodySchemaValidationTestProvider(private val rules: List<SchemaValidationRule>) : TestCaseProvider<RequestBody> {
    private val log = LoggerFactory.getLogger(RequestBodySchemaValidationTestProvider::class.java)

    public fun isApplicable(requestBody: RequestBody): Boolean {
        return requestBody.content != null && requestBody.content.any { isMediaTypeSupported(it.key) }
    }

    override fun provideTestCases(
        spec: RequestBody,
        context: TestGenerationContext,
    ): Outcome<List<TestCase>> = runProviderSafely(this, buildRequestBodyContext(context, spec)) {
        processSpec(spec, context)
    }

    public fun processSpec(spec: RequestBody, context: TestGenerationContext): List<TestCase> {
        val deref = tryGetRequestBodyFromRef(spec, context.openAPI)
        logUnsupportedMediaTypes(deref)
        if (!isApplicable(deref)) return emptyList()
        return deref.content.asSequence()
            .filter { isMediaTypeSupported(it.key) }
            .mapNotNull { mediaType ->
                mediaType.value.schema?.let { schema ->
                    mediaType.key to tryGetSchemaFromRef(schema, context.openAPI)
                }
            }
            .flatMap { (mediaTypeName, schema) ->
                context.schemaMerger.getSchemaFlatCombinations(
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
                                val expectedResponse = context.responseExampleExtractor.extractExpectedResponseExampleWithMediaType(context, BAD_REQUEST_CODE)
                                context.validCase.copy(
                                    name = "Incorrect Request Body: ${invalidValue.buildDescription()}",
                                    rule = rule::class.java.name,
                                    body = invalidValue.value,
                                    requestBodyMediaType = mediaTypeName,
                                    expectedStatusCode = BAD_REQUEST_CODE,
                                    expectedBody = expectedResponse.body,
                                    responseBodyMediaType = expectedResponse.mediaType,
                                )
                            }
                    }
                }
            }
            .distinctBy { testCase ->
                GeneratedCaseKey(
                    name = testCase.name,
                    rule = testCase.rule,
                    body = testCase.body,
                )
            }.toList()
    }

    private fun logUnsupportedMediaTypes(requestBody: RequestBody) {
        requestBody.content.orEmpty()
            .keys
            .filterNot { isMediaTypeSupported(it) }
            .forEach { mediaType ->
                log.warn("Unsupported request body media type: {}", mediaType)
            }
    }
}

private data class GeneratedCaseKey(
    val name: String,
    val rule: String?,
    val body: Any?,
)
