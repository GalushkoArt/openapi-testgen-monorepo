# Java Spring Rest-Assured Sample

Demonstrates using the Gradle plugin with the `template` generator to produce Rest-Assured Java
tests integrated with Spring Boot.

**What you'll learn:**
- Using the `template` generator with `restassured-java`
- Configuring template variables for package and base URL
- Task wiring with `compileTestJava`

## Quick Start

```bash
# Generate Rest-Assured tests
./gradlew :samples:java-spring-rest-assured:generateOpenApiTests

# Run tests (includes generation via wiring)
./gradlew :samples:java-spring-rest-assured:test
```

## Documentation

- [Getting Started](https://docs.galushko.art/openapi-test-generator/getting-started/)
- [Template Generator Guide](https://docs.galushko.art/openapi-test-generator/how-to/generators/template-generator/)
- [Gradle Plugin Reference](https://docs.galushko.art/openapi-test-generator/reference/gradle-plugin/)
- [Sample Walkthrough](https://docs.galushko.art/openapi-test-generator/samples/java-spring-rest-assured/)

## Requirements

- JDK 21
- Gradle (use wrapper from repo root)

## Project Structure

```
samples/java-spring-rest-assured/
├── build.gradle.kts                     # Plugin configuration
├── open-api-test-generation-config.yaml # YAML config example
├── src/test/java/                       # Generated from YAML config
└── build/generated/openapi-tests/       # Generated from default task
```

## Plugin Configuration

```kotlin
plugins {
    id("art.galushko.openapi-test-generator")
}

openApiTestGenerator {
    specFile.set(file("${rootDir}/samples/openapi.yaml").toURI().toString())
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests/art/galushko/java/spring/rest/assured"))
    generator.set("template")
    generatorOptions.putAll(mapOf(
        "templateSet" to "restassured-java",
        "templateVariables" to mapOf(
            "package" to "art.galushko.java.spring.rest.assured.generatedtests",
            "baseUrl" to "http://localhost:8080/v1",
            "springBootTest" to "true",
        ),
    ))
    testGenerationSettings {
        validSecurityValues.putAll(mapOf("ApiKeyAuth" to "test-api-key-123"))
        errorMode.set(ErrorMode.FAIL_FAST)
    }
}
```

## Key Features Demonstrated

### Template Variables

| Variable | Description |
|----------|-------------|
| `package` | Java package for generated test classes |
| `baseUrl` | Base URL for API requests |
| `springBootTest` | Enable Spring Boot test annotations |

### Task Wiring

The plugin automatically:
- Adds output directory to test source sets
- Wires `compileTestJava` to depend on `generateOpenApiTests`

Disable with `manualOnly.set(true)` for manual control.

## Available Tasks

| Task | Description |
|------|-------------|
| `generateOpenApiTests` | Generate tests to build directory |
| `generateOpenApiTestsToSrc` | Generate tests to src/test/java from config |
| `openApiGenerate` | Generate Spring server stubs (OpenAPI Generator) |

## Generated Output

Rest-Assured test classes with JUnit 5 annotations, ready to compile and run against your API.
