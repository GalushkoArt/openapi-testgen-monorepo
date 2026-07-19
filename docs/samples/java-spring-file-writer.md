---
description: Demonstrates the test-suite-writer generator for outputting test suites as JSON or YAML files. Shows MERGE mode for preserving manual edits and protected fields when regenerating.
---

# Java Spring file writer sample

This sample demonstrates the `test-suite-writer` generator, which outputs test suites as JSON or YAML files instead of executable code. This is useful for data-driven testing frameworks or when you need to process test cases programmatically.

[View on GitHub](https://github.com/GalushkoArt/openapi-testgen-monorepo/tree/main/samples/java-spring-file-writer){ .md-button }

## Overview

| Property | Value |
|----------|-------|
| Module | `samples:java-spring-file-writer` |
| Generator | `test-suite-writer` |
| Output format | JSON (with YAML variant) |
| Output location | `src/test/resources/` |

## What it demonstrates

- Using the `test-suite-writer` generator to produce JSON test suites
- **MERGE mode**: Preserving manual edits when regenerating
- **Protected fields**: Preventing overwrite of specific test case fields
- **Multiple generation tasks**: JSON and YAML outputs from the same spec
- Reading test suites at runtime with the `model` library

## Plugin configuration

```kotlin
openApiTestGenerator {
    specFile.set(file("${rootDir}/samples/openapi.yaml").toURI().toString())
    outputDir.set(layout.projectDirectory.dir("src/test/resources"))
    generator.set("test-suite-writer")

    generatorOptions.putAll(
        mapOf(
            "outputFileName" to "openapi-test-suites.json",
            "writeMode" to "MERGE",
            "preventOverwriteSuites" to false,
            "preventOverwriteCases" to false,
            "protectedTestCaseFields" to "expectedStatusCode,expectedBody",
            "indent" to "    ",
        )
    )

    testGenerationSettings {
        ignoreTestCases.putAll(mapOf(
            "/orders" to mapOf("GET" to listOf("Invalid Query page parameter: Integer Breaking"))
        ))
        validSecurityValues.putAll(mapOf("ApiKeyAuth" to "test-api-key-123"))
        overrideBasicTestData.putAll(mapOf("invalidApiKey" to "unrealistic_key"))
        maxSchemaDepth.set(15)
        errorMode.set(ErrorMode.FAIL_FAST)
    }
}
```

### Key generator options

| Option | Value | Description |
|--------|-------|-------------|
| `outputFileName` | `openapi-test-suites.json` | Name of the output file |
| `writeMode` | `MERGE` | Merge with existing file instead of overwriting |
| `preventOverwriteCases` | `false` | Overwrite existing test cases |
| `protectedTestCaseFields` | `expectedStatusCode,expectedBody` | Fields preserved when overwriting cases |
| `indent` | `    ` | 4-space indentation for readability |

## Additional YAML generation

The sample also registers a second task that generates YAML format using a config file:

Note: this sample uses `open-api-test-generation-config.yaml`, but the filename is arbitrary.

```kotlin
tasks.register<OpenApiTestGeneratorTask>("generateOpenApiTestsYaml") {
    configFile.set(layout.projectDirectory.file("open-api-test-generation-config.yaml"))
    testGenerationSettings {
        validSecurityValues.putAll(mapOf("ApiKeyAuth" to "test-api-key-12"))
    }
}
```

See [`open-api-test-generation-config.yaml`](https://github.com/GalushkoArt/openapi-testgen-monorepo/blob/main/samples/java-spring-file-writer/open-api-test-generation-config.yaml) for the YAML config example.
Its output is kept under `build/generated/test-suites/yaml` so the task does not declare the
project source tree as generated output.

## Output structure

In `SINGLE_FILE` mode (default), the generated JSON file is a map keyed by `operationName`:

```json
{
  "listUsers": {
    "path": "/users",
    "method": "GET",
    "operationName": "listUsers",
    "testCases": [
      {
        "name": "No security values provided",
        "method": "GET",
        "path": "/users",
        "expectedStatusCode": 401
      }
    ]
  }
}
```

For field definitions and the canonical schema, see:

- Reference: [Model](../reference/model.md) (TestSuite, TestCase)

## Running the tests

The sample includes a parameterized test that reads the JSON file and executes each test case:

```bash
# Generate test suites
./gradlew :samples:java-spring-file-writer:generateOpenApiTests

# Generate YAML variant
./gradlew :samples:java-spring-file-writer:generateOpenApiTestsYaml

# Run all tests (generation is wired to processTestResources)
./gradlew :samples:java-spring-file-writer:test
```

## Using test suites at runtime

The sample depends on `openapi.testgen.model` to deserialize test suites:

```kotlin
dependencies {
    implementation("art.galushko.openapi.testgen:model")
}
```

```java
ObjectMapper mapper = new ObjectMapper();
Map<String, TestSuite> suites = mapper.readValue(
    new File("src/test/resources/openapi-test-suites.json"),
    new TypeReference<Map<String, TestSuite>>() {}
);

for (Map.Entry<String, TestSuite> entry : suites.entrySet()) {
    TestSuite suite = entry.getValue();
    for (TestCase testCase : suite.getTestCases()) {
        // Execute test case with your HTTP client
    }
}
```

## Merge behavior

With `writeMode: MERGE`, the writer can preserve existing suites/cases and protect manual edits via `preventOverwrite*` flags and `protectedTestCaseFields`.
For the canonical merge semantics and edge cases, see [Generators](../how-to/generators.md#merge-semantics).

## Related docs

- [Test-suite-writer generator](../how-to/generators.md#test-suite-writer-generator)
- [Gradle plugin reference](../reference/gradle-plugin.md)
- [YAML configuration](../how-to/configuration.md#yaml-configuration)
