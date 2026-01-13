package art.galushko.openapi.testgen.providers

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetRequestBodyFromRef
import art.galushko.openapi.testgen.spi.TestCaseProvider
import art.galushko.openapi.testgen.util.mergeTestCaseOutcomes
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.parameters.RequestBody

/**
 * Aggregates request-body-focused [TestCaseProvider]s and applies them to an operation's request body.
 *
 * Inputs: resolved request body (from `$ref` if present) and [TestGenerationContext] with a valid baseline.
 * Output: merged [Outcome] of request-body-derived test cases (400 for built-in providers).
 * Constraints: skips when no request body is defined or when content is empty after dereference.
 * Determinism: preserves provider order for the request body.
 * Settings: nested schema rules are filtered via `TestGenerationSettings.ignoreSchemaValidationRules` during wiring;
 * `ignoreTestCases` filtering happens after suite assembly.
 *
 * @param requestBodyProviders providers invoked for the request body
 */
internal class RequestBodyTestCaseProviderForOperation(
    private val requestBodyProviders: List<TestCaseProvider<RequestBody>>
) : TestCaseProvider<Operation> {
    override fun provideTestCases(spec: Operation, context: TestGenerationContext): Outcome<List<TestCase>> {
        val requestBody = spec.requestBody ?: return Outcome.Success(emptyList())
        val deref = tryGetRequestBodyFromRef(requestBody, context.openAPI)
        if (deref.content == null || deref.content.isEmpty()) return Outcome.Success(emptyList())

        val outcomes = requestBodyProviders.asSequence().map { provider ->
            provider.provideTestCases(deref, context)
        }
        return mergeTestCaseOutcomes(outcomes)
    }
}


