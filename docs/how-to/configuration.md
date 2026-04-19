---
description: Configure test generation using YAML config files, CLI flags, and Gradle DSL. Covers operation filtering, ignore rules, security values, and positive testing.
---

# Configuration

Configure test generation using YAML config files, CLI flags, and Gradle DSL.

## YAML configuration

You can provide a YAML config file that mirrors execution options (used by both CLI and Gradle plugin).

The config filename is arbitrary; examples use `openapi-testgen.yaml`.

### Example config

```yaml
specFile: "openapi.yaml"
outputDir: "./build/generated"
generator: "test-suite-writer"
generatorOptions:
  outputFileName: "openapi-test-suites.json"
  writeMode: "MERGE"
testGenerationSettings:
  maxSchemaDepth: 50
  maxMergedSchemaDepth: 50
  maxSchemaCombinations: 100
  maxTestCasesPerOperation: 1000
  maxErrors: 100
  errorMode: "COLLECT_ALL"
  validSecurityValues:
    ApiKeyAuth: "test-key"
alwaysWriteTests: false
logLevel: "INFO"
```

### Using the config

#### CLI

```bash
openapi-testgen \
  --config-file ./openapi-testgen.yaml \
  --spec-file ./src/test/resources/openapi.yaml
```

#### Gradle plugin

```kotlin
openApiTestGenerator {
    configFile.set("openapi-testgen.yaml")
}
```

### Overrides and merge behavior

- Overrides win over config values.
- Nested maps are deep-merged; lists/arrays are replaced.

See [Distribution settings](../reference/distribution-settings.md) for defaults and precedence.

### Config schema (overview)

Top-level keys you'll typically use:

- `specFile` (required)
- `outputDir` (required)
- `generator` (required): `template` or `test-suite-writer`
- `generatorOptions` (optional): generator-specific options
- `testGenerationSettings` (optional): core generation settings (budgets, filters, security values, example generation)
- `alwaysWriteTests` (optional)
- `logLevel` (optional)

For the authoritative list of keys, defaults, and precedence rules, see [Distribution settings](../reference/distribution-settings.md).

### Generator options

`generatorOptions` is generator-specific.

See [Generators](generators.md) for generator-specific keys and behavior.

### Common next steps

- Target a subset of endpoints: [Include operations](#include-operations)
- Provide credentials for auth schemes: [Security values](#security-values)
- Exclude noisy endpoints/cases: [Ignore rules](#ignore-rules)
- Add the valid (2xx) baseline case: [Positive testing](#positive-testing)

---

## Include operations

Use `includeOperations` to generate tests only for selected paths and HTTP methods. This is the recommended approach when you need to target specific API operations.

### TL;DR - Target specific path and method

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

If you don't have the CLI installed yet, see [Installation](../getting-started/installation.md).

### When to use

- Testing a single endpoint during development
- Generating tests for a subset of operations in a large spec
- Improving generation time by reducing scope

### Performance benefit

!!! tip "Filtering happens before generation"
    `includeOperations` filters operations **before** generation. This is typically faster than
    filtering by test case names via `ignoreTestCases` (which happens after suite generation).

### Include configuration

#### YAML config

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

#### CLI

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

    For the canonical nested-key and list syntax, see [CLI reference - Settings](../reference/cli.md#settings).

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

#### Gradle DSL

Add `includeOperations` inside the `testGenerationSettings` block (see [Gradle integration](../getting-started/gradle-integration.md) for full plugin setup):

```kotlin
openApiTestGenerator {
    // ... specFile, outputDir, generator, generatorOptions ...
    testGenerationSettings {
        includeOperations.putAll(
            mapOf(
                "/users/{userId}" to listOf("GET")
            )
        )
    }
}
```

### Configuration rules (reference)

See [Distribution settings](../reference/distribution-settings.md#include-configuration) for the authoritative semantics: accepted shapes, wildcards, and precedence rules.

### Examples

#### Multiple methods on one path

```yaml
testGenerationSettings:
  includeOperations:
    "/users/{userId}": ["GET", "DELETE"]
```

#### Multiple paths

```yaml
testGenerationSettings:
  includeOperations:
    "/users/{userId}": ["GET"]
    "/orders": ["POST"]
```

#### All methods on a path (wildcard method)

```yaml
testGenerationSettings:
  includeOperations:
    "/users": ["*"]
```

#### Specific method on all paths (wildcard path)

```yaml
testGenerationSettings:
  includeOperations:
    "*": ["GET"]
```

#### Combine wildcards with specific entries

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

### Expected output

Running the CLI with `includeOperations` set to `"/users/{userId}": ["GET"]` produces output containing only the `getUser` operation.

!!! note "Output excerpt"
    The snippet below shows selected fields from the generated test cases. Fields like
    `queryParams`, `cookie`, `securityValues`, `requestBodyMediaType`, `expectedBody`,
    `responseBodyMediaType`, and `needToComplete` are omitted
    for brevity. The excerpt is from `samples/java-spring-file-writer/src/test/resources/openapi-test-suites.json`.
    See [Test-suite-writer](generators.md#test-suite-writer-generator) for full output details.

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

### Interaction with ignoreTestCases

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

### Verification

After generating, verify that only the targeted operations appear in the output:

```bash
# List operation keys
jq -c 'keys' ./generated/test-suites.json
# Expected (targeted): ["getUser"]
# Expected (all):      ["createOrder","createUser","getUser","listOrders","listUsers"]

# Verify test case names for a specific operation
jq '.getUser.testCases[].name' ./generated/test-suites.json
```

### Programmatic invocation (Kotlin)

Programmatic invocation uses `TestGenerationRunner.withDefaults(...)` with `TestGeneratorOverrides` so you can pass `includeOperations` dynamically.
The canonical runnable example (including dependency coordinates) lives in [Module: `distribution-bundle`](../modules/distribution-bundle.md#programmatic-invocation-kotlin).
Use `TestGenerationRunner.builder()` when you need custom modules, extractors, or settings.

---

## Ignore rules

Filter generated test cases by path, HTTP method, or test case name using the ignore configuration.

### Configuration location

- YAML: `testGenerationSettings.ignoreTestCases`
- Gradle: `testGenerationSettings { ignoreTestCases.putAll(...) }`
- CLI: `--setting ignoreTestCases.<path>.<method>[]=...` or `--setting ignoreTestCases.<path>=*`

### Ignore config structure

The ignore configuration uses a hierarchical pattern:

- Path key → `"*"`: ignore the entire path
- Path key → method → `"*"`: ignore all tests for a specific HTTP method
- Path key → method → list of names: ignore specific test cases by name (exact match only)

Notes:
- Path keys are matched exactly (no globbing). Use `*` as the wildcard path for all paths.
- Test case names are exact matches (no wildcards or regex).
- For the authoritative wildcard/merge behavior and warnings, see [Distribution settings](../reference/distribution-settings.md#ignore-configuration).

### Examples

#### Ignore entire path

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/api/internal": "*"
```

#### Ignore specific method

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/api/users":
      "DELETE": "*"
```

#### Ignore specific test cases

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/api/users":
      "GET":
        - "Missing required query param: role"
```

#### Wildcard path for all paths

```yaml
testGenerationSettings:
  ignoreTestCases:
    "*":
      "OPTIONS": "*"
```

#### Combine multiple filters

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/health": "*"
    "/api/admin": "*"
    "*":
      "OPTIONS": "*"
    "/api/pets/{petId}":
      "GET":
        - "Missing required path param: petId"
      "DELETE": "*"
```

### Alternative: target by exclusion

If `includeOperations` is not suitable (for example, you need to keep an existing config and only
exclude a few paths or tests), you can use `ignoreTestCases` to remove everything else.

!!! warning "Performance note"
    Path-level and method-level ignores can skip work before generation, but ignoring specific test case names happens after suite generation.
    Prefer `includeOperations` for inclusion-based targeting on large specs.

Notes:
- Test case name matching is exact only (no wildcards or regex).
- CLI list values use `[]` when ignoring specific test names (see [CLI reference - Settings](../reference/cli.md#settings)).

Example: keep only GET `/users/{userId}` by ignoring other paths:

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/users": "*"
    "/orders": "*"
    # /users/{userId} GET is NOT ignored (tests are generated)
```

CLI list syntax for specific test case names:

```bash
openapi-testgen \
  --setting 'ignoreTestCases./users/{userId}.GET[]=No security values provided'
```

!!! tip "Recommended approach"
    Use [`includeOperations`](#include-operations) to target specific operations directly.

---

## Security values

The generator uses `validSecurityValues` to build a baseline valid case (and to generate auth-related negative cases).

### YAML config

```yaml
testGenerationSettings:
  validSecurityValues:
    ApiKeyAuth: "test-api-key-123"
```

### CLI override

`--setting` keys map directly to `TestGenerationSettings` fields:

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./build/generated \
  --generator template \
  --setting validSecurityValues.ApiKeyAuth=test-api-key-123
```

### Gradle DSL

```kotlin
openApiTestGenerator {
    testGenerationSettings {
        validSecurityValues.put("ApiKeyAuth", "test-api-key-123")
    }
}
```

### OAuth2/OpenID Connect scope metadata

For OAuth2 and OpenID Connect schemes, generated test cases can include structured scope metadata in `securityValues.other.authorizationScopes`.

See [TestCase](../reference/model.md#authorizationscopes) for the canonical schema, examples, and presence rules.

---

## Positive testing

By default, the generator produces only negative test cases that validate error handling (4xx responses).
To also test that valid requests succeed with 2xx responses, enable the `includeValidCase` option.

### YAML
```yaml
testGenerationSettings:
    includeValidCase: true
```

### CLI
```bash
openapi-testgen --setting includeValidCase=true ...
```

### Gradle
```kotlin
openApiTestGenerator {
    testGenerationSettings {
        includeValidCase.set(true)
    }
}
```

### What you get

Enabling `includeValidCase` adds a single baseline positive test case (named "Test Valid Case") to each generated suite.
The expected status code is derived from the first 2xx response defined in the OpenAPI spec for the operation.

See: [Distribution settings - Output options](../reference/distribution-settings.md#output-options)

---

## Related docs

- [Budget controls](../concepts/architecture.md#budget-controls) - Tuning budget limits
- [Distribution settings](../reference/distribution-settings.md) - Default values and precedence
- [CLI reference](../reference/cli.md) - Flags and nested `--setting` / `--generator-option` syntax
- [Gradle plugin reference](../reference/gradle-plugin.md) - DSL fields and task wiring
