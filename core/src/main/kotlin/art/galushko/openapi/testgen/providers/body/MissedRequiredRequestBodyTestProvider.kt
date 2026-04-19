package art.galushko.openapi.testgen.providers.body

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetRequestBodyFromRef
import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.spi.TestCaseProvider
import art.galushko.openapi.testgen.testdata.extractExpectedResponseExampleWithMediaType
import art.galushko.openapi.testgen.util.Consts.BAD_REQUEST_CODE
import art.galushko.openapi.testgen.util.buildRequestBodyContext
import art.galushko.openapi.testgen.util.runProviderSafely
import io.swagger.v3.oas.models.parameters.RequestBody

/**
 * Produces a negative test case that omits a required request body.
 *
 * Inputs: request body (dereferenced) and [TestGenerationContext] with a valid baseline.
 * Output: single [TestCase] with `expectedStatusCode` 400 and `body = null`.
 * Constraints: applies only when the request body is marked `required = true`.
 * Determinism: deterministic for identical context and request body.
 * Settings: expected response examples follow `TestGenerationSettings.exampleValues`.
 */
internal class MissedRequiredRequestBodyTestProvider : TestCaseProvider<RequestBody> {
    /**
     * Checks whether the request body is required.
     *
     * @param requestBody the request body to inspect
     * @return true when the body exists and is marked as required
     */
    public fun isApplicable(requestBody: RequestBody): Boolean {
        return requestBody.required == true
    }

    /**
     * Builds a copy of the baseline test case with the request body removed.
     *
     * @param context with validCase the baseline "valid" test case
     * @return the derived negative test case
     */
    public fun getTestcaseWithInvalidValue(context: TestGenerationContext): TestCase {
        val expectedResponse = context.responseExampleExtractor.extractExpectedResponseExampleWithMediaType(context, BAD_REQUEST_CODE)
        return context.validCase.copy(
            name = "Required Request Body is missing",
            rule = MissedRequiredRequestBodyTestProvider::class.java.name,
            body = null,
            expectedStatusCode = BAD_REQUEST_CODE,
            expectedBody = expectedResponse.body,
            responseBodyMediaType = expectedResponse.mediaType,
        )
    }

    override fun provideTestCases(
        spec: RequestBody,
        context: TestGenerationContext,
    ): Outcome<List<TestCase>> = runProviderSafely(this, buildRequestBodyContext(context, spec)) {
        processSpec(spec, context)
    }

    public fun processSpec(spec: RequestBody, context: TestGenerationContext): List<TestCase> {
        val deref = tryGetRequestBodyFromRef(spec, context.openAPI)
        return if (isApplicable(deref)) listOf(getTestcaseWithInvalidValue(context)) else emptyList()
    }
}


