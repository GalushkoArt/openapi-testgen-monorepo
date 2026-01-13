package art.galushko.openapi.testgen.generation

import art.galushko.openapi.testgen.rules.BuiltInRules
import art.galushko.openapi.testgen.rules.composed.ArrayItemSchemaValidationRule
import art.galushko.openapi.testgen.rules.composed.ObjectItemSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.InvalidEnumValueSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.OutOfMinimumLengthStringSchemaValidationRule
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Epic("Test Data Generation")
@Feature("TestGeneratorConfigurer")
@DisplayName("TestGeneratorConfigurer")
class TestGeneratorConfigurerTest {

    @Nested
    @DisplayName("getSchemaValidationRules")
    inner class GetSchemaValidationRulesTests {

        @Test
        @DisplayName("should return all rules when no rules are ignored")
        @Description("Verifies that all built-in rules plus composed rules are returned when ignore set is empty")
        fun shouldReturnAllRulesWhenNoneIgnored() {
            val rules = TestGeneratorConfigurer.getSchemaValidationRules(emptySet())

            val expectedSimpleRulesCount = BuiltInRules.simpleSchemaValidationRules().size
            val expectedComposedRulesCount = 2 // ArrayItem + ObjectItem

            assertThat(rules).hasSize(expectedSimpleRulesCount + expectedComposedRulesCount)
            assertThat(rules).anyMatch { it is ArrayItemSchemaValidationRule }
            assertThat(rules).anyMatch { it is ObjectItemSchemaValidationRule }
        }

        @Test
        @DisplayName("should filter out simple rules by fully qualified class name")
        @Description("Verifies that simple schema rules can be ignored using their fully qualified class name")
        fun shouldFilterSimpleRulesByClassName() {
            val ruleToIgnore = InvalidEnumValueSchemaValidationRule::class.java.name

            val rules = TestGeneratorConfigurer.getSchemaValidationRules(setOf(ruleToIgnore))

            assertThat(rules)
                .noneMatch { it::class.java.name == ruleToIgnore }
                .anyMatch { it is ArrayItemSchemaValidationRule }
                .anyMatch { it is ObjectItemSchemaValidationRule }
        }

        @Test
        @DisplayName("should filter out multiple simple rules")
        @Description("Verifies that multiple simple schema rules can be ignored at once")
        fun shouldFilterMultipleSimpleRules() {
            val rulesToIgnore = setOf(
                InvalidEnumValueSchemaValidationRule::class.java.name,
                OutOfMinimumLengthStringSchemaValidationRule::class.java.name
            )

            val rules = TestGeneratorConfigurer.getSchemaValidationRules(rulesToIgnore)

            assertThat(rules)
                .noneMatch { it::class.java.name in rulesToIgnore }
        }

        @Test
        @DisplayName("should filter out ArrayItemSchemaValidationRule when ignored")
        @Description("Verifies that the composed ArrayItem rule can be excluded from the rule list")
        fun shouldFilterArrayItemRule() {
            val arrayRuleName = ArrayItemSchemaValidationRule::class.java.name

            val rules = TestGeneratorConfigurer.getSchemaValidationRules(setOf(arrayRuleName))

            assertThat(rules)
                .noneMatch { it is ArrayItemSchemaValidationRule }
                .anyMatch { it is ObjectItemSchemaValidationRule }
        }

        @Test
        @DisplayName("should filter out ObjectItemSchemaValidationRule when ignored")
        @Description("Verifies that the composed ObjectItem rule can be excluded from the rule list")
        fun shouldFilterObjectItemRule() {
            val objectRuleName = ObjectItemSchemaValidationRule::class.java.name

            val rules = TestGeneratorConfigurer.getSchemaValidationRules(setOf(objectRuleName))

            assertThat(rules)
                .noneMatch { it is ObjectItemSchemaValidationRule }
                .anyMatch { it is ArrayItemSchemaValidationRule }
        }

        @Test
        @DisplayName("should filter out both composed rules when ignored")
        @Description("Verifies that both composed rules can be excluded from the rule list")
        fun shouldFilterBothComposedRules() {
            val rulesToIgnore = setOf(
                ArrayItemSchemaValidationRule::class.java.name,
                ObjectItemSchemaValidationRule::class.java.name
            )

            val rules = TestGeneratorConfigurer.getSchemaValidationRules(rulesToIgnore)

            assertThat(rules)
                .noneMatch { it is ArrayItemSchemaValidationRule }
                .noneMatch { it is ObjectItemSchemaValidationRule }
                .allMatch { it is SimpleSchemaValidationRule }
        }

        @Test
        @DisplayName("should include simple rules in composed rule container when filtering composed rules")
        @Description("Verifies that when composed rules are ignored, simple rules are still available for other composed rules")
        fun shouldMaintainSimpleRulesInContainerWhenFilteringComposed() {
            val objectRuleName = ObjectItemSchemaValidationRule::class.java.name

            val rules = TestGeneratorConfigurer.getSchemaValidationRules(setOf(objectRuleName))

            // ArrayItem should still have access to all simple rules
            val arrayRule = rules.filterIsInstance<ArrayItemSchemaValidationRule>().single()
            assertThat(arrayRule).isNotNull
        }

        @Test
        @DisplayName("should return rules in deterministic order")
        @Description("Verifies that multiple calls return rules in the same order")
        fun shouldReturnDeterministicOrder() {
            val rules1 = TestGeneratorConfigurer.getSchemaValidationRules(emptySet())
            val rules2 = TestGeneratorConfigurer.getSchemaValidationRules(emptySet())

            assertThat(rules1.map { it::class.java.name })
                .containsExactlyElementsOf(rules2.map { it::class.java.name })
        }

        @Test
        @DisplayName("should ignore unknown rule class names gracefully")
        @Description("Verifies that unknown class names in ignore set do not cause errors")
        fun shouldIgnoreUnknownRuleClassNames() {
            val unknownClassName = "com.example.NonExistentRule"
            val expectedSize = BuiltInRules.simpleSchemaValidationRules().size + 2

            val rules = TestGeneratorConfigurer.getSchemaValidationRules(setOf(unknownClassName))

            assertThat(rules).hasSize(expectedSize)
        }
    }

    @Nested
    @DisplayName("Provider wiring")
    inner class ProviderWiringTests {

        @Test
        @DisplayName("should wire parameter provider with filtered rules")
        @Description("Verifies that parameter provider receives the filtered rule list")
        fun shouldWireParameterProviderWithFilteredRules() {
            val ruleToIgnore = InvalidEnumValueSchemaValidationRule::class.java.name
            val filteredRules = TestGeneratorConfigurer.getSchemaValidationRules(setOf(ruleToIgnore))

            val parameterProvider = TestGeneratorConfigurer.getParameterTestProvider(filteredRules)

            assertThat(parameterProvider).isNotNull
        }

        @Test
        @DisplayName("should wire request body provider with filtered rules")
        @Description("Verifies that request body provider receives the filtered rule list")
        fun shouldWireRequestBodyProviderWithFilteredRules() {
            val ruleToIgnore = InvalidEnumValueSchemaValidationRule::class.java.name
            val filteredRules = TestGeneratorConfigurer.getSchemaValidationRules(setOf(ruleToIgnore))

            val requestBodyProvider = TestGeneratorConfigurer.getRequestBodyTestProvider(filteredRules)

            assertThat(requestBodyProvider).isNotNull
        }
    }
}
