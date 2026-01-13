package art.galushko.openapi.testgen.config

import art.galushko.openapi.testgen.example.config.ExampleValueSettings
import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGeneratorFactory
import art.galushko.openapi.testgen.generator.ArtifactGeneratorFactory
import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.spi.ArtifactGenerator
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@Epic("Configuration")
@Feature("Module Contributions API")
@DisplayName("TestGenerationModule")
class TestGenerationModuleTest {

    @Test
    @DisplayName("should sort modules by id deterministically")
    fun shouldSortModulesByIdDeterministically() {
        val sorted = TestGenerationEngine.sortAndValidateModules(
            listOf(TestModuleB(), TestModuleA()),
        )

        assertThat(sorted.map { it.id }).containsExactly("a", "b")
    }

    @Test
    @DisplayName("should reject duplicate module ids")
    fun shouldRejectDuplicateModuleIds() {
        val expected =
            "Duplicate TestGenerationModule ids: 'dup' -> " +
                "art.galushko.openapi.testgen.config.TestModuleDuplicateId, " +
                "art.galushko.openapi.testgen.config.TestModuleDuplicateId"

        assertThatThrownBy {
            TestGenerationEngine.sortAndValidateModules(
                listOf(TestModuleDuplicateId(), TestModuleDuplicateId()),
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage(expected)
    }

    @Test
    @DisplayName("should fail fast on duplicate artifact generator factory ids")
    fun shouldFailFastOnDuplicateArtifactGeneratorFactoryIds(
        @TempDir tempDir: Path,
    ) {
        val expected =
            "Artifact generator factory 'dup' is contributed by multiple modules: " +
                "'a' (art.galushko.openapi.testgen.config.DuplicateGeneratorFactoryFromModuleA) and " +
                "'b' (art.galushko.openapi.testgen.config.DuplicateGeneratorFactoryFromModuleB)"

        val options = TestGeneratorExecutionOptions(
            specFile = "spec.yaml",
            outputDir = tempDir,
            generatorId = "dup",
            generatorOptions = emptyMap(),
            testGenerationSettings = TestGenerationSettings(),
            alwaysWriteTests = false,
        )

        assertThatThrownBy {
            TestGenerationEngine.createArtifactGenerator(
                options = options,
                modules = listOf(
                    ModuleWithDuplicateFactoryB(),
                    ModuleWithDuplicateFactoryA(),
                ),
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage(expected)
    }

    @Test
    @DisplayName("should not fail for pattern schemas when pattern provider is missing")
    fun shouldNotFailForPatternSchemasWhenPatternProviderIsMissing() {
        val settings = ExampleValueSettings(providers = listOf("pattern"))
        val generator = SchemaExampleValueGeneratorFactory().create(settings)

        val schema = Schema<Any>().apply {
            type = "string"
            pattern = "^[A-Z]{3}$"
        }

        assertThatCode { generator.getExampleValue("value", schema, OpenAPI()) }
            .doesNotThrowAnyException()
    }
}

private class TestModuleA : TestGenerationModule {
    override val id: String = "a"
}

private class TestModuleB : TestGenerationModule {
    override val id: String = "b"
}

private class TestModuleDuplicateId : TestGenerationModule {
    override val id: String = "dup"
}

private class ModuleWithDuplicateFactoryA : TestGenerationModule {
    override val id: String = "a"
    override fun artifactGeneratorFactories(): List<ArtifactGeneratorFactory> = listOf(DuplicateGeneratorFactoryFromModuleA)
}

private class ModuleWithDuplicateFactoryB : TestGenerationModule {
    override val id: String = "b"
    override fun artifactGeneratorFactories(): List<ArtifactGeneratorFactory> = listOf(DuplicateGeneratorFactoryFromModuleB)
}

private object DuplicateGeneratorFactoryFromModuleA : ArtifactGeneratorFactory {
    override val id: String = "dup"
    override val description: String = "duplicate factory A"
    override fun create(outputDir: java.io.File, options: Map<String, Any?>): ArtifactGenerator = NoOpGenerator
}

private object DuplicateGeneratorFactoryFromModuleB : ArtifactGeneratorFactory {
    override val id: String = "dup"
    override val description: String = "duplicate factory B"
    override fun create(outputDir: java.io.File, options: Map<String, Any?>): ArtifactGenerator = NoOpGenerator
}

private object NoOpGenerator : ArtifactGenerator {
    override fun generateTests(testSuite: TestSuite) = Unit
}


