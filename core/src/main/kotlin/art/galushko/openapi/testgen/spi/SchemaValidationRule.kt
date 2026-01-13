package art.galushko.openapi.testgen.spi

import art.galushko.openapi.testgen.generation.TestGenerationContext
import io.swagger.v3.oas.models.media.Schema

/**
 * Base contract for schema validation rules that produce invalid example values.
 *
 * Inputs: a schema node and [TestGenerationContext].
 * Output: a (possibly empty) sequence of [RuleValue] instances.
 * Errors: rules should avoid throwing; provider wrappers will convert exceptions to failures.
 * Determinism: rule output order must be stable for identical inputs.
 */
public interface SchemaValidationRule {
    /**
     * Provides a short, human-readable rule name used in composed descriptions.
     */
    public fun getRuleName(): String

    /**
     * Applies the rule to the provided [schema] within the [context].
     *
     * @param schema the (possibly referenced) schema to validate
     * @param context with a valid test case, operation specification and OpenAPI document
     * @return a sequence of [RuleValue] results; empty if the rule is not applicable
     */
    public fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue>
}


