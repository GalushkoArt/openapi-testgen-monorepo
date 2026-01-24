# CLI reference

Command: `openapi-testgen`

## Installation

### npm (Recommended)

```bash
npm install -g @openapi-testgen/cli
openapi-testgen --help
```

### Download Binary

See [Installation Guide](../getting-started/installation.md) for all options including native binaries and building from source.

## Usage

```bash
openapi-testgen [options]
```

## Options

- `--help`, `-h`: show help
- `--version`, `-V`: show version
- `--config-file <path>`: path to YAML config file
- `--spec-file <path>`: path to OpenAPI spec file (YAML/JSON)
- `--output-dir <path>`: output directory for generated files
- `--generator <id>`: generator id (e.g. `template`, `test-suite-writer`)
- `--generator-option <key=value>`: generator option (repeatable). Supports dot notation for nested maps and `[]` for lists.
- `--setting <key.nested.path=value>`: test generation setting (repeatable). Supports dot notation for nested maps and `[]` for lists.
- `--always-write-test`: write artifacts even if generation fails (default: false)
- `--log-level <level>`: log level for generator logs (ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF; default: INFO)

## Nested option examples

### Template variables

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./build/generated-tests \
  --generator template \
  --generator-option templateVariables.package=com.example.generated \
  --generator-option templateVariables.baseUrl=http://localhost:8080
```

### List values via `[]`

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./build/generated-tests \
  --generator template \
  --setting exampleValues.providers[]=enum \
  --setting exampleValues.providers[]=const
```

### Example values options

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./build/generated-tests \
  --generator template \
  --setting exampleValues.includeOptionalExampleProperties=true \
  --setting exampleValues.includeWriteOnly=false \
  --setting exampleValues.useSchemaExampleFallback=true
```

### Target operations with includeOperations

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./build/generated-tests \
  --generator test-suite-writer \
  --generator-option outputFileName=generated.json \
  --setting 'includeOperations./users/{userId}[]=GET'
```

## Example: Generate tests for a single operation

This walkthrough generates tests for only `GET /users/{userId}` using the sample spec.

### Step 1: Create config file

```yaml
# openapi-testgen.yaml
specFile: "samples/openapi.yaml"
outputDir: "./build/test-single-operation"
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

### Step 2: Build and run CLI

```bash
# Build CLI distribution
./gradlew :cli:installDist

# Run with config file
./cli/build/install/openapi-testgen/bin/openapi-testgen \
  --config-file ./openapi-testgen.yaml
```

### Step 3: Verify output

The output file is at `./build/test-single-operation/test-suites.json`.

Expected content (excerpt; only `getUser` operation). Fields like `queryParams`, `cookie`,
`securityValues`, `expectedBody`, and `needToComplete` are omitted for brevity.
See [Test-suite-writer](../how-to/generators/test-suite-writer.md) for full output details.

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

### Alternative: CLI flags only (no config file)

```bash
./cli/build/install/openapi-testgen/bin/openapi-testgen \
  --spec-file samples/openapi.yaml \
  --output-dir ./build/test-single-operation \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json \
  --setting 'validSecurityValues.ApiKeyAuth=test-api-key-123' \
  --setting 'includeOperations./users/{userId}[]=GET'
```

For more details on targeting operations, see [Include operations](../how-to/configuration/include-operations.md).

## CI integration

### Using in shell scripts

```bash
#!/bin/bash
set -e

openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer
```

### Environment variables

YAML config files do not expand environment variables by themselves. Use CLI flags for overrides:

```bash
API_TEST_KEY=my-secret-key openapi-testgen \
  --config-file openapi-testgen.yaml \
  --setting "validSecurityValues.ApiKeyAuth=${API_TEST_KEY}"
```

Or use a wrapper script:

```bash
#!/bin/bash
openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --setting "validSecurityValues.ApiKeyAuth=${API_TEST_KEY:-default-key}"
```

## Exit codes

- `0`: success
- `1`: failure

## See also

- [Include operations](../how-to/configuration/include-operations.md) - Target specific operations
- [Ignore rules](../how-to/configuration/ignore-rules.md) - Filter by exclusion
- [CI/CD integration](../how-to/integration/ci-cd.md) - CI job wiring patterns
- [Test-suite-writer](../how-to/generators/test-suite-writer.md) - Output format details

## Distribution defaults

See [Distribution settings](distribution-settings.md) for the shared defaults and precedence
used by the CLI and Gradle plugin.
