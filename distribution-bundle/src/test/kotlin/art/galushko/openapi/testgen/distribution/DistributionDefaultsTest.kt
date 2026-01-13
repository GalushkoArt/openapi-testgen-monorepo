package art.galushko.openapi.testgen.distribution

import art.galushko.openapi.testgen.generator.template.TemplateGeneratorModule
import art.galushko.openapi.testgen.pattern.support.PatternModuleSettingsExtractor
import art.galushko.openapi.testgen.pattern.support.PatternSupportModule
import art.galushko.openapi.testgen.pattern.value.PatternGenerationOptions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DistributionDefaults")
class DistributionDefaultsTest {

    @Nested
    @DisplayName("modules")
    inner class ModulesTest {

        @Test
        @DisplayName("should return TemplateGeneratorModule and PatternSupportModule with default options")
        fun shouldReturnDefaultModules() {
            val modules = DistributionDefaults.modules()

            assertThat(modules).hasSize(2)
            assertThat(modules[0]).isSameAs(TemplateGeneratorModule)
            assertThat(modules[1]).isInstanceOf(PatternSupportModule::class.java)
        }

        @Test
        @DisplayName("should use provided pattern options when creating PatternSupportModule")
        fun shouldUseProvidedPatternOptions() {
            val customOptions = PatternGenerationOptions(defaultMinLength = 10)

            val modules = DistributionDefaults.modules(customOptions)

            assertThat(modules).hasSize(2)
            assertThat(modules[1]).isInstanceOf(PatternSupportModule::class.java)
        }
    }

    @Nested
    @DisplayName("extractors")
    inner class ExtractorsTest {

        @Test
        @DisplayName("should return PatternModuleSettingsExtractor")
        fun shouldReturnPatternModuleSettingsExtractor() {
            val extractors = DistributionDefaults.extractors()

            assertThat(extractors).hasSize(1)
            assertThat(extractors[0]).isSameAs(PatternModuleSettingsExtractor)
        }
    }

    @Nested
    @DisplayName("settings")
    inner class SettingsTest {

        @Test
        @DisplayName("should include pattern provider in provider list")
        fun shouldIncludePatternProvider() {
            val settings = DistributionDefaults.settings()

            assertThat(settings.exampleValues.providers).contains("pattern")
        }

        @Test
        @DisplayName("should insert pattern provider before plain-string")
        fun shouldInsertPatternBeforePlainString() {
            val settings = DistributionDefaults.settings()
            val providers = settings.exampleValues.providers

            val patternIndex = providers.indexOf("pattern")
            val plainStringIndex = providers.indexOf("plain-string")

            assertThat(patternIndex)
                .isGreaterThanOrEqualTo(0)
                .isLessThan(plainStringIndex)
        }

        @Test
        @DisplayName("should have non-empty example values settings")
        fun shouldHaveNonEmptyExampleValuesSettings() {
            val settings = DistributionDefaults.settings()

            assertThat(settings.exampleValues.providers).isNotEmpty()
        }
    }
}

@DisplayName("TestGenerationResult")
class TestGenerationResultTest {

    @Test
    @DisplayName("Success should contain report and testsWritten flag")
    fun successShouldContainReportAndFlag() {
        val report = art.galushko.openapi.testgen.model.error.GenerationReport(
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

        val result = TestGenerationResult.Success(report = report, testsWritten = true)

        assertThat(result.report).isSameAs(report)
        assertThat(result.testsWritten).isTrue()
    }

    @Test
    @DisplayName("Failure should contain report and message")
    fun failureShouldContainReportAndMessage() {
        val report = art.galushko.openapi.testgen.model.error.GenerationReport(
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

        val result = TestGenerationResult.Failure(report = report, message = "Test failure")

        assertThat(result.report).isSameAs(report)
        assertThat(result.message).isEqualTo("Test failure")
    }

    @Test
    @DisplayName("Success with testsWritten false should be valid")
    fun successWithTestsNotWrittenShouldBeValid() {
        val report = art.galushko.openapi.testgen.model.error.GenerationReport(
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

        val result = TestGenerationResult.Success(report = report, testsWritten = false)

        assertThat(result.testsWritten).isFalse()
    }
}
