---
description: Configure the test-suite-writer generator to output test suites as JSON or YAML files. Covers output modes, merge behavior, and output structure, with links to the canonical option catalog.
---

# Test-suite-writer generator (`test-suite-writer`)

This page describes the built-in `test-suite-writer` generator and how it writes JSON/YAML `TestSuite` files.

For core entry points and extension context, see the [core module](../../modules/core.md).
For distribution defaults and wiring, see [distribution settings](../../reference/distribution-settings.md).
For configuration via YAML, see [YAML config](../configuration/yaml-config.md).

## Output behavior

`TestSuiteWriter` writes test suites as JSON or YAML. It is **stateful** and **not thread-safe**.
Create a new instance per generation run.

Output modes:

- `SINGLE_FILE`: aggregate all suites into a single file keyed by operation name.
- `MULTIPLE_FILES`: write one file per suite (prefix + operation name + extension).

Write modes:

- `writeMode = OVERWRITE`: overwrite output for suites generated in the current run.
- `writeMode = MERGE` (default): load existing output and merge by suite `operationName` and test case `name`.

Merge behavior (when `writeMode = MERGE`): see [Generator options](../../reference/catalogs/generator-options.md#test-suite-writer-options) for the canonical
semantics (suite/case overwrite flags, protected fields, and atomic writes).

## Options

See [Generator options](../../reference/catalogs/generator-options.md#test-suite-writer-options) for the authoritative list of supported options and defaults.

Commonly used options:

- `outputFileName` (for `SINGLE_FILE`)
- `format` (`JSON` / `YAML`)
- `writeMode` (`MERGE` / `OVERWRITE`)
- `outputMode` (`SINGLE_FILE` / `MULTIPLE_FILES`)

## Extending generators

To add a custom generator (new generator id), see [Custom generators](../extension/custom-generators.md) and [Custom modules](../extension/custom-modules.md).

## Output structure

The `test-suite-writer` generator outputs test suites keyed by operation name. Each suite contains the path, method, operation name, and an array of test cases.

### Example output

```json
{
  "getUser": {
    "path": "/users/{userId}",
    "method": "GET",
    "operationName": "getUser",
    "testCases": [
      {
        "name": "Invalid Path userId parameter: Invalid Pattern",
        "method": "GET",
        "path": "/users/{userId}",
        "pathParams": { "userId": "AE." },
        "headers": [{ "key": "X-API-Key", "value": "test-api-key-123" }],
        "expectedStatusCode": 400,
        "rule": "...InvalidPatternSchemaValidationRule"
      }
    ]
  }
}
```

!!! note "Query parameter encoding"
    The `queryParams` field uses structured values. Your test runner should serialize objects
    according to the parameter's `style` and `explode` settings from the OpenAPI spec.

For detailed field definitions, types, and complete examples, see:

- [TestSuite reference](../../reference/model/test-suite.md) - Suite structure and fields
- [TestCase reference](../../reference/model/test-case.md) - All test case fields including `securityValues`, `body`, `expectedBody`, and OAuth2 scope metadata

### Output location

- `SINGLE_FILE` mode: `{outputDir}/{outputFileName}` (e.g., `./build/generated/test-suites.json`)
- `MULTIPLE_FILES` mode: `{outputDir}/{fileNamePrefix}{operationName}.{format}` per operation

## Related docs

- Template generator module: [generator-template](../../modules/generator-template.md)
- [Include operations](../configuration/include-operations.md) - Target specific operations
