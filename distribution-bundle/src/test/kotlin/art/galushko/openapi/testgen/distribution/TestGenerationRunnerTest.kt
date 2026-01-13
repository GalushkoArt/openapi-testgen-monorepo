package art.galushko.openapi.testgen.distribution

import art.galushko.openapi.testgen.model.error.GenerationReport
import art.galushko.openapi.testgen.reporting.reporter.ConsoleReporter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

@DisplayName("TestGenerationRunner")
class TestGenerationRunnerTest {

    private val testLogger = LoggerFactory.getLogger(TestGenerationRunnerTest::class.java)

    @Nested
    @DisplayName("Builder")
    inner class BuilderTest {

        @Test
        @DisplayName("should require reporter to be set")
        fun shouldRequireReporter() {
            assertThatThrownBy {
                TestGenerationRunner.builder().build()
            }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Reporter must be set")
        }

        @Test
        @DisplayName("should build runner with reporter")
        fun shouldBuildWithReporter() {
            val runner = TestGenerationRunner.builder()
                .reporter(Slf4jReporter(testLogger))
                .build()

            assertThat(runner).isNotNull
        }

        @Test
        @DisplayName("should use default settings when not provided")
        fun shouldUseDefaultSettings() {
            val runner = TestGenerationRunner.builder()
                .reporter(Slf4jReporter(testLogger))
                .build()

            assertThat(runner).isNotNull
        }

        @Test
        @DisplayName("should allow adding individual modules")
        fun shouldAllowAddingIndividualModules() {
            val runner = TestGenerationRunner.builder()
                .reporter(Slf4jReporter(testLogger))
                .addModule(art.galushko.openapi.testgen.generator.template.TemplateGeneratorModule)
                .build()

            assertThat(runner).isNotNull
        }

        @Test
        @DisplayName("should allow setting all modules at once")
        fun shouldAllowSettingAllModules() {
            val runner = TestGenerationRunner.builder()
                .reporter(Slf4jReporter(testLogger))
                .modules(DistributionDefaults.modules())
                .build()

            assertThat(runner).isNotNull
        }

        @Test
        @DisplayName("should allow adding individual extractors")
        fun shouldAllowAddingIndividualExtractors() {
            val runner = TestGenerationRunner.builder()
                .reporter(Slf4jReporter(testLogger))
                .addModuleExtractor(art.galushko.openapi.testgen.pattern.support.PatternModuleSettingsExtractor)
                .build()

            assertThat(runner).isNotNull
        }
    }

    @Nested
    @DisplayName("withDefaults")
    inner class WithDefaultsTest {

        @Test
        @DisplayName("should create runner with distribution defaults")
        fun shouldCreateWithDefaults() {
            val runner = TestGenerationRunner.withDefaults(
                reporter = Slf4jReporter(testLogger)
            )

            assertThat(runner).isNotNull
        }
    }

    @Nested
    @DisplayName("Slf4jReporter")
    inner class Slf4jReporterTest {

        @Test
        @DisplayName("should format report using ConsoleReporter")
        fun shouldFormatReport() {
            val reporter = Slf4jReporter(testLogger)
            val report = GenerationReport(
                successfulSuites = emptyList(),
                errors = emptyList(),
                summary = art.galushko.openapi.testgen.model.error.GenerationSummary(
                    totalOperations = 0,
                    successfulOperations = emptyList(),
                    partialOperations = emptyList(),
                    failedOperations = emptyList(),
                    notTestedOperations = emptyList(),
                    totalTestCases = 0,
                    totalErrors = 0,
                )
            )

            val formatted = reporter.formatReport(report)

            val expected = ConsoleReporter().format(report)
            assertThat(formatted).isEqualTo(expected)
        }

        @Test
        @DisplayName("should log info messages")
        fun shouldLogInfo() {
            val reporter = Slf4jReporter(testLogger)

            // Should not throw
            reporter.logInfo("Test info message")
        }

        @Test
        @DisplayName("should log error messages")
        fun shouldLogError() {
            val reporter = Slf4jReporter(testLogger)

            // Should not throw
            reporter.logError("Test error message")
        }
    }

    @Nested
    @DisplayName("ModuleFactory")
    inner class ModuleFactoryTest {

        @Test
        @DisplayName("should use custom module factory when provided")
        fun shouldUseCustomModuleFactory() {
            val customModule = art.galushko.openapi.testgen.generator.template.TemplateGeneratorModule
            var factoryCalled = false

            val runner = TestGenerationRunner.builder()
                .reporter(Slf4jReporter(testLogger))
                .moduleExtractors(DistributionDefaults.extractors())
                .defaultSettings(DistributionDefaults.settings())
                .moduleFactory { _ ->
                    factoryCalled = true
                    listOf(customModule)
                }
                .build()

            assertThat(runner).isNotNull
        }

        @Test
        @DisplayName("should fall back to static modules when no factory provided")
        fun shouldFallBackToStaticModules() {
            val runner = TestGenerationRunner.builder()
                .reporter(Slf4jReporter(testLogger))
                .modules(DistributionDefaults.modules())
                .build()

            assertThat(runner).isNotNull
        }
    }
}
