---
description: Quick-start guide for generating your first OpenAPI-derived tests with the CLI or Gradle plugin, including what the generated output looks like.
---

# Getting started

Use OpenAPI Test Generator when you want deterministic negative test cases that verify your API rejects invalid requests exactly where the OpenAPI contract says it should.

This guide shows the fastest way to get useful output, then points you to the deeper install and reference docs.

## 5-minute quick start

### Prerequisites

- An OpenAPI 3.0.x or 3.1.x specification
- Node.js 18+ for the npm CLI, or Java 21+ for the JVM distribution / Gradle plugin

!!! note "Webhook-only specs"
    Generation currently targets operations under `paths`. If your spec only defines `webhooks`, generation succeeds but produces zero suites. See [Troubleshooting](../how-to/troubleshooting.md#webhook-only-spec-produces-no-suites).

### CLI quick start

Install the npm package:

```bash
npm install -g @openapi-testgen/cli
openapi-testgen --help
```

Or run it without installing:

```bash
npx @openapi-testgen/cli --help
```

Generate JSON test suites:

```bash
openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option outputFileName=test-suites.json \
  --setting 'validSecurityValues.ApiKeyAuth=test-api-key-123'
```

Generate executable RestAssured tests:

```bash
openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./generated-tests \
  --generator template \
  --generator-option templateSet=restassured-java \
  --generator-option templateVariables.package=com.example.generated \
  --generator-option templateVariables.baseUrl=http://localhost:8080 \
  --setting 'validSecurityValues.ApiKeyAuth=test-api-key-123'
```

!!! tip "Security values"
    The `--setting 'validSecurityValues.<schemeName>=<value>'` flag provides valid credentials for auth-protected operations. The key must match a name under `components.securitySchemes` in your spec (e.g., `ApiKeyAuth`), not the header name (e.g., `X-API-Key`). Without it, the generator still produces auth-failure test cases but cannot generate valid-request cases for secured endpoints.

!!! tip "YAML config file"
    For projects with many options, use `--config-file openapi-testgen.yaml` instead of individual flags. CLI flags override config file values. See [Configuration](../how-to/configuration.md) for the config file format.

### Additional CLI flags

Beyond the core flags shown above, the CLI provides several useful options:

- `--log-level <level>`: set the log verbosity (ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF; default: INFO). Use `--log-level DEBUG` to troubleshoot generation issues.
- `--always-write-test`: force writing output files even when generation encounters errors. Useful for inspecting partial results; when files are written, the command still exits successfully and the report retains the errors.
- `--parser-setting <key=value>`: tune the OpenAPI parser (repeatable). For example, `--parser-setting yamlCodePointLimit=10000000` raises the YAML size limit for large specs.

!!! note "Setting separators"
    Multiple `--setting` values in a single flag are separated by **semicolons** (`;`), for example `--setting 'maxErrors=10;maxSchemaDepth=5'`. Multiple `--generator-option` and `--parser-setting` values in a single flag are separated by **commas** (`,`).

See the [CLI reference](../reference/cli.md) for the complete list of flags and nested option syntax.

## What the output looks like

### `test-suite-writer` output

The JSON/YAML writer produces `TestSuite` objects keyed by operation name:

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
                "expectedStatusCode": 400
            },
            {
                "name": "No security values provided",
                "method": "GET",
                "path": "/users/{userId}",
                "headers": [],
                "expectedStatusCode": 401
            }
        ]
    }
}
```

Each test case contains the invalid input, the request shape, and the expected failure status code.

### `template` output

The template generator writes Java or Kotlin source files under `outputDir`.

- Built-in template sets: `restassured-java`, `restassured-kotlin`
- Built-in RestAssured templates require `templateVariables.baseUrl`
- Request-body cases can emit `Content-Type` from `requestBodyMediaType`
- Response assertions use `expectedBody` when the generator can resolve one

See [Generators](../how-to/generators.md) for template customization and output options.

## Gradle quick start

```kotlin
plugins {
    id("art.galushko.openapi-test-generator") version "<version>"
}

openApiTestGenerator {
    specFile.set("src/test/resources/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests"))
    generator.set("template")
    generatorOptions.putAll(
        mapOf(
            "templateSet" to "restassured-java",
            "templateVariables" to mapOf(
                "package" to "com.example.generated",
                "baseUrl" to "http://localhost:8080",
            ),
        )
    )
    testGenerationSettings {
        validSecurityValues.put("ApiKeyAuth", "test-api-key-123")
    }
}
```

```bash
./gradlew generateOpenApiTests
```

By default, template output is wired into the test source set and `test-suite-writer` output is wired into test resources.

## What you can do next

- [Installation](installation.md) for npm, native, JVM, and plugin install paths
- [Gradle integration](gradle-integration.md) for build setup
- [Configuration](../how-to/configuration.md) for YAML, include/ignore rules, and security values
- [Negative testing](../how-to/negative-testing.md) for scenario coverage
- [Samples](../samples/index.md) for runnable example projects
- [CLI reference](../reference/cli.md) and [Gradle plugin reference](../reference/gradle-plugin.md) for exact flags and DSL fields
