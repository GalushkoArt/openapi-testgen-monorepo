package art.galushko.openapi.testgen.example.config

/**
 * Exception thrown when configuration parsing fails.
 * Provides clear context about which field failed and why.
 *
 * @property field The name of the configuration field that failed
 * @property expected Description of what type/value was expected
 * @property actual Description of what was actually found
 */
public class ConfigurationException(
    public val field: String,
    public val expected: String,
    public val actual: String,
) : IllegalArgumentException(
    "Configuration error for '$field': expected $expected, got $actual"
)
