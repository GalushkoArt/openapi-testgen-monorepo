---
description: Core SPI (Service Provider Interface) for extending the generator with custom validation rules, test providers, generators, and value providers. Includes interface definitions, registration, and implementation checklist.
---

# Core SPI (Extension Interfaces)

The SPI lives in `core/src/main/kotlin/.../spi` and defines the stable extension surface for the core module. Implementations should be deterministic,
side-effect free, and compatible with the generation pipeline.

For core entry points, see the [core module](../modules/core.md). For built-in catalogs, see [rules](catalogs/rules-catalog.md) and
[providers](catalogs/providers-catalog.md). For distribution defaults, see [distribution settings](distribution-settings.md). For contributor workflow, see
[development setup](../contributing/development-setup.md).

## Validation Rules

Validation rules are the primary extension point for generating new negative test cases.

### SchemaValidationRule

`SchemaValidationRule` produces invalid values as `RuleValue` instances.

Contract summary:

- Input: `Schema<*>` + `TestGenerationContext`
- Output: `Sequence<RuleValue>` (empty when not applicable)
- Must be deterministic and side-effect free

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

### SimpleSchemaValidationRule

`SimpleSchemaValidationRule` is a marker interface for rules that operate on a single schema node. Composed rules (array/object item traversal) are wired
separately and can re-apply the full rule list to nested schemas.

```kotlin
public interface SimpleSchemaValidationRule : SchemaValidationRule
```

### AuthValidationRule

`AuthValidationRule` produces complete negative `TestCase` objects because auth permutations can touch multiple fields and expected status codes.

Contract summary:

- `decide(context)`: whether the rule is applicable
- `apply(context)`: returns `Sequence<TestCase>`
- Must set an explicit `expectedStatusCode` (401/403 for most auth-negative cases)

```kotlin
public interface AuthValidationRule {
    fun getRuleName(): String
    fun decide(context: TestGenerationContext): Boolean
    fun apply(context: TestGenerationContext): Sequence<TestCase>
}
```

### Rule registration

Rules are registered via:

1. **Built-in**: Add to `BuiltInRules.simpleSchemaValidationRules()` or `BuiltInRules.authValidationRules()`
2. **Module**: Implement `TestGenerationModule.extraSimpleSchemaRules()` or `extraAuthRules()`

See [Custom rules](../how-to/extending.md#custom-validation-rules) for module-based registration.

### Rule filtering

Rules can be ignored via settings:

- `ignoreSchemaValidationRules`: List of FQCNs to skip
- `ignoreAuthValidationRules`: List of FQCNs to skip

The FQCN is the fully qualified class name (e.g., `art.galushko.openapi.testgen.rules.schema.OutOfMinimumLengthStringSchemaValidationRule`), not the value
from `getRuleName()`.

### RuleRegistry

`RuleRegistry` assembles deterministically ordered rule lists and applies ignore filters (default: `ManualRuleRegistry`). Most extensions should register
additional rules via `TestGenerationModule` rather than implement a custom registry.

## Test Providers

Providers are responsible for turning rules (and other logic) into generated `TestCase` objects.

### TestCaseProvider

`TestCaseProvider<T>` consumes a specific OpenAPI element (the generic `T`) and the current `TestGenerationContext`, and returns an
`Outcome<List<TestCase>>`.

```kotlin
public interface TestCaseProvider<T> {
    public fun provideTestCases(spec: T, context: TestGenerationContext): Outcome<List<TestCase>>
}
```

Typical `T` values:

- `Operation` for top-level providers that run per operation.
- `Parameter` for parameter-focused providers (delegated to by an operation provider).
- `RequestBody` for request-body-focused providers (delegated to by an operation provider).

Core runs operation-level providers in a fixed order (auth -> parameters -> request body) via `ProviderOrchestrator`. Most extensions should add new *rules*
rather than new providers; adding a new provider typically requires embedding and custom wiring.

Guidelines:

- Do not mutate inputs (valid case, context, OpenAPI models).
- Preserve deterministic ordering.
- Prefer composition of smaller providers per concern.
- Use provider boundary helpers (`runProviderSafely`) so exceptions become structured failures.

## Generators

Generators emit artifacts based on generated test suites.

### ArtifactGenerator

`ArtifactGenerator` writes artifacts for a `TestSuite` (or a list of suites).

```kotlin
public interface ArtifactGenerator {
    public fun generateTests(testSuite: TestSuite)
    public fun generateTests(testSuites: List<TestSuite>)
}
```

Guidelines:

- Keep output deterministic (stable ordering, no timestamps).
- Validate inputs early and fail with actionable error messages.
- Localize I/O; avoid global state.

### ArtifactGeneratorFactory

Factories create generator instances and validate generator options.

```kotlin
public interface ArtifactGeneratorFactory {
    public val id: String
    public val description: String
    public fun create(outputDir: File, options: Map<String, Any?>): ArtifactGenerator
}
```

Guidelines:

- Use a stable, unique generator id.
- Validate options up-front.
- Create a fresh generator instance per execution run.

### Generator registration

Generators are selected by `id` via `ArtifactGeneratorRegistry`. Register factories by contributing them from a `TestGenerationModule` and passing that
module to the engine wiring (CLI, Gradle plugin, or embedding code).

## Value Providers

Schema-derived example values are generated via the `example-value` module.

### SchemaValueProvider

`SchemaValueProvider` is a functional interface:

```kotlin
fun provide(schema: Schema<*>, variationIndex: Int): Any?
```

Guidelines:

- Return `null` when not applicable.
- Keep outputs deterministic for identical inputs.
- Use `variationIndex` to generate deterministic variations when uniqueness is required.

See also: [module catalog](../modules/index.md#example-value)

## SecuritySchemeToScope

`SecuritySchemeToScope` pairs a resolved OpenAPI `SecurityScheme` with its name and scopes. Auth rules and `SecurityValueProvider` use this model when
deriving valid or invalid security values.

## Implementation checklist

- Deterministic iteration order; no non-deterministic maps/sets.
- No mutation of `TestGenerationContext` or `OpenAPI` models.
- Return empty sequences when a rule/provider is not applicable.
- Set `expectedStatusCode` explicitly for auth rules.
- Wrap provider logic with `runProviderSafely`.

## Related docs

- Reference: [Rules catalog](catalogs/rules-catalog.md)
- Reference: [Providers catalog](catalogs/providers-catalog.md)
- How-to: [Generators](../how-to/generators.md)
- Reference: [Model](model.md)
- How-to: [Custom rules](../how-to/extending.md#custom-validation-rules)
- How-to: [Custom providers](../how-to/extending.md#custom-providers)
- How-to: [Custom generators](../how-to/extending.md#custom-generators)
- How-to: [Custom modules](../how-to/extending.md#custom-modules)
- Concepts: [Architecture](../concepts/architecture.md#determinism-guarantees)
- Module: [Pattern support](../modules/index.md#pattern-support)
