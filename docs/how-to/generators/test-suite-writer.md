# Test-suite-writer generator (`test-suite-writer`)

This page describes the built-in `test-suite-writer` generator and its options.

For core entry points and extension context, see the [core module](../../modules/core.md).
For distribution defaults and wiring, see [distribution settings](../../reference/distribution-settings.md).
For configuration via YAML, see [YAML config](../configuration/yaml-config.md).

## Overview

The `generator` package defines the artifact generation infrastructure and the built-in
`test-suite-writer` generator. Template-based code generation lives in the `generator-template`
module and is wired via `TestGenerationModule`.

## ArtifactGeneratorFactory

Factories implement `ArtifactGeneratorFactory`:

- `id` must be unique and stable.
- `description` is user-facing.
- `create(outputDir, options)` should validate options and return a fresh generator instance.

## ArtifactGeneratorRegistry

`ArtifactGeneratorRegistry`:

- Registers built-in factories from `BuiltInGenerators`.
- Accepts extra factories via constructor injection.
- Rejects duplicate IDs.
- Exposes `availableIds()` and `availableGenerators()` in sorted order.

`ArtifactGeneratorConfigurer` is a convenience for creating a generator from built-ins only.

## BuiltInGenerators and GeneratorIds

Built-in generators are listed explicitly in `BuiltInGenerators` (no reflection). IDs are defined
in `GeneratorIds`:

- `test-suite-writer`
- `template` (provided by `generator-template` module)

## TestSuiteWriter (built-in)

`TestSuiteWriter` writes test suites as JSON or YAML. It is **stateful** and **not thread-safe**.
Create a new instance per generation run.

Output modes:

- `SINGLE_FILE`: aggregate all suites into a single file keyed by operation name.
- `MULTIPLE_FILES`: write one file per suite (prefix + operation name + extension).

Merge behavior:

- `writeMode = MERGE` loads existing suites and merges by test case name.
- `preventOverwriteSuites` prevents overwriting existing suites; existing suites are kept as-is and
  no case updates or additions occur for them (default false).
- `preventOverwriteCases` prevents overwriting existing test cases; missing cases are added (default true).
  This works independently of `preventOverwriteSuites` - case-level protection applies even when
  suite overwrite is allowed.
- `protectedTestCaseFields` lists fields to preserve when overwriting existing cases; applies only
  when `preventOverwriteCases` is false.

Writes are atomic: data is written to a temp file and moved into place.

## TestSuiteWriter options

Parsed by `transformAndValidateWriterOptions` from a map:

- `outputMode`: `SINGLE_FILE` or `MULTIPLE_FILES` (default `SINGLE_FILE`; invalid values fall back).
- `outputFileName`: required for `SINGLE_FILE` (ignored for `MULTIPLE_FILES`).
- `format`: `JSON` or `YAML` (default `JSON`; invalid values fall back).
- `indent`: indentation string for JSON output (default 4 spaces).
- `writeMode`: `MERGE` (default) or `OVERWRITE` (invalid strings fall back to `MERGE`).
- `preventOverwriteSuites`: boolean (default `false` when unset or invalid string).
- `preventOverwriteCases`: boolean (default `true` when unset or invalid string).
- `protectedTestCaseFields`: list or comma-separated string (default empty; used when overwriting cases).
- `fileNamePrefix`: prefix for per-suite files (default empty).

Invalid types for `writeMode`, `preventOverwriteSuites`, `preventOverwriteCases`, or
`protectedTestCaseFields` throw `IllegalArgumentException`. Invalid strings generally fall back to
defaults.

## Extending generators

To add a custom generator:

1. Implement `ArtifactGeneratorFactory` and `ArtifactGenerator`.
2. Register the factory via `TestGenerationModule.artifactGeneratorFactories`.
3. Pass your module into `TestGenerationEngine.createArtifactGenerator` or `generateReport`.

## Output structure

The `test-suite-writer` generator outputs test suites keyed by operation name. Each suite contains the path, method, operation name, and an array of test cases.

### Example output

Running with `samples/openapi.yaml` targeting `GET /users/{userId}`:

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
        "queryParams": {},
        "pathParams": {
          "userId": "AE."
        },
        "headers": [
          {
            "key": "X-API-Key",
            "value": "test-api-key-123"
          }
        ],
        "cookie": [],
        "securityValues": {
          "queryParams": {},
          "headers": [
            {
              "key": "X-API-Key",
              "value": "test-api-key-123"
            }
          ],
          "cookie": [],
          "other": {}
        },
        "body": null,
        "expectedBody": {
          "code": "bad_request",
          "message": "Invalid input"
        },
        "needToComplete": false,
        "expectedStatusCode": 400,
        "rule": "art.galushko.openapi.testgen.pattern.support.InvalidPatternSchemaValidationRule"
      },
      {
        "name": "Invalid X-API-Key API key security",
        "method": "GET",
        "path": "/users/{userId}",
        "queryParams": {},
        "pathParams": {
          "userId": "wha_262laxjwhyaz8"
        },
        "headers": [
          {
            "key": "X-API-Key",
            "value": "unrealistic_key"
          }
        ],
        "cookie": [],
        "securityValues": {
          "queryParams": {},
          "headers": [
            {
              "key": "X-API-Key",
              "value": "unrealistic_key"
            }
          ],
          "cookie": [],
          "other": {}
        },
        "body": null,
        "expectedBody": {
          "code": "unauthorized",
          "message": "API key required"
        },
        "needToComplete": false,
        "expectedStatusCode": 401,
        "rule": "art.galushko.openapi.testgen.rules.auth.InvalidSecurityValuesAuthValidationRule"
      },
      {
        "name": "No security values provided",
        "method": "GET",
        "path": "/users/{userId}",
        "queryParams": {},
        "pathParams": {
          "userId": "wha_262laxjwhyaz8"
        },
        "headers": [],
        "cookie": [],
        "securityValues": {
          "queryParams": {},
          "headers": [],
          "cookie": [],
          "other": {}
        },
        "body": null,
        "expectedBody": {
          "code": "unauthorized",
          "message": "API key required"
        },
        "needToComplete": false,
        "expectedStatusCode": 401,
        "rule": "art.galushko.openapi.testgen.rules.auth.AllSecurityMissedAuthValidationRule"
      }
    ]
  }
}
```

When OAuth2 or OpenID Connect schemes are used, test cases include structured scope metadata in
`securityValues.other.authorizationScopes`. See [TestCase](../../reference/model/test-case.md) for
the exact shape and examples.

### Request body example

Excerpt from `samples/java-spring-file-writer/src/test/resources/openapi-test-suites.json` for `POST /orders`:

```json
{
  "name": "Incorrect Request Body: Object Property items Below Min Items Array",
  "method": "POST",
  "path": "/orders",
  "headers": [
    {
      "key": "X-API-Key",
      "value": "test-api-key-123"
    }
  ],
  "body": {
    "items": [],
    "userId": "a"
  },
  "expectedBody": {
    "code": "bad_request",
    "message": "Invalid input"
  },
  "expectedStatusCode": 400,
  "rule": "art.galushko.openapi.testgen.rules.composed.ObjectItemSchemaValidationRule"
}
```

Other fields (`queryParams`, `pathParams`, `cookie`, `securityValues`, `needToComplete`) are omitted here.

### Example output: Query parameter tests

Running with `core/src/test/resources/oas/circular.openapi.yaml` generates tests for query parameter
validation:

```json
{
  "listPersons": {
    "path": "/persons",
    "method": "GET",
    "operationName": "listPersons",
    "testCases": [
      {
        "name": "Missed Required Query Parameter person",
        "method": "GET",
        "path": "/persons",
        "queryParams": {},
        "expectedStatusCode": 400,
        "rule": "art.galushko.openapi.testgen.providers.parameter.MissedRequiredParameterTestProvider"
      },
      {
        "name": "Invalid Query person parameter: Missed Required Object Properties age",
        "method": "GET",
        "path": "/persons",
        "queryParams": {
          "person": {
            "name": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
          }
        },
        "expectedStatusCode": 400,
        "rule": "art.galushko.openapi.testgen.rules.schema.MissedRequiredObjectPropertiesSchemaValidationRule"
      }
    ]
  }
}
```

For a complete guide on query parameter tests, see [Query Parameter Validation Tests](../negative-testing/query-parameters.md).

!!! note "Query parameter encoding"
    The `queryParams` field uses structured values. Your test runner should serialize objects
    according to the parameter's `style` and `explode` settings from the OpenAPI spec.

### Test case fields

| Field | Description |
|-------|-------------|
| `name` | Human-readable test case name |
| `method` | HTTP method (GET, POST, etc.) |
| `path` | API path with parameter placeholders |
| `queryParams` | Query parameters to send |
| `pathParams` | Path parameter values |
| `headers` | Request headers |
| `cookie` | Cookie values |
| `securityValues` | Security material derived from OpenAPI security requirements |
| `body` | Request body (null for GET) |
| `expectedBody` | Expected response body example (map/list/string/primitive) resolved from response examples when available |
| `needToComplete` | Whether this generated case requires manual completion |
| `expectedStatusCode` | Expected HTTP status (400, 401, etc.; 0 = unspecified) |
| `rule` | Fully qualified class name of the rule that generated this case |

### Output location

- `SINGLE_FILE` mode: `{outputDir}/{outputFileName}` (e.g., `./build/generated/test-suites.json`)
- `MULTIPLE_FILES` mode: `{outputDir}/{fileNamePrefix}{operationName}.{format}` per operation

## Related docs

- Template generator module: [generator-template](../../modules/generator-template.md)
- [Include operations](../configuration/include-operations.md) - Target specific operations
