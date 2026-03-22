package art.galushko.openapi.testgen.plugin

import art.galushko.openapi.testgen.config.ParserSettings
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.internal.project.DefaultProject
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ParserSettingsExtension")
class ParserSettingsExtensionTest {

    private lateinit var project: DefaultProject
    private lateinit var extension: ParserSettingsExtension

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build() as DefaultProject
        extension = project.objects.newInstance(ParserSettingsExtension::class.java)
    }

    @Test
    @DisplayName("Extension should have null defaults and empty map output")
    fun testDefaultValues() {
        assertThat(extension.yamlCodePointLimit.orNull).isNull()
        assertThat(extension.yamlMaxAliasesForCollections.orNull).isNull()
        assertThat(extension.yamlAllowRecursiveKeys.orNull).isNull()
        assertThat(extension.yamlNestingDepthLimit.orNull).isNull()
        assertThat(extension.buildParserSettingsMap()).isEmpty()
    }

    @Test
    @DisplayName("buildParserSettingsMap should be compatible with ParserSettings.fromMap")
    fun testRoundTripCompatibility() {
        extension.yamlCodePointLimit.set(10_000_000)
        extension.yamlMaxAliasesForCollections.set(100)
        extension.yamlAllowRecursiveKeys.set(true)
        extension.yamlNestingDepthLimit.set(75)

        val map = extension.buildParserSettingsMap()
        val parsed = ParserSettings.fromMap(map)

        assertThat(parsed).isEqualTo(
            ParserSettings(
                yamlCodePointLimit = 10_000_000,
                yamlMaxAliasesForCollections = 100,
                yamlAllowRecursiveKeys = true,
                yamlNestingDepthLimit = 75,
            ),
        )
    }

    @Test
    @DisplayName("buildParserSettingsMap should only contain explicitly set values")
    fun testOnlyExplicitValuesIncluded() {
        extension.yamlCodePointLimit.set(7_000_000)

        assertThat(extension.buildParserSettingsMap())
            .containsExactlyEntriesOf(
                mapOf(
                    "yamlCodePointLimit" to 7_000_000,
                ),
            )
    }
}
