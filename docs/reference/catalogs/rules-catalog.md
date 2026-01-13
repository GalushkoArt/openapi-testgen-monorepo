# Rules

## Overview
Rules encode OpenAPI constraints and produce negative test inputs. The generator uses two kinds of
rules:
- `SchemaValidationRule` (and `SimpleSchemaValidationRule`): return `RuleValue` instances that
  providers turn into `TestCase` objects.
- `AuthValidationRule`: return full `TestCase` objects because auth changes span headers, cookies,
  query params, and security metadata.

The rule system is described in [architecture](../../concepts/architecture.md) and wired by
`core/src/main/kotlin/art/galushko/openapi/testgen/generation/TestGeneratorConfigurer.kt`.

For core entry points and extension context, see the [core module](../../modules/core.md).
For rule interfaces and extension points, see the [Validation rules SPI](../spi/validation-rules.md).
For user-facing configuration (ignore rules), see [ignore rules](../../how-to/configuration/ignore-rules.md).

## RuleValue contract
`RuleValue` carries a description stack and a value. `buildDescription()` concatenates the
stack to form the final description used in test case names. Composed rules use `grow()` to
prepend a prefix (for example, "Array Item " or "Object Property <name> ") to nested rule
values.

## Registration and ordering
- `ManualRuleRegistry` returns built-in rules plus any extra rules from `TestGenerationModule`.
- Rule lists are sorted by fully qualified class name for deterministic ordering.
- `BuiltInRules` creates lists sorted by rule name, but `ManualRuleRegistry` applies the final
  class-name ordering.
- `TestGeneratorConfigurer.getSchemaValidationRules` appends composed rules after simple rules in
  this order: `ArrayItemSchemaValidationRule`, then `ObjectItemSchemaValidationRule`.
- `TestGenerationSettings.ignoreSchemaValidationRules` and `ignoreAuthValidationRules` expect fully
  qualified class names (e.g., `art.galushko.openapi.testgen.rules.schema.InvalidEnumValueSchemaValidationRule`).
- Unknown class names in ignore settings are logged as warnings but do not cause failures.
- Ignoring `DateSchemaValidationRule` or `DateTimeSchemaValidationRule` disables all date/date-time
  variants because parameterized rules share a class (e.g., "Five Digit Year Date" and "Zero Month Date"
  are both instances of `DateSchemaValidationRule`).

## Settings and budgets
Rules interact with settings through the `TestGenerationContext`:
- `overrideBasicTestData` supplies invalid primitives for schema and auth rules.
- `validSecurityValues` supplies valid credentials for auth rule permutations.
- `exampleValues` controls schema example generation for array/object/object-missing rules.
- `maxSchemaCombinations` limits composed-schema expansion via `CombinationBudget`.
- `maxMergedSchemaDepth` bounds composed schema merging in `SchemaMerger`.
- `maxSchemaDepth` controls recursion for composed rules (array/object traversal).

## Built-in schema rules

Array constraints:
- `art.galushko.openapi.testgen.rules.schema.BelowMinItemsArraySchemaValidationRule` - arrays with
  fewer items than `minItems`.
- `art.galushko.openapi.testgen.rules.schema.AboveMaxItemsArraySchemaValidationRule` - arrays with
  more items than `maxItems`.
- `art.galushko.openapi.testgen.rules.schema.NonUniqueItemsArraySchemaValidationRule` - duplicate
  items when `uniqueItems = true`.

Boundary constraints:
- `art.galushko.openapi.testgen.rules.schema.OutOfMinimumBoundaryNumberSchemaValidationRule` - value
  below `minimum` (or equal when `exclusiveMinimum = true`).
- `art.galushko.openapi.testgen.rules.schema.OutOfMaximumBoundaryNumberSchemaValidationRule` - value
  above `maximum` (or equal when `exclusiveMaximum = true`).
- `art.galushko.openapi.testgen.rules.schema.MultipleOfBreakingSchemaValidationRule` - value that
  violates `multipleOf`.

Enum constraints:
- `art.galushko.openapi.testgen.rules.schema.InvalidEnumValueSchemaValidationRule` - value outside
  the declared enum set.

Type/format constraints:
- `art.galushko.openapi.testgen.rules.schema.InvalidTypeValidationRule` - wrong primitive type for
  number/integer/boolean schemas.
- `art.galushko.openapi.testgen.rules.schema.IntegerBreakingSchemaValidationRule` - decimal value
  for integer schemas.
- `art.galushko.openapi.testgen.rules.schema.WrongInt32FormatSchemaValidationRule` - out-of-range
  int32 value.

String constraints:
- `art.galushko.openapi.testgen.rules.schema.OutOfMinimumLengthStringSchemaValidationRule` - string
  shorter than `minLength`.
- `art.galushko.openapi.testgen.rules.schema.OutOfMaximumLengthStringSchemaValidationRule` - string
  longer than `maxLength`.
- `art.galushko.openapi.testgen.rules.schema.WrongEmailFormatSchemaValidationRule` - invalid email
  for `format = email`.
- `art.galushko.openapi.testgen.rules.schema.WrongUuidFormatSchemaValidationRule` - invalid UUID for
  `format = uuid`.

Object constraints:
- `art.galushko.openapi.testgen.rules.schema.MissedRequiredObjectPropertiesSchemaValidationRule` -
  objects with one required property omitted.

Date format constraints (parameterized, all share a class):
- `art.galushko.openapi.testgen.rules.schema.DateSchemaValidationRule`
  - Rule names: "Five Digit Year Date", "Thirteen Month Date", "Thirty Second Day Date",
    "Three Digit Year Date", "Zero Day Date", "Zero Month Date".

Date-time format constraints (parameterized, all share a class):
- `art.galushko.openapi.testgen.rules.schema.DateTimeSchemaValidationRule`
  - Rule names: "Five Digit Year DateTime", "Sixty Minutes DateTime", "Sixty One Seconds DateTime",
    "Thirteen Month DateTime", "Thirty Second Day DateTime", "Three Digit Year DateTime",
    "Twenty Four Hour DateTime", "Zero Day DateTime", "Zero Month DateTime".

Composed schema rules:
- `art.galushko.openapi.testgen.rules.composed.ArrayItemSchemaValidationRule` - applies all schema
  rules to array items and wraps the result in an array value.
- `art.galushko.openapi.testgen.rules.composed.ObjectItemSchemaValidationRule` - applies all schema
  rules to object properties and wraps the result in an object value.

## Pattern module rules

The `pattern-support` module contributes additional rules when enabled (enabled by default in
`distribution-bundle`). These rules require the regexp-gen library.

- `art.galushko.openapi.testgen.pattern.support.InvalidPatternSchemaValidationRule` - generates a
  string that does NOT match the schema `pattern` regex. Applies to string schemas with a `pattern`
  constraint. Rule name: "Invalid Pattern".

!!! note "Library limitations"
    The regexp-gen library has some limitations:

    - Negative lookahead/lookbehind not supported (`(?!...)`, `(?<!...)`)
    - Word boundary assertions not supported (`\b`, `\B`)
    - Backreferences not supported
    - Some patterns may not produce non-matching strings (e.g., `.*`)

    When generation fails, the rule logs an error and is skipped for that schema.

See [pattern-support module](../../modules/pattern-support.md) for configuration options.

## Built-in auth rules
Auth rules return full `TestCase` objects and set expected status codes directly:
- `art.galushko.openapi.testgen.rules.auth.AllSecurityMissedAuthValidationRule` - removes all
  security values, expects 401.
- `art.galushko.openapi.testgen.rules.auth.MissingSecurityValuesAuthValidationRule` - omits one or
  more schemes from a multi-scheme requirement, expects 401.
- `art.galushko.openapi.testgen.rules.auth.InvalidSecurityValuesAuthValidationRule` - supplies
  invalid auth values, expects 401.
- `art.galushko.openapi.testgen.rules.auth.InsufficientScopesAuthValidationRule` - removes scopes
  from OAuth2/OpenID requirements, expects 403.
- `art.galushko.openapi.testgen.rules.auth.IncorrectScopesAuthValidationRule` - uses invalid scopes,
  expects 403.

## Extension points
- Implement `SimpleSchemaValidationRule` or `AuthValidationRule` and return them from
  `TestGenerationModule.extraSimpleSchemaRules` / `extraAuthRules`.
- Provide a custom `RuleRegistry` if you need alternative ordering or rule sourcing.
