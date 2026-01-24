package art.galushko.openapi.testgen.example.generator.internal

import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGeneratorOptions
import io.swagger.v3.oas.models.OpenAPI

/**
 * Internal context object that encapsulates traversal state during example generation.
 *
 * Reduces method parameter lists by grouping related parameters into a single context object.
 * This context is passed through recursive calls during schema traversal.
 *
 * @property name the parameter or property name (used in error messages)
 * @property openAPI the OpenAPI specification for resolving references
 * @property depth current recursion depth (incremented on nested schema traversal)
 * @property visitedRefs mutable set tracking visited $ref paths to detect circular references
 * @property variationIndex index used to generate varied values (for unique items, etc.)
 * @property options configuration options controlling generation behavior
 */
internal data class ExampleGenerationContext(
    val name: String,
    val openAPI: OpenAPI,
    val options: SchemaExampleValueGeneratorOptions,
    val depth: Int = 0,
    val visitedRefs: MutableSet<String> = mutableSetOf(),
    val variationIndex: Int = 0,
) {
    /**
     * Checks if further descent into a schema is allowed based on depth limits and circular reference detection.
     *
     * @return true if descent is allowed, false otherwise
     */
    fun isDepthAllowed(): Boolean {
        return depth <= options.maxExampleDepth
    }

    /**
     * Creates a new context for descending into a nested schema.
     *
     * @param newName optional new name for the nested context (defaults to current name)
     * @return a new context with incremented depth
     */
    fun descend(newName: String = name): ExampleGenerationContext =
        copy(name = newName, depth = depth + 1)

    /**
     * Creates a new context with a specific variation index.
     *
     * @param newVariationIndex the new variation index to use
     * @return a new context with the updated variation index
     */
    fun withVariation(newVariationIndex: Int): ExampleGenerationContext =
        copy(variationIndex = newVariationIndex)

    /**
     * Registers a $ref as visited for circular reference detection.
     *
     * @param ref the $ref path to register
     */
    fun registerVisited(ref: String?) {
        ref?.let { visitedRefs.add(it) }
    }

    /**
     * Creates a copy of visitedRefs to use in parallel traversal paths.
     *
     * @return a new context with a copied visitedRefs set
     */
    fun copyVisitedRefs(): ExampleGenerationContext =
        copy(visitedRefs = visitedRefs.toMutableSet())
}
