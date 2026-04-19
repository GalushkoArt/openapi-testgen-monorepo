package art.galushko.openapi.testgen.util

/**
 * Constants used across the generator.
 *
 * Media types are ordered to define deterministic selection when multiple content types exist.
 */
public object Consts {
    public const val UUID_FORMAT: String = "uuid"
    public const val DATE_FORMAT: String = "date"
    public const val DATE_TIME_FORMAT: String = "date-time"
    public const val EMAIL_FORMAT: String = "email"
    public const val INT32_FORMAT: String = "int32"
    public const val INT64_FORMAT: String = "int64"
    public const val BAD_REQUEST_CODE: Int = 400
    public const val UNAUTHORIZED_CODE: Int = 401
    public const val FORBIDDEN_CODE: Int = 403
}


