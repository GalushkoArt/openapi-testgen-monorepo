# OpenAPI Test Generator Gradle Plugin

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/art.galushko.openapi-test-generator)](https://plugins.gradle.org/plugin/art.galushko.openapi-test-generator)

Gradle plugin for generating API tests from OpenAPI specifications.

## Installation

```kotlin
plugins {
    id("art.galushko.openapi-test-generator") version "0.9.0"
}
```

[View on Gradle Plugin Portal](https://plugins.gradle.org/plugin/art.galushko.openapi-test-generator)

## Quick Start

```kotlin
openApiTestGenerator {
    specFile.set("src/main/resources/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests"))
    generator.set("template")
    generatorOptions.putAll(mapOf(
        "templateSet" to "restassured-java",
        "templateVariables" to mapOf(
            "package" to "com.example.generated",
            "baseUrl" to "http://localhost:8080",
        ),
    ))
}
```

```bash
./gradlew generateOpenApiTests
```

## Configuration

### Basic Options

| Property | Type | Description |
|----------|------|-------------|
| `specFile` | `String` | Path or URI to OpenAPI spec (required) |
| `outputDir` | `Directory` | Output directory for generated files |
| `generator` | `String` | Generator id: `template` or `test-suite-writer` |
| `generatorOptions` | `Map` | Generator-specific options |
| `configFile` | `String` | Optional YAML config file path |
| `alwaysWriteTests` | `Boolean` | Write output even on errors (default: `false`) |
| `manualOnly` | `Boolean` | Disable automatic task wiring (default: `false`) |
| `logLevel` | `String` | Log level: TRACE, DEBUG, INFO, WARN, ERROR, OFF |

### Test Generation Settings

```kotlin
openApiTestGenerator {
    testGenerationSettings {
        // Budget controls
        maxSchemaDepth.set(50)
        maxSchemaCombinations.set(100)
        maxTestCasesPerOperation.set(1000)

        // Error handling
        errorMode.set(ErrorMode.COLLECT_ALL)
        maxErrors.set(100)

        // Security values
        validSecurityValues.put("ApiKeyAuth", "test-key")

        // Filtering
        ignoreSchemaValidationRules.add("InvalidEnumValue")
        ignoreTestCases.putAll(mapOf(
            "/internal/*" to mapOf("*" to listOf("*"))
        ))

        // Module settings (raw maps)
        exampleValues.putAll(mapOf(
            "providers" to listOf("enum", "const", "pattern", "plain-string"),
            "maxExampleDepth" to 30,
        ))
        patternGeneration.putAll(mapOf(
            "defaultMinLength" to 10,
        ))
    }
}
```

### Configuration File

Use a YAML config file with DSL overrides:

```kotlin
openApiTestGenerator {
    configFile.set("openapi-testgen.yaml")
    specFile.set("src/main/resources/openapi.yaml")  // Overrides config file
}
```

DSL values override config file values. Nested maps are deep-merged; lists are replaced.

## Task Wiring

The plugin registers `generateOpenApiTests`. Default wiring depends on the generator:

| Generator | Wiring |
|-----------|--------|
| `template` | Adds output to test sources; `compileTestJava`/`compileTestKotlin` depends on generation |
| `test-suite-writer` | `processTestResources` depends on generation |

Disable automatic wiring:

```kotlin
openApiTestGenerator {
    manualOnly.set(true)
}
```

## Generators

### Template Generator

Generates Java/Kotlin test classes:

```kotlin
openApiTestGenerator {
    generator.set("template")
    generatorOptions.putAll(mapOf(
        "templateSet" to "restassured-java",  // or "restassured-kotlin"
        "templateVariables" to mapOf(
            "package" to "com.example.tests",
            "baseUrl" to "http://localhost:8080",
            "springBootTest" to "true",
        ),
    ))
}
```

### Test Suite Writer

Generates JSON/YAML test data:

```kotlin
openApiTestGenerator {
    generator.set("test-suite-writer")
    generatorOptions.putAll(mapOf(
        "outputFileName" to "test-suites.json",  // or .yaml
        "writeMode" to "OVERWRITE",              // or "MERGE"
        "preventOverwriteCases" to "false",
    ))
}
```

## Multiple Generation Tasks

Register additional tasks with different configurations:

```kotlin
import art.galushko.openapi.testgen.plugin.OpenApiTestGeneratorTask

tasks.register<OpenApiTestGeneratorTask>("generateOpenApiTestsYaml") {
    configFile.set("yaml-config.yaml")
    outputDir.set(layout.projectDirectory.dir("src/test/resources"))
}
```

## Documentation

- [Getting Started](https://docs.galushko.art/openapi-test-generator/getting-started/)
- [Gradle Plugin Reference](https://docs.galushko.art/openapi-test-generator/reference/gradle-plugin/)
- [Generator Options](https://docs.galushko.art/openapi-test-generator/reference/catalogs/generator-options/)
- [Distribution Settings](https://docs.galushko.art/openapi-test-generator/reference/distribution-settings/)
- [API Reference (Dokka)](https://docs.galushko.art/openapi-test-generator/api/)

## Development

```bash
./gradlew :plugin:test
./gradlew :plugin:check
```
