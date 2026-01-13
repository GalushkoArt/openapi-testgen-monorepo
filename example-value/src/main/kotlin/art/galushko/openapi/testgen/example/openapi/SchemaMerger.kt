package art.galushko.openapi.testgen.example.openapi

import art.galushko.openapi.testgen.example.util.CombinationBudget
import art.galushko.openapi.testgen.example.util.cartesianProduct
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.JsonSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.parser.util.SchemaTypeUtil

/**
 * Options controlling how composed schemas are merged.
 *
 * @property maxMergedSchemaDepth maximum depth for composed schema merging (allOf/anyOf/oneOf)
 */
public data class SchemaMergerOptions(
    public val maxMergedSchemaDepth: Int = DEFAULT_MAX_MERGED_SCHEMA_DEPTH,
) {
    init {
        require(maxMergedSchemaDepth > 0) {
            "maxMergedSchemaDepth must be positive, was $maxMergedSchemaDepth"
        }
    }

    public companion object {
        public const val DEFAULT_MAX_MERGED_SCHEMA_DEPTH: Int = 50
    }
}

/**
 * Merges composed schemas (allOf/anyOf/oneOf) and produces flat schema combinations
 * for validation and example generation.
 *
 * This component does not resolve `$ref` automatically; callers provide a resolve function when needed.
 *
 * @param options configuration controlling merge behavior (e.g., max depth)
 */
@Suppress("TooManyFunctions")
public class SchemaMerger(
    private val options: SchemaMergerOptions = SchemaMergerOptions(),
) {

    private fun hasSubschemas(input: Schema<*>): Boolean = input.allOf?.isNotEmpty() ?: input.anyOf?.isNotEmpty() ?: input.oneOf?.isNotEmpty() ?: false

    /**
     * Merges a schema with its allOf/anyOf/oneOf subschemas into a single flat schema.
     *
     * @param input the source schema containing composed subschemas
     * @param depth current recursion depth for cycle protection
     * @param visitedRefs mutable set tracking visited `$ref` pointers to prevent infinite loops
     * @param resolve function to dereference `$ref` schemas
     * @return merged schema with all subschema constraints applied
     */
    public fun mergeWithSubSchemas(
        input: Schema<*>,
        depth: Int,
        visitedRefs: MutableSet<String>,
        resolve: (Schema<*>) -> Schema<*> = { it },
    ): Schema<*> = if (hasSubschemas(input)) {
        val subSchemasToMerge = (input.allOf?.filter { s -> unvisited(s, visitedRefs) } ?: emptyList()) +
            pickUnvisitedOneOfAnyOf(input, visitedRefs)
        merge(input, subSchemasToMerge, depth + 1, visitedRefs, resolve)
    } else {
        input
    }

    private inline fun <reified T : Schema<*>> copyWithoutSubSchemasWhenSubSchemasPresent(input: T): T {
        if (!hasSubschemas(input)) {
            return input
        }
        val copy = jsonMapper.readValue(jsonMapper.writeValueAsString(input), T::class.java)
        copy.name = input.name
        copy.specVersion = input.specVersion
        copy.allOf = null
        copy.anyOf = null
        copy.oneOf = null
        return copy
    }

    private fun unvisited(
        schema: Schema<*>,
        visitedRefs: MutableSet<String>,
    ): Boolean = schema.`$ref`?.let { !visitedRefs.contains(it) } ?: true

    private fun hasUnvisitedSubschemas(
        schema: Schema<*>,
        visitedRefs: MutableSet<String>,
    ): Boolean = schema.allOf?.any { s -> unvisited(s, visitedRefs) }
        ?: schema.oneOf?.any { s -> unvisited(s, visitedRefs) }
        ?: schema.anyOf?.any { s -> unvisited(s, visitedRefs) }
        ?: false

    /**
     * Expands a schema with oneOf/anyOf into all possible flat combinations.
     *
     * For schemas with `oneOf: [A, B]` and `anyOf: [C, D]`, this produces the Cartesian product
     * of valid combinations, each merged into a standalone schema.
     *
     * @param schema the schema to expand
     * @param depth current recursion depth
     * @param visitedRefs mutable set tracking visited `$ref` pointers
     * @param budget optional budget to limit combinatorial explosion
     * @param resolve function to dereference `$ref` schemas
     * @return list of flat schemas representing all valid combinations
     */
    public fun getSchemaFlatCombinations(
        schema: Schema<*>,
        depth: Int = 0,
        visitedRefs: MutableSet<String> = mutableSetOf(),
        budget: CombinationBudget? = null,
        resolve: (Schema<*>) -> Schema<*> = { it },
    ): List<Schema<*>> {
        if (depth > options.maxMergedSchemaDepth || !hasUnvisitedSubschemas(schema, visitedRefs)) {
            return listOf(copyWithoutSubSchemasWhenSubSchemasPresent(schema)).also { budget?.consume(it.size) }
        }
        val unvisitedAllOfs = schema.allOf?.filter { unvisited(it, visitedRefs) } ?: listOf()
        return getOneOfAnyOfCombinations(schema, visitedRefs).flatMap { schemas ->
            val visitedRefsForCombination = visitedRefs.toMutableList()
            val schemasToMerge = (schemas + unvisitedAllOfs).filter { schema -> schema.`$ref`?.let { visitedRefsForCombination.add(it) } ?: true }
            schemasToMerge.map {
                if (hasSubschemas(it)) {
                    getSchemaFlatCombinations(it, depth + 1, visitedRefsForCombination.toMutableSet(), budget, resolve)
                } else {
                    listOf(it)
                }
            }.cartesianProduct().map { copyWithoutSubSchemasWhenSubSchemasPresent(merge(schema, it, depth + 1, mutableSetOf(), resolve)) }
        }.distinctBy { jsonMapper.writeValueAsString(it) }.also { budget?.consume(it.size) }
    }

    /**
     * Returns combinations of oneOf and anyOf subschemas for expansion.
     *
     * @param schema the composed schema
     * @param visitedRefs set of already visited `$ref` pointers
     * @return list of schema lists representing each valid combination
     */
    public fun getOneOfAnyOfCombinations(schema: Schema<*>, visitedRefs: MutableSet<String>): List<List<Schema<*>>> {
        if (schema.anyOf?.any { unvisited(it, visitedRefs) } != true) {
            return schema.oneOf?.filter { unvisited(it, visitedRefs) }?.map { listOf(it) } ?: listOf(listOf())
        }
        return schema.anyOf?.filter { unvisited(it, visitedRefs) }?.flatMap { anyOf ->
            schema.oneOf?.filter { unvisited(it, visitedRefs) }?.map { oneOf -> listOf(anyOf, oneOf) } ?: listOf(listOf(anyOf))
        } ?: listOf(listOf())
    }

    /**
     * Merges multiple schemas into a single target schema by combining their constraints.
     *
     * Constraints are combined using "meet" semantics:
     * - Numeric bounds: tighter bounds win (max of minimums, min of maximums)
     * - Enums: intersection of allowed values
     * - Required fields: union
     * - Properties: merged recursively via allOf when conflicting
     *
     * @param source the base schema providing structure and initial constraints
     * @param schemasToMerge list of schemas whose constraints should be merged
     * @param depth current recursion depth for cycle protection
     * @param visitedRefs mutable set tracking visited `$ref` pointers
     * @param resolve function to dereference `$ref` schemas
     * @return new schema with all constraints merged
     */
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "LongMethod")
    public fun merge(
        source: Schema<*>,
        schemasToMerge: List<Schema<*>>,
        depth: Int,
        visitedRefs: MutableSet<String>,
        resolve: (Schema<*>) -> Schema<*>,
    ): Schema<*> {
        if (depth > options.maxMergedSchemaDepth || source.`$ref`?.let { !visitedRefs.add(it) } ?: false) {
            return copyWithoutSubSchemasWhenSubSchemasPresent(source)
        }
        val inputs = schemasToMerge.filter { schema -> schema.`$ref`?.let { visitedRefs.add(it) } ?: true }
            .map { resolve(it) }
            .map { input -> mergeWithSubSchemas(input, depth, visitedRefs.toMutableSet(), resolve) }
        val target = createTargetFrom(source)

        target.description = source.description
        target.format = source.format
        target.xml = source.xml
        source.extensions?.forEach { (k, v) -> target.addExtension(k, v) }

        // Copy source properties to the target before merging
        if (source.properties != null) {
            if (target.properties == null) target.properties = LinkedHashMap()
            source.properties.forEach { (k, v) ->
                target.properties[k] = resolve(v)
            }
        }

        // Copy source items for arrays
        if (source.items != null) {
            target.items = resolve(source.items)
        }

        // Copy source additionalProperties for maps
        if (source.additionalProperties != null) {
            target.additionalProperties = when (val ap = source.additionalProperties) {
                is Schema<*> -> resolve(ap)
                else -> ap
            }
        }

        val requiredAcc = linkedSetOf<String>()
        source.required?.let { requiredAcc += it }
        var enumAcc: Set<Any> = source.enum?.toSet() ?: emptySet()
        var nullableAcc = (source.nullable == true)
        var uniqueItemsAcc = (source.uniqueItems == true)

        var typesAcc: Set<String>? = when (target) {
            is JsonSchema -> target.types?.toSet()
            is ComposedSchema -> target.types?.toSet()
            else -> source.type?.let { setOf(it) }
        }

        inputs.forEach { inc ->
            inc.required?.let { requiredAcc += it }
            meetScalarConstraints(target, inc)

            if (target.items != null || inc.items != null) {
                val existing = target.items
                val incoming = inc.items
                when {
                    existing == null && incoming != null ->
                        target.items = resolve(incoming)

                    existing != null && incoming != null -> {
                        val resIncoming = resolve(incoming)
                        if (existing !== resIncoming && !schemasAreEquivalent(existing, resIncoming)) {
                            target.items = composedAllOf(target.specVersion, existing, resIncoming)
                        }
                    }
                }
                uniqueItemsAcc = uniqueItemsAcc || (inc.uniqueItems == true)
            }

            if (inc.additionalProperties != null) {
                when (val ap = inc.additionalProperties) {
                    is Boolean -> if (!ap) target.additionalProperties = false
                    // Note: true (allow any) is the least restrictive, so no action needed
                    is Schema<*> -> {
                        val existing = target.additionalProperties
                        val apResolved = resolve(ap)
                        when (existing) {
                            null, true -> target.additionalProperties = apResolved
                            false -> { // already most restrictive
                            }

                            is Schema<*> -> target.additionalProperties = composedAllOf(target.specVersion, existing, apResolved)
                        }
                    }
                }
            }

            if (inc.properties != null) {
                if (target.properties == null) target.properties = LinkedHashMap()
                inc.properties.forEach { (k, v) ->
                    val incProp = resolve(v)
                    val existing = target.properties[k]
                    target.properties[k] = when {
                        existing == null -> incProp
                        existing === incProp || schemasAreEquivalent(existing, incProp) -> existing
                        else -> composedAllOf(target.specVersion, existing, incProp)
                    }
                }
            }

            inc.enum?.let { e ->
                enumAcc = when (enumAcc.isEmpty()) {
                    true -> e.toSet()
                    else -> enumAcc.intersect(e.toSet())
                }
            }

            nullableAcc = nullableAcc && (inc.nullable == true)
            inc.extensions?.forEach { (k, v) -> target.addExtension(k, v) }

            val incTypes: Set<String>? = when (inc) {
                is JsonSchema -> inc.types?.toSet()
                is ComposedSchema -> inc.types?.toSet()
                else -> inc.type?.let { setOf(it) }
            }
            typesAcc = when {
                typesAcc == null -> incTypes ?: typesAcc
                incTypes == null -> typesAcc
                else -> typesAcc.intersect(incTypes)
            }
        }

        if (requiredAcc.isNotEmpty()) {
            val merged = (target.required ?: emptyList()) + requiredAcc
            target.required = merged.distinct()
        }

        @Suppress("UNCHECKED_CAST")
        enumAcc.takeIf { it.isNotEmpty() }?.let { (target as Schema<Any>)._enum(it.toList()) }
        if (uniqueItemsAcc) target.uniqueItems = true
        if (nullableAcc) target.nullable = true

        when (target) {
            is JsonSchema -> typesAcc?.takeIf { it.isNotEmpty() }?.let { target.types = it }
            is ComposedSchema -> typesAcc?.takeIf { it.isNotEmpty() }?.let { target.types = it }
            else -> typesAcc?.singleOrNull()?.let { target.type = it }
        }

        return target
    }

    private fun pickUnvisitedOneOfAnyOf(schema: Schema<*>, visitedRefs: Set<String>): List<Schema<*>> {
        val oneOfs = schema.oneOf?.filter { s -> s.`$ref`?.let { !visitedRefs.contains(it) } ?: true }?.toSet() ?: emptySet()
        val anyOfs = schema.anyOf?.filter { s -> s.`$ref`?.let { !visitedRefs.contains(it) } ?: true }?.toSet() ?: emptySet()
        val intersect = oneOfs.intersect(anyOfs)
        if (intersect.isEmpty()) { // no intersection
            return (oneOfs.firstOrNull()?.let { listOf(it) } ?: emptyList()) + (anyOfs.firstOrNull()?.let { listOf(it) } ?: emptyList())
        }
        val symmetricalDiff = oneOfs.plus(anyOfs).minus(intersect)
        return if (symmetricalDiff.isNotEmpty()) { // not equal sets
            listOf(symmetricalDiff.first(), intersect.first())
        } else { // equal sets
            intersect.take(2)
        }
    }

    @Suppress("NestedBlockDepth")
    private fun createTargetFrom(source: Schema<*>): Schema<*> {
        val target = when (source) {
            is ArraySchema -> ArraySchema().apply { specVersion = source.specVersion }
            is MapSchema -> MapSchema().apply { specVersion = source.specVersion }
            else -> if (source.specVersion == SpecVersion.V30) {
                SchemaTypeUtil.createSchema(getSchemaType(source), source.format)
            } else {
                JsonSchema().apply {
                    format = source.format
                    when (source) {
                        is JsonSchema -> types = source.types
                        is ComposedSchema -> types = source.types
                    }
                }
            }
        }
        target.specVersion = source.specVersion
        return target
    }

    private fun getSchemaType(schema: Schema<*>): String? {
        if (schema.specVersion == SpecVersion.V30) return schema.type
        val types = when (schema) {
            is JsonSchema -> schema.types
            is ComposedSchema -> schema.types
            else -> null
        }
        return if (types != null && types.size == 1) types.first() else null
    }

    private fun composedAllOf(spec: SpecVersion?, a: Schema<*>, b: Schema<*>): Schema<*> =
        ComposedSchema().apply {
            specVersion = spec
            allOf = listOf(a, b)
        }

    private fun <T : Comparable<T>> min(a: T?, b: T?): T? = when {
        a == null -> b
        b == null -> a
        a <= b -> a
        else -> b
    }

    private fun <T : Comparable<T>> max(a: T?, b: T?): T? = when {
        a == null -> b
        b == null -> a
        a >= b -> a
        else -> b
    }

    @Suppress("CyclomaticComplexMethod")
    private fun meetScalarConstraints(target: Schema<*>, inc: Schema<*>) {
        target.maximum = min(target.maximum, inc.maximum)
        target.minimum = max(target.minimum, inc.minimum)

        // Exclusive boolean flags (OAS 3.0): accumulate with OR
        inc.exclusiveMaximum?.let { target.exclusiveMaximum = it }
        inc.exclusiveMinimum?.let { target.exclusiveMinimum = it }

        // Exclusive numeric values (OAS 3.1): meet constraints
        target.exclusiveMaximumValue = min(target.exclusiveMaximumValue, inc.exclusiveMaximumValue)
        target.exclusiveMinimumValue = max(target.exclusiveMinimumValue, inc.exclusiveMinimumValue)

        target.maxLength = min(target.maxLength, inc.maxLength)
        target.minLength = max(target.minLength, inc.minLength)
        target.maxItems = min(target.maxItems, inc.maxItems)
        target.minItems = max(target.minItems, inc.minItems)
        target.maxProperties = min(target.maxProperties, inc.maxProperties)
        target.minProperties = max(target.minProperties, inc.minProperties)

        inc.maxContains?.let { target.maxContains = min(target.maxContains, it) }
        inc.minContains?.let { target.minContains = max(target.minContains, it) }

        if (target.pattern == null && inc.pattern != null) target.pattern = inc.pattern
        if (target.format == null && inc.format != null) target.format = inc.format

        if (inc.readOnly == true) target.readOnly = true
        if (inc.writeOnly == true) target.writeOnly = true

        if (target.specVersion == SpecVersion.V30 && inc.type != null) {
            if (target.type == null || target.type == inc.type) {
                target.type = inc.type
            }
        }
    }

    private val jsonMapper by lazy {
        jacksonObjectMapper().apply {
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }

    /**
     * Compares two schemas for structural equivalence using JSON serialization.
     * Returns true if both schemas serialize to the same JSON representation.
     */
    @Suppress("SwallowedException")
    private fun schemasAreEquivalent(a: Schema<*>, b: Schema<*>): Boolean {
        return try {
            val jsonA = jsonMapper.writeValueAsString(a)
            val jsonB = jsonMapper.writeValueAsString(b)
            jsonA == jsonB
        } catch (_: Exception) {
            // Exception intentionally swallowed: serialization failure is non-critical
            // Fallback to equality provides safe default behavior
            a == b
        }
    }
}
