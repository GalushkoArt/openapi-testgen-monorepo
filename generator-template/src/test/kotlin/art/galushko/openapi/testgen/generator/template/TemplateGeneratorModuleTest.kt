package art.galushko.openapi.testgen.generator.template

import art.galushko.openapi.testgen.config.TestGenerationModule
import art.galushko.openapi.testgen.generator.GeneratorIds
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@Epic("Generator")
@Feature("TemplateGeneratorModule")
@DisplayName("TemplateGeneratorModule Tests")
class TemplateGeneratorModuleTest {

    @Test
    @Story("Module Metadata")
    @DisplayName("id should be 'template'")
    @Description("Module id is stable and used for deterministic ordering & duplicate detection")
    fun shouldExposeTemplateModuleId() {
        assertThat(TemplateGeneratorModule.id).isEqualTo("template")
    }

    @Test
    @Story("Module Metadata")
    @DisplayName("should implement TestGenerationModule")
    @Description("Confirms module is wired as a TestGenerationModule contribution point")
    fun shouldImplementTestGenerationModule() {
        assertThat(TemplateGeneratorModule).isInstanceOf(TestGenerationModule::class.java)
    }

    @Test
    @Story("Factory Contribution")
    @DisplayName("artifactGeneratorFactories should contain exactly TemplateArtifactGeneratorFactory")
    @Description("Module contributes exactly one template factory and nothing else")
    fun shouldContributeTemplateFactory() {
        val factories = TemplateGeneratorModule.artifactGeneratorFactories()

        assertThat(factories).containsExactly(TemplateArtifactGeneratorFactory)
    }

    @Test
    @Story("Factory Contribution")
    @DisplayName("contributed factory should be registered under GeneratorIds.TEMPLATE")
    @Description("Verifies the module-factory pair agrees on the 'template' generator id")
    fun shouldContributeFactoryUnderTemplateId() {
        val factories = TemplateGeneratorModule.artifactGeneratorFactories()

        assertThat(factories).hasSize(1)
        assertThat(factories[0].id).isEqualTo(GeneratorIds.TEMPLATE)
    }
}
