package art.galushko.openapi.testgen.generator.template

import java.util.function.Function

// Generic template context data classes
internal data class GenericClassTemplateContext(
    val className: String,
    val operationName: String,
    val operationPath: String,
    val methods: List<GenericMethodTemplateContext>,
    val customVariables: Map<String, Any?>,
    val fileHeaderComment: String?,
) {
    fun escapeString(): Function<Object, Object> = Function(escapeString)
}

internal data class GenericMethodTemplateContext(
    val methodName: String,
    val testCaseName: String,
    val description: String,
    val httpMethod: String,
    val path: String,
    val expectedStatusCode: Int,
    val headers: List<GenericParamContext>,
    val pathParams: List<GenericParamContext>,
    val queryParams: List<GenericParamContext>,
    val cookies: List<GenericParamContext>,
    val requestBody: GenericBodyContext?,
    val expectedResponseBody: GenericBodyContext?,
    val needToComplete: Boolean,
    val customVariables: Map<String, Any?>,
    val shouldHaveBody: Boolean = httpMethod.uppercase() in listOf("POST", "PUT", "PATCH"),
) {
    fun escapeString(): Function<Object, Object> = Function(escapeString)
}

internal data class GenericParamContext(
    val key: String,
    val value: String,
)

internal data class GenericBodyContext(
    val rawBody: String,
    val body: Any,
)
