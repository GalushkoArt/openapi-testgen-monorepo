package art.galushko.openapi.testgen.spi

/**
 * Registry for obtaining validation rule instances in a deterministic order.
 *
 * Implementations are responsible for assembling rule lists (built-in + extensions),
 * applying ignore filters, and providing stable ordering.
 */
public interface RuleRegistry {
    /**
     * Returns the rules for the given type after applying ignore filters.
     *
     * @param T rule base type
     * @param ruleClass base class/interface to match
     * @param ignoredClassNames set of fully qualified class names to exclude
     * @return deterministically ordered list of rule instances
     */
    public fun <T : Any> getRules(
        ruleClass: Class<T>,
        ignoredClassNames: Set<String> = emptySet(),
    ): List<T>
}


