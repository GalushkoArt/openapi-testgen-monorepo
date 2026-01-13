# Custom rules

This guide shows how to add a **new validation rule** that generates additional negative cases.

## Choose your approach

- **Contribute to this repo** (recommended for reusable rules): add the rule to `core` and register it in `BuiltInRules`.
- **Embed as a feature module** (your own build): implement the rule in your codebase and contribute it via `TestGenerationModule`.

## 1) Implement a rule

### Schema rule (`SimpleSchemaValidationRule`)

Implement `SimpleSchemaValidationRule` when you want to produce invalid values for a single schema node:

```kotlin
import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import io.swagger.v3.oas.models.media.Schema

public class MyRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "My Rule"

    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        // Guard clauses first: return emptySequence() when not applicable.
        return emptySequence()
    }
}
```

### Auth rule (`AuthValidationRule`)

Implement `AuthValidationRule` when generating auth-related `TestCase`s (expected status codes usually 401/403).

## 2) Register the rule

### Contributing to this repo

1. Add your implementation under `core/src/main/kotlin/.../rules/`.
2. Register it in `BuiltInRules` (schema rules) or `BuiltInRules.authValidationRules()` (auth rules).
3. Add focused tests under `core/src/test/kotlin/...` (see existing rule tests for naming and ordering expectations).

### Embedding via `TestGenerationModule`

Create a module and contribute your rule:

```kotlin
import art.galushko.openapi.testgen.config.TestGenerationModule
import art.galushko.openapi.testgen.config.TestGeneratorExecutionOptions
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule

public class MyRulesModule : TestGenerationModule {
    override val id: String = "my-rules"

    override fun extraSimpleSchemaRules(options: TestGeneratorExecutionOptions): List<SimpleSchemaValidationRule> =
        listOf(MyRule())
}
```

Then add it to execution (for embedding, see [distribution-bundle](../../modules/distribution-bundle.md)).

## 3) Verify

- Run unit tests: `./gradlew :core:test`
- Ensure output is deterministic (same inputs → same cases, stable ordering)

## Notes

!!! note
    `ignoreSchemaValidationRules` and `ignoreAuthValidationRules` expect **fully qualified class names** (FQCN), not `getRuleName()`.

## Related docs

- Reference: [Validation rules SPI](../../reference/spi/validation-rules.md)
- Concepts: [Determinism](../../concepts/determinism.md)

# Add custom validation rules

Rules encode constraints and produce negative variations. Add a custom rule when you need a new kind of schema/auth validation case.

## Choose rule type

- **Schema rule**: implement `SimpleSchemaValidationRule` and return `Sequence<RuleValue>` from `apply(schema, context)`.
- **Auth rule**: implement `AuthValidationRule` and return `Sequence<TestCase>` from `apply(context)` with explicit `expectedStatusCode`.

## Implement a schema rule

Create a rule class:

```kotlin
import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import io.swagger.v3.oas.models.media.Schema

class MyCustomSchemaRule : SimpleSchemaValidationRule {
    override fun getRuleName(): String = "My Custom Rule"

    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        // Return emptySequence() when not applicable.
        // Return deterministic values only.
        return emptySequence()
    }
}
```

## Register the rule

Register custom rules via a `TestGenerationModule` implementation and pass it into the engine/runner wiring.

See: [Custom modules](custom-modules.md)

## Test the rule

- Add unit tests under `core/src/test/kotlin` next to other rule tests.
- Keep tests deterministic and focused; verify exact expected `RuleValue` ordering.

