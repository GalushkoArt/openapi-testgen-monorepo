package art.galushko.openapi.testgen.pattern.support

import art.galushko.openapi.testgen.example.config.ConfigurationException
import art.galushko.openapi.testgen.pattern.value.PatternGenerationOptions
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@Epic("Pattern Support")
@Feature("Settings Extractor")
@DisplayName("PatternModuleSettingsExtractor")
class PatternModuleSettingsExtractorTest {

    @Test
    @DisplayName("settingsKey should return 'patternGeneration'")
    fun settingsKeyShouldReturnPatternGeneration() {
        assertThat(PatternModuleSettingsExtractor.settingsKey).isEqualTo("patternGeneration")
        assertThat(PatternModuleSettingsExtractor.SETTINGS_KEY).isEqualTo("patternGeneration")
    }

    @Test
    @DisplayName("parse should return default PatternGenerationOptions when null")
    fun parseShouldReturnDefaultWhenNull() {
        val result = PatternModuleSettingsExtractor.parse(null)

        assertThat(result).isEqualTo(PatternGenerationOptions())
    }

    @Test
    @DisplayName("parse should parse Map to PatternGenerationOptions")
    fun parseShouldParseMapToOptions() {
        val input = mapOf(
            "defaultMinLength" to 5,
            "spaceChars" to "abc",
        )

        val result = PatternModuleSettingsExtractor.parse(input)

        assertThat(result).isEqualTo(
            PatternGenerationOptions(
                defaultMinLength = 5,
                spaceChars = "abc",
            )
        )
    }

    @Test
    @DisplayName("parse should throw ConfigurationException for non-Map input")
    fun parseShouldThrowForNonMapInput() {
        assertThatThrownBy { PatternModuleSettingsExtractor.parse("invalid") }
            .isInstanceOf(ConfigurationException::class.java)
            .hasMessageContaining("testGenerationSettings.patternGeneration")
            .hasMessageContaining("Map<String, Any?>")
            .hasMessageContaining("kotlin.String")
    }

    @Test
    @DisplayName("parse should throw ConfigurationException for Map with non-String keys")
    fun parseShouldThrowForMapWithNonStringKeys() {
        val input = mapOf<Any, Any?>(
            123 to "value",
        )

        assertThatThrownBy { PatternModuleSettingsExtractor.parse(input) }
            .isInstanceOf(ConfigurationException::class.java)
            .hasMessageContaining("string keys")
    }
}
