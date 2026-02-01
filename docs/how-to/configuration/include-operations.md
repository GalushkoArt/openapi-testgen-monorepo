---
description: Generate tests for specific API operations using includeOperations. Filter by path and HTTP method to target individual endpoints, improving generation speed for large OpenAPI specifications.
---

# Include operations

Use `includeOperations` to generate tests only for selected paths and HTTP methods. This is the recommended approach when you need to target specific API operations.

## TL;DR - Target specific path and method

Generate tests for only specific operations:

```bash
# Generate tests for GET /users/{userId} only
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json \
  --setting 'includeOperations./users/{userId}[]=GET'
```

If you don’t have the CLI installed yet, see [Installation](../../getting-started/installation.md) (or run via `npx` from [npm Installation](../../getting-started/npm-installation.md)).

## When to use

- Testing a single endpoint during development
- Generating tests for a subset of operations in a large spec
- Improving generation time by reducing scope

## Performance benefit

!!! tip "Filtering happens before generation"
    `includeOperations` filters operations **before** generation. This is typically faster than
    filtering by test case names via `ignoreTestCases` (which happens after suite generation).

## Configuration

### YAML config

```yaml
# openapi-testgen.yaml
specFile: "./openapi.yaml"
outputDir: "./generated"
generator: "test-suite-writer"
generatorOptions:
  format: "json"
  outputFileName: "test-suites.json"

testGenerationSettings:
  validSecurityValues:
    ApiKeyAuth: "test-api-key-123"
  includeOperations:
    "/users/{userId}": ["GET"]
```

### CLI

Using a config file:

```bash
openapi-testgen --config-file ./openapi-testgen.yaml
```

Using `--setting` flags (no config file):

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json \
  --setting 'validSecurityValues.ApiKeyAuth=test-api-key-123' \
  --setting 'includeOperations./users/{userId}[]=GET'
```

!!! note "CLI list syntax"
    Use `key[]=value` to append to list values. Each `--setting 'includeOperations./path[]=METHOD'` adds a method to the list for that path.

    For the canonical nested-key and list syntax, see [CLI reference - Settings](../../reference/cli.md#settings).

Wildcard path example:

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json \
  --setting 'includeOperations.*[]=GET'
```

### Gradle DSL

```kotlin
openApiTestGenerator {
    specFile.set(file("openapi.yaml"))
    outputDir.set(file("build/generated-tests"))
    generator.set("test-suite-writer")
    generatorOptions.putAll(
        mapOf(
            "format" to "json",
            "outputFileName" to "test-suites.json"
        )
    )

    testGenerationSettings {
        validSecurityValues.put("ApiKeyAuth", "test-api-key-123")
        includeOperations.putAll(
            mapOf(
                "/users/{userId}" to listOf("GET")
            )
        )
    }
}
```

## Configuration rules (reference)

See [Distribution settings](../../reference/distribution-settings.md#include-configuration) for the authoritative semantics: accepted shapes, wildcards, and precedence rules.

## Examples

### Multiple methods on one path

```yaml
testGenerationSettings:
  includeOperations:
    "/users/{userId}": ["GET", "DELETE"]
```

### Multiple paths

```yaml
testGenerationSettings:
  includeOperations:
    "/users/{userId}": ["GET"]
    "/orders": ["POST"]
```

### All methods on a path (wildcard method)

```yaml
testGenerationSettings:
  includeOperations:
    "/users": ["*"]
```

### Specific method on all paths (wildcard path)

```yaml
testGenerationSettings:
  includeOperations:
    "*": ["GET"]
```

### Combine wildcards with specific entries

```yaml
testGenerationSettings:
  includeOperations:
    "/users/{userId}": ["GET", "DELETE"]  # Specific methods for this path
    "*": ["OPTIONS"]                       # OPTIONS for all other paths
```

!!! warning "Exact path takes precedence"
    When both an exact path and wildcard path are configured, the exact path entry
    takes precedence. In the example above, `/users/{userId}` will only have GET and DELETE
    tests generated (not OPTIONS), while all other paths will have OPTIONS tests.

## Expected output

Running the CLI with `includeOperations` set to `"/users/{userId}": ["GET"]` produces output containing only the `getUser` operation.

!!! note "Output excerpt"
    The snippet below shows selected fields from the generated test cases. Fields like
    `queryParams`, `cookie`, `securityValues`, `expectedBody`, and `needToComplete` are omitted
    for brevity. The excerpt is from `samples/java-spring-file-writer/src/test/resources/openapi-test-suites.json`.
    See [Test-suite-writer](../generators/test-suite-writer.md) for full output details.

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
        "pathParams": {
          "userId": "AE."
        },
        "headers": [
          {
            "key": "X-API-Key",
            "value": "test-api-key-123"
          }
        ],
        "expectedStatusCode": 400,
        "rule": "art.galushko.openapi.testgen.pattern.support.InvalidPatternSchemaValidationRule"
      },
      {
        "name": "Invalid X-API-Key API key security",
        "method": "GET",
        "path": "/users/{userId}",
        "pathParams": {
          "userId": "wha_262laxjwhyaz8"
        },
        "headers": [
          {
            "key": "X-API-Key",
            "value": "unrealistic_key"
          }
        ],
        "expectedStatusCode": 401,
        "rule": "art.galushko.openapi.testgen.rules.auth.InvalidSecurityValuesAuthValidationRule"
      },
      {
        "name": "No security values provided",
        "method": "GET",
        "path": "/users/{userId}",
        "pathParams": {
          "userId": "wha_262laxjwhyaz8"
        },
        "headers": [],
        "expectedStatusCode": 401,
        "rule": "art.galushko.openapi.testgen.rules.auth.AllSecurityMissedAuthValidationRule"
      }
    ]
  }
}
```

Output location: `./generated/test-suites.json` (or the path specified in `outputDir`/`outputFileName`)

## Interaction with ignoreTestCases

When both `includeOperations` and `ignoreTestCases` are configured:

1. `includeOperations` filters first (before generation)
2. `ignoreTestCases` filters the generated results (after generation)

Example: Generate only GET `/users/{userId}` but skip auth tests:

```yaml
testGenerationSettings:
  includeOperations:
    "/users/{userId}": ["GET"]
  ignoreTestCases:
    "/users/{userId}":
      "GET":
        - "Invalid X-API-Key API key security"
        - "No security values provided"
```

## Before vs After Verification

Verify that `includeOperations` filters to only the targeted operation.

### Without filtering (all operations)

```bash
npx @openapi-testgen/cli \
  --spec-file openapi.yaml \
  --output-dir ./generated-all \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json

# List generated operations
jq -c 'keys' ./generated-all/test-suites.json
```

Output (all operations):

```json
["createOrder","createUser","getUser","listOrders","listUsers"]
```

### With filtering (targeted operation)

```bash
npx @openapi-testgen/cli \
  --spec-file openapi.yaml \
  --output-dir ./generated-targeted \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json \
  --setting 'includeOperations./users/{userId}[]=GET'

# Verify only getUser is generated
jq -c 'keys' ./generated-targeted/test-suites.json
```

Output (only targeted operation):

```json
["getUser"]
```

### Verify test case names

```bash
jq '.getUser.testCases[].name' ./generated-targeted/test-suites.json
```

Output:

```json
"Invalid Path userId parameter: Invalid Pattern"
"Invalid X-API-Key API key security"
"No security values provided"
```

## Programmatic Invocation (Kotlin)

Programmatic invocation uses `TestGenerationRunner.withDefaults(...)` with `TestGeneratorOverrides` so you can pass `includeOperations` dynamically.
The canonical runnable example (including dependency coordinates) lives in [Module: `distribution-bundle`](../../modules/distribution-bundle.md#programmatic-invocation-kotlin).
Use `TestGenerationRunner.builder()` when you need custom modules, extractors, or settings.

## Related docs

- [YAML config](yaml-config.md) - Config file structure and precedence
- [Ignore rules](ignore-rules.md) - Filter by exclusion
- [Distribution settings](../../reference/distribution-settings.md) - Default values
- [CLI reference](../../reference/cli.md#settings) - CLI settings syntax
- [Gradle plugin reference](../../reference/gradle-plugin.md#test-generation-settings) - Gradle testGenerationSettings DSL
