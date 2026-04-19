---
description: The core module is the generation engine that parses OpenAPI specifications, builds test suites using providers and rules, enforces budget controls, and produces artifacts through generators. It provides the TestGenerationEngine facade and built-in test-suite-writer generator.
---

# Module: `core`

`core` is the **generation engine**: it parses OpenAPI, builds per-operation test suites using providers and rules, applies budgets/filters, and emits artifacts through generators.

It is used by the CLI and Gradle plugin via the `distribution-bundle` module.

## Depends on

- `model`
- `example-value`

## Used by

- `pattern-support`
- `generator-template`
- `distribution-bundle`

## Responsibilities

- Parse and resolve OpenAPI documents.
- Build a baseline *valid* `TestCase` per operation.
- Apply providers and rules to derive negative test cases.
- Enforce budgets and deterministic ordering.
- Produce a `GenerationReport` and run artifact generators.

## Key packages

- `config/`: execution options, settings, merge semantics, module wiring.
- `generation/`: suite generation orchestration and context.
- `generation/orchestration/`: provider execution and outcome aggregation.
- `providers/`: operation and element-level test case providers.
- `rules/`: schema/auth validation rules plus composed rules.
- `testdata/`: valid-case construction, basic/security values, example extraction helpers.
- `generator/`: artifact generator registry and built-in generator wiring.
- `generator/writer/`: JSON/YAML test suite writer implementation.

## Key entry points

- `TestGenerationEngine`: public facade used by CLI, Gradle plugin, and embedding code
- `TestGeneratorExecutionOptionsFactory`: merges YAML config with CLI/Gradle overrides
- `TestGenerationSettings`: typed generation settings (budgets, ignore config, error handling)
- `GeneratorIds`: stable ids (`template`, `test-suite-writer`)
- Built-in generator: `test-suite-writer` (JSON/YAML writer)

## Determinism and budgets

The core is designed to be deterministic:

- Provider execution order is fixed (auth → parameters → request body).
- Rules are sorted deterministically (fully-qualified class name; composed rules appended after simple rules).
- Output ordering is stable and merge behavior is deterministic.

Budget controls prevent combinatorial explosion:

- `maxSchemaDepth` / `maxMergedSchemaDepth` cap recursion.
- `maxSchemaCombinations` limits allOf/anyOf/oneOf expansion via `CombinationBudget`.
- `maxTestCasesPerOperation` is enforced by `TestCaseBudgetValidator`.

## Configuration

Core behavior is configured primarily via `TestGenerationSettings` (budgets, ignore filters, example value settings, and module-owned settings).

See: [YAML config](../how-to/configuration.md#yaml-configuration) and [Distribution settings](../reference/distribution-settings.md).

## Extension points

- **Rules**: implement `SimpleSchemaValidationRule` or `AuthValidationRule` and contribute via `TestGenerationModule`.
  See: [SPI](../reference/spi.md) and [rules catalog](../reference/catalogs/rules-catalog.md).
- **Providers**: implement `TestCaseProvider<T>`.
  See: [SPI](../reference/spi.md) and [providers catalog](../reference/catalogs/providers-catalog.md).
- **Generators**: implement `ArtifactGeneratorFactory` and `ArtifactGenerator`.
  See: [test-suite-writer generator](../how-to/generators.md#test-suite-writer-generator) and the generator SPI docs.
- **Schema values**: implement `SchemaValueProvider` in `example-value` and contribute via a `TestGenerationModule`.
  See: [module catalog](index.md#example-value).
- **Module settings**: implement `ModuleSettingsExtractor` to parse module-specific settings from `testGenerationSettings`.

## Testing and fixtures

- Unit tests: `core/src/test/kotlin`
- Fixtures/snapshots: `core/src/test/resources`
- Run: `./gradlew :core:test` (or `./gradlew :core:check`)

## API reference

- Dokka API reference: [`docs/api/core/index.html`](../api/core/index.html)

## Related docs

- Concepts:
  - [Architecture](../concepts/architecture.md)
  - [Test generation flow](../concepts/architecture.md#data-flow)
  - [Provider-rule model](../concepts/architecture.md#provider-rule-architecture-pattern)
- Reference:
  - [Distribution settings](../reference/distribution-settings.md)
  - [Rules catalog](../reference/catalogs/rules-catalog.md)
  - [Providers catalog](../reference/catalogs/providers-catalog.md)
  - [API reference](../reference/api.md)
- How-to:
  - [Test-suite-writer](../how-to/generators.md#test-suite-writer-generator)
  - [Template generator](../how-to/generators.md#template-generator)
  - [Ignore rules](../how-to/configuration.md#ignore-rules)
