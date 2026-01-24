package art.galushko.openapi.testgen

import art.galushko.openapi.testgen.config.GeneratorConfig
import art.galushko.openapi.testgen.config.TestGenerationEngine.createArtifactGenerator
import art.galushko.openapi.testgen.config.TestGenerationEngine.generateReport
import art.galushko.openapi.testgen.config.TestGeneratorExecutionOptionsFactory
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.reporting.reporter.JsonErrorReporter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

@Suppress("MaximumLineLength", "MaxLineLength")
@Epic("Test Generator")
@Feature("Test Generation Tests")
class TestGeneratorTest {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private data class TestGenerationSpec(
        val specFileName: String,
        val outputFileName: String,
        val testName: String,
        val ignoreSettings: Map<String, Any> = emptyMap(),
        val numberOfNotTestedOperations: Int = 0,
        val includeValidCase: Boolean = false,
    )

    @Test
    @DisplayName("Generate Tests (JSON): should process OpenAPI spec and write suites JSON by operation name")
    @Description("Verifies that generateTests processes each path/operation and produces a JSON map keyed by operationName matching the expected content")
    fun generateTestsShouldWriteTestSuitesJsonAggregatedByName() {
        testGenerationForOpenApiSchema(
            TestGenerationSpec(
                specFileName = "oas/openapi.yaml",
                outputFileName = "oas/openapi-generated-test-suites.json",
                testName = "general-openapi",
                ignoreSettings = mapOf(
                    "/fake" to "*",
                    "*" to mapOf(
                        "head" to "*",
                        "*" to "*", // should be ignored
                    ),
                    "/projects/{projectId}" to mapOf("delete" to "*"),
                    "/users" to mapOf("get" to listOf("Invalid Query role parameter: Array Item Invalid Enum Value")),
                ),
                numberOfNotTestedOperations = 1,
                includeValidCase = false,
            )
        )
    }

    @Test
    @DisplayName("Generate Security Tests (JSON): should process security-focused OpenAPI spec and write suites JSON by operation name")
    @Description("Verifies that generateTests processes security schemes and auth requirements, producing test suites with authentication test cases")
    fun generateSecurityTestsShouldWriteTestSuitesJsonAggregatedByName() {
        testGenerationForOpenApiSchema(
            TestGenerationSpec(
                specFileName = "oas/security.openapi.yaml",
                outputFileName = "oas/security-generated-test-suites.json",
                testName = "security-openapi",
                includeValidCase = true,
            )
        )
    }

    @Test
    @DisplayName("Generate Response Body Examples (JSON): should resolve expected bodies from spec")
    @Description("Verifies that response examples and schema fallbacks are used to populate expectedBody")
    fun generateResponseBodyExamplesShouldResolveExpectedBodies() {
        testGenerationForOpenApiSchema(
            TestGenerationSpec(
                specFileName = "oas/response-body.openapi.yaml",
                outputFileName = "oas/response-body-generated-test-suites.json",
                testName = "response-body-openapi",
                includeValidCase = true,
            )
        )
    }

    @Test
    @DisplayName("Cycle detection: circular reference is handled without overflow")
    @Description(
        """
        Verifies that the current implementation causes a StackOverflowError when processing:
        - circular schema references (Person references Address, Address references Person)
        - self-referencing schemas (LinkedList.next references LinkedList)
        - complex circular references (TreeNode.children contains TreeNode, TreeNode.metadata.parent references TreeNode)
        """
    )
    fun shouldHandleCircularReferencesWithoutStackOverflow() {
        testGenerationForOpenApiSchema(
            TestGenerationSpec(
                specFileName = "oas/circular.openapi.yaml",
                outputFileName = "oas/circular-generated-test-suites-by-name.json",
                testName = "circular-openapi",
                includeValidCase = true,
            )
        )
    }

    @Nested
    @Story("Complex Schema Validation")
    @DisplayName("Complex Schema Tests")
    inner class ComplexSchemaTests {

        @Test
        @DisplayName("Schema Composition: allOf with anyOf")
        @Description("Verifies test generation for allOf schema merging combined with anyOf for flexible contact information and metadata types")
        fun shouldHandleCreateUserWithAllOfAndAnyOf() {
            testGenerationForOpenApiSchema(
                TestGenerationSpec(
                    specFileName = "oas/complex/createUser.openapi.yaml",
                    outputFileName = "oas/complex/createUser-test-suites.json",
                    testName = "complex-createUser",
                    includeValidCase = true,
                )
            )
        }

        @Test
        @DisplayName("Discriminated Union: oneOf with discriminator property")
        @Description("Verifies test generation for discriminated union (Cat or Dog) using oneOf with discriminator property")
        fun shouldHandleCreatePetWithDiscriminator() {
            testGenerationForOpenApiSchema(
                TestGenerationSpec(
                    specFileName = "oas/complex/createPet.openapi.yaml",
                    outputFileName = "oas/complex/createPet-test-suites.json",
                    testName = "complex-createPet",
                    includeValidCase = true,
                )
            )
        }

        @Test
        @DisplayName("Schema Composition: oneOf with object on the same level")
        @Description("Verifies test generation for oneOf with object on the same level")
        fun shouldHandleCreateProduct() {
            testGenerationForOpenApiSchema(
                TestGenerationSpec(
                    specFileName = "oas/complex/createProduct.openapi.yaml",
                    outputFileName = "oas/complex/createProduct-test-suites.json",
                    testName = "complex-createProduct",
                    includeValidCase = true,
                )
            )
        }

        @Test
        @DisplayName("Resource Listing: complex string, integer, and array types, integer or string alternative types, anyOf with contradiction types")
        @Description("Verifies test generation for complex string, integer, and array types, integer or string alternative types, anyOf with contradiction types")
        fun shouldHandleListUsers() {
            testGenerationForOpenApiSchema(
                TestGenerationSpec(
                    specFileName = "oas/complex/listUsers.openapi.yaml",
                    outputFileName = "oas/complex/listUsers-test-suites.json",
                    testName = "complex-listUsers",
                    includeValidCase = true,
                )
            )
        }

        @Test
        @DisplayName("Discriminated Union: complex properties of the object with the discriminator")
        @Description("Verifies test generation for complex properties of the object with the discriminator")
        fun shouldHandleProcessPaymentWithDiscriminator() {
            testGenerationForOpenApiSchema(
                TestGenerationSpec(
                    specFileName = "oas/complex/processPayment.openapi.yaml",
                    outputFileName = "oas/complex/processPayment-test-suites.json",
                    testName = "complex-processPayment",
                    includeValidCase = true,
                )
            )
        }

        @Test
        @DisplayName("Resource Update: object with requirements in anyOf")
        @Description("Verifies test generation for object with requirements in anyOf")
        fun shouldHandleUpdateUser() {
            testGenerationForOpenApiSchema(
                TestGenerationSpec(
                    specFileName = "oas/complex/updateUser.openapi.yaml",
                    outputFileName = "oas/complex/updateUser-test-suites.json",
                    testName = "complex-updateUser",
                    includeValidCase = true,
                )
            )
        }

        @Test
        @DisplayName("Array Validation: oneOf and anyOf with contains - Validate Arrays")
        @Description("Verifies test generation for complex array structures including oneOf for homogeneous types, anyOf with constraints, and contains validation")
        fun shouldHandleValidateArrays() {
            testGenerationForOpenApiSchema(
                TestGenerationSpec(
                    specFileName = "oas/complex/validateArrays.openapi.yaml",
                    outputFileName = "oas/complex/validateArrays-test-suites.json",
                    testName = "complex-validateArrays",
                    includeValidCase = true,
                )
            )
        }

        @Test
        @DisplayName("Object Schema Composition: nested oneOf, anyOf, allOf - Validate Complex Combinations of object")
        @Description("Verifies test generation for advanced schema combinations including oneOf within anyOf, allOf within anyOf, and anyOf within allOf")
        fun shouldHandleValidateComplexCombinations() {
            testGenerationForOpenApiSchema(
                TestGenerationSpec(
                    specFileName = "oas/complex/validateComplexCombinations.openapi.yaml",
                    outputFileName = "oas/complex/validateComplexCombinations-test-suites.json",
                    testName = "complex-validateComplexCombinations",
                    includeValidCase = true,
                )
            )
        }

        @Test
        @DisplayName("Nullable Types: nullable property and oneOf with null type - Validate Nullables")
        @Description("Verifies test generation for nullable string handling using both nullable property attribute and oneOf with explicit null type")
        fun shouldHandleValidateNullables() {
            testGenerationForOpenApiSchema(
                TestGenerationSpec(
                    specFileName = "oas/complex/validateNullables.openapi.yaml",
                    outputFileName = "oas/complex/validateNullables-test-suites.json",
                    testName = "complex-validateNullables",
                    includeValidCase = true,
                )
            )
        }
    }

    private fun testGenerationForOpenApiSchema(spec: TestGenerationSpec) {
        val outputDir = "build/tmp/json-generator-test"
        val generatorOptions = mapOf(
            "outputFileName" to spec.outputFileName,
            "writeMode" to "OVERWRITE",
            "preventOverwriteSuites" to false,
            "preventOverwriteCases" to false,
        )
        val options = TestGeneratorExecutionOptionsFactory.fromConfig(
            GeneratorConfig(
                specFile = spec.specFileName,
                outputDir = outputDir,
                generator = "test-suite-writer",
                generatorOptions = generatorOptions,
                testGenerationSettings = mapOf(
                    "ignoreTestCases" to spec.ignoreSettings,
                    "includeValidCase" to spec.includeValidCase,
                ),
            )
        )

        val writer = createArtifactGenerator(options)

        val report = step("Generate tests for ${spec.testName}") {
            generateReport(options)
        }

        step("Write generated test suites for ${spec.testName}") {
            writer.generateTests(report.successfulSuites)
        }

        step("Verify generated test suites for ${spec.testName}") {
            assertSoftly { assertions ->
                assertions.assertThat(report.hasErrors).isFalse
                assertions.assertThat(report.errors).hasSize(report.summary.totalErrors)
                assertions.assertThat(report.summary.partialOperations).hasSize(0)
                assertions.assertThat(report.summary.failedOperations).hasSize(0)
                assertions.assertThat(report.summary.notTestedOperations).hasSize(spec.numberOfNotTestedOperations)

                val producedFile = File(outputDir, spec.outputFileName)
                require(producedFile.exists()) { "Produced JSON file not found: ${producedFile.absolutePath}" }

                val produced: Map<String, TestSuite> = objectMapper.readValue(producedFile)

                // objectMapper.writeValueAsString(produced)
                val expected: Map<String, TestSuite> = this::class.java.classLoader.getResourceAsStream(spec.outputFileName).use {
                    objectMapper.readValue(requireNotNull(it))
                }

                assertThat(produced).usingRecursiveComparison().isEqualTo(expected)
            }
        }
    }

    @Nested
    @Story("Outcome Handling")
    @DisplayName("Outcome Tests - PartialSuccess and Failure")
    inner class OutcomeHandlingTests {

        private val jsonErrorReporter = JsonErrorReporter(objectMapper)

        @Test
        @DisplayName("PartialSuccess: should handle operation where some providers succeed and others fail due to budget")
        @Description(
            """
            Verifies that when maxSchemaCombinations is set low, operations with complex schemas
            result in PartialSuccess - some test cases are generated successfully while others
            fail due to BudgetExceededException. The report should contain:
            - Successful suites for simple operations
            - Partial results for operations with mixed simple/complex schemas
            - Errors containing budget exceeded information
            """
        )
        fun shouldHandlePartialSuccessWhenBudgetExceededOnComplexSchemas() {
            testOutcomeHandling(
                specFileName = "oas/outcome/partialSuccess.openapi.yaml",
                expectedReportFileName = "oas/outcome/partialSuccess-report.json",
                testName = "partial-success",
                maxSchemaCombinations = 3,
            )
        }

        @Test
        @DisplayName("Failure: should handle complete operation failure when budget exceeded immediately")
        @Description(
            """
            Verifies that when maxSchemaCombinations is set very low and schema is extremely complex,
            operations fail completely with Failure outcome. The report should contain:
            - No successful suites for budget-exceeded operations
            - Failed operations in the summary
            - Errors containing budget exceeded information
            """
        )
        fun shouldHandleFailureWhenBudgetExceededImmediately() {
            testOutcomeHandling(
                specFileName = "oas/outcome/failure.openapi.yaml",
                expectedReportFileName = "oas/outcome/failure-report.json",
                testName = "failure-budget-exceeded",
                maxSchemaCombinations = 2,
            )
        }

        @Test
        @DisplayName("Failure: should handle valid case builder failure for unsupported media type")
        @Description(
            """
            Verifies that when ValidCaseBuilder fails (e.g., unsupported media type),
            the operation results in Failure outcome with appropriate error context.
            """
        )
        fun shouldHandleValidCaseBuilderFailure() {
            testOutcomeHandling(
                specFileName = "oas/outcome/failure.openapi.yaml",
                expectedReportFileName = "oas/outcome/validCaseFailure-report.json",
                testName = "validcase-failure",
            )
        }

        private fun testOutcomeHandling(
            specFileName: String,
            expectedReportFileName: String,
            testName: String,
            maxSchemaCombinations: Int = 100,
        ) {
            val outputDir = "build/tmp/json-generator-test"
            val options = TestGeneratorExecutionOptionsFactory.fromConfig(
                GeneratorConfig(
                    specFile = specFileName,
                    outputDir = outputDir,
                    generator = "test-suite-writer",
                    generatorOptions = mapOf(
                        "outputFileName" to "oas/outcome/$testName-test-suites.json",
                        "writeMode" to "OVERWRITE",
                    ),
                    testGenerationSettings = mapOf(
                        "maxSchemaCombinations" to maxSchemaCombinations,
                    ),
                )
            )

            val report = step("Generate tests for $testName") {
                generateReport(options)
            }

            step("Write report JSON for $testName") {
                val reportJson = jsonErrorReporter.format(report)
                File(outputDir, expectedReportFileName).apply {
                    parentFile.mkdirs()
                    writeText(reportJson)
                }
            }

            step("Verify report matches expected for $testName") {
                val producedFile = File(outputDir, expectedReportFileName)
                require(producedFile.exists()) { "Produced report file not found: ${producedFile.absolutePath}" }

                val produced: JsonNode = objectMapper.readTree(producedFile)

                val expected: JsonNode = this::class.java.classLoader.getResourceAsStream(expectedReportFileName).use {
                    objectMapper.readTree(it)
                }

                expected.get("errors").asSequence().forEach { error -> error.properties().removeAll { it.key == ("exceptionText") } }
                produced.get("errors").asSequence().forEach { error -> error.properties().removeAll { it.key == ("exceptionText") } }

                assertThat(produced).isEqualTo(expected)
            }
        }
    }
}


