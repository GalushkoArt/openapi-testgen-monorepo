package art.galushko.openapi.testgen.rules.composed

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isObject
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.spi.RuleContainer
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SchemaValidationRule
import art.galushko.openapi.testgen.testdata.tryGetExample
import io.swagger.v3.oas.models.media.Schema
import org.slf4j.LoggerFactory

/**
 * Applies nested property validation rules to object schemas, producing invalid object examples.
 *
 * Inputs: object schema (dereferenced) and [TestGenerationContext].
 * Output: [RuleValue]s whose descriptions are prefixed with "Object Property <name> " and whose values are objects
 * with an invalid property value.
 * Constraints: applies only to object schemas with properties; skips when recursion depth exceeds
 * `TestGenerationSettings.maxSchemaDepth` or when schema cycles are detected via structural hashing.
 * Determinism: preserves rule order from [RuleContainer] and schema-combination order from `SchemaMerger`.
 * Settings: `maxSchemaDepth` controls recursion limit (default 10); `maxSchemaCombinations` limits composed-schema
 * expansion; example generation follows `exampleValues` provider order.
 *
 * @param ruleContainer container providing access to all validation rules for recursive application
 */
internal class ObjectItemSchemaValidationRule(
    private val ruleContainer: RuleContainer,
) : SchemaValidationRule {
    private val log = LoggerFactory.getLogger(ObjectItemSchemaValidationRule::class.java)

    override fun getRuleName(): String = "Object Property "

    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val deref = tryGetSchemaFromRef(schema, context.openAPI)
        if (!isObject(deref) || deref.properties == null || deref.properties.isEmpty()) return emptySequence()
        val baseObject = tryGetExample(context, schema, deref) {
            context.schemaExampleValueGenerator.getExampleObject("object", schema, context.openAPI).toMutableMap()
        }
        return deref.properties.entries
            .asSequence()
            .flatMap { (propertyName, propertySchema) -> processProperty(propertyName, propertySchema, baseObject, context) }
    }

    private fun processProperty(
        propName: String,
        propertySchema: Schema<*>,
        baseObject: MutableMap<String, Any>,
        context: TestGenerationContext,
    ): Sequence<RuleValue> {
        val propSchema = tryGetSchemaFromRef(propertySchema, context.openAPI)

        val updated = context.withVisitedSchema(propertySchema, propName)
        if (updated == null) {
            val reason = context.checkSkip(propertySchema)
            val path = (context.schemaPath + propName).joinToString(" -> ")
            log.warn(
                "Skipping nested property '{}' for operation {}. Reason: {}. Path: {}. " +
                    $$"Suggestion: Use $ref to break the cycle or simplify the schema structure.",
                propName,
                context.operation.operationId ?: "unknown",
                reason,
                path,
            )
            return emptySequence()
        }

        return context.schemaMerger.getSchemaFlatCombinations(
            propSchema,
            context.depth,
            context.visitedSchemaRefs.toMutableSet(),
            updated.combinationBudget,
        ) {
            tryGetSchemaFromRef(it, context.openAPI)
        }.asSequence().flatMap { flat ->
            ruleContainer.getAllRules().asSequence().flatMap { rule ->
                rule.apply(flat, updated).map { rv ->
                    val value = baseObject.toMutableMap()
                    value[propName] = rv.value
                    rv.grow("${getRuleName()}$propName ", value)
                }
            }
        }
    }
}


