package art.galushko.openapi.testgen.example.generator

import art.galushko.openapi.testgen.example.openapi.SchemaMerger
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isArray
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isObject
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetResponseFromRef
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema

/**
 * Generates synthetic example values from OpenAPI schemas for use in tests.
 *
 * Provider order matters: the first provider that returns a non-null value wins.
 *
 * @property valueProviders ordered list of providers that attempt to generate values
 * @property schemaMerger merges composed schemas before generating values
 * @property options configuration options controlling generation behavior
 */
@Suppress("ReturnCount", "TooManyFunctions", "MagicNumber")
public class SchemaExampleValueGenerator(
    private val valueProviders: List<SchemaValueProvider>,
    private val schemaMerger: SchemaMerger = SchemaMerger(),
    private val options: SchemaExampleValueGeneratorOptions = SchemaExampleValueGeneratorOptions(),
) {

    /**
     * Retrieves the example value for a given parameter based on its schema.
     *
     * Behavior:
     * - When variationIndex is 0, schema-level `example` is returned when present.
     * - Otherwise, composed schemas are merged before value generation.
     *
     * @param name parameter name (used in error messages)
     * @param schema parameter schema (may be a $ref)
     * @param openAPI OpenAPI model used to resolve references
     * @param variationIndex index used to generate varied values
     * @return example value matching the schema
     * @throws IllegalStateException if no provider can generate a value
     */
    public fun getExampleValue(name: String, schema: Schema<*>, openAPI: OpenAPI, variationIndex: Int = 0): Any {
        return getExampleValueInternal(name, schema, openAPI, 0, mutableSetOf(), variationIndex)
    }

    private fun isDepthAllowed(depth: Int, schema: Schema<*>, visitedRefs: MutableSet<String>): Boolean =
        depth <= options.maxExampleDepth || schema.`$ref`?.let { !visitedRefs.contains(it) } ?: true

    @Suppress("CyclomaticComplexMethod", "LongParameterList")
    private fun getExampleValueInternal(
        name: String,
        schema: Schema<*>,
        openAPI: OpenAPI,
        depth: Int,
        visitedRefs: MutableSet<String>,
        variationIndex: Int = 0,
    ): Any {
        schema.`$ref`?.let { visitedRefs.add(it) }
        val dereferenced = tryGetSchemaFromRef(schema, openAPI)

        if (variationIndex == 0) {
            dereferenced.example?.let { return it }
        }

        val mergedSchema = schemaMerger.mergeWithSubSchemas(dereferenced, depth, visitedRefs) {
            tryGetSchemaFromRef(it, openAPI)
        }

        if (isDepthAllowed(depth + 1, schema, visitedRefs)) {
            if (isArray(mergedSchema)) {
                return getExampleArrayValuesInternal(name, mergedSchema, openAPI, depth + 1, visitedRefs)
            }
            if (isObject(mergedSchema)) {
                return getExampleObjectInternal(name, mergedSchema, openAPI, depth + 1, visitedRefs, variationIndex)
            }
        }

        return valueProviders.firstNotNullOfOrNull { it.provide(mergedSchema, variationIndex) }
            ?: throw IllegalStateException("Provide example for param $name")
    }

    /**
     * Produces an example array value that satisfies array constraints.
     *
     * @param name parameter name (used in error messages)
     * @param schema array schema
     * @param openAPI OpenAPI model used to resolve references
     * @return list of example items with size >= minItems
     * @throws IllegalStateException if item schema is missing
     */
    public fun getExampleArrayValues(name: String, schema: Schema<*>, openAPI: OpenAPI): List<Any> {
        return getExampleArrayValuesInternal(name, schema, openAPI, 0, mutableSetOf())
    }

    private fun getExampleArrayValuesInternal(
        name: String,
        schema: Schema<*>,
        openAPI: OpenAPI,
        depth: Int,
        visitedRefs: MutableSet<String>,
    ): List<Any> {
        val mergedSchema = schemaMerger.mergeWithSubSchemas(schema, depth, visitedRefs) {
            tryGetSchemaFromRef(it, openAPI)
        }
        val items = mergedSchema.items ?: throw IllegalStateException("Empty array item schema for param $name")
        val mergedItems = schemaMerger.mergeWithSubSchemas(items, depth, visitedRefs) {
            tryGetSchemaFromRef(it, openAPI)
        }
        return getExampleArrayValuesByItem(name, mergedSchema, mergedItems, openAPI, depth, visitedRefs)
    }

    @Suppress("LongParameterList")
    public fun getExampleArrayValuesByItem(
        name: String,
        arraySchema: Schema<*>,
        itemSchema: Schema<*>,
        openAPI: OpenAPI,
        depth: Int = 0,
        visitedRefs: MutableSet<String> = mutableSetOf(),
    ): List<Any> {
        val result = ArrayList<Any>()
        if (!isDepthAllowed(depth + 1, itemSchema, visitedRefs)) {
            return emptyList()
        }
        val minimumSize = arraySchema.minItems ?: 0
        val requiresUniqueItems = arraySchema.uniqueItems == true
        var variationIndex = 0
        while (result.size < minimumSize) {
            val item = getExampleValueInternal(
                name,
                itemSchema,
                openAPI,
                depth + 1,
                visitedRefs.toMutableSet(),
                if (requiresUniqueItems) variationIndex else 0,
            )
            result.add(item)
            variationIndex++
        }
        return result
    }

    /**
     * Generates a valid object based on the provided schema.
     *
     * Required properties are populated; optional properties are omitted.
     *
     * @param name object name (used in error messages)
     * @param schema object schema (may be a $ref or composed)
     * @param openAPI OpenAPI model used to resolve references
     * @return map containing required properties populated with example values
     * @throws IllegalStateException if schema is invalid for an object
     */
    public fun getExampleObject(name: String, schema: Schema<*>, openAPI: OpenAPI): Map<String, Any> {
        return getExampleObjectInternal(name, schema, openAPI, 0, mutableSetOf())
    }

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "LongParameterList")
    private fun getExampleObjectInternal(
        name: String,
        schema: Schema<*>,
        openAPI: OpenAPI,
        depth: Int,
        visitedRefs: MutableSet<String>,
        variationIndex: Int = 0,
    ): Map<String, Any> {
        if (depth > options.maxExampleDepth || schema.`$ref`?.let { !visitedRefs.add(it) } ?: false) {
            return emptyMap()
        }

        val dereferenced = tryGetSchemaFromRef(schema, openAPI)

        val mergedSchema = schemaMerger.mergeWithSubSchemas(dereferenced, depth, visitedRefs) {
            tryGetSchemaFromRef(it, openAPI)
        }
        val result = LinkedHashMap<String, Any>()
        val required = mergedSchema.required ?: return result
        val properties = mergedSchema.properties ?: throw IllegalStateException("No properties in object schema $name")
        for (propertyName in required.sorted()) {
            val property = properties[propertyName] ?: throw IllegalStateException("Required property schema not found $name")
            if (isDepthAllowed(depth + 1, property, visitedRefs)) {
                result[propertyName] = getExampleValueInternal(
                    name = propertyName,
                    schema = property,
                    openAPI = openAPI,
                    depth = depth + 1,
                    visitedRefs = visitedRefs,
                    variationIndex = variationIndex,
                )
            }
        }
        return result
    }

    /**
     * Extracts expected response example from an operation for the given status code.
     *
     * Only `application/json` content is considered. The method returns the first available
     * response example when present.
     *
     * @param operation the OpenAPI operation
     * @param openAPI the OpenAPI specification
     * @param statusCode the HTTP status code to look for
     * @return the example value if found, null otherwise
     */
    public fun extractExpectedResponseExample(operation: Operation, openAPI: OpenAPI, statusCode: Int): Any? {
        val resp = tryGetResponseFromRef(operation, openAPI, statusCode) ?: return null
        val content = resp.content ?: return null

        // Prefer JSON examples only
        val mediaType = content["application/json"] ?: return null

        mediaType.example?.let { return it }
        val examples = mediaType.examples
        if (examples != null && examples.isNotEmpty()) {
            val first = examples.values.firstOrNull { ex -> ex.value != null }
            if (first?.value != null) return first.value
        }
        return null
    }
}
