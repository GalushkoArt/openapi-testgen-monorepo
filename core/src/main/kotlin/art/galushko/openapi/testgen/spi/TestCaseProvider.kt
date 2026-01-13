package art.galushko.openapi.testgen.spi

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.error.Outcome

/**
 * Produces derived test cases for a specific OpenAPI element.
 *
 * Inputs: an OpenAPI element and a [TestGenerationContext] with a valid baseline case.
 * Output: [Outcome] wrapping the generated test cases.
 * Errors: providers should convert failures to [Outcome.Failure] (see `runProviderSafely`).
 * Determinism: providers should preserve input order and avoid non-deterministic iteration.
 *
 * @param T the OpenAPI element type this provider consumes (e.g., Operation, Parameter, RequestBody)
 */
public interface TestCaseProvider<T> {
    /**
     * Returns [Outcome] with test cases generated from the given baseline valid case and OpenAPI element.
     *
     * @param spec the OpenAPI element to analyze; when null, no cases are produced
     * @param context with a valid test case, operation specification and OpenAPI document
     * @return Outcome with generated test cases; empty when not applicable
     */
    public fun provideTestCases(spec: T, context: TestGenerationContext): Outcome<List<TestCase>>
}


