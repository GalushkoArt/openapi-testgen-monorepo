package art.galushko.openapi.testgen.rules.schema

import art.galushko.openapi.testgen.generation.Conditions.ruleAppliedTo
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.rules.BuiltInRules.dateValidationRules
import art.galushko.openapi.testgen.rules.ValidationRuleTest
import art.galushko.openapi.testgen.spi.RuleValue
import io.qameta.allure.Description
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class WrongDateSchemaValidationRuleTests : ValidationRuleTest() {
    private val dateSchemaValidationRules = dateValidationRules()
    private val dateSchemaValidationRule = dateSchemaValidationRules.first()

    @Test
    @DisplayName("Wrong Date Schema: apply() should return expected invalid example when applicable")
    @Description("""
        Verifies that the apply method returns a stream with invalid date values when the rule is applicable.

        RFC 3339 Compliance (Date Format):
        Valid date format: YYYY-MM-DD (e.g., 2024-12-31)
        - Year: 4 digits (0000-9999)
        - Month: 01-12
        - Day: 01-31 (depending on month)

        Test violations:
        - Three-digit year: 917-07-21 (year must be 4 digits)
        - Five-digit year: 10017-07-21 (year must be 4 digits)
        - Zero month: 2017-00-21 (month must be 01-12)
        - Thirteen month: 2017-13-21 (month must be 01-12)
        - Zero day: 2017-07-00 (day must be 01-31)
        - Thirty-second day: 2017-07-32 (day must be 01-31)

        OpenAPI Spec: format: date maps to RFC 3339 full-date
        Reference: https://datatracker.ietf.org/doc/html/rfc3339#section-5.6
        OpenAPI: https://spec.openapis.org/oas/v3.0.3#data-types
    """)
    fun allSetTest() {
        val schema = StringSchema().format("date")

        val results = step("Call apply") { dateSchemaValidationRules.flatMap { it.apply(schema, createTestContext()) }.toSet() }

        assertThat(results).isEqualTo(setOf(
            RuleValue("Five Digit Year Date", "10017-07-21"),
            RuleValue("Thirteen Month Date", "2017-13-21"),
            RuleValue("Thirty Second Day Date", "2017-07-32"),
            RuleValue("Three Digit Year Date", "917-07-21"),
            RuleValue("Zero Day Date", "2017-07-00"),
            RuleValue("Zero Month Date", "2017-00-21"),
        ))
    }

    @Suppress("LongMethod")
    fun ruleAndSchemaProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "UUID String Schema",
            StringSchema().format("uuid"),
            emptySequence<Any>()
        ),
        Arguments.of(
            "DateTime String Schema",
            StringSchema().format("date-time"),
            emptySequence<Any>()
        ),
        Arguments.of(
            "String Schema without format",
            StringSchema(),
            emptySequence<Any>()
        ),
        Arguments.of(
            "Integer Schema with date format",
            IntegerSchema().format("date"),
            sequenceOf("10017-07-21")
        )
    )

    @ParameterizedTest
    @MethodSource("ruleAndSchemaProvider")
    @DisplayName("Wrong Date Schema: apply() should return optional with invalid example when applicable")
    @Description("Verify that the apply method returns a stream with an invalid example value only when the rule is applicable or empty Sequence")
    fun wrongDateSchemaApplyTest(
        scenario: String,
        schema: Schema<*>,
        expected: Sequence<Any>
    ) {
        val result = step("Call apply") { dateSchemaValidationRule.apply(schema, createTestContext()) }
        assertThat(result.toList()).`is`(ruleAppliedTo(dateSchemaValidationRule, expected))
    }
}
