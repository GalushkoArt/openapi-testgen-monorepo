package art.galushko.openapi.testgen.generator.template

import art.galushko.openapi.testgen.generator.ArtifactGeneratorFactory
import art.galushko.openapi.testgen.generator.GeneratorIds
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.spi.ArtifactGenerator
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@Epic("Generator")
@Feature("TemplateArtifactGeneratorFactory")
@DisplayName("TemplateArtifactGeneratorFactory Tests")
class TemplateArtifactGeneratorFactoryTest {

    @Nested
    @Story("SPI Metadata")
    @DisplayName("SPI Metadata")
    inner class MetadataTests {

        @Test
        @DisplayName("id should equal GeneratorIds.TEMPLATE")
        @Description("Verifies the factory registers under the canonical 'template' id")
        fun shouldExposeTemplateId() {
            assertThat(TemplateArtifactGeneratorFactory.id).isEqualTo(GeneratorIds.TEMPLATE)
        }

        @Test
        @DisplayName("description should be the documented Mustache description")
        @Description("Verifies the human-readable description is stable and non-blank")
        fun shouldExposeDescription() {
            assertThat(TemplateArtifactGeneratorFactory.description)
                .isEqualTo("Generates test code using Mustache templates")
        }

        @Test
        @DisplayName("should implement ArtifactGeneratorFactory")
        @Description("Confirms the object is wired as an ArtifactGeneratorFactory SPI")
        fun shouldImplementFactorySpi() {
            assertThat(TemplateArtifactGeneratorFactory).isInstanceOf(ArtifactGeneratorFactory::class.java)
        }
    }

    @Nested
    @Story("Generator Creation")
    @DisplayName("create()")
    inner class CreateTests {

        @Test
        @DisplayName("should create an ArtifactGenerator that produces output when invoked")
        @Description("End-to-end SPI test: factory.create() yields a working generator")
        fun shouldCreateWorkingGenerator(@TempDir tempDir: Path) {
            val generator: ArtifactGenerator = TemplateArtifactGeneratorFactory.create(
                outputDir = tempDir.toFile(),
                options = mapOf("templateSet" to "restassured-java"),
            )

            val testSuite = TestSuite(
                path = "/users",
                method = "POST",
                operationName = "createUser",
                testCases = listOf(
                    TestCase(
                        name = "Valid Case",
                        method = "POST",
                        path = "/users",
                        expectedStatusCode = 201,
                    )
                )
            )

            step("Generate tests via factory-produced generator") {
                generator.generateTests(testSuite)
            }

            step("Verify generator produced the expected output file") {
                val generatedFile = tempDir.resolve("CreateUserTest.java").toFile()
                assertThat(generatedFile).exists()
                assertThat(generatedFile.readText()).contains("class CreateUserTest")
            }
        }

        @Test
        @DisplayName("should return distinct generator instances on each call")
        @Description("Verifies the factory does not cache generators, so each call is independent")
        fun shouldReturnDistinctInstances(@TempDir tempDir: Path) {
            val options = mapOf("templateSet" to "restassured-java")

            val generator1 = TemplateArtifactGeneratorFactory.create(tempDir.toFile(), options)
            val generator2 = TemplateArtifactGeneratorFactory.create(tempDir.toFile(), options)

            assertThat(generator1).isNotSameAs(generator2)
        }
    }
}
