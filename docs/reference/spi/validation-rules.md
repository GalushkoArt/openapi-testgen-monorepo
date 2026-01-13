# Validation rules SPI

Validation rules are the primary extension point for generating new negative cases.

## SchemaValidationRule

`SchemaValidationRule` produces invalid values as `RuleValue` instances.

Contract summary:

- Input: `Schema<*>` + `TestGenerationContext`
- Output: `Sequence<RuleValue>` (empty when not applicable)
- Must be deterministic and side-effect free

## SimpleSchemaValidationRule

`SimpleSchemaValidationRule` is a marker interface for rules that operate on a single schema node.

Composed rules (array/object item traversal) are wired separately and can re-apply the full rule list to nested schemas.

## AuthValidationRule

`AuthValidationRule` produces complete negative `TestCase` objects because auth permutations can touch multiple fields and expected status codes.

Contract summary:

- `decide(context)`: whether the rule is applicable
- `apply(context)`: returns `Sequence<TestCase>`
- Must set an explicit `expectedStatusCode` (401/403 for most auth-negative cases)

