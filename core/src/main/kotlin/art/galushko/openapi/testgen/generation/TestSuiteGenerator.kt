package art.galushko.openapi.testgen.generation

import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.model.error.Outcome
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation

/**
 * Generates a [TestSuite] for a specific API path and operation.
 */
internal interface TestSuiteGenerator {
    /**
     * Generates a [TestSuite] for the given endpoint.
     *
     * @param openAPI the [OpenAPI] specification model
     * @param path the API path for which the test suite is generated
     * @param method the HTTP method for the API path
     * @param operation the OpenAPI [Operation] describing the endpoint
     * @return an [Outcome] wrapping the [TestSuite] and any collected errors
     */
    public fun generateTestSuite(openAPI: OpenAPI, path: String, method: String, operation: Operation): Outcome<TestSuite>
}


