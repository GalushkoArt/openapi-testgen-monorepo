package art.galushko.openapi.testgen.rules.composed

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isArray
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.spi.RuleContainer
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SchemaValidationRule
import art.galushko.openapi.testgen.testdata.tryGetExample
import io.swagger.v3.oas.models.media.Schema
import org.slf4j.LoggerFactory

/**
 * Applies nested item validation rules to array schemas, producing invalid array examples.
 *
 * Inputs: array schema (dereferenced) and [TestGenerationContext].
 * Output: [RuleValue]s whose descriptions are prefixed with "Array Item " and whose values are arrays with an invalid item.
 * Constraints: applies only to array schemas with items; skips when recursion depth exceeds
 * `TestGenerationSettings.maxSchemaDepth` or when schema cycles are detected via structural hashing.
 * Determinism: preserves rule order from [RuleContainer] and schema-combination order from `SchemaMerger`.
 * Settings: `maxSchemaDepth` controls recursion limit (default 10); `maxSchemaCombinations` limits composed-schema
 * expansion; example generation follows `exampleValues` provider order.
 *
 * @param ruleContainer container providing access to all validation rules for recursive application
 */
internal class ArrayItemSchemaValidationRule(
    private val ruleContainer: RuleContainer,
) : SchemaValidationRule {
    private val log = LoggerFactory.getLogger(ArrayItemSchemaValidationRule::class.java)

    override fun getRuleName(): String = "Array Item "

    @Suppress("CyclomaticComplexMethod", "ComplexCondition", "ReturnCount")
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val deref = tryGetSchemaFromRef(schema, context.openAPI)
        val canProcess = isArray(deref) && (deref.maxItems == null || deref.maxItems != 0)
        if (!canProcess) return emptySequence()

        val rawItem = deref.items ?: return emptySequence()

        val updatedContext = context.withVisitedSchema(rawItem, "item")
        if (updatedContext == null) {
            val reason = context.checkSkip(rawItem)
            val path = (context.schemaPath + "item").joinToString(" -> ")
            log.warn(
                "Skipping nested array item validation for operation {}. Reason: {}. Path: {}. " +
                    $$"Suggestion: Use $ref to break the cycle or simplify the schema structure.",
                context.operation.operationId ?: "unknown",
                reason,
                path,
            )
            return emptySequence()
        }

        val rawItemSchema = tryGetSchemaFromRef(rawItem, context.openAPI)
        return context.schemaMerger.getSchemaFlatCombinations(
            rawItemSchema,
            context.depth,
            context.visitedSchemaRefs.toMutableSet(),
            updatedContext.combinationBudget,
        ) {
            tryGetSchemaFromRef(it, context.openAPI)
        }.asSequence().flatMap { itemSchema ->
            val exampleArrayValues = tryGetExample(context, schema, deref) {
                context.schemaExampleValueGenerator.getExampleArrayValuesByItem("array", deref, itemSchema, context.openAPI)
            }

            ruleContainer.getAllRules().asSequence().flatMap { rule ->
                rule.apply(rawItemSchema, updatedContext).map { value ->
                    val array = exampleArrayValues.toMutableList()
                    if (array.isEmpty()) array.add(0, value.value) else array[0] = value.value
                    value.grow(getRuleName(), array)
                }
            }
        }
    }
}


