package art.galushko.openapi.testgen.providers.parameter

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetParametersFromRef
import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.spi.TestCaseProvider
import art.galushko.openapi.testgen.testdata.extractExpectedResponseExampleWithMediaType
import art.galushko.openapi.testgen.util.Consts.BAD_REQUEST_CODE
import art.galushko.openapi.testgen.util.buildParameterContext
import art.galushko.openapi.testgen.util.exceptions.UnsupportedParameterType
import art.galushko.openapi.testgen.util.remove
import art.galushko.openapi.testgen.util.runProviderSafely
import io.swagger.v3.oas.models.parameters.CookieParameter
import io.swagger.v3.oas.models.parameters.HeaderParameter
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.PathParameter
import io.swagger.v3.oas.models.parameters.QueryParameter

/**
 * Produces a negative test case that omits a required non-path parameter.
 *
 * Inputs: parameter (dereferenced) and [TestGenerationContext] with a valid baseline.
 * Output: single [TestCase] with `expectedStatusCode` 400 and the required parameter removed.
 * Constraints: applies only to required query/header/cookie parameters; path parameters are excluded.
 * Determinism: deterministic for identical context and parameter.
 * Settings: expected response examples follow `TestGenerationSettings.exampleValues`.
 */
internal class MissedRequiredParameterTestProvider : TestCaseProvider<Parameter> {
    /**
     * Checks whether the rule applies to the given parameter.
     *
     * @param parameter the parameter to check
     * @return true when the parameter is required and not a path parameter
     */
    public fun isApplicable(parameter: Parameter): Boolean {
        return parameter.required == true && parameter !is PathParameter
    }

    /**
     * Builds a copy of the baseline test case with the given required parameter removed.
     *
     * @param context with validCase the baseline "valid" test case
     * @param parameter the required parameter to omit
     * @return the derived negative test case
     * @throws UnsupportedParameterType when the parameter type is unsupported
     */
    public fun getTestcaseWithInvalidValue(context: TestGenerationContext, parameter: Parameter): TestCase {
        val validCase = context.validCase
        val expectedResponse = context.responseExampleExtractor.extractExpectedResponseExampleWithMediaType(context, BAD_REQUEST_CODE)
        val common = validCase.copy(
            rule = MissedRequiredParameterTestProvider::class.java.name,
            expectedStatusCode = BAD_REQUEST_CODE,
            expectedBody = expectedResponse.body,
            responseBodyMediaType = expectedResponse.mediaType,
        )
        return when (parameter) {
            is QueryParameter -> common.copy(
                name = "Missed Required Query Parameter ${parameter.name}",
                queryParams = validCase.queryParams.remove(parameter.name)
            )

            is HeaderParameter -> common.copy(
                name = "Missed Required Header Parameter ${parameter.name}",
                headers = validCase.headers.remove(parameter.name, true)
            )

            is CookieParameter -> common.copy(
                name = "Missed Required Cookie Parameter ${parameter.name}",
                cookie = validCase.cookie.remove(parameter.name)
            )

            else -> throw UnsupportedParameterType(parameter)
        }
    }

    override fun provideTestCases(
        spec: Parameter,
        context: TestGenerationContext,
    ): Outcome<List<TestCase>> = runProviderSafely(this, buildParameterContext(context, spec)) {
        processSpec(spec, context)
    }

    public fun processSpec(spec: Parameter, context: TestGenerationContext): List<TestCase> {
        val deref = tryGetParametersFromRef(spec, context.openAPI)
        return if (isApplicable(deref)) listOf(getTestcaseWithInvalidValue(context, deref)) else emptyList()
    }
}


