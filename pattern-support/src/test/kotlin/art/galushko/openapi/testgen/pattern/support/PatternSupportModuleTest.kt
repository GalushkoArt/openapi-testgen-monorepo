package art.galushko.openapi.testgen.pattern.support

import art.galushko.openapi.testgen.config.TestGeneratorExecutionOptions
import art.galushko.openapi.testgen.config.TestGenerationSettings
import art.galushko.openapi.testgen.pattern.value.PatternGenerationOptions
import art.galushko.openapi.testgen.pattern.value.PatternValueProvider
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path

class PatternSupportModuleTest {

    @Test
    @DisplayName("should expose stable module id and contribute provider + rule")
    fun shouldContributeProviderAndRule() {
        val module = PatternSupportModule(
            PatternGenerationOptions(spaceChars = " \t"),
        )
        assertThat(module.id).isEqualTo("pattern-support")

        val options = TestGeneratorExecutionOptions(
            specFile = "noop",
            outputDir = Path.of("build/tmp/pattern-module-test"),
            generatorId = "noop",
            generatorOptions = emptyMap(),
            testGenerationSettings = TestGenerationSettings(),
            alwaysWriteTests = false,
        )

        assertThat(module.artifactGeneratorFactories()).isEmpty()
        assertThat(module.extraAuthRules(options)).isEmpty()

        val providers = module.schemaValueProviders(options)
        assertThat(providers)
            .containsKey("pattern")
        assertThat(providers["pattern"])
            .isInstanceOf(PatternValueProvider::class.java)

        val provider = providers.getValue("pattern")
        val schema = StringSchema().pattern("^[a-z]+\\s+[a-z]+$")
        val provided = provider.provide(schema, variationIndex = 0)

        assertThat(provided).isInstanceOf(String::class.java)
        assertThat(Regex(schema.pattern).matches(provided as String)).isTrue()
        assertThat(provided.filter { it.isWhitespace() }.all { it == ' ' || it == '\t' }).isTrue()

        val rules = module.extraSimpleSchemaRules(options)
        assertThat(rules)
            .hasSize(1)
        assertThat(rules.first())
            .isInstanceOf(InvalidPatternSchemaValidationRule::class.java)
    }
}


