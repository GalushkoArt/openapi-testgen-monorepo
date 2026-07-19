# OpenAPI Test Generator Gradle Plugin

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/art.galushko.openapi-test-generator)](https://plugins.gradle.org/plugin/art.galushko.openapi-test-generator)

Gradle plugin that generates API tests from OpenAPI/Swagger specifications and wires them into
your build: template output lands in the test source set, test-suite-writer output in test
resources, both compiled and run by the regular `test` task.

## Installation

```kotlin
plugins {
    id("art.galushko.openapi-test-generator") version "<version>"
}
```

See the [installation guide](https://docs.galushko.art/openapi-test-generator/getting-started/installation/#version-placeholders) for where to look up `<version>`.

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

## Documentation

Everything else — the full DSL field list, `testGenerationSettings`, task wiring, registering
additional tasks, and generator options — lives on the docs site:

| Topic | Page |
|-------|------|
| Extension fields, task wiring, additional tasks | [Gradle plugin reference](https://docs.galushko.art/openapi-test-generator/reference/gradle-plugin/) |
| Setting keys, defaults, and precedence | [Distribution settings](https://docs.galushko.art/openapi-test-generator/reference/distribution-settings/) |
| Config file, filtering, security values | [Configuration](https://docs.galushko.art/openapi-test-generator/how-to/configuration/) |
| Template and test-suite-writer output | [Generators](https://docs.galushko.art/openapi-test-generator/how-to/generators/) |
| First run, quick starts | [Getting started](https://docs.galushko.art/openapi-test-generator/getting-started/) |
| API reference (Dokka) | [API reference](https://docs.galushko.art/openapi-test-generator/api/) |

## Development

```bash
./gradlew :plugin:test
./gradlew :plugin:check
```
