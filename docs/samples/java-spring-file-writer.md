# Java Spring file writer sample

This sample demonstrates the `test-suite-writer` generator, which outputs test suites as JSON or YAML files instead of executable code. This is useful for data-driven testing frameworks or when you need to process test cases programmatically.

[View on GitHub](https://github.com/GalushkoArt/openapi-testgen-monorepo/tree/master/samples/java-spring-file-writer){ .md-button }

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
            "preventOverwriteSuites" to "false",
            "preventOverwriteCases" to "true",
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
| `preventOverwriteCases` | `true` | Don't overwrite existing test cases |
| `protectedTestCaseFields` | `expectedStatusCode,expectedBody` | Fields that won't be overwritten during merge |
| `indent` | `    ` | 4-space indentation for readability |

## Additional YAML generation

The sample also registers a second task that generates YAML format using a config file:

```kotlin
tasks.register<OpenApiTestGeneratorTask>("generateOpenApiTestsYaml") {
    configFile.set("open-api-test-generation-config.yaml")
    testGenerationSettings {
        validSecurityValues.putAll(mapOf("ApiKeyAuth" to "test-api-key-12"))
    }
}
```

See [`open-api-test-generation-config.yaml`](https://github.com/GalushkoArt/openapi-testgen-monorepo/blob/master/samples/java-spring-file-writer/open-api-test-generation-config.yaml) for the YAML config example.

## Output structure

The generated JSON file contains an array of test suites:

```json
[
  {
    "operationPath": "/users",
    "operationName": "listUsers",
    "httpMethod": "GET",
    "testCases": [
      {
        "name": "Valid request",
        "description": "Valid request for listUsers",
        "expectedStatusCode": 200,
        "headers": [
          { "key": "X-API-Key", "value": "test-api-key-123" }
        ],
        "queryParams": [],
        "pathParams": [],
        "cookies": [],
        "body": null
      },
      {
        "name": "Missing security: AllSecurityMissed",
        "description": "Test case without any security credentials",
        "expectedStatusCode": 401,
        "headers": [],
        ...
      }
    ]
  }
]
```

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
List<TestSuite> suites = mapper.readValue(
    new File("src/test/resources/openapi-test-suites.json"),
    new TypeReference<List<TestSuite>>() {}
);

for (TestSuite suite : suites) {
    for (TestCase testCase : suite.getTestCases()) {
        // Execute test case with your HTTP client
    }
}
```

## Merge behavior

With `writeMode: MERGE`:

1. **New test suites** are added to the file
2. **Existing test suites** are updated with new test cases
3. **Existing test cases** (matched by name) preserve their `protectedTestCaseFields`
4. **Manual edits** to `expectedStatusCode` or `expectedBody` survive regeneration

This allows you to:

- Generate initial test cases automatically
- Manually adjust expected values for your specific implementation
- Regenerate when the OpenAPI spec changes without losing manual edits

## Related docs

- [Test-suite-writer generator](../how-to/generators/test-suite-writer.md)
- [Gradle plugin reference](../reference/gradle-plugin.md)
- [YAML configuration](../how-to/configuration/yaml-config.md)
