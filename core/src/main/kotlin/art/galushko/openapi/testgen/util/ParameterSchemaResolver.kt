package art.galushko.openapi.testgen.util

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.example.util.MediaTypePrioritizer.orderedMediaTypeKeys
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter

/**
 * Resolves a parameter schema using precedence rules:
 * `schema` first, then the first `content` media type with a schema.
 */
internal fun resolveParameterSchema(parameter: Parameter, openAPI: OpenAPI): ResolvedParameterSchema {
    val hasSchema = parameter.schema != null
    val hasContent = !parameter.content.isNullOrEmpty()
    if (hasSchema) {
        return ResolvedParameterSchema(
            schema = tryGetSchemaFromRef(checkNotNull(parameter.schema), openAPI),
            source = ParameterSchemaSource.SCHEMA,
            hasBothSchemaAndContent = hasContent,
        )
    }

    val contentSchema = findSchemaFromContent(parameter, openAPI)
    if (contentSchema != null) {
        return ResolvedParameterSchema(
            schema = contentSchema,
            source = ParameterSchemaSource.CONTENT,
            hasBothSchemaAndContent = false,
        )
    }

    return ResolvedParameterSchema(
        schema = null,
        source = ParameterSchemaSource.NONE,
        hasBothSchemaAndContent = false,
    )
}

internal enum class ParameterSchemaSource {
    SCHEMA,
    CONTENT,
    NONE,
}

internal data class ResolvedParameterSchema(
    val schema: Schema<*>?,
    val source: ParameterSchemaSource,
    val hasBothSchemaAndContent: Boolean,
)

private fun findSchemaFromContent(parameter: Parameter, openAPI: OpenAPI): Schema<*>? {
    val content = parameter.content ?: return null
    val mediaTypeName = orderedMediaTypeKeys(content)
        .firstOrNull { mediaType -> content[mediaType]?.schema != null } ?: return null
    val schema = content[mediaTypeName]?.schema ?: return null
    return tryGetSchemaFromRef(schema, openAPI)
}
