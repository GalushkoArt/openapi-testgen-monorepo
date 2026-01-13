package art.galushko.openapi.testgen.spi

import art.galushko.openapi.testgen.model.TestSuite

/**
 * Interface for generating test artifacts based on a [TestSuite].
 *
 * Implementations typically write files to an output directory.
 */
public interface ArtifactGenerator {
    /**
     * Generates test artifacts or code for the provided [TestSuite].
     *
     * @param testSuite suite describing test cases to generate
     */
    public fun generateTests(testSuite: TestSuite)

    /**
     * Generates test artifacts or code for the provided [TestSuite]s.
     *
     * @param testSuites suites describing test cases to generate
     */
    public fun generateTests(testSuites: List<TestSuite>) {
        testSuites.forEach { suite ->
            generateTests(suite)
        }
    }
}


