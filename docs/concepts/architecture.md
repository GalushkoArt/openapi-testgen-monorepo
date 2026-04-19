---
description: System architecture including module dependencies, data flow, provider-rule model, budget controls, determinism guarantees, and extension points. The primary reference for understanding how the generator is structured.
---

# Architecture

## System Overview

OpenAPI Test Generator is a Kotlin/JVM toolchain that turns OpenAPI specs into executable tests or test-suite artifacts. It targets:

- CLI users who want a fast generator for local runs or CI
- Gradle users who want generation wired into build pipelines
- Integrators who embed the core engine to customize rules, data providers, or generators

The design centers on a small, deterministic core that owns parsing, rule application, and test-suite assembly, with optional feature modules (template
generation, pattern-based values) injected explicitly by CLI/Gradle to avoid reflection-based discovery.

## What This Tool Tests

OpenAPI Test Generator validates **infrastructure-level behavior** - the contract compliance layer between API consumers and your controllers. This is distinct
from business logic testing.

### Infrastructure validation (what we test)

- **Parameter validation**: Type checking, format constraints (date, email, uuid), min/max bounds, enum membership, required vs optional
- **Request body validation**: JSON schema compliance, nested object structure, array limits, property constraints
- **Security enforcement**: Authentication presence, valid credentials format, correct security scheme usage

### Business logic (what we don't test)

- Data semantics (e.g., "order total must equal sum of line items")
- Authorization rules beyond API-level security (e.g., "users can only access their own orders")
- Side effects (e.g., "creating an order should send a confirmation email")
- State transitions (e.g., "orders can only be cancelled before shipping")

The generated tests verify that your API **rejects invalid requests** with appropriate error codes (400, 401, 403) - they don't verify what happens when valid
requests are processed.

## Related docs

- Documentation index: [docs home](../index.md)
- Getting started: [Getting started](../getting-started/index.md)
- How-to guides: [Configuration](../how-to/configuration.md), [Generators](../how-to/generators.md), [Negative testing](../how-to/negative-testing.md)
- Development: [development setup](../contributing/development-setup.md)
- Core module: [core](../modules/core.md)
- Reference:
    - [Distribution settings](../reference/distribution-settings.md)
    - [Rules catalog](../reference/catalogs/rules-catalog.md)
    - [Providers catalog](../reference/catalogs/providers-catalog.md)
    - [API reference](../reference/api.md)

## Module Dependency Graph

### Layered Architecture

```mermaid
flowchart TB
    subgraph Entry["Entry Points"]
        CLI["CLI<br/>(native + JVM)"]
        Plugin["Gradle Plugin<br/>(OpenApiTestGeneratorTask)"]
    end

    subgraph Dist["Distribution Bundle"]
        Runner["TestGenerationRunner<br/>(orchestration, reporter, default wiring)"]
    end

    subgraph Features["Feature Modules"]
        PatternSupport["pattern-support<br/>(regex rule + module wiring)"]
        Template["generator-template<br/>(Mustache codegen)"]
    end

    subgraph Core["Core"]
        Providers["Providers<br/>(param, body, auth)"]
        Rules["Rules<br/>(schema, auth)"]
        Engine["TestGenerationEngine<br/>(orchestration, config)"]
    end

    subgraph Values["Value Generation"]
        PatternValue["pattern-value<br/>(regex value generator)"]
        ExampleValue["example-value<br/>(value providers, schema merger)"]
    end

    subgraph Model["Model"]
        Types["TestCase, TestSuite, Outcome,<br/>GenerationReport, ErrorMode"]
    end

    CLI --> Runner
    Plugin --> Runner
    Runner --> Template
    Runner --> PatternSupport
    PatternSupport --> Core
    Template --> Core
    PatternSupport --> PatternValue
    Core --> ExampleValue
    PatternValue --> ExampleValue
    ExampleValue --> Model
```

### Value Provider Dependency Chain

The value generation stack follows a strict dependency direction:

```mermaid
flowchart LR
    PV["pattern-value"] --> EV["example-value"] --> M["model"]
```

**pattern-value** contains:

- `PatternValueProvider` (implements SchemaValueProvider SPI)
- `PatternValueGenerator` (regexp-gen wrapper)
- No core dependency

**example-value** contains:

- `SchemaValueProvider` SPI interface
- Built-in providers (enum, date, uuid, etc.)
- `SchemaMerger`, `CartesianProduct`

This layering enables standalone use of `pattern-value` for regex-based string generation without pulling in the test generation framework.

### Module Responsibilities

| Module                  | Responsibility                                  | Key Types                                                                   |
|-------------------------|-------------------------------------------------|-----------------------------------------------------------------------------|
| **build-logic**         | Convention plugins for centralized build config | `testgen.kotlin-base`, `testgen.quality`, `testgen.library`                 |
| **model**               | Data classes, error types                       | `TestCase`, `TestSuite`, `Outcome`, `GenerationError`                       |
| **example-value**       | Schema value generation SPI + builtins          | `SchemaValueProvider`, `SchemaExampleValueGenerator`, `SchemaMerger`        |
| **core**                | Parsing, generation, orchestration              | `TestGenerationEngine`, providers, rules, `TestSuiteWriter`                 |
| **pattern-value**       | Regex-based value generation                    | `PatternGenerationOptions`, `PatternValueGenerator`, `PatternValueProvider` |
| **pattern-support**     | Pattern integration module                      | `PatternSupportModule`, `PatternModuleSettingsExtractor`                    |
| **generator-template**  | Mustache-based code generation                  | `TemplateGeneratorModule`, `TemplateArtifactGenerator`                      |
| **distribution-bundle** | Bundles modules, execution wiring               | `TestGenerationRunner`, `TestGenerationReporter`, `DistributionDefaults`    |
| **plugin**              | Gradle build integration                        | `OpenApiTestGeneratorPlugin`, `OpenApiTestGeneratorTask`                    |
| **cli**                 | Command-line interface + native                 | `GenerateCommand`, Picocli wiring                                           |

## Data Flow

1. **Entry point**: CLI or Gradle creates a `TestGenerationRunner` (via `withDefaults()` or builder) with an environment-specific `TestGenerationReporter`.
2. **Configuration resolution**: `TestGenerationRunner.execute()` calls `TestGeneratorExecutionOptionsFactory.fromConfig` to merge overrides over YAML config
   and apply defaults.
3. **Parse OpenAPI**: `OpenApiSpecParser.parseOpenApi` loads and resolves the OpenAPI document with `ParseOptions().apply { isResolveFully = true }`.
4. **Filter operations**: If `includeOperations` is set, the engine filters paths/methods before generation. `ignoreTestCases` is applied later, after suites
   are assembled.
5. **Build generation pipeline**: `TestGenerationEngine.createProcessor` wires schema merging, value providers, rules, and providers via
   `TestGeneratorConfigurer`.
6. **Generate suites per operation** (from `paths`):
    - `ValidCaseBuilder` constructs a baseline valid test case from operation parameters, request body, and security requirements.
    - `TestGenerationContext` wraps the valid case, OpenAPI model, and helper services (data providers, schema merger).
    - `ProviderOrchestrator` executes providers in fixed order (auth -> parameters -> request body).
    - Each provider applies rules to generate negative test cases expecting 400/401/403 status codes.
    - `OutcomeAggregator` collects provider results, merging test cases and errors.
    - If `includeValidCase` is enabled, the baseline valid case (with 2xx expected status) is prepended to the test list.
    - `TestCaseBudgetValidator` enforces per-operation limits on generated test cases.
    - Budgets and error handling determine whether the result is success, partial success, or failure.
   OpenAPI `webhooks` are parsed but are currently out of scope for suite generation.
7. **Report outcomes**: `GenerationReport` captures successes, partials, and failures with `GenerationError` metadata. `TestGenerationRunner` formats and logs
   the report via its reporter.
8. **Emit artifacts**: `TestGenerationEngine.createArtifactGenerator` builds a generator by id (`test-suite-writer` or `template`) and writes files.
9. **Return result**: `TestGenerationRunner` returns `TestGenerationResult.Success` or `TestGenerationResult.Failure` for CLI/Gradle to handle appropriately.

### Outputs

- `TestSuite`: operation-level container (`operationName` is the stable identifier).
- `TestCase`: individual scenario (request inputs + expected status/body).
- `GenerationReport`: aggregated suites + errors + summary (success/partial/failure/not-tested).

## Core Abstractions

### Provider-Rule Architecture Pattern

The generator separates **what to vary** (providers) from **how to vary** it (rules).

For each OpenAPI operation, `ValidCaseBuilder` constructs a baseline *valid* `TestCase` that includes only what is required: required parameters, first
supported request body media type, and security values. For required parameters, schema resolution uses `schema` first; if `schema` is absent, it falls back
to `content` schema. If both `schema` and `content` are defined for a parameter, `schema` is used and a warning is logged.

**Providers** decide what to vary for a given operation:

- Auth provider: derives auth-negative cases (missing/invalid credentials, scope variations).
- Parameter provider: derives parameter-negative cases (missing required params, schema violations).
- Request body provider: derives request-body-negative cases (missing body, schema violations).

Providers implement `TestCaseProvider<T>`, return `Outcome<List<TestCase>>`, and must be pure (no mutation of inputs). They are orchestrated in a fixed order
by `ProviderOrchestrator`.

**Rules** decide how to vary a specific schema or security constraint:

- `SchemaValidationRule` produces `Sequence<RuleValue>` (lazy, deterministic). A provider turns each `RuleValue` into a negative `TestCase`.
- `AuthValidationRule` produces `Sequence<TestCase>` because auth variations often span multiple request fields and expected status codes (401/403).

**Composition** for array/object schemas is handled through `ArrayItemSchemaValidationRule` and `ObjectItemSchemaValidationRule` using a `RuleContainer`.

**Registry**: `ManualRuleRegistry` wires `BuiltInRules` plus any extra rules, ensuring deterministic ordering.

### Generator Extensibility Model

- **Artifact generators** are created through `ArtifactGeneratorFactory` and `ArtifactGeneratorRegistry` using generator ids (`GeneratorIds`).
- **Built-in** generators are registered explicitly by `BuiltInGenerators` (currently `test-suite-writer`).
- **Feature modules** implement `TestGenerationModule` to contribute:
    - `ArtifactGeneratorFactory` implementations
    - `SchemaValueProvider` instances (by id)
    - Additional `SimpleSchemaValidationRule` and `AuthValidationRule` rules
- CLI and Gradle pass modules explicitly (e.g., `TemplateGeneratorModule`, `PatternSupportModule`) to enable optional capabilities.

### Configuration Resolution Chain

- **Inputs**:
    - YAML config (`GeneratorConfig`) loaded by `GeneratorConfigLoader`.
    - Environment overrides from CLI args or Gradle DSL (`TestGeneratorOverrides`).
- **Merge**:
    - `TestGeneratorExecutionOptionsFactory` merges overrides over config (overrides win).
    - Module-specific settings are extracted via `ModuleSettingsExtractor` before core settings parsing.
    - `TestGenerationSettings` and `ExampleValueSettings` supply defaults when values are missing.
- **Defaults**:
    - CLI and Gradle insert the `pattern` example-value provider into the default provider order before `plain-string`.
- **Note**: The merge is deep for nested maps in `generatorOptions` and `testGenerationSettings`. Collections are replaced (not merged), and map/non-map
  mismatches prefer the override value (no fail-fast).

### Budget Controls

OpenAPI schemas can be deeply nested or contain combinatorial compositions (`allOf`/`anyOf`/`oneOf`). Budget controls cap worst-case behavior and keep
generation tractable.

| Budget | Description |
|--------|-------------|
| `maxSchemaDepth` | Recursion limit when traversing schemas for validation |
| `maxMergedSchemaDepth` | Recursion limit when merging composed schemas into a flattened view |
| `maxSchemaCombinations` | Cap on composed-schema combinations (prevents `anyOf`/`oneOf` explosion) |
| `maxTestCasesPerOperation` | Maximum test cases generated for a single operation |
| `maxErrors` | Cap on collected errors when using `COLLECT_ALL` mode |

Defaults and types are documented in [Distribution settings](../reference/distribution-settings.md#budget-controls).

#### What happens when a budget is exceeded

Some budget violations are converted into structured generation errors (via provider boundaries), while others may stop generation for a specific operation
depending on where the limit is enforced. When a budget is exceeded, the error includes the operation path and method, the current count vs. the limit, and
suggested solutions (increase limit, simplify schema, or use ignore rules).

#### How to tune budgets

- **Increase budgets** when your schemas are legitimately complex and you need more coverage.
- **Decrease budgets** in CI when you prefer faster feedback.
- Consider using [ignore rules](../how-to/configuration.md#ignore-rules) for problematic operations instead of globally increasing limits.

For configuration keys, defaults, and where to set them (YAML vs CLI vs Gradle), see [Distribution settings](../reference/distribution-settings.md#budget-controls) and [YAML config](../how-to/configuration.md#yaml-configuration).

### Schema Processing

#### SchemaMerger

Handles OpenAPI schema composition (allOf, anyOf, oneOf):

- Flattens composed schemas into a single merged schema for validation.
- Preserves constraint semantics (e.g., tightest bounds win for min/max).
- Tracks recursion depth via `SchemaMergerOptions.maxMergedSchemaDepth`.
- Works with `CombinationBudget` to limit cartesian explosion.

#### Cycle Detection

- `SchemaStructureHasher` computes structural hashes to detect schema cycles.
- `TestGenerationContext` implementations track `visitedSchemaRefs` and `visitedStructures` to prevent infinite loops.
- Cycle detection is deterministic and does not rely on object identity.

### Example Value Generation

Providers are tried in configured order until one produces a value:

1. **schema-example**: Uses OpenAPI `example` or `examples` from the schema.
2. **pattern** (pattern-support module): Generates values matching regex patterns.
3. **plain-string**: Falls back to basic type-appropriate values.

`ExampleValueSettings.providers` controls the order. `SchemaExampleValueGeneratorFactory` wires the configured providers into a `SchemaExampleValueGenerator`.

## Module Deep-Dives

Per-module responsibilities and entry points are documented under [Modules](../modules/index.md).
Start there for the authoritative "what lives where" view, then jump into the module page for the area you're working on.

Common starting points:

- [core](../modules/core.md)
- [distribution-bundle](../modules/distribution-bundle.md)
- [plugin](../modules/index.md#plugin)
- [cli](../modules/index.md#cli)
- [example-value](../modules/index.md#example-value)
- [pattern-support](../modules/index.md#pattern-support)

## Cross-Cutting Concerns

### Ignore Configuration

Test case and rule filtering is controlled via `TestGenerationSettings`:

- **ignoreTestCases**: Map of exact paths to operation/test case filters. Supports wildcard path (`*`) and wildcard method (`*`); test case names are exact
  matches.
- **ignoreSchemaValidationRules**: Set of fully qualified schema rule class names to skip.
- **ignoreAuthValidationRules**: Set of fully qualified auth rule class names to skip.

`IgnoreConfigHandler` applies filters during generation:

1. Path matching: Filters operations by exact path or wildcard path.
2. Method matching: Filters by HTTP method within matched paths (case-insensitive; wildcard supported).
3. Test case matching: Filters individual test cases by exact name.

Filtering is applied deterministically after test case generation to ensure stable output.

### GraalVM / Native Image Compatibility

- CLI uses the GraalVM Native Build Tools plugin (`:cli:nativeCompile`).
- Rule and generator wiring is explicit (`BuiltInRules`, `BuiltInGenerators`, `TestGenerationModule`), avoiding reflection for discovery.
- Reflection still exists in specific areas: the template generator uses Mustache's `ReflectionObjectHandler`, and the Gradle plugin uses Kotlin reflection to
  copy settings.
- The OpenAPI parser and Jackson can require reflection configuration for native images (see CLI documentation for agent-based config generation).

### Determinism Guarantees

The generator produces stable outputs for the same inputs: the same set of test cases, in a stable order, with stable names and expected status codes.

- **Providers**: execute in a fixed order (auth → parameters → request body).
- **Rules**: registered and sorted deterministically (`BuiltInRules`, `ManualRuleRegistry`, `BuiltInGenerators`); ignore filters are applied after sorting.
- **Modules**: `TestGenerationEngine` validates and sorts modules by id and then class name to ensure stable ordering.
- **Value providers**: `ExampleValueSettings.providers` defines provider precedence; missing providers are logged, and fallback ordering is deterministic.
- **Writers**: `TestSuiteWriter` sorts suite keys and test cases during merge to stabilize output; merges preserve deterministic ordering.

#### Common pitfalls

- Unordered maps/sets in configuration can cause unstable output if iterated directly. Prefer stable iteration order or explicit sorting at boundaries.
- Time/randomness must not affect test case names or values.

### Error Handling Philosophy

- **Errors as values**: Most domain errors are captured as values, not thrown exceptions.
- **Outcome type hierarchy**:
    - `Outcome.Success<T>`: Operation completed with result.
    - `Outcome.PartialSuccess<T>`: Operation produced results but also encountered errors.
    - `Outcome.Failure`: Operation failed entirely with error list.
- **Error aggregation**: `OutcomeAggregator` merges provider results, accumulating test cases and
  errors. Final `Outcome` type depends on whether any test cases were produced.
- **Error boundary**: `runProviderSafely` wraps provider execution, converting uncaught exceptions to `Outcome.Failure` with meaningful context.
- **Error modes**: `ErrorHandlingConfig` controls behavior:
    - `ErrorMode.FAIL_FAST`: Stop on first error.
    - `ErrorMode.COLLECT_ALL`: Accumulate errors up to `maxErrors` limit (default 100).
- **Budget violations**: `BudgetExceededException` from provider logic (schema combinations/depth) is converted to `GenerationError` at provider boundaries.
  `TestCaseBudgetValidator` exceptions currently propagate unless caught by the caller.
- **Error context**: Every `GenerationError` includes `ErrorContext` with operation path, method, operationId, and optionally field/rule name for precise error
  location.
