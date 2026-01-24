# Include operations

Use `includeOperations` to generate tests only for selected paths and HTTP methods. This is the recommended approach when you need to target specific API operations.

## When to use

- Testing a single endpoint during development
- Generating tests for a subset of operations in a large spec
- Improving generation time by reducing scope

## Performance benefit

!!! tip "Filtering happens before generation"
    Unlike `ignoreTestCases` which filters **after** test case generation, `includeOperations`
    filters **before** generation. For large OpenAPI specifications, this significantly
    reduces generation time when targeting specific operations.

## Configuration

### YAML config

```yaml
# openapi-testgen.yaml
specFile: "samples/openapi.yaml"
outputDir: "./build/generated"
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
./gradlew :cli:installDist

./cli/build/install/openapi-testgen/bin/openapi-testgen \
  --config-file ./openapi-testgen.yaml
```

Using `--setting` flags (no config file):

```bash
./cli/build/install/openapi-testgen/bin/openapi-testgen \
  --spec-file samples/openapi.yaml \
  --output-dir ./build/generated \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json \
  --setting 'validSecurityValues.ApiKeyAuth=test-api-key-123' \
  --setting 'includeOperations./users/{userId}[]=GET'
```

!!! note "CLI list syntax"
    Use `key[]=value` to append to list values. Each `--setting 'includeOperations./path[]=METHOD'`
    adds a method to the list for that path.

Wildcard path example:

```bash
./cli/build/install/openapi-testgen/bin/openapi-testgen \
  --spec-file samples/openapi.yaml \
  --output-dir ./build/generated \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json \
  --setting 'includeOperations.*[]=GET'
```

### Gradle DSL

```kotlin
openApiTestGenerator {
    specFile.set(file("samples/openapi.yaml"))
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

## Configuration rules

| Rule | Description |
|------|-------------|
| Empty config | Generate tests for all operations (default) |
| Path matching | Exact match only (no globbing) |
| Method matching | Case-insensitive (`get`, `GET`, `Get` all match) |
| Method list | Must be non-empty; use a single string for shorthand (e.g., `"/users": "GET"`) |
| Wildcard path | Use `"*"` to match all paths |
| Wildcard method | Use `["*"]` to match all methods for a path |
| Precedence | Exact path entries take precedence over wildcard path |

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

Output location: `./build/generated/test-suites.json` (or the path specified in `outputDir`/`outputFileName`)

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

## Related docs

- [YAML config](yaml-config.md) - Full configuration reference
- [Ignore rules](ignore-rules.md) - Filter by exclusion
- [Distribution settings](../../reference/distribution-settings.md) - Default values
- [CLI reference](../../reference/cli.md) - Command-line options
- [Gradle plugin reference](../../reference/gradle-plugin.md) - Plugin DSL
