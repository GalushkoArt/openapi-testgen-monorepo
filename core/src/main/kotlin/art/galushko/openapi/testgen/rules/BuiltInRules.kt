package art.galushko.openapi.testgen.rules

import art.galushko.openapi.testgen.rules.auth.AllSecurityMissedAuthValidationRule
import art.galushko.openapi.testgen.rules.auth.IncorrectScopesAuthValidationRule
import art.galushko.openapi.testgen.rules.auth.InsufficientScopesAuthValidationRule
import art.galushko.openapi.testgen.rules.auth.InvalidSecurityValuesAuthValidationRule
import art.galushko.openapi.testgen.rules.auth.MissingSecurityValuesAuthValidationRule
import art.galushko.openapi.testgen.rules.schema.AboveMaxItemsArraySchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.BelowMinItemsArraySchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.DateSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.DateTimeSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.IntegerBreakingSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.InvalidEnumValueSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.InvalidTypeValidationRule
import art.galushko.openapi.testgen.rules.schema.MissedRequiredObjectPropertiesSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.MultipleOfBreakingSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.NonUniqueItemsArraySchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.NullForRequiredPropertySchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.OutOfMaximumBoundaryNumberSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.OutOfMaximumLengthStringSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.OutOfMinimumBoundaryNumberSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.OutOfMinimumLengthStringSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.UnexpectedAdditionalPropertySchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.WrongEmailFormatSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.WrongInt32FormatSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.WrongInt64FormatSchemaValidationRule
import art.galushko.openapi.testgen.rules.schema.WrongUuidFormatSchemaValidationRule
import art.galushko.openapi.testgen.spi.AuthValidationRule
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule

/**
 * Factory for creating built-in validation rules.
 *
 * Provides explicit lists of all built-in rules without reflection.
 * Ordering within this object is deterministic; [ManualRuleRegistry] performs the final class-name sort when wiring.
 */
public object BuiltInRules {

    /**
     * Creates all built-in date validation rules.
     *
     * Output: list of date-format validation rules with deterministic ordering by rule name.
     */
    public fun dateValidationRules(): List<SimpleSchemaValidationRule> = listOf(
        DateSchemaValidationRule("Five Digit Year Date") { it.fiveDigitYearDate() },
        DateSchemaValidationRule("Thirteen Month Date") { it.thirteenMonthDate() },
        DateSchemaValidationRule("Thirty Second Day Date") { it.thirtySecondDayDate() },
        DateSchemaValidationRule("Three Digit Year Date") { it.threeDigitYearDate() },
        DateSchemaValidationRule("Zero Day Date") { it.zeroDayDate() },
        DateSchemaValidationRule("Zero Month Date") { it.zeroMonthDate() },
    )

    /**
     * Creates all built-in datetime validation rules.
     *
     * Output: list of date-time format validation rules with deterministic ordering by rule name.
     */
    public fun dateTimeValidationRules(): List<SimpleSchemaValidationRule> = listOf(
        DateTimeSchemaValidationRule("Five Digit Year DateTime") { it.fiveDigitYearDateTime() },
        DateTimeSchemaValidationRule("Sixty Minutes DateTime") { it.sixtyMinutesDateTime() },
        DateTimeSchemaValidationRule("Sixty One Seconds DateTime") { it.sixtyOneSecondsDateTime() },
        DateTimeSchemaValidationRule("Thirteen Month DateTime") { it.thirteenMonthDateTime() },
        DateTimeSchemaValidationRule("Thirty Second Day DateTime") { it.thirtySecondDayDateTime() },
        DateTimeSchemaValidationRule("Three Digit Year DateTime") { it.threeDigitYearDateTime() },
        DateTimeSchemaValidationRule("Twenty Four Hour DateTime") { it.twentyFourHourDateTime() },
        DateTimeSchemaValidationRule("Zero Day DateTime") { it.zeroDayDateTime() },
        DateTimeSchemaValidationRule("Zero Month DateTime") { it.zeroMonthDateTime() },
    )

    /**
     * Creates all built-in simple schema validation rules.
     *
     * Output: list of built-in [SimpleSchemaValidationRule] instances sorted by rule name.
     * Determinism: stable ordering within this factory; [ManualRuleRegistry] applies class-name ordering for the final list.
     */
    public fun simpleSchemaValidationRules(): List<SimpleSchemaValidationRule> = (
        listOf(
            // Array validation
            AboveMaxItemsArraySchemaValidationRule(),
            BelowMinItemsArraySchemaValidationRule(),
            NonUniqueItemsArraySchemaValidationRule(),

            // Boundary validation
            MultipleOfBreakingSchemaValidationRule(),
            OutOfMaximumBoundaryNumberSchemaValidationRule(),
            OutOfMinimumBoundaryNumberSchemaValidationRule(),

            // Enum validation
            InvalidEnumValueSchemaValidationRule(),

            // Type validation
            IntegerBreakingSchemaValidationRule(),
            InvalidTypeValidationRule(),
            WrongInt32FormatSchemaValidationRule(),
            WrongInt64FormatSchemaValidationRule(),

            // String validation
            OutOfMaximumLengthStringSchemaValidationRule(),
            OutOfMinimumLengthStringSchemaValidationRule(),
            WrongEmailFormatSchemaValidationRule(),
            WrongUuidFormatSchemaValidationRule(),

            // Object validation
            MissedRequiredObjectPropertiesSchemaValidationRule(),
            NullForRequiredPropertySchemaValidationRule(),
            UnexpectedAdditionalPropertySchemaValidationRule(),
        ) +
            // Date validation (parameterized)
            dateValidationRules() +
            // DateTime validation (parameterized)
            dateTimeValidationRules()
        ).sortedBy { it.getRuleName() }

    /**
     * Creates all built-in auth validation rules.
     *
     * Output: list of built-in [AuthValidationRule] instances sorted by rule name.
     * Determinism: stable ordering within this factory; [ManualRuleRegistry] applies class-name ordering for the final list.
     */
    public fun authValidationRules(): List<AuthValidationRule> = listOf(
        AllSecurityMissedAuthValidationRule(),
        IncorrectScopesAuthValidationRule(),
        InsufficientScopesAuthValidationRule(),
        InvalidSecurityValuesAuthValidationRule(),
        MissingSecurityValuesAuthValidationRule(),
    ).sortedBy { it.getRuleName() }
}
