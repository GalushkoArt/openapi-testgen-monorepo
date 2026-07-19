package art.galushko.openapi.testgen.openapi

import art.galushko.openapi.testgen.SnapshotSupport
import art.galushko.openapi.testgen.config.GeneratorConfig
import art.galushko.openapi.testgen.config.TestGenerationEngine.createArtifactGenerator
import art.galushko.openapi.testgen.config.TestGenerationEngine.generateReport
import art.galushko.openapi.testgen.config.TestGeneratorExecutionOptionsFactory
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.model.TestSuite
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

@Epic("Test Generator")
@Feature("Swagger 2.0 Generation")
class Swagger2GenerationTest {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @Test
    @DisplayName("Generate Swagger 2.0 Tests (JSON): should write suites JSON by operation name")
    @Description("Verifies that Swagger 2.0 input is normalized and produces stable generated suites")
    fun generateSwagger2TestsShouldWriteTestSuitesJsonAggregatedByName() {
        val outputDir = "build/tmp/swagger2-generator-test"
        val outputFileName = "oas/swagger2/minimal-generated-test-suites.json"
        val options = options(
            specFile = "oas/swagger2/minimal.swagger.yaml",
            outputDir = outputDir,
            outputFileName = outputFileName,
            includeValidCase = false,
        )

        val writer = createArtifactGenerator(options)
        val report = step("Generate tests for minimal Swagger 2.0") {
            generateReport(options)
        }

        step("Write generated test suites for minimal Swagger 2.0") {
            writer.generateTests(report.successfulSuites)
        }

        step("Verify generated Swagger 2.0 suites") {
            val producedFile = File(outputDir, outputFileName)
            require(producedFile.exists()) { "Produced JSON file not found: ${producedFile.absolutePath}" }

            val produced: Map<String, TestSuite> = objectMapper.readValue(producedFile)

            assertSoftly { assertions ->
                assertions.assertThat(report.hasErrors).isFalse
                assertions.assertThat(report.summary.totalOperations).isEqualTo(1)
                assertions.assertThat(report.summary.totalTestCases).isEqualTo(6)
                if (!SnapshotSupport.maybeUpdateSnapshot(producedFile, outputFileName)) {
                    val expected: Map<String, TestSuite> =
                        this::class.java.classLoader.getResourceAsStream(outputFileName).use {
                            objectMapper.readValue(requireNotNull(it))
                        }
                    assertions.assertThat(produced).usingRecursiveComparison().isEqualTo(expected)
                }
            }
        }
    }

    @Test
    @DisplayName("Swagger 2.0 body and path-level parameters should generate supported test cases")
    fun shouldGenerateFromSwagger2BodyAndPathLevelParameters() {
        val report = generateReport(
            options(
                specFile = "oas/swagger2/definitions-composition.swagger.yaml",
                outputDir = "build/tmp/swagger2-generator-test",
                outputFileName = "oas/swagger2/definitions-composition-generated-test-suites.json",
                includeValidCase = true,
            )
        )

        val suite = report.successfulSuites.single { it.operationName == "createOwnerPet" }
        val validCase = suite.testCases.single { it.name == "Test Valid Case" }

        assertSoftly { softly ->
            softly.assertThat(report.hasErrors).isFalse
            softly.assertThat(report.summary.totalOperations).isEqualTo(1)
            softly.assertThat(report.summary.totalTestCases).isEqualTo(18)
            softly.assertThat(suite.testCases.map { it.name }).contains(
                "Invalid Path ownerId parameter: Out Of Minimum Length String",
                "Required Request Body is missing",
                "Incorrect Request Body: Missed Required Object Properties age",
                "Incorrect Request Body: Null For Required Property age",
            )
            softly.assertThat(validCase.pathParams).containsEntry("ownerId", "aaaa")
            softly.assertThat(validCase.requestBodyMediaType).isEqualTo("application/json")
            softly.assertThat(validCase.expectedStatusCode).isEqualTo(201)
            softly.assertThat(validCase.expectedBody).isEqualTo(
                mapOf("id" to "pet-1", "ownerId" to "owner-1", "name" to "Milo", "type" to "cat")
            )
        }
    }

    @Test
    @DisplayName("Equivalent Swagger 2.0 and OpenAPI 3.0 fixtures should generate equivalent suites")
    fun equivalentSwagger2AndOpenApi3FixturesShouldGenerateEquivalentSuites() {
        val swaggerSuites = generateSuites("oas/swagger2/minimal.swagger.yaml", includeValidCase = true)
        val openApiSuites = generateSuites("oas/swagger2/minimal.openapi.yaml", includeValidCase = true)

        assertThat(objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(swaggerSuites))
            .isEqualTo(objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(openApiSuites))
    }

    private fun generateSuites(specFile: String, includeValidCase: Boolean): List<TestSuite> {
        val report = generateReport(
            options(
                specFile = specFile,
                outputDir = "build/tmp/swagger2-generator-test",
                outputFileName = "unused.json",
                includeValidCase = includeValidCase,
            )
        )
        assertThat(report.hasErrors).isFalse
        return report.successfulSuites
    }

    private fun options(
        specFile: String,
        outputDir: String,
        outputFileName: String,
        includeValidCase: Boolean,
    ) = TestGeneratorExecutionOptionsFactory.fromConfig(
        GeneratorConfig(
            specFile = specFile,
            outputDir = outputDir,
            generator = "test-suite-writer",
            generatorOptions = mapOf(
                "outputFileName" to outputFileName,
                "writeMode" to "OVERWRITE",
                "preventOverwriteSuites" to false,
                "preventOverwriteCases" to false,
            ),
            testGenerationSettings = mapOf("includeValidCase" to includeValidCase),
        )
    )
}
