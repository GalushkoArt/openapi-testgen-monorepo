package art.galushko.openapi.testgen.openapi

import art.galushko.openapi.testgen.config.ParserSettings
import io.swagger.v3.parser.util.DeserializationUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path

@DisplayName("OpenApiSpecParser")
class OpenApiSpecParserTest {

    private val originalOptions = snapshotOptions(DeserializationUtils.getOptions())
    private val specPath: String by lazy {
        Path.of(requireNotNull(this::class.java.getResource("/oas/openapi.yaml")).toURI()).toString()
    }

    @AfterEach
    fun restoreParserOptions() {
        restoreOptions(originalOptions)
    }

    @Test
    @DisplayName("should use swagger parser defaults for unspecified values inside the configured parse scope")
    fun shouldUseDefaultsForUnspecifiedValuesInsideConfiguredScope() {
        val defaults = DeserializationUtils.Options()
        val customOptions = optionsSnapshot(
            maxYamlDepth = 321,
            maxYamlReferences = 654L,
            validateYamlInput = false,
            yamlCycleCheck = false,
            maxYamlCodePoints = 9_999_999,
            maxYamlAliasesForCollections = 777,
            yamlAllowRecursiveKeys = true,
        )
        restoreOptions(customOptions)

        OpenApiSpecParser.withConfiguredParserOptions(ParserSettings(yamlCodePointLimit = 7_777_777)) {
            val options = DeserializationUtils.getOptions()

            assertThat(options.maxYamlCodePoints).isEqualTo(7_777_777)
            assertThat(options.maxYamlAliasesForCollections).isEqualTo(defaults.maxYamlAliasesForCollections)
            assertThat(options.isYamlAllowRecursiveKeys).isEqualTo(defaults.isYamlAllowRecursiveKeys)
            assertThat(options.maxYamlDepth).isEqualTo(defaults.maxYamlDepth)
        }

        assertThat(snapshotOptions(DeserializationUtils.getOptions())).isEqualTo(customOptions)
    }

    @Test
    @DisplayName("should restore parser options after successful parse")
    fun shouldRestoreParserOptionsAfterSuccessfulParse() {
        val customOptions = optionsSnapshot(
            maxYamlDepth = 444,
            maxYamlReferences = 888L,
            validateYamlInput = false,
            yamlCycleCheck = false,
            maxYamlCodePoints = 5_555_555,
            maxYamlAliasesForCollections = 99,
            yamlAllowRecursiveKeys = true,
        )
        restoreOptions(customOptions)

        OpenApiSpecParser.parseOpenApi(
            specPath,
            ParserSettings(
                yamlCodePointLimit = 7_777_777,
                yamlMaxAliasesForCollections = 123,
                yamlAllowRecursiveKeys = false,
                yamlNestingDepthLimit = 222,
            ),
        )

        assertThat(snapshotOptions(DeserializationUtils.getOptions())).isEqualTo(customOptions)
    }

    @Test
    @DisplayName("should restore parser options after failed parse")
    fun shouldRestoreParserOptionsAfterFailedParse() {
        val customOptions = optionsSnapshot(
            maxYamlDepth = 111,
            maxYamlReferences = 222L,
            validateYamlInput = false,
            yamlCycleCheck = false,
            maxYamlCodePoints = 3_333_333,
            maxYamlAliasesForCollections = 55,
            yamlAllowRecursiveKeys = true,
        )
        restoreOptions(customOptions)

        assertThatThrownBy {
            OpenApiSpecParser.parseOpenApi(
                inputSpec = "file:///definitely-missing-openapi.yaml",
                parserSettings = ParserSettings(
                    yamlCodePointLimit = 7_777_777,
                    yamlMaxAliasesForCollections = 123,
                    yamlAllowRecursiveKeys = false,
                    yamlNestingDepthLimit = 222,
                ),
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Parsed OpenAPI model is null")

        assertThat(snapshotOptions(DeserializationUtils.getOptions())).isEqualTo(customOptions)
    }

    private data class OptionsSnapshot(
        val maxYamlDepth: Int?,
        val maxYamlReferences: Long?,
        val validateYamlInput: Boolean,
        val yamlCycleCheck: Boolean,
        val maxYamlCodePoints: Int?,
        val maxYamlAliasesForCollections: Int?,
        val yamlAllowRecursiveKeys: Boolean,
    )

    @Suppress("LongParameterList")
    private fun optionsSnapshot(
        maxYamlDepth: Int?,
        maxYamlReferences: Long?,
        validateYamlInput: Boolean,
        yamlCycleCheck: Boolean,
        maxYamlCodePoints: Int?,
        maxYamlAliasesForCollections: Int?,
        yamlAllowRecursiveKeys: Boolean,
    ): OptionsSnapshot =
        OptionsSnapshot(
            maxYamlDepth = maxYamlDepth,
            maxYamlReferences = maxYamlReferences,
            validateYamlInput = validateYamlInput,
            yamlCycleCheck = yamlCycleCheck,
            maxYamlCodePoints = maxYamlCodePoints,
            maxYamlAliasesForCollections = maxYamlAliasesForCollections,
            yamlAllowRecursiveKeys = yamlAllowRecursiveKeys,
        )

    private fun snapshotOptions(options: DeserializationUtils.Options): OptionsSnapshot =
        OptionsSnapshot(
            maxYamlDepth = options.maxYamlDepth,
            maxYamlReferences = options.maxYamlReferences,
            validateYamlInput = options.isValidateYamlInput,
            yamlCycleCheck = options.isYamlCycleCheck,
            maxYamlCodePoints = options.maxYamlCodePoints,
            maxYamlAliasesForCollections = options.maxYamlAliasesForCollections,
            yamlAllowRecursiveKeys = options.isYamlAllowRecursiveKeys,
        )

    private fun restoreOptions(snapshot: OptionsSnapshot) {
        val options = DeserializationUtils.getOptions()
        options.maxYamlDepth = snapshot.maxYamlDepth
        options.maxYamlReferences = snapshot.maxYamlReferences
        options.isValidateYamlInput = snapshot.validateYamlInput
        options.isYamlCycleCheck = snapshot.yamlCycleCheck
        options.maxYamlCodePoints = snapshot.maxYamlCodePoints
        options.maxYamlAliasesForCollections = snapshot.maxYamlAliasesForCollections
        options.isYamlAllowRecursiveKeys = snapshot.yamlAllowRecursiveKeys
    }
}
