package art.galushko.openapi.testgen.spi

/**
 * Marker for simple schema validation rules that operate on a single schema node.
 *
 * Composed rules (array/object item rules) are wired separately and may delegate to this set.
 */
public interface SimpleSchemaValidationRule : SchemaValidationRule


