package art.galushko.openapi.testgen.testdata

import org.slf4j.LoggerFactory

/**
 * Immutable provider for generating default invalid values for tests.
 *
 * Inputs: optional overrides keyed by method name (e.g., "invalidEnumValue").
 * Output: deterministic invalid values for schema and auth rules.
 * Errors: unknown override keys are logged as warnings.
 *
 * @property overrides Runtime overrides for basic test data values. Unknown keys are logged as warnings.
 */
@Suppress("TooManyFunctions")
public class BasicTestDataProvider(
    overrides: Map<String, String> = emptyMap()
) {

    private val log = LoggerFactory.getLogger(BasicTestDataProvider::class.java)
    private val overrides: Map<String, String>

    init {
        val knownKeys = setOf(
            "invalidEnumValue",
            "invalidUuidValue",
            "invalidEmailValue",
            "invalidApiKey",
            "invalidAuthorizationHeader",
            "invalidSecurityScope",
            "nonIntegerValue",
            "outOfInt32RangeValue",
            "threeDigitYearDate",
            "fiveDigitYearDate",
            "zeroMonthDate",
            "thirteenMonthDate",
            "zeroDayDate",
            "thirtySecondDayDate",
            "threeDigitYearDateTime",
            "fiveDigitYearDateTime",
            "zeroMonthDateTime",
            "thirteenMonthDateTime",
            "zeroDayDateTime",
            "thirtySecondDayDateTime",
            "twentyFourHourDateTime",
            "sixtyMinutesDateTime",
            "sixtyOneSecondsDateTime"
        )

        overrides.keys.minus(knownKeys).forEach { unknown ->
            log.warn("No such BasicTestDataProvider method to override: {}", unknown)
        }
        this.overrides = overrides.toMap()
    }

    private fun valueFor(methodName: String, defaultValue: String): String =
        overrides[methodName] ?: defaultValue

    /**
     * Arbitrary string that is not present in any enum.
     * @return an invalid enum value
     */
    public fun invalidEnumValue(): String = valueFor("invalidEnumValue", "invalid_enum1")

    /**
     * Malformed UUID string.
     * @return a non-UUID string
     */
    public fun invalidUuidValue(): String = valueFor("invalidUuidValue", "8e258b27-c787-49ef-9539-11461b251ffg")

    /**
     * Malformed email address.
     * @return an invalid email string
     */
    public fun invalidEmailValue(): String = valueFor("invalidEmailValue", "invalid.email@example")

    /**
     * Placeholder invalid API key.
     * @return a non-working API key string
     */
    public fun invalidApiKey(): String = valueFor("invalidApiKey", "some_really_invalid_api_key")

    /**
     * Placeholder invalid authorization header.
     * @return a non-working authorization header string
     */
    public fun invalidAuthorizationHeader(): String = valueFor("invalidAuthorizationHeader", "bearer some_really_invalid_authorization_header")

    /**
     * Malformed security scope.
     * @return an invalid security scope string
     */
    public fun invalidSecurityScope(): String = valueFor("invalidSecurityScope", "some_invalid_scope")

    /**
     * Numeric string that is not an integer.
     * @return a non-integer numeric string
     */
    public fun nonIntegerValue(): String = valueFor("nonIntegerValue", "1.5")

    /**
     * Numeric string just above Int.MAX_VALUE.
     * @return a 32-bit out-of-range integer string
     */
    public fun outOfInt32RangeValue(): String = valueFor("outOfInt32RangeValue", "2147483648")

    /**
     * Date with a three-digit year.
     * @return an invalid date string
     */
    public fun threeDigitYearDate(): String = valueFor("threeDigitYearDate", "917-07-21")

    /**
     * Date with a five-digit year.
     * @return an invalid date string
     */
    public fun fiveDigitYearDate(): String = valueFor("fiveDigitYearDate", "10017-07-21")

    /**
     * Date with month 00.
     * @return an invalid date string
     */
    public fun zeroMonthDate(): String = valueFor("zeroMonthDate", "2017-00-21")

    /**
     * Date with month 13.
     * @return an invalid date string
     */
    public fun thirteenMonthDate(): String = valueFor("thirteenMonthDate", "2017-13-21")

    /**
     * Date with day 00.
     * @return an invalid date string
     */
    public fun zeroDayDate(): String = valueFor("zeroDayDate", "2017-07-00")

    /**
     * Date with day 32.
     * @return an invalid date string
     */
    public fun thirtySecondDayDate(): String = valueFor("thirtySecondDayDate", "2017-07-32")

    /**
     * Date-time with a three-digit year.
     * @return an invalid date-time string
     */
    public fun threeDigitYearDateTime(): String = valueFor("threeDigitYearDateTime", "917-07-21T17:32:28Z")

    /**
     * Date-time with a five-digit year.
     * @return an invalid date-time string
     */
    public fun fiveDigitYearDateTime(): String = valueFor("fiveDigitYearDateTime", "10917-07-21T17:32:28Z")

    /**
     * Date-time with month 00.
     * @return an invalid date-time string
     */
    public fun zeroMonthDateTime(): String = valueFor("zeroMonthDateTime", "2017-00-21T17:32:28Z")

    /**
     * Date-time with month 13.
     * @return an invalid date-time string
     */
    public fun thirteenMonthDateTime(): String = valueFor("thirteenMonthDateTime", "2017-13-21T17:32:28Z")

    /**
     * Date-time with day 00.
     * @return an invalid date-time string
     */
    public fun zeroDayDateTime(): String = valueFor("zeroDayDateTime", "2017-07-00T17:32:28Z")

    /**
     * Date-time with day 32.
     * @return an invalid date-time string
     */
    public fun thirtySecondDayDateTime(): String = valueFor("thirtySecondDayDateTime", "2017-07-32T17:32:28Z")

    /**
     * Date-time with hour 24.
     * @return an invalid date-time string
     */
    public fun twentyFourHourDateTime(): String = valueFor("twentyFourHourDateTime", "2017-07-21T24:32:28Z")

    /**
     * Date-time with minute 60.
     * @return an invalid date-time string
     */
    public fun sixtyMinutesDateTime(): String = valueFor("sixtyMinutesDateTime", "2017-07-21T17:60:28Z")

    /**
     * Date-time with second 61.
     * @return an invalid date-time string
     */
    public fun sixtyOneSecondsDateTime(): String = valueFor("sixtyOneSecondsDateTime", "2017-07-21T17:32:61Z")
}


