---
description: Overview of sample projects demonstrating OpenAPI Test Generator integration with different generators, languages, and configurations including Spring Boot, RestAssured, and data-driven testing approaches.
---

# Samples

The repository includes runnable sample projects demonstrating how to integrate OpenAPI Test Generator with different generators and configurations.

[View samples on GitHub](https://github.com/GalushkoArt/openapi-testgen-monorepo/tree/main/samples){ .md-button }

## Available samples

| Sample | Generator | Language | Description |
|--------|-----------|----------|-------------|
| [Java Spring file writer](java-spring-file-writer.md) | `test-suite-writer` | Java | JSON/YAML test suites as test resources |
| [Java Spring RestAssured](java-spring-rest-assured.md) | `template` | Java | Generated RestAssured test classes |
| [Kotlin Spring RestAssured](kotlin-spring-rest-assured.md) | `template` | Kotlin | Generated tests with custom templates |

## Common setup

All samples share:

- **Spring Boot 3.x** with validation
- **Java 21** toolchain
- **OpenAPI Generator** for server-side Spring interfaces
- **Shared OpenAPI spec**: [`samples/openapi.yaml`](https://github.com/GalushkoArt/openapi-testgen-monorepo/blob/main/samples/openapi.yaml)

The shared spec defines a simple Users & Orders API with:

- 5 operations across 2 services
- API key authentication (`X-API-Key` header)
- Pagination, validation constraints, and enum types

## Running samples

From the repository root:

```bash
# Generate tests for a specific sample
./gradlew :samples:java-spring-file-writer:generateOpenApiTests

# Run tests (includes generation via task wiring)
./gradlew :samples:java-spring-rest-assured:test

# Run all sample tests
./gradlew samples:test
```

## Sample structure

Each sample follows this structure:

```
samples/<sample-name>/
├── build.gradle.kts              # Plugin configuration
├── open-api-test-generation-config.yaml  # Optional YAML config (filename is arbitrary)
├── src/
│   ├── main/kotlin|java/         # Spring Boot application
│   └── test/
│       ├── kotlin|java/          # Test utilities and base classes
│       └── resources/            # Test resources (for file-writer)
└── templates/                    # Custom Mustache templates (Kotlin sample)
```

## Key configuration patterns

### Test generation settings

All samples demonstrate common settings:

```kotlin
testGenerationSettings {
    // Skip specific test cases
    ignoreTestCases.putAll(mapOf(
        "/orders" to mapOf("GET" to listOf("Invalid Query page parameter: Integer Breaking"))
    ))

    // Provide valid security values
    validSecurityValues.putAll(mapOf("ApiKeyAuth" to "test-api-key-123"))

    // Override default test data
    overrideBasicTestData.putAll(mapOf("invalidApiKey" to "unrealistic_key"))

    // Budget controls
    maxSchemaDepth.set(15)
    maxSchemaCombinations.set(100)
    maxTestCasesPerOperation.set(1000)
    errorMode.set(ErrorMode.FAIL_FAST)
}
```

### Multiple generation tasks

Samples show how to register additional generation tasks:

```kotlin
// Additional task with YAML config (filename is arbitrary)
tasks.register<OpenApiTestGeneratorTask>("generateOpenApiTestsYaml") {
    configFile.set("open-api-test-generation-config.yaml")
}

// Wire into build
tasks.named("compileTestJava") { dependsOn("generateOpenApiTestsYaml") }
```
