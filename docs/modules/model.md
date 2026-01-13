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

# Model module

## Overview

`model` contains the shared data model types used across all modules (core, generators, CLI, and Gradle plugin).

## Key types

- `TestSuite`: a suite of `TestCase` objects produced per OpenAPI operation.
- `TestCase`: a single test scenario including request data and expected outcome.
- Error model types used for reporting generation failures and partial successes.

## API reference

- Dokka API reference: [`docs/api/model/index.html`](../api/model/index.html)

## Related documentation

- Reference: [TestSuite](../reference/model/test-suite.md)
- Reference: [TestCase](../reference/model/test-case.md)
- Reference: [errors](../reference/model/errors.md)

