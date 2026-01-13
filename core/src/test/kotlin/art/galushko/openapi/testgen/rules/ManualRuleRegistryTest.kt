package art.galushko.openapi.testgen.rules

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.spi.AuthValidationRule
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.media.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Epic("Test Data Generation")
@Feature("Rule Registry")
@DisplayName("ManualRuleRegistry")
class ManualRuleRegistryTest {

    @Nested
    @DisplayName("getRules for SimpleSchemaValidationRule")
    inner class SimpleSchemaValidationRuleTests {

        @Test
        @DisplayName("should return built-in rules sorted by class name")
        @Description("Verifies that built-in rules are returned in deterministic order sorted by fully qualified class name")
        fun shouldReturnBuiltInRulesSortedByClassName() {
            val registry = ManualRuleRegistry()

            val rules = registry.getRules(SimpleSchemaValidationRule::class.java, emptySet())

            assertThat(rules)
                .isNotEmpty
                .isSortedAccordingTo(Comparator.comparing { it::class.java.name })
        }

        @Test
        @DisplayName("should include extra rules merged with built-in rules")
        @Description("Verifies that extra rules provided to constructor are merged with built-in rules")
        fun shouldIncludeExtraRules() {
            val extraRule = TestSimpleSchemaValidationRule("Extra Rule")
            val registry = ManualRuleRegistry(extraSimpleSchemaRules = listOf(extraRule))

            val rules = registry.getRules(SimpleSchemaValidationRule::class.java, emptySet())

            assertThat(rules).contains(extraRule)
        }

        @Test
        @DisplayName("should filter out ignored rules by class name")
        @Description("Verifies that rules with class names in ignoredClassNames set are excluded from results")
        fun shouldFilterIgnoredRules() {
            val registry = ManualRuleRegistry()
            val allRules = registry.getRules(SimpleSchemaValidationRule::class.java, emptySet())
            val ruleToIgnore = allRules.first()
            val ignoredClassName = ruleToIgnore::class.java.name

            val filteredRules = registry.getRules(
                SimpleSchemaValidationRule::class.java,
                setOf(ignoredClassName)
            )

            assertThat(filteredRules)
                .hasSize(allRules.size - 1)
                .noneMatch { it::class.java.name == ignoredClassName }
        }

        @Test
        @DisplayName("should maintain sorted order after filtering ignored rules")
        @Description("Verifies that rules remain sorted by class name even after filtering")
        fun shouldMaintainSortedOrderAfterFiltering() {
            val registry = ManualRuleRegistry()
            val allRules = registry.getRules(SimpleSchemaValidationRule::class.java, emptySet())
            val ruleToIgnore = allRules[allRules.size / 2]

            val filteredRules = registry.getRules(
                SimpleSchemaValidationRule::class.java,
                setOf(ruleToIgnore::class.java.name)
            )

            assertThat(filteredRules)
                .isSortedAccordingTo(Comparator.comparing { it::class.java.name })
        }
    }

    @Nested
    @DisplayName("getRules for AuthValidationRule")
    inner class AuthValidationRuleTests {

        @Test
        @DisplayName("should return built-in auth rules sorted by class name")
        @Description("Verifies that built-in auth rules are returned in deterministic order")
        fun shouldReturnBuiltInAuthRulesSortedByClassName() {
            val registry = ManualRuleRegistry()

            val rules = registry.getRules(AuthValidationRule::class.java, emptySet())

            assertThat(rules)
                .isNotEmpty
                .isSortedAccordingTo(Comparator.comparing { it::class.java.name })
        }

        @Test
        @DisplayName("should include extra auth rules merged with built-in rules")
        @Description("Verifies that extra auth rules provided to constructor are merged with built-in rules")
        fun shouldIncludeExtraAuthRules() {
            val extraRule = TestAuthValidationRule("Extra Auth Rule")
            val registry = ManualRuleRegistry(extraAuthRules = listOf(extraRule))

            val rules = registry.getRules(AuthValidationRule::class.java, emptySet())

            assertThat(rules).contains(extraRule)
        }

        @Test
        @DisplayName("should filter out ignored auth rules by class name")
        @Description("Verifies that auth rules with class names in ignoredClassNames set are excluded")
        fun shouldFilterIgnoredAuthRules() {
            val registry = ManualRuleRegistry()
            val allRules = registry.getRules(AuthValidationRule::class.java, emptySet())
            val ruleToIgnore = allRules.first()
            val ignoredClassName = ruleToIgnore::class.java.name

            val filteredRules = registry.getRules(
                AuthValidationRule::class.java,
                setOf(ignoredClassName)
            )

            assertThat(filteredRules)
                .hasSize(allRules.size - 1)
                .noneMatch { it::class.java.name == ignoredClassName }
        }
    }

    @Nested
    @DisplayName("Unknown rule class handling")
    inner class UnknownRuleClassTests {

        @Test
        @DisplayName("should return empty list for unknown rule class")
        @Description("Verifies that requesting an unknown rule class returns an empty list")
        fun shouldReturnEmptyListForUnknownClass() {
            val registry = ManualRuleRegistry()

            val rules = registry.getRules(UnknownRuleType::class.java, emptySet())

            assertThat(rules).isEmpty()
        }
    }

    @Nested
    @DisplayName("Determinism guarantees")
    inner class DeterminismTests {

        @Test
        @DisplayName("should return same order on multiple calls")
        @Description("Verifies that multiple calls to getRules return rules in the same order")
        fun shouldReturnSameOrderOnMultipleCalls() {
            val registry = ManualRuleRegistry()

            val firstCall = registry.getRules(SimpleSchemaValidationRule::class.java, emptySet())
            val secondCall = registry.getRules(SimpleSchemaValidationRule::class.java, emptySet())

            assertThat(firstCall.map { it::class.java.name })
                .containsExactlyElementsOf(secondCall.map { it::class.java.name })
        }

        @Test
        @DisplayName("should return same order from different registry instances")
        @Description("Verifies that different ManualRuleRegistry instances return rules in the same order")
        fun shouldReturnSameOrderFromDifferentInstances() {
            val registry1 = ManualRuleRegistry()
            val registry2 = ManualRuleRegistry()

            val fromRegistry1 = registry1.getRules(SimpleSchemaValidationRule::class.java, emptySet())
            val fromRegistry2 = registry2.getRules(SimpleSchemaValidationRule::class.java, emptySet())

            assertThat(fromRegistry1.map { it::class.java.name })
                .containsExactlyElementsOf(fromRegistry2.map { it::class.java.name })
        }
    }

    // Test helper classes
    private class TestSimpleSchemaValidationRule(private val name: String) : SimpleSchemaValidationRule {
        override fun getRuleName(): String = name
        override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> = emptySequence()
    }

    private class TestAuthValidationRule(private val name: String) : AuthValidationRule {
        override fun getRuleName(): String = name
        override fun decide(context: TestGenerationContext): Boolean = false
        override fun apply(context: TestGenerationContext): Sequence<TestCase> = emptySequence()
    }

    private interface UnknownRuleType
}
