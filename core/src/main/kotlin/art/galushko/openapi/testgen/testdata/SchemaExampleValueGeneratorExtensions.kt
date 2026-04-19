package art.galushko.openapi.testgen.testdata

import art.galushko.openapi.testgen.example.response.ExtractedResponseExample
import art.galushko.openapi.testgen.example.response.ResponseExampleExtractor
import art.galushko.openapi.testgen.generation.TestGenerationContext
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema

/**
 * Extracts expected response example using TestGenerationContext.
 *
 * This extension provides a context-aware wrapper around the base
 * [ResponseExampleExtractor.extractExpectedResponseExample] method.
 */
public fun ResponseExampleExtractor.extractExpectedResponseExample(
    context: TestGenerationContext,
    statusCode: Int,
): Any? = extractExpectedResponseExample(context.operation, context.openAPI, statusCode)

/**
 * Extracts expected response example using TestGenerationContext and a named example.
 *
 * @param context test generation context containing operation details
 * @param statusCode HTTP status code to resolve
 * @param exampleName optional example name to select from named examples
 */
public fun ResponseExampleExtractor.extractExpectedResponseExample(
    context: TestGenerationContext,
    statusCode: Int,
    exampleName: String?,
): Any? = extractExpectedResponseExample(context.operation, context.openAPI, statusCode, exampleName)

/**
 * Extracts expected response example with selected media type using TestGenerationContext.
 *
 * @param context test generation context containing operation details
 * @param statusCode HTTP status code to resolve
 */
public fun ResponseExampleExtractor.extractExpectedResponseExampleWithMediaType(
    context: TestGenerationContext,
    statusCode: Int,
): ExtractedResponseExample = extractExpectedResponseExampleWithMediaType(context.operation, context.openAPI, statusCode)

/**
 * Extracts expected response example with selected media type using TestGenerationContext and a named example.
 *
 * @param context test generation context containing operation details
 * @param statusCode HTTP status code to resolve
 * @param exampleName optional example name to select from named examples
 */
public fun ResponseExampleExtractor.extractExpectedResponseExampleWithMediaType(
    context: TestGenerationContext,
    statusCode: Int,
    exampleName: String?,
): ExtractedResponseExample = extractExpectedResponseExampleWithMediaType(context.operation, context.openAPI, statusCode, exampleName)

/**
 * Attempts to generate an example value, throwing an exception if it fails.
 *
 * @param context test generation context containing operation details
 * @param schema original schema (may be a reference)
 * @param deref dereferenced schema
 * @param provide lambda that generates the example value
 * @return generated example value, or null if generation fails
 */
public fun <T : Any> tryGetExample(
    context: TestGenerationContext,
    schema: Schema<*>,
    deref: Schema<*>,
    provide: () -> T,
): T {
    return try {
        provide()
    } catch (e: IllegalStateException) {
        val message = buildExampleErrorMessage(context, schema, deref, e)
        throw IllegalStateException(message, e)
    }
}

/**
 * Builds a detailed error message, for example, generation failures.
 *
 * Includes operation context, schema information, and root cause to aid debugging.
 *
 * @param context test generation context with operation details
 * @param schema original schema (may be a reference)
 * @param deref dereferenced schema
 * @param cause underlying exception
 * @return formatted error message
 */
private fun buildExampleErrorMessage(
    context: TestGenerationContext,
    schema: Schema<*>,
    deref: Schema<*>,
    cause: Throwable,
): String {
    val operation = buildOperationInfoLocal(context.operation)
    val schemaInfo = buildSchemaInfoLocal(schema)
    val derefInfo = buildSchemaInfoLocal(deref)
    return "Failed to generate example value for $operation: schema=$schemaInfo, dereferenced=$derefInfo, cause=${cause.message}"
}

private fun buildOperationInfoLocal(operation: Operation): String {
    val id = operation.operationId ?: "unknown"
    return "operation $id"
}

private fun buildSchemaInfoLocal(schema: Schema<*>): String {
    val type = schema.type ?: "unknown"
    val ref = schema.`$ref` ?: ""
    return if (ref.isNotEmpty()) "type=$type, ref=$ref" else "type=$type"
}
