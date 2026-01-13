# TestSuite

This document is non-normative and targets contributors. For core entry points, see
the [core module](../../modules/core.md).
For output formats and generator usage, see [Getting started](../../getting-started/index.md).

## Scope and responsibilities

The `model` module contains the canonical data classes shared across the system. It is data-only
and has no dependency on `core`. It is consumed by `core`, `example-value`, generators, and
reporting layers.

## Key types

### TestCase

See [TestCase](test-case.md).

### TestSuite

`TestSuite` groups cases for one operation. `operationName` is the stable identifier used by
`test-suite-writer` for keys and filenames. When `operationName` is null or blank, writers may skip
the suite.

### SecurityValues

`SecurityValues` stores security-only values (query/header/cookie) derived from OpenAPI security
requirements. It helps auth rules reason about security material separately from general
parameters.

### KeyValuePair

`KeyValuePair` preserves ordering for headers and cookies, where duplicates may exist.

## Serialization expectations

- Data classes are designed to preserve JSON shape for generators.
- Header/cookie ordering is preserved via `List<KeyValuePair<...>>`.
- `rule` and `operationName` are used as stable identifiers for output and traceability.

## How core uses these models

- `ValidCaseBuilder` creates the baseline `TestCase`.
- Providers derive invalid `TestCase` instances by changing a single field or security value.
- `TestSuiteWriter` uses `TestSuite.operationName` as the suite key or filename.
- Reporting aggregates `TestSuite` values into a `GenerationReport`.

See the [core module](../../modules/core.md) for the pipeline overview and
the [test-suite-writer generator](../../how-to/generators/test-suite-writer.md) for output behavior.

## Related docs

- Error model: [errors](errors.md)
- Modules: [model](../../modules/model.md), [core](../../modules/core.md)
