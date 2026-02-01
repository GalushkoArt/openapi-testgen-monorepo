---
description: Reference documentation for error handling types in the model module, including Outcome, GenerationError, ErrorContext, GenerationReport, and BudgetExceededException.
---

# Model Error Types

This page is the schema reference for error payloads (`Outcome`, `GenerationError`, `ErrorContext`, `GenerationReport`, and budget exceptions) produced during generation.
It is also useful to contributors when debugging providers/rules and understanding how failures are reported.

See also: [Error handling](../../concepts/error-handling.md), the [core module](../../modules/core.md), and [distribution settings](../distribution-settings.md).

## Outcome

`Outcome<T>` is the result type used during generation:

- `Outcome.Success`: full result with no errors.
- `Outcome.PartialSuccess`: result plus errors (best-effort generation).
- `Outcome.Failure`: errors only, no usable result.

Providers should return `Outcome` rather than throwing, using `runProviderSafely`.

## GenerationError

`GenerationError` captures a provider failure:

- `providerClass`: fully qualified provider class name.
- `message`: human-readable error description.
- `context`: where the error occurred.
- `exceptionText`: optional stack trace for unexpected failures.

## ErrorContext

`ErrorContext` provides hierarchical context for error reporting:

- `Operation`: path, method, and optional operation id.
- `Parameter`: operation + parameter name/location + optional `$ref`.
- `RequestBody`: operation + optional `$ref`.

## GenerationReport

`GenerationReport` aggregates results across operations:

- `successfulSuites`: generated test suites.
- `errors`: all collected `GenerationError` instances.
- `summary`: `GenerationSummary` with per-outcome operation lists.
- `hasErrors`: convenience flag.

`OperationInfo` is the per-operation metadata stored in the summary lists.

## ErrorHandlingConfig and ErrorMode

`ErrorHandlingConfig` controls report building:

- `ErrorMode.FAIL_FAST`: stop on the first error.
- `ErrorMode.COLLECT_ALL`: collect errors up to `maxErrors`.

## BudgetExceededException

`BudgetExceededException` is thrown when a hard limit is exceeded:

- `BudgetType.SCHEMA_COMBINATIONS` -> `testGenerationSettings.maxSchemaCombinations`.
- `BudgetType.TEST_CASES_PER_OPERATION` -> `testGenerationSettings.maxTestCasesPerOperation`.

Some budget violations are converted to `Outcome.Failure` at provider boundaries, while others
(such as per-operation test case limits) may propagate unless callers handle them explicitly.

See the [core module](../../modules/core.md) for where these errors are handled.
