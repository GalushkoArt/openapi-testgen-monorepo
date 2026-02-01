---
description: Definitions of key terms and concepts used throughout the OpenAPI Test Generator documentation, including providers, rules, outcomes, budgets, and core data types.
---

# Glossary

## Artifact generator

Component that emits outputs for generated suites (source code or JSON/YAML), based on `generator` selection. Implementations include `TemplateArtifactGenerator` (Mustache templates) and `TestSuiteWriter` (JSON/YAML files).

## Budget

Configured limit that prevents combinatorial explosion (schema depth, schema combinations, test case count). See [Budget controls](budget-controls.md).

## CombinationBudget

Tracks schema combination counts during generation to prevent explosion from nested `allOf`/`anyOf`/`oneOf` compositions. Throws `BudgetExceededException` when the limit is reached.

## ErrorContext

Hierarchical context attached to errors identifying the source location. Types include `Operation` (path/method), `Parameter` (name/location), and `RequestBody`.

## ErrorMode

Controls error collection behavior during generation:

- `FAIL_FAST`: Stop on the first error encountered
- `COLLECT_ALL`: Collect errors up to `maxErrors` limit (default: 100)

## GenerationError

Domain error captured during generation containing:

- `providerClass`: Fully qualified name of the provider that failed
- `message`: Human-readable error description
- `context`: `ErrorContext` identifying the location
- `exceptionText`: Optional stack trace for unexpected exceptions

## GenerationReport

Aggregated result across all operations containing:

- `successfulSuites`: List of generated `TestSuite` objects
- `errors`: List of `GenerationError` entries
- `summary`: Per-outcome operation lists (success, partial, failure, not-tested)

## Generator

An output backend identified by `generator` id. Built-in generators:

- `template`: Mustache-based code generation (Java/Kotlin tests)
- `test-suite-writer`: JSON/YAML file output

## ModuleSettingsExtractor

SPI interface for extracting module-specific settings from configuration maps before core settings parsing. Example: `PatternModuleSettingsExtractor` reads `patternGeneration` options.

## Outcome

Result type for generation operations:

- `Success<T>`: Operation completed with result, no errors
- `PartialSuccess<T>`: Operation produced results but also encountered errors
- `Failure`: Operation failed entirely with error list, no usable result

## OutcomeAggregator

Internal component that merges provider results, combining test cases and accumulating errors. Determines final `Outcome` type based on whether any test cases were produced.

## Provider

Component that derives negative test cases from a baseline valid case for a specific concern. Built-in providers:

- `AuthTestCaseProviderForOperation`: Security scheme violations
- `ParameterTestCaseProviderForOperation`: Parameter validation
- `RequestBodyTestCaseProviderForOperation`: Request body validation

## Rule

Component that encodes a specific OpenAPI constraint and produces invalid values or full negative test cases. Two types:

- `SchemaValidationRule`: Returns `RuleValue` entries (providers turn these into test cases)
- `AuthValidationRule`: Returns complete `TestCase` objects

## RuleContainer

Interface providing access to all validation rules. Used by composed rules (`ArrayItemSchemaValidationRule`, `ObjectItemSchemaValidationRule`) for recursive validation of nested schemas.

## SchemaMerger

Flattens composed schemas (`allOf`/`anyOf`/`oneOf`) into unified views for validation. Respects `maxMergedSchemaDepth` limit and works with `CombinationBudget` to control explosion.

## SchemaValueProvider

SPI interface for providing example values from OpenAPI schemas. Built-in providers include `PlainStringValueProvider`, `EnumValueProvider`, `DateValueProvider`. External providers like `PatternValueProvider` generate regex-matching values.

## Test case

A single scenario (`TestCase`) containing:

- Request inputs (path, query params, headers, cookies, body)
- Expected outcome (status code, description)
- Security values if applicable

## Test suite

A per-operation container (`TestSuite`) holding:

- Operation metadata (path, method, operationId)
- List of test cases for that operation
- Optional valid baseline case

## TestGenerationContext

Interface providing shared context during generation:

- OpenAPI model and current operation
- Valid case baseline
- Helper services (data providers, schema merger)
- Depth tracking and budget controls
- Cycle detection via visited schema refs

## ValidCaseBuilder

Constructs baseline valid `TestCase` instances from operation parameters, request body, and security requirements. Uses `SchemaExampleValueGenerator` to derive values from schema examples, patterns, or defaults.
