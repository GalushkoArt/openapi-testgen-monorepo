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
    public const val APPLICATION_XML: String = "application/xml"
    public const val APPLICATION_JSON: String = "application/json"
    public const val APPLICATION_XWWW_FORM_URLENCODED: String = "application/x-www-form-urlencoded"
    public const val BAD_REQUEST_CODE: Int = 400
    public const val UNAUTHORIZED_CODE: Int = 401
    public const val FORBIDDEN_CODE: Int = 403

    /**
     * Supported content types in request/response bodies, in selection order.
     */
    @JvmField
    public val supportedMediaTypes: List<String> = listOf(APPLICATION_JSON, APPLICATION_XML, APPLICATION_XWWW_FORM_URLENCODED)
}


