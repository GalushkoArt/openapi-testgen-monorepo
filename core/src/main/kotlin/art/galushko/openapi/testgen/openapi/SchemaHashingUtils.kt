package art.galushko.openapi.testgen.openapi

import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.XML

/**
 * Prime number used for a hash combination to reduce collisions.
 * Value 31 is commonly used as its prime and allows compiler optimization (31 * i == (i << 5) - i).
 */
private const val HASH_PRIME = 31

/**
 * Initial seed value for hash computation.
 * Using a non-zero seed ensures better hash distribution.
 */
private const val HASH_SEED = 17

/**
 * Represents the reason for skipping a specific operation or condition in the execution process.
 */
public enum class SkipReason {
    /**
     * Indicates that a cycle was detected during the processing or traversal of the test generation
     * workflow. This typically occurs when a recursive dependency or reference is encountered,
     * causing an infinite loop.
     *
     * Used as a reason to skip further processing of a specific node or entity to prevent
     * indefinite execution.
     */
    CYCLE_DETECTED,

    /**
     * Indicates that the depth of a structure has exceeded the allowable limit during generation or processing.
     * This is used as a reason for skipping further processing of an element or branch.
     */
    DEPTH_EXCEEDED,
}

/**
 * Computes deterministic structural hash codes for OpenAPI schemas to enable cycle detection.
 *
 * This hasher creates consistent hash values based on a schema's structural attributes rather than
 * object identity, allowing detection of semantically equivalent schemas that would cause infinite
 * recursion during test generation. The hash computation is depth-limited to prevent stack overflow
 * on deeply nested or circular schemas.
 *
 * ## Hash Components
 *
 * The structural hash includes:
 * - **Schema attributes**: `$ref`, `type`, `minItems`, `maxItems`, `uniqueItems`
 * - **Validation metadata**: `required` fields (sorted), `enum` values (sorted)
 * - **XML metadata**: element name, namespace, prefix, attribute flag
 * - **Nested structures**: Properties (recursively hashed), array items (recursively hashed)
 *
 * ## Determinism Guarantees
 *
 * - Property names and enum values are sorted before hashing for consistent ordering
 * - Empty collections (`required`, `enum`) are normalized to produce the same hash as null
 * - Depth tracking prevents infinite recursion on circular schemas
 *
 * ## Usage Example
 *
 * ```kotlin
 * val hasher = SchemaStructureHasher(maxDepth = 50)
 * val schema = ObjectSchema().apply {
 *     addProperty("name", StringSchema())
 *     addProperty("age", IntegerSchema())
 * }
 * val hash = hasher.computeSchemaStructureHash(schema, depth = 0)
 * ```
 *
 * @property maxDepth Maximum recursion depth for nested property and item hashing.
 *                    Schemas deeper than this threshold will hash to 0.
 * @see art.galushko.openapi.testgen.generation.TestGenerationContext.checkSkip
 */
internal class SchemaStructureHasher(
    private val maxDepth: Int,
) {
    /**
     * Computes a deterministic structural hash for the given schema.
     *
     * The hash is computed recursively, including:
     * - Direct schema attributes (type, constraints, XML metadata)
     * - Property schemas (for object types)
     * - Item schemas (for array types)
     *
     * Recursion terminates when:
     * - The schema is null (returns 0)
     * - The depth exceeds maxDepth (returns 0)
     *
     * @param schema The OpenAPI schema to hash, or null
     * @param depth Current recursion depth (0 for top-level schema)
     * @return Structural hash code, or 0 if schema is null or depth limit reached
     */
    public fun computeSchemaStructureHash(schema: Schema<*>, depth: Int): Int {
        if (depth >= maxDepth) return 0

        var hash = HASH_SEED

        hash = addHash(hash, calculateObjectSchemaHash(schema))
        hash = addHash(hash, calculatePropertiesHash(schema.properties, depth + 1))

        val items = schema.items
        if (items != null) {
            hash = addHash(hash, computeSchemaStructureHash(items, depth + 1))
        }

        return hash
    }

    /**
     * Calculates a hash for a schema's direct attributes (non-recursive).
     *
     * This method hashes the schema's own properties without recursing into nested structures.
     * Property and item recursion is handled separately by [calculatePropertiesHash] and
     * [computeSchemaStructureHash].
     *
     * Included attributes:
     * - `$ref`: Reference string for schema reuse
     * - `type`/`types`: Schema type (string, object, array, etc.)
     * - `minItems`, `maxItems`, `uniqueItems`: Array validation constraints
     * - `required`: Required property names (sorted for determinism)
     * - `enum`: Enumerated values (sorted by string representation for determinism)
     * - XML metadata: Via [calculateXmlHash]
     *
     * @param schema The schema to hash, or null
     * @return Hash of direct schema attributes, or 0 if the schema is null
     */
    private fun calculateObjectSchemaHash(schema: Schema<*>): Int {
        var hash = HASH_SEED

        hash = combineHash(hash, schema.`$ref`)
        hash = combineHash(hash, schema.type ?: schema.types)
        hash = combineHash(hash, schema.minItems)
        hash = combineHash(hash, schema.maxItems)
        hash = combineHash(hash, schema.uniqueItems)
        hash = combineHash(hash, schema.required?.sorted()?.takeIf { it.isNotEmpty() })
        hash = combineHash(hash, schema.enum?.sortedBy { it?.toString() }?.takeIf { it.isNotEmpty() })
        hash = addHash(hash, calculateXmlHash(schema.xml))

        return hash
    }

    /**
     * Recursively calculates a hash for a schema's property map.
     *
     * Properties are sorted by name before hashing to ensure deterministic ordering regardless
     * of the map's iteration order. Each property's schema is recursively hashed via
     * [computeSchemaStructureHash], allowing deep nested structures to be captured.
     *
     * This enables distinction between schemas with:
     * - Different property names
     * - Different property types
     * - Different nested property structures
     *
     * @param properties Map of property name to schema, or null
     * @param depth Current recursion depth for nested property hashing
     * @return Combined hash of all properties, or 0 if the property map is null/empty
     */
    private fun calculatePropertiesHash(properties: Map<String, Schema<*>>?, depth: Int): Int {
        if (properties == null || properties.isEmpty()) return 0

        var hash = HASH_SEED

        // Sort by property name to ensure consistent ordering
        properties.entries.sortedBy { it.key }.forEach { (name, propertySchema) ->
            hash = combineHash(hash, name)
            hash = addHash(hash, computeSchemaStructureHash(propertySchema, depth))
        }

        return hash
    }

    /**
     * Calculates a hash for XML serialization metadata.
     *
     * XML metadata affects how schemas are serialized to/from XML and is considered part of
     * the schema's structural identity. All XML properties are included:
     * - `name`: XML element name
     * - `namespace`: XML namespace URI
     * - `prefix`: XML namespace prefix
     * - `attribute`: Whether to serialize as XML attribute or element
     *
     * @param xml XML metadata object, or null
     * @return Hash of XML properties, or 0 if XML is null
     */
    private fun calculateXmlHash(xml: XML?): Int {
        if (xml == null) return 0
        var hash = HASH_SEED

        hash = combineHash(hash, xml.name)
        hash = combineHash(hash, xml.namespace)
        hash = combineHash(hash, xml.prefix)
        hash = combineHash(hash, xml.attribute)

        return hash
    }

    /**
     * Combines a hash value with a single object's hash code.
     *
     * Uses prime multiplication to reduce hash collisions. Null values contribute 0 to the hash.
     *
     * @param current Current accumulated hash value
     * @param value Object to hash and combine, or null
     * @return Updated hash value incorporating the object
     */
    private fun combineHash(current: Int, value: Any?): Int {
        return HASH_PRIME * current + (value?.hashCode() ?: 0)
    }

    /**
     * Combines a hash value with another pre-computed hash.
     *
     * Used to incorporate sub-hashes (e.g., from nested properties or XML metadata) into
     * the parent hash. Uses prime multiplication for distribution.
     *
     * @param current Current accumulated hash value
     * @param additionalHash Pre-computed hash to combine
     * @return Updated hash value incorporating the additional hash
     */
    private fun addHash(current: Int, additionalHash: Int): Int {
        return HASH_PRIME * current + additionalHash
    }
}


