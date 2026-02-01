---
description: Step-by-step guide to running negative test generation using the CLI or Gradle plugin. Covers JSON suite generation, executable test output, security values configuration, and CI/CD integration.
---

# Running negative generation (CLI + Gradle)

Use the negative-testing guides to understand *what* gets generated. Use this page to consistently reproduce examples and run them in a build.

## Choose an output type

- **Executable tests**: use the `template` generator (Gradle plugin recommended). Generated sources are compiled and run by `./gradlew test`.
- **Data-driven suites**: use the `test-suite-writer` generator (CLI or Gradle). Output is `.json` / `.yaml` consumed by your own runner.

## CLI: generate JSON suites

Prerequisite: have `openapi-testgen` available (install it or run via `npx`). See [Installation](../../getting-started/installation.md) and [npm Installation](../../getting-started/npm-installation.md).

Generate JSON suites (single-file mode):

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json
```

### Target a specific operation (recommended for large specs)

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json \
  --setting 'includeOperations./users/{userId}[]=GET'
```

### Provide security values (avoid placeholders)

`validSecurityValues` keys are **security scheme names** (from `components.securitySchemes`), not header names:

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json \
  --setting 'validSecurityValues.ApiKeyAuth=test-api-key-123'
```

### Verify output

```bash
# List operation keys (SINGLE_FILE output)
jq 'keys' generated/test-suites.json

# Count all test cases
jq '[.[] | .testCases[]] | length' generated/test-suites.json
```

## Gradle plugin: executable tests (template generator)

Generate and run RestAssured JUnit tests as part of your build:

```kotlin
openApiTestGenerator {
    specFile.set(file("openapi.yaml"))
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests"))
    generator.set("template")
    generatorOptions.putAll(
        mapOf(
            "templateSet" to "restassured-java",
            "templateVariables" to mapOf(
                "package" to "com.example.generated.tests",
                "baseUrl" to (System.getenv("API_BASE_URL") ?: "http://localhost:8080"),
            )
        )
    )
    testGenerationSettings {
        validSecurityValues.put("ApiKeyAuth", System.getenv("API_TEST_KEY") ?: "test-api-key-123")
    }
}
```

Run:

```bash
./gradlew test
```

## Gradle plugin: JSON suites (test-suite-writer)

```kotlin
openApiTestGenerator {
    specFile.set(file("openapi.yaml"))
    outputDir.set(layout.buildDirectory.dir("generated/test-suites"))
    generator.set("test-suite-writer")
    generatorOptions.putAll(
        mapOf(
            "format" to "json",
            "outputFileName" to "test-suites.json"
        )
    )
}
```

If you split generation and tests into separate jobs, see [CI/CD integration](../integration/ci-cd.md) for wiring the output into the build.

## Related docs

- [CLI reference](../../reference/cli.md)
- [Gradle plugin reference](../../reference/gradle-plugin.md)
- [Include operations](../configuration/include-operations.md)
- [Security values](../configuration/security-values.md)
