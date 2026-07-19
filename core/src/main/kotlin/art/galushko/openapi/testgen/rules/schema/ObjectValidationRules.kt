@file:Suppress("Filename", "MatchingDeclarationName")

package art.galushko.openapi.testgen.rules.schema

import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.isObject
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers.tryGetSchemaFromRef
import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import art.galushko.openapi.testgen.testdata.tryGetExample
import art.galushko.openapi.testgen.util.remove
import io.swagger.v3.oas.models.media.Schema

/**
 * Produces objects missing one required property at a time.
 *
 * Inputs: object schema with `required` and `properties`, plus [TestGenerationContext].
 * Output: [RuleValue] sequence where each value omits a single required property; the description includes the property name.
 * Constraints: returns empty when schema is not an object or lacks required properties.
 * Determinism: deterministic for identical schema/context.
 * Settings: example generation follows `TestGenerationSettings.exampleValues`.
 */
internal class MissedRequiredObjectPropertiesSchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Missed Required Object Properties "
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val deref = tryGetSchemaFromRef(schema, context.openAPI)
        if (!isObject(deref) || deref.properties.isNullOrEmpty() || deref.required.isNullOrEmpty()) return emptySequence()
        val exampleObject = tryGetExample(context, schema, deref) {
            context.schemaExampleValueGenerator.getExampleObject("object", schema, context.openAPI)
        }
        return deref.required.asSequence().map { required: String ->
            RuleValue(getRuleName() + required, exampleObject.remove(required))
        }
    }
}

/**
 * Produces objects where one required, non-nullable property is explicitly `null`.
 *
 * Inputs: object schema with `required` and `properties`, plus [TestGenerationContext].
 * Output: [RuleValue] sequence where each value sets a single required non-nullable property to
 * `null`; the description includes the property name.
 * Constraints: returns empty when schema is not an object, lacks required properties, or all
 * required properties are nullable (`nullable: true` in 3.0, `"null"` in `type` for 3.1, or either
 * form declared by any `oneOf`/`anyOf`/`allOf` branch, resolved through `$ref`). A property whose
 * composition merely might accept `null` is skipped rather than risking a negative case that a
 * compliant server accepts.
 * Determinism: deterministic for identical schema/context.
 * Settings: example generation follows `TestGenerationSettings.exampleValues`.
 */
internal class NullForRequiredPropertySchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Null For Required Property "
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val deref = tryGetSchemaFromRef(schema, context.openAPI)
        if (!isObject(deref) || deref.properties.isNullOrEmpty() || deref.required.isNullOrEmpty()) return emptySequence()
        val nonNullableRequired = deref.required.filter { property ->
            val propertySchema = deref.properties[property]?.let { tryGetSchemaFromRef(it, context.openAPI) }
            propertySchema != null && !isNullable(propertySchema, context)
        }
        if (nonNullableRequired.isEmpty()) return emptySequence()
        val exampleObject = tryGetExample(context, schema, deref) {
            context.schemaExampleValueGenerator.getExampleObject("object", schema, context.openAPI)
        }
        return nonNullableRequired.asSequence().map { property ->
            val withNull = LinkedHashMap<String, Any?>(exampleObject)
            withNull[property] = null
            RuleValue(getRuleName() + property, withNull)
        }
    }

    private fun isNullable(schema: Schema<*>, context: TestGenerationContext): Boolean =
        isNullable(schema, context, HashSet())

    private fun isNullable(schema: Schema<*>, context: TestGenerationContext, visited: MutableSet<Schema<*>>): Boolean {
        if (!visited.add(schema)) return false
        if (schema.nullable == true || schema.types?.contains("null") == true) return true
        return sequenceOf(schema.oneOf, schema.anyOf, schema.allOf)
            .flatMap { it.orEmpty() }
            .any { branch -> isNullable(tryGetSchemaFromRef(branch, context.openAPI), context, visited) }
    }
}

/**
 * Produces an object carrying a property that the schema does not declare.
 *
 * Inputs: object schema with `additionalProperties: false`, plus [TestGenerationContext].
 * Output: single [RuleValue] whose value is the example object plus one undeclared property.
 * Constraints: returns empty unless the schema is an object that forbids additional properties.
 * Determinism: deterministic for identical schema/context; the extra property name avoids
 * declared property names.
 * Settings: example generation follows `TestGenerationSettings.exampleValues`; the extra value
 * comes from `TestGenerationSettings.overrideBasicTestData`.
 */
internal class UnexpectedAdditionalPropertySchemaValidationRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "Unexpected Additional Property"
    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        val deref = tryGetSchemaFromRef(schema, context.openAPI)
        if (!isObject(deref) || deref.additionalProperties != false) return emptySequence()
        val exampleObject = tryGetExample(context, schema, deref) {
            context.schemaExampleValueGenerator.getExampleObject("object", schema, context.openAPI)
        }
        val declared = deref.properties?.keys.orEmpty()
        val extraName = generateSequence("unexpectedProperty") { it + "X" }
            .first { it !in declared && it !in exampleObject.keys }
        val withExtra = LinkedHashMap<String, Any?>(exampleObject)
        withExtra[extraName] = context.basicTestData.unexpectedAdditionalPropertyValue()
        return listOf(RuleValue(getRuleName(), withExtra)).asSequence()
    }
}

