package art.galushko.openapi.testgen.spi

/**
 * Container providing access to all schema validation rules.
 *
 * Used by composed rules to access the complete rule set for recursive validation.
 * Ordering should match the registry output for deterministic rule application.
 */
public interface RuleContainer {
    /**
     * Returns all validation rules in the container.
     *
     * @return list of all schema validation rules
     */
    public fun getAllRules(): List<SchemaValidationRule>
}


