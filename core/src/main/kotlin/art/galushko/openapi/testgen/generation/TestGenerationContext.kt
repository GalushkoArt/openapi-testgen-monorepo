package art.galushko.openapi.testgen.generation

import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGenerator
import art.galushko.openapi.testgen.example.openapi.SchemaMerger
import art.galushko.openapi.testgen.example.response.ResponseExampleExtractor
import art.galushko.openapi.testgen.example.util.CombinationBudget
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.openapi.SkipReason
import art.galushko.openapi.testgen.testdata.BasicTestDataProvider
import art.galushko.openapi.testgen.testdata.SecurityValueProvider
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import java.util.*

/**
 * SPI-facing context for rule/provider evaluation during test generation.
 *
 * This contract is intentionally small and stable. Concrete orchestration may carry more
 * internal state than what is exposed here.
 *
 * Determinism: providers and rules should treat this context as immutable and avoid side effects.
 */
public interface TestGenerationContext {
    /** The parsed OpenAPI specification being processed. */
    public val openAPI: OpenAPI

    /** The current operation being processed. */
    public val operation: Operation

    /** The valid (baseline) test case from which negative cases are derived. */
    public val validCase: TestCase

    /** Provider for basic test data values (e.g., strings, integers). */
    public val basicTestData: BasicTestDataProvider

    /** Provider for security scheme values. */
    public val securityValueProvider: SecurityValueProvider

    /** Generator for deriving example values from schemas. */
    public val schemaExampleValueGenerator: SchemaExampleValueGenerator

    /** Extractor for response examples from operations. */
    public val responseExampleExtractor: ResponseExampleExtractor

    /** Merger for composed schemas (allOf/anyOf/oneOf). */
    public val schemaMerger: SchemaMerger

    /** Maximum schema traversal depth. */
    public val maxDepth: Int

    /** Optional budget limiting schema combinations to prevent explosion. */
    public val combinationBudget: CombinationBudget?

    /** Set of `$ref` pointers already visited in the current traversal path. */
    public val visitedSchemaRefs: Set<String>

    /** Current recursion depth in schema traversal. */
    public val depth: Int

    /** Path of property/item names leading to the current schema location. */
    public val schemaPath: List<String>

    /**
     * Checks if the schema should be skipped based on cycles or depth.
     *
     * @param schema the schema to check
     * @return skip reason if schema should be skipped, null otherwise
     */
    public fun checkSkip(schema: Schema<*>): SkipReason?

    /**
     * Returns a new context with the schema visited, or null if it should be skipped.
     *
     * @param schema the schema being visited
     * @param name the name to append to the schema path
     * @return new context with updated traversal state, or null if schema should be skipped
     */
    public fun withVisitedSchema(schema: Schema<*>, name: String): TestGenerationContext?
}

@Suppress("LongParameterList")
internal class DefaultTestGenerationContext(
    override val openAPI: OpenAPI,
    override val operation: Operation,
    override val validCase: TestCase,
    override val basicTestData: BasicTestDataProvider,
    override val securityValueProvider: SecurityValueProvider,
    override val schemaExampleValueGenerator: SchemaExampleValueGenerator,
    override val responseExampleExtractor: ResponseExampleExtractor,
    override val schemaMerger: SchemaMerger,
    override val maxDepth: Int,
    override val combinationBudget: CombinationBudget?,
    override val visitedSchemaRefs: Set<String> = emptySet(),
    private val visitedSchemaInstances: IdentitySchemaSet = IdentitySchemaSet.empty(),
    override val depth: Int = 0,
    override val schemaPath: List<String> = emptyList(),
) : TestGenerationContext {

    override fun checkSkip(schema: Schema<*>): SkipReason? {
        if (depth >= maxDepth) return SkipReason.DEPTH_EXCEEDED

        val ref = schema.`$ref`
        if (ref != null && ref in visitedSchemaRefs) return SkipReason.CYCLE_DETECTED

        if (schema in visitedSchemaInstances) return SkipReason.CYCLE_DETECTED

        return null
    }

    override fun withVisitedSchema(schema: Schema<*>, name: String): TestGenerationContext? {
        if (checkSkip(schema) != null) return null

        val newDepth = depth + 1
        val ref = schema.`$ref`
        val newVisitedSchemas = if (ref != null) visitedSchemaRefs + ref else visitedSchemaRefs

        return DefaultTestGenerationContext(
            openAPI = openAPI,
            operation = operation,
            validCase = validCase,
            basicTestData = basicTestData,
            securityValueProvider = securityValueProvider,
            schemaExampleValueGenerator = schemaExampleValueGenerator,
            responseExampleExtractor = responseExampleExtractor,
            schemaMerger = schemaMerger,
            maxDepth = maxDepth,
            combinationBudget = combinationBudget,
            visitedSchemaRefs = newVisitedSchemas,
            visitedSchemaInstances = visitedSchemaInstances + schema,
            depth = newDepth,
            schemaPath = schemaPath + name,
        )
    }

    override fun toString(): String =
        "DefaultTestGenerationContext(depth=$depth, path=$schemaPath, refs=$visitedSchemaRefs, instances=${visitedSchemaInstances.size})"
}

internal class IdentitySchemaSet private constructor(
    private val schemas: IdentityHashMap<Schema<*>, Unit>,
) {
    val size: Int
        get() = schemas.size

    operator fun contains(schema: Schema<*>): Boolean = schemas.containsKey(schema)

    operator fun plus(schema: Schema<*>): IdentitySchemaSet {
        if (schema in this) return this
        val copy = IdentityHashMap(schemas)
        copy[schema] = Unit
        return IdentitySchemaSet(copy)
    }

    companion object {
        fun empty(): IdentitySchemaSet = IdentitySchemaSet(IdentityHashMap())
    }
}


