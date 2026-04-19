package art.galushko.openapi.testgen.example.generator

import art.galushko.openapi.testgen.example.generator.internal.ExampleGenerationContext
import art.galushko.openapi.testgen.example.openapi.SchemaMerger
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isArray
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isObject
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import io.swagger.v3.oas.models.OpenAPI
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
        val ctx = ExampleGenerationContext(name, openAPI, options, variationIndex = variationIndex)
        return generateExampleValue(schema, ctx)
    }

    /**
     * Retrieves the example value with custom options for per-call customization.
     *
     * This method allows overriding the default options for specific use cases,
     * such as generating response examples with different behavior than request examples.
     *
     * @param name parameter name (used in error messages)
     * @param schema parameter schema (may be a $ref)
     * @param openAPI OpenAPI model used to resolve references
     * @param options custom options for this specific call
     * @param variationIndex index used to generate varied values
     * @return example value matching the schema
     * @throws IllegalStateException if no provider can generate a value
     */
    public fun getExampleValueWithOptions(
        name: String,
        schema: Schema<*>,
        openAPI: OpenAPI,
        options: SchemaExampleValueGeneratorOptions,
        variationIndex: Int = 0,
    ): Any {
        val ctx = ExampleGenerationContext(name, openAPI, options, variationIndex = variationIndex)
        return generateExampleValue(schema, ctx)
    }

    internal fun responseOptions(): SchemaExampleValueGeneratorOptions {
        return options.copy(
            includeOptionalExampleProperties = true,
            includeWriteOnly = false,
            useSchemaExampleFallback = true,
        )
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
        val ctx = ExampleGenerationContext(name, openAPI, options)
        return generateArrayValues(schema, ctx)
    }

    /**
     * Produces an example array value from explicit array and item schemas.
     *
     * @param name parameter name (used in error messages)
     * @param arraySchema array schema containing constraints (minItems, uniqueItems)
     * @param itemSchema schema for array items
     * @param openAPI OpenAPI model used to resolve references
     * @param depth current traversal depth
     * @param visitedRefs set of visited $ref paths for circular reference detection
     * @return list of example items
     */
    public fun getExampleArrayValuesByItem(
        name: String,
        arraySchema: Schema<*>,
        itemSchema: Schema<*>,
        openAPI: OpenAPI,
        depth: Int = 0,
        visitedRefs: MutableSet<String> = mutableSetOf(),
    ): List<Any> {
        val ctx = ExampleGenerationContext(
            name = name,
            openAPI = openAPI,
            options = options,
            depth = depth,
            visitedRefs = visitedRefs,
        )
        return generateArrayValuesByItem(arraySchema, itemSchema, ctx)
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
        val ctx = ExampleGenerationContext(name, openAPI, options)
        return generateObject(schema, ctx)
    }

    @Suppress("ReturnCount")
    private fun generateExampleValue(schema: Schema<*>, ctx: ExampleGenerationContext): Any {
        ctx.registerVisited(schema.`$ref`)
        val dereferenced = tryGetSchemaFromRef(schema, ctx.openAPI)

        if (ctx.variationIndex == 0) {
            dereferenced.example?.let { return it }
            if (ctx.options.useSchemaExampleFallback) {
                dereferenced.examples?.firstOrNull()?.let { return it }
                dereferenced.default?.let { return it }
            }
        }

        val mergedSchema = schemaMerger.mergeWithSubSchemas(dereferenced, ctx.depth, ctx.visitedRefs) {
            tryGetSchemaFromRef(it, ctx.openAPI)
        }

        if (isArray(mergedSchema)) {
            if (!ctx.isDepthAllowed()) return emptyList<Any>()
            return generateArrayValues(mergedSchema, ctx)
        }
        if (isObject(mergedSchema)) {
            if (!ctx.isDepthAllowed()) return emptyMap<String, Any>()
            return generateObject(mergedSchema, ctx)
        }

        return valueProviders.firstNotNullOfOrNull { it.provide(mergedSchema, ctx.variationIndex) }
            ?: throw IllegalStateException("Provide example for param ${ctx.name}")
    }

    private fun generateArrayValues(schema: Schema<*>, ctx: ExampleGenerationContext): List<Any> {
        val mergedSchema = schemaMerger.mergeWithSubSchemas(schema, ctx.depth, ctx.visitedRefs) {
            tryGetSchemaFromRef(it, ctx.openAPI)
        }
        val items = mergedSchema.items
            ?: throw IllegalStateException("Empty array item schema for param ${ctx.name}")
        val mergedItems = schemaMerger.mergeWithSubSchemas(items, ctx.depth, ctx.visitedRefs) {
            tryGetSchemaFromRef(it, ctx.openAPI)
        }
        return generateArrayValuesByItem(mergedSchema, mergedItems, ctx)
    }

    private fun generateArrayValuesByItem(
        arraySchema: Schema<*>,
        itemSchema: Schema<*>,
        ctx: ExampleGenerationContext,
    ): List<Any> {
        val descendedCtx = ctx.descend()
        if (!descendedCtx.isDepthAllowed()) {
            return emptyList()
        }

        val result = ArrayList<Any>()
        val minimumSize = arraySchema.minItems ?: 0
        val requiresUniqueItems = arraySchema.uniqueItems == true
        var variationIndex = 0

        while (result.size < minimumSize) {
            val itemCtx = if (requiresUniqueItems) {
                descendedCtx.copyVisitedRefs().withVariation(variationIndex)
            } else {
                descendedCtx.copyVisitedRefs().withVariation(0)
            }
            val item = generateExampleValue(itemSchema, itemCtx)
            result.add(item)
            variationIndex++
        }
        return result
    }

    private fun generateObject(schema: Schema<*>, ctx: ExampleGenerationContext): Map<String, Any> {
        if (!isDepthAllowedForObject(schema, ctx)) {
            return emptyMap()
        }

        val dereferenced = tryGetSchemaFromRef(schema, ctx.openAPI)
        val mergedSchema = schemaMerger.mergeWithSubSchemas(dereferenced, ctx.depth, ctx.visitedRefs) {
            tryGetSchemaFromRef(it, ctx.openAPI)
        }

        val result = LinkedHashMap<String, Any>()
        val required = mergedSchema.required?.toSet() ?: emptySet()
        val properties = mergedSchema.properties

        if (properties == null) {
            if (required.isNotEmpty()) {
                throw IllegalStateException("No properties in object schema ${ctx.name}")
            }
            return result
        }

        val propertyNames = collectPropertyNames(required, properties, ctx)
        for (propertyName in propertyNames.sorted()) {
            val property = properties[propertyName]
                ?: throw IllegalStateException("Required $propertyName property schema not found ${ctx.name}")

            if (!ctx.options.includeWriteOnly && property.writeOnly == true) {
                continue
            }

            val descendedCtx = ctx.copyVisitedRefs().descend(propertyName)
            if (descendedCtx.isDepthAllowed()) {
                result[propertyName] = generateExampleValue(property, descendedCtx)
            }
        }
        return result
    }

    private fun isDepthAllowedForObject(schema: Schema<*>, ctx: ExampleGenerationContext): Boolean {
        val ref = schema.`$ref`
        if (ctx.depth > ctx.options.maxExampleDepth) {
            return false
        }
        if (ref != null && !ctx.visitedRefs.add(ref)) {
            return false
        }
        return true
    }

    private fun collectPropertyNames(
        required: Set<String>,
        properties: Map<String, Schema<*>>,
        ctx: ExampleGenerationContext,
    ): Set<String> {
        val propertyNames = LinkedHashSet<String>()
        propertyNames.addAll(required)

        if (ctx.options.includeOptionalExampleProperties) {
            properties.filterKeys { it !in required }
                .filter { (_, propSchema) ->
                    hasExplicitExample(propSchema, ctx.openAPI, ctx.options.useSchemaExampleFallback)
                }
                .keys
                .forEach { propertyNames.add(it) }
        }
        return propertyNames
    }

    private fun hasExplicitExample(schema: Schema<*>, openAPI: OpenAPI, includeDefault: Boolean): Boolean {
        val deref = tryGetSchemaFromRef(schema, openAPI)
        if (deref.example != null) return true
        if (deref.examples?.isNotEmpty() == true) return true
        if (includeDefault && deref.default != null) return true
        return false
    }
}
