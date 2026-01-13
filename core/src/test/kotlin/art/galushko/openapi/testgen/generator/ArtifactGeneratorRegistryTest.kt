package art.galushko.openapi.testgen.generator

import art.galushko.openapi.testgen.generator.writer.TestSuiteWriter
import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.spi.ArtifactGenerator
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@Epic("Generator")
@Feature("ArtifactGeneratorRegistry")
@DisplayName("ArtifactGeneratorRegistry Tests")
class ArtifactGeneratorRegistryTest {

    @TempDir
    lateinit var tempDir: File

    @Nested
    @Story("Built-in Generators")
    @DisplayName("Built-in Generator Discovery")
    inner class BuiltInGeneratorsTests {

        @Test
        @DisplayName("should discover test-suite-writer generator")
        @Description("Verifies that built-in generators are registered at initialization")
        fun shouldDiscoverBuiltInGenerators() {
            val registry = ArtifactGeneratorRegistry()
            val ids = registry.availableIds()

            assertThat(ids).containsExactlyInAnyOrder(
                GeneratorIds.TEST_SUITE_WRITER
            )
        }

        @Test
        @DisplayName("should return available generators sorted by id")
        @Description("Verifies that availableIds() returns a sorted set")
        fun shouldReturnSortedAvailableIds() {
            val registry = ArtifactGeneratorRegistry()
            val ids = registry.availableIds().toList()

            assertThat(ids).isSortedAccordingTo(Comparator.naturalOrder())
        }

        @Test
        @DisplayName("should return available generator factories sorted by id")
        @Description("Verifies that availableGenerators() returns factories sorted by id")
        fun shouldReturnSortedAvailableGenerators() {
            val registry = ArtifactGeneratorRegistry()
            val factories = registry.availableGenerators()

            assertThat(factories.map { it.id }).isSortedAccordingTo(Comparator.naturalOrder())
        }

        @Test
        @DisplayName("should have correct descriptions for built-in generators")
        @Description("Verifies that built-in generators have meaningful descriptions")
        fun shouldHaveCorrectDescriptions() {
            val registry = ArtifactGeneratorRegistry()
            val factories = registry.availableGenerators()

            val writerFactory = factories.find { it.id == GeneratorIds.TEST_SUITE_WRITER }

            assertThat(writerFactory?.description).isEqualTo("Writes test suites to JSON/YAML files")
        }
    }

    @Nested
    @Story("Generator Creation")
    @DisplayName("Generator Creation via Registry")
    inner class GeneratorCreationTests {

        @Test
        @DisplayName("should create generator from extra factory")
        @Description("Verifies that generators can be supplied via extraFactories constructor parameter")
        fun shouldCreateGeneratorFromExtraFactory() {
            class FakeTemplateGenerator : ArtifactGenerator {
                override fun generateTests(testSuite: TestSuite) {}
            }

            val templateFactory = object : ArtifactGeneratorFactory {
                override val id: String = GeneratorIds.TEMPLATE
                override val description: String = "Template generator"
                override fun create(outputDir: File, options: Map<String, Any?>): ArtifactGenerator = FakeTemplateGenerator()
            }

            val registry = ArtifactGeneratorRegistry(extraFactories = listOf(templateFactory))
            val generator = registry.create(GeneratorIds.TEMPLATE, tempDir, emptyMap())

            assertThat(generator).isInstanceOf(FakeTemplateGenerator::class.java)
        }

        @Test
        @DisplayName("should create TestSuiteWriter")
        @Description("Verifies that 'test-suite-writer' generator creates TestSuiteWriter instance")
        fun shouldCreateTestSuiteWriter() {
            val registry = ArtifactGeneratorRegistry()
            val generator = registry.create(
                GeneratorIds.TEST_SUITE_WRITER,
                tempDir,
                mapOf("outputFileName" to "test.json")
            )

            assertThat(generator).isInstanceOf(TestSuiteWriter::class.java)
        }

        @Test
        @DisplayName("should pass options to factory create method")
        @Description("Verifies that generator options are properly delegated to factory")
        fun shouldPassOptionsToFactory() {
            val registry = ArtifactGeneratorRegistry()
            val customOptions = mapOf(
                "outputFileName" to "test.json",
                "format" to "YAML",
            )

            // Create generator with custom options
            val generator = registry.create(GeneratorIds.TEST_SUITE_WRITER, tempDir, customOptions)

            // Verify generator was created (options validation happens inside TestSuiteWriter constructor)
            assertThat(generator).isInstanceOf(TestSuiteWriter::class.java)
        }

        @Test
        @DisplayName("should throw for unknown generator with helpful message")
        @Description("Verifies that unknown generator ID throws exception with available generators listed")
        fun shouldThrowForUnknownGenerator() {
            val registry = ArtifactGeneratorRegistry()

            assertThatThrownBy {
                registry.create("unknown-generator", tempDir, emptyMap())
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Unknown generator: 'unknown-generator'. Available: test-suite-writer")
        }

        @Test
        @DisplayName("should create generator from manually registered factory")
        @Description("Verifies that manually registered generators can be created")
        fun shouldCreateGeneratorFromManuallyRegisteredFactory() {
            val registry = ArtifactGeneratorRegistry()

            val customFactory = object : ArtifactGeneratorFactory {
                override val id: String = "custom-generator"
                override val description: String = "Custom generator"
                override fun create(outputDir: File, options: Map<String, Any?>): ArtifactGenerator =
                    object : ArtifactGenerator {
                        override fun generateTests(testSuite: TestSuite) {}
                    }
            }

            registry.register(customFactory)
            val generator = registry.create("custom-generator", tempDir, emptyMap())

            assertThat(generator).isNotNull()
        }
    }

    @Nested
    @Story("Manual Registration")
    @DisplayName("Manual Generator Registration")
    inner class ManualRegistrationTests {

        @Test
        @DisplayName("should allow manual registration of custom generator")
        @Description("Verifies that custom generators can be registered manually")
        fun shouldAllowManualRegistration() {
            val registry = ArtifactGeneratorRegistry()

            val customFactory = object : ArtifactGeneratorFactory {
                override val id: String = "custom-test-generator"
                override val description: String = "Custom test generator"
                override fun create(outputDir: File, options: Map<String, Any?>): ArtifactGenerator =
                    object : ArtifactGenerator {
                        override fun generateTests(testSuite: TestSuite) {}
                    }
            }

            registry.register(customFactory)

            assertThat(registry.availableIds()).contains("custom-test-generator")
        }

        @Test
        @DisplayName("should reject duplicate manual registration")
        @Description("Verifies that registering a generator with existing ID throws exception")
        fun shouldRejectDuplicateRegistration() {
            val registry = ArtifactGeneratorRegistry()

            val duplicateFactory = object : ArtifactGeneratorFactory {
                override val id: String = GeneratorIds.TEST_SUITE_WRITER
                override val description: String = "Duplicate test-suite-writer generator"
                override fun create(outputDir: File, options: Map<String, Any?>): ArtifactGenerator =
                    throw UnsupportedOperationException("Should not be called")
            }

            assertThatThrownBy {
                registry.register(duplicateFactory)
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Generator 'test-suite-writer' already registered by art.galushko.openapi.testgen.generator.writer.TestSuiteWriterGeneratorFactory")
        }

        @Test
        @DisplayName("should maintain registration order for custom generators")
        @Description("Verifies that manually registered generators are included in sorted output")
        fun shouldMaintainRegistrationOrderForCustomGenerators() {
            val registry = ArtifactGeneratorRegistry()

            val customFactory = object : ArtifactGeneratorFactory {
                override val id: String = "aaa-custom-generator"
                override val description: String = "Custom generator that sorts first"
                override fun create(outputDir: File, options: Map<String, Any?>): ArtifactGenerator =
                    object : ArtifactGenerator {
                        override fun generateTests(testSuite: TestSuite) {}
                    }
            }

            registry.register(customFactory)
            val ids = registry.availableIds().toList()

            // Verify custom generator is first due to alphabetical sorting
            assertThat(ids).first().isEqualTo("aaa-custom-generator")
            assertThat(ids).containsExactly(
                "aaa-custom-generator",
                GeneratorIds.TEST_SUITE_WRITER
            )
        }

        @Test
        @DisplayName("should support extra factories via constructor")
        @Description("Verifies that extra factories can be passed via constructor")
        fun shouldSupportExtraFactoriesViaConstructor() {
            val customFactory = object : ArtifactGeneratorFactory {
                override val id: String = "extra-custom-generator"
                override val description: String = "Extra custom generator"
                override fun create(outputDir: File, options: Map<String, Any?>): ArtifactGenerator =
                    object : ArtifactGenerator {
                        override fun generateTests(testSuite: TestSuite) {}
                    }
            }

            val registry = ArtifactGeneratorRegistry(extraFactories = listOf(customFactory))

            assertThat(registry.availableIds()).containsExactlyInAnyOrder(
                GeneratorIds.TEST_SUITE_WRITER,
                "extra-custom-generator"
            )
        }
    }

    @Nested
    @Story("ArtifactGeneratorConfigurer Delegation")
    @DisplayName("ArtifactGeneratorConfigurer Delegation")
    inner class ConfigurerDelegationTests {

        @Test
        @DisplayName("should create generator via configurer")
        @Description("Verifies that ArtifactGeneratorConfigurer delegates to registry")
        fun shouldCreateGeneratorViaConfigurer() {
            val generator = ArtifactGeneratorConfigurer.createArtifactGenerator(
                GeneratorIds.TEST_SUITE_WRITER,
                mapOf("outputFileName" to "test.json"),
                tempDir
            )

            assertThat(generator).isInstanceOf(TestSuiteWriter::class.java)
        }
    }

    @Nested
    @Story("GeneratorIds Constants")
    @DisplayName("GeneratorIds Constants")
    inner class GeneratorIdsTests {

        @Test
        @DisplayName("should have correct TEMPLATE constant")
        @Description("Verifies that TEMPLATE constant matches expected value")
        fun shouldHaveCorrectTemplateConstant() {
            assertThat(GeneratorIds.TEMPLATE).isEqualTo("template")
        }

        @Test
        @DisplayName("should have correct TEST_SUITE_WRITER constant")
        @Description("Verifies that TEST_SUITE_WRITER constant matches expected value")
        fun shouldHaveCorrectTestSuiteWriterConstant() {
            assertThat(GeneratorIds.TEST_SUITE_WRITER).isEqualTo("test-suite-writer")
        }
    }

    @Nested
    @Story("ArtifactGeneratorFactory Interface")
    @DisplayName("ArtifactGeneratorFactory Implementations")
    inner class FactoryImplementationTests {

        @Test
        @DisplayName("should have all factory implementations as Kotlin objects")
        @Description("Verifies that built-in factories are implemented as singleton objects")
        fun shouldHaveFactoryImplementationsAsObjects() {
            val registry = ArtifactGeneratorRegistry()
            val factories = registry.availableGenerators()

            // All built-in factories should be Kotlin objects (singletons)
            factories.forEach { factory ->
                // Kotlin objects have an INSTANCE field
                val instanceField = factory::class.java.declaredFields.find { it.name == "INSTANCE" }
                assertThat(instanceField)
                    .describedAs("Factory ${factory.id} should be a Kotlin object")
                    .isNotNull()
            }
        }

        @Test
        @DisplayName("should have unique IDs for all factories")
        @Description("Verifies that all factory IDs are unique")
        fun shouldHaveUniqueIdsForAllFactories() {
            val registry = ArtifactGeneratorRegistry()
            val factories = registry.availableGenerators()
            val ids = factories.map { it.id }

            assertThat(ids).doesNotHaveDuplicates()
        }

        @Test
        @DisplayName("should have non-empty descriptions for all factories")
        @Description("Verifies that all factories provide meaningful descriptions")
        fun shouldHaveNonEmptyDescriptionsForAllFactories() {
            val registry = ArtifactGeneratorRegistry()
            val factories = registry.availableGenerators()

            factories.forEach { factory ->
                assertThat(factory.description)
                    .describedAs("Factory ${factory.id} should have a description")
                    .isNotBlank()
            }
        }

        @Test
        @DisplayName("should create different generator instances on each call")
        @Description("Verifies that factory.create() returns new instances")
        fun shouldCreateDifferentGeneratorInstances() {
            val registry = ArtifactGeneratorRegistry()
            val factory = registry.availableGenerators().first { it.id == GeneratorIds.TEST_SUITE_WRITER }

            val generator1 = factory.create(tempDir, mapOf("outputFileName" to "test.json"))
            val generator2 = factory.create(tempDir, mapOf("outputFileName" to "test.json"))

            // Should create new instances, not return same object
            assertThat(generator1).isNotSameAs(generator2)
        }
    }
}
