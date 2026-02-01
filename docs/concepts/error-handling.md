---
description: Describes the error handling strategy using explicit result types (Outcome, GenerationReport) to support best-effort behavior without hiding failures. Covers error modes, error aggregation, and the alwaysWriteTests option.
---

# Error handling

Generation uses explicit result types to support best-effort behavior without hiding failures.

## Outcome

`Outcome<T>` is the primary result type used internally:

- `Success`: result with no errors
- `PartialSuccess`: result plus errors (best-effort)
- `Failure`: errors only, no usable result

Providers should return `Outcome` rather than throwing.

## GenerationReport

Across the whole spec, results are aggregated into a `GenerationReport`:

- `successfulSuites`: generated suites
- `errors`: collected `GenerationError` entries
- `summary`: operation lists for success/partial/failure/not-tested

## Error modes

`ErrorMode` controls how the report builder behaves:

- `FAIL_FAST`: stop on the first error
- `COLLECT_ALL`: collect errors up to `maxErrors` (default: 100)

## alwaysWriteTests

By default, artifacts are written only when generation succeeds.
If you want outputs even on failures, enable `alwaysWriteTests` (CLI: `--always-write-test`).

## Example errors

### Budget exceeded error

When `maxSchemaCombinations` is exceeded during request body validation:

```json
{
  "providerClass": "art.galushko.openapi.testgen.providers.body.RequestBodySchemaValidationTestProvider",
  "message": "Budget exceeded for POST /partial-success:\n  Generated: 5 schema combinations\n  Limit: 3\n\nPossible solutions:\n  1. Increase the limit by setting testGenerationSettings.maxSchemaCombinations to a higher value\n  2. Simplify the OpenAPI schema by reducing nested oneOf/anyOf/allOf structures\n  3. Use ignoreTestCases or ignoreSchemaValidationRules to exclude specific scenarios\n",
  "context": {
    "operation": {
      "path": "/partial-success",
      "method": "POST",
      "operationId": "partialSuccessOperation"
    }
  },
  "exceptionText": null
}
```

This error occurs when complex composed schemas (`oneOf`/`anyOf`/`allOf`) generate more combinations than the configured limit. The error message includes actionable solutions.

### GenerationReport with partial success

When some operations succeed while others have errors:

```json
{
  "successfulSuites": [...],
  "errors": [...],
  "summary": {
    "totalOperations": 2,
    "successfulOperations": [
      { "operationId": "simpleOperation", "path": "/simple-operation", "method": "GET" }
    ],
    "partialOperations": [
      { "operationId": "partialSuccessOperation", "path": "/partial-success", "method": "POST" }
    ],
    "failedOperations": [],
    "notTestedOperations": [],
    "totalTestCases": 7,
    "totalErrors": 1
  },
  "hasErrors": true
}
```

The `summary` section provides a quick overview of which operations succeeded, partially succeeded, or failed entirely.

## Related docs

- [Model: Errors](../reference/model/errors.md) - Complete error type hierarchy
- [Budget controls](budget-controls.md) - Tuning budget limits
- [Troubleshooting](../how-to/troubleshooting.md) - Common error resolution
