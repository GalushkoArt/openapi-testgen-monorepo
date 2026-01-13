# Core SPI (Extension Interfaces)

This document is non-normative and targets contributors building custom rules, providers, or
generators. For core entry points, see the [core module](../../modules/core.md). For built-in catalogs,
see [rules](../catalogs/rules-catalog.md) and [providers](../catalogs/providers-catalog.md).
For distribution defaults, see [distribution settings](../distribution-settings.md).
For contributor workflow, see [development setup](../../contributing/development-setup.md).

## Overview

The SPI lives in `core/src/main/kotlin/.../spi` and defines the stable extension surface for the
core module. Implementations should be deterministic, side-effect free, and compatible with the
generation pipeline.

## Pages

- [Validation rules](validation-rules.md)
- [Test providers](test-providers.md)
- [Generators](generators.md)
- [Value providers](value-providers.md)

## Rules

### SchemaValidationRule

- Inputs: a schema node and `TestGenerationContext`.
- Output: `Sequence<RuleValue>`; return an empty sequence when not applicable.
- Determinism: preserve output order for identical inputs.
- Errors: avoid throwing; providers convert exceptions to `Outcome.Failure` via `runProviderSafely`.

### SimpleSchemaValidationRule

Marker interface for rules that operate on a single schema node. Composed rules (array/object item)
are wired separately and may delegate to this set.

### AuthValidationRule

- Inputs: `TestGenerationContext` (valid case + OpenAPI model).
- Output: `Sequence<TestCase>` because auth scenarios span headers, cookies, and query params.
- `decide(...)` controls applicability; `apply(...)` builds full test cases.
- Built-in rules set `expectedStatusCode` to 401/403; custom rules should set explicit status codes.

## RuleValue and composed rules

`RuleValue` carries a description stack and a value. `buildDescription()` concatenates the stack
into a test case description. Composed rules (array/object item rules) use `grow(prefix, newValue)`
to prepend a contextual prefix while preserving nested descriptions.

`RuleContainer` exposes the full rule list so composed rules can re-apply all rules to nested
schemas. Ordering should match registry output to keep descriptions and outputs deterministic.

## RuleRegistry

`RuleRegistry` returns rule instances for a requested type, applying ignore filters and stable
ordering. The default implementation is `ManualRuleRegistry`, which sorts by fully qualified class
name and logs unknown ignore entries.

## TestCaseProvider

Providers implement `TestCaseProvider<T>` and return `Outcome<List<TestCase>>`. Providers should:

- Use `runProviderSafely(...)` to convert exceptions into `Outcome.Failure`.
- Preserve input order and provider order.
- Avoid mutating `TestGenerationContext` or input OpenAPI models.

## ArtifactGenerator

`ArtifactGenerator` writes artifacts for a `TestSuite`. The default `generateTests(List)` helper
just iterates and delegates to `generateTests(TestSuite)`.

## SecuritySchemeToScope

`SecuritySchemeToScope` pairs a resolved OpenAPI `SecurityScheme` with its name and scopes. Auth
rules and `SecurityValueProvider` use this model when deriving valid or invalid security values.

## Implementation checklist

- Deterministic iteration order; no non-deterministic maps/sets.
- No mutation of `TestGenerationContext` or `OpenAPI` models.
- Return empty sequences when a rule/provider is not applicable.
- Set `expectedStatusCode` explicitly for auth rules.
- Wrap provider logic with `runProviderSafely`.
