---
description: The model module contains the canonical data structures used across all modules, including TestSuite, TestCase, Outcome, and error reporting types. It has no dependencies and is the foundation of the module hierarchy.
---

# Module: `model`

The `model` module contains the **canonical data structures** produced by generation and consumed by generators (codegen, test-suite writer, reporting).

## Depends on

- None

## Used by

- `example-value`, `core`, `distribution-bundle`, `plugin`, `cli`, and feature modules

## Key types

- `TestSuite`: operation-level container for generated test cases
- `TestCase`: a single generated case (request data + expected status/body + metadata)
- `Outcome<T>`: success / partial success / failure value type
- `GenerationReport` / `GenerationError`: structured reporting payloads
- `ErrorMode` / `ErrorHandlingConfig`: fail-fast vs collect-all behavior

## Related docs

- Model reference:
  - [TestSuite](../reference/model/test-suite.md)
  - [TestCase](../reference/model/test-case.md)
  - [Errors](../reference/model/errors.md)
- Concepts:
  - [Error handling](../concepts/error-handling.md)

## API reference

- Dokka API reference: [`docs/api/model/index.html`](../api/model/index.html)

