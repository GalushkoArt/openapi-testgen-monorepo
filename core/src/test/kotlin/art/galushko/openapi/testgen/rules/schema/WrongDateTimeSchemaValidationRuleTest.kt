package art.galushko.openapi.testgen.rules.schema

import art.galushko.openapi.testgen.generation.Conditions.ruleAppliedTo
import art.galushko.openapi.testgen.generation.createTestContext
import art.galushko.openapi.testgen.generation.step
import art.galushko.openapi.testgen.rules.BuiltInRules.dateTimeValidationRules
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

class WrongDateTimeSchemaValidationRuleTest : ValidationRuleTest() {
    private val dateTimeValidationRules = dateTimeValidationRules()
    private val dateTimeValidationRule = dateTimeValidationRules.first()

    @Test
    @DisplayName("Wrong DateTime Schema: apply() should return expected invalid example when applicable")
    @Description("""
        Verifies that the apply method returns a stream with invalid datetime values when the rule is applicable.

        RFC 3339 Compliance (DateTime Format):
        Valid datetime format: YYYY-MM-DDTHH:MM:SSZ (e.g., 2024-12-31T23:59:59Z)
        - Year: 4 digits (0000-9999)
        - Month: 01-12
        - Day: 01-31 (depending on month)
        - Hour: 00-23
        - Minute: 00-59
        - Second: 00-60 (60 for leap seconds, but commonly 00-59)

        Test violations:
        - Three-digit year: 917-07-21T17:32:28Z (year must be 4 digits)
        - Five-digit year: 10917-07-21T17:32:28Z (year must be 4 digits)
        - Zero month: 2017-00-21T17:32:28Z (month must be 01-12)
        - Thirteen month: 2017-13-21T17:32:28Z (month must be 01-12)
        - Zero day: 2017-07-00T17:32:28Z (day must be 01-31)
        - Thirty-second day: 2017-07-32T17:32:28Z (day must be 01-31)
        - Twenty-four hour: 2017-07-21T24:32:28Z (hour must be 00-23)
        - Sixty minutes: 2017-07-21T17:60:28Z (minute must be 00-59)
        - Sixty-one seconds: 2017-07-21T17:32:61Z (second must be 00-60)

        OpenAPI Spec: format: date-time maps to RFC 3339 date-time
        Reference: https://datatracker.ietf.org/doc/html/rfc3339#section-5.6
        OpenAPI: https://spec.openapis.org/oas/v3.0.3#data-types
    """)
    fun allSetTest() {
        val schema = StringSchema().format("date-time")

        val results = step("Call apply") { dateTimeValidationRules.flatMap { it.apply(schema, createTestContext()) }.toSet() }

        assertThat(results).isEqualTo(setOf(
            RuleValue("Five Digit Year DateTime", "10917-07-21T17:32:28Z"),
            RuleValue("Sixty Minutes DateTime", "2017-07-21T17:60:28Z"),
            RuleValue("Sixty One Seconds DateTime", "2017-07-21T17:32:61Z"),
            RuleValue("Thirteen Month DateTime", "2017-13-21T17:32:28Z"),
            RuleValue("Thirty Second Day DateTime", "2017-07-32T17:32:28Z"),
            RuleValue("Three Digit Year DateTime", "917-07-21T17:32:28Z"),
            RuleValue("Twenty Four Hour DateTime", "2017-07-21T24:32:28Z"),
            RuleValue("Zero Day DateTime", "2017-07-00T17:32:28Z"),
            RuleValue("Zero Month DateTime", "2017-00-21T17:32:28Z"),
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
            "Date String Schema",
            StringSchema().format("date"),
            emptySequence<Any>()
        ),
        Arguments.of(
            "String Schema without format",
            StringSchema(),
            emptySequence<Any>()
        ),
        Arguments.of(
            "Integer Schema with date-time format",
            IntegerSchema().format("date-time"),
            sequenceOf("10917-07-21T17:32:28Z")
        )
    )

    @ParameterizedTest
    @MethodSource("ruleAndSchemaProvider")
    @DisplayName("Wrong DateTime Schema: apply() should return optional with invalid example when applicable")
    @Description("Verify that the apply method returns a stream with an invalid example value only when the rule is applicable or empty Sequence")
    fun wrongDateTimeSchemaApplyTest(
        scenario: String,
        schema: Schema<*>,
        expected: Sequence<Any>
    ) {
        val result = step("Call apply") { dateTimeValidationRule.apply(schema, createTestContext()) }
        assertThat(result.toList()).`is`(ruleAppliedTo(dateTimeValidationRule, expected))
    }
}
