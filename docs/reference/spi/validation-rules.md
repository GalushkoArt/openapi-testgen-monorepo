---
description: Reference documentation for the validation rules SPI, including SchemaValidationRule, SimpleSchemaValidationRule, and AuthValidationRule interfaces. Explains how to implement custom rules that generate negative test cases.
---

# Validation rules SPI

Validation rules are the primary extension point for generating new negative test cases.

## SchemaValidationRule

`SchemaValidationRule` produces invalid values as `RuleValue` instances.

Contract summary:

- Input: `Schema<*>` + `TestGenerationContext`
- Output: `Sequence<RuleValue>` (empty when not applicable)
- Must be deterministic and side-effect free

### Interface

```kotlin
public interface SchemaValidationRule {
    fun getRuleName(): String
    fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue>
}
```

### RuleValue

`RuleValue` contains the rule description and an invalid value:

```kotlin
public data class RuleValue(
    val description: ArrayDeque<String>,
    val value: Any,
) {
    constructor(description: String, value: Any)
    fun buildDescription(): String
    fun grow(prefix: String, newValue: Any): RuleValue
}
```

- `description`: Stack of description parts (outermost prefix first)
- `value`: The invalid value to substitute
- `buildDescription()`: Concatenates the description stack into a test case name
- `grow()`: Used by composed rules to prepend context (e.g., array index, property path)

## SimpleSchemaValidationRule

`SimpleSchemaValidationRule` is a marker interface for rules that operate on a single schema node.

Composed rules (array/object item traversal) are wired separately and can re-apply the full rule list to nested schemas.

```kotlin
public interface SimpleSchemaValidationRule : SchemaValidationRule
```

## AuthValidationRule

`AuthValidationRule` produces complete negative `TestCase` objects because auth permutations can touch multiple fields and expected status codes.

Contract summary:

- `decide(context)`: whether the rule is applicable
- `apply(context)`: returns `Sequence<TestCase>`
- Must set an explicit `expectedStatusCode` (401/403 for most auth-negative cases)

### Interface

```kotlin
public interface AuthValidationRule {
    fun getRuleName(): String
    fun decide(context: TestGenerationContext): Boolean
    fun apply(context: TestGenerationContext): Sequence<TestCase>
}
```

## Registration

Rules are registered via:

1. **Built-in**: Add to `BuiltInRules.simpleSchemaValidationRules()` or `BuiltInRules.authValidationRules()`
2. **Module**: Implement `TestGenerationModule.extraSimpleSchemaRules()` or `extraAuthRules()`

See [Custom rules](../../how-to/extension/custom-rules.md) for module-based registration.

## Filtering

Rules can be ignored via settings:

- `ignoreSchemaValidationRules`: List of FQCNs to skip
- `ignoreAuthValidationRules`: List of FQCNs to skip

The FQCN is the fully qualified class name (e.g., `art.galushko.openapi.testgen.rules.schema.OutOfMinimumLengthStringSchemaValidationRule`), not the value from `getRuleName()`.

## Tutorials

- [Custom validation rules](../../how-to/extension/custom-rules.md) - Step-by-step guide to implementing and testing custom rules

## Related documentation

- Reference: [Rules catalog](../catalogs/rules-catalog.md)
- Reference: [TestCase model](../model/test-case.md)
- Concepts: [Determinism](../../concepts/determinism.md)
- Module: [Pattern support](../../modules/pattern-support.md) - Reference implementation
