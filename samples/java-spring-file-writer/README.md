# Java Spring File Writer Sample

Demonstrates using the Gradle plugin with the `test-suite-writer` generator to produce JSON/YAML
test suites as test resources.

**What you'll learn:**
- Using the `test-suite-writer` generator
- Configuring merge mode and protected fields
- Task wiring with `processTestResources`

## Quick Start

```bash
# Generate JSON test suites
./gradlew :samples:java-spring-file-writer:generateOpenApiTests

# Run tests (includes generation via wiring)
./gradlew :samples:java-spring-file-writer:test
```

## Documentation

- [Getting Started](https://docs.galushko.art/openapi-test-generator/getting-started/)
- [Test Suite Writer Guide](https://docs.galushko.art/openapi-test-generator/how-to/generators/test-suite-writer/)
- [Gradle Plugin Reference](https://docs.galushko.art/openapi-test-generator/reference/gradle-plugin/)
- [Sample Walkthrough](https://docs.galushko.art/openapi-test-generator/samples/java-spring-file-writer/)

## Requirements

- JDK 21
- Gradle (use wrapper from repo root)

## Project Structure

```
samples/java-spring-file-writer/
├── build.gradle.kts                     # Plugin configuration
├── open-api-test-generation-config.yaml # YAML config example
└── src/test/resources/
    └── openapi-test-suites.json         # Generated output
```

## Plugin Configuration

```kotlin
plugins {
    id("art.galushko.openapi-test-generator")
}

openApiTestGenerator {
    specFile.set(file("${rootDir}/samples/openapi.yaml").toURI().toString())
    outputDir.set(layout.projectDirectory.dir("src/test/resources"))
    generator.set("test-suite-writer")
    generatorOptions.putAll(mapOf(
        "outputFileName" to "openapi-test-suites.json",
        "writeMode" to "MERGE",
        "preventOverwriteCases" to "true",
        "protectedTestCaseFields" to "expectedStatusCode,expectedBody",
    ))
    testGenerationSettings {
        ignoreTestCases.putAll(mapOf(
            "/orders" to mapOf("GET" to listOf("Invalid Query page parameter: Integer Breaking"))
        ))
        validSecurityValues.putAll(mapOf("ApiKeyAuth" to "test-api-key-123"))
        errorMode.set(ErrorMode.FAIL_FAST)
    }
}
```

## Key Features Demonstrated

### Merge Mode

With `writeMode: MERGE`, existing test cases are preserved and only new ones are added:
- Existing test cases with the same name are kept unchanged
- Protected fields (`expectedStatusCode`, `expectedBody`) are never overwritten
- Useful for maintaining manual test case adjustments

### Task Wiring

The plugin automatically wires `processTestResources` to depend on `generateOpenApiTests` when
the generator is `test-suite-writer`.

Disable with `manualOnly.set(true)` for manual control.

## Available Tasks

| Task | Description |
|------|-------------|
| `generateOpenApiTests` | Generate JSON suites (default task) |
| `generateOpenApiTestsYaml` | Generate YAML suites from config file |
| `openApiGenerate` | Generate Spring server stubs (OpenAPI Generator) |

## Generated Output

The `test-suite-writer` produces structured JSON/YAML that can be consumed by data-driven test
frameworks or custom test runners.
