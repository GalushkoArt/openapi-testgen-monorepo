package art.galushko.openapi.testgen.generator.writer

import art.galushko.openapi.testgen.model.TestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TestCase fields drift guard")
class TestCaseFieldsGuardTest {

    /**
     * TestSuiteWriter.mergeCasePreservingFields dispatches on string field names that must
     * mirror the TestCase model. This guard fails when TestCase gains, loses, or renames a
     * field so the merge logic (and the `protectedTestCaseFields` option docs) get updated
     * together with the model.
     */
    @Test
    @DisplayName("mergeCasePreservingFields must cover every TestCase constructor field")
    fun mergeFieldsMustMirrorTestCase() {
        val primaryConstructor = TestCase::class.java.constructors
            .filter { !it.isSynthetic }
            .maxByOrNull { it.parameterCount }
            ?: error("No public TestCase constructor found")

        val fieldNames = primaryConstructor.parameters.map { it.name }

        assertThat(fieldNames)
            .withFailMessage(
                "TestCase fields changed: %s. Update TestSuiteWriter.mergeCasePreservingFields " +
                    "and the protectedTestCaseFields documentation, then adjust this guard.",
                fieldNames,
            )
            .containsExactlyInAnyOrder(
                "name",
                "method",
                "path",
                "queryParams",
                "pathParams",
                "headers",
                "cookie",
                "securityValues",
                "body",
                "requestBodyMediaType",
                "expectedBody",
                "responseBodyMediaType",
                "needToComplete",
                "expectedStatusCode",
                "rule",
            )
    }
}
