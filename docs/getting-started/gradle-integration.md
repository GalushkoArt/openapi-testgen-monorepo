# Gradle integration

This tutorial integrates test generation into a Gradle project using the plugin.

## 1) Apply the plugin

The plugin is published to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/art.galushko.openapi-test-generator).

```kotlin
plugins {
    id("art.galushko.openapi-test-generator") version "0.9.0"
}
```

!!! tip "Finding the latest version"
    Check the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/art.galushko.openapi-test-generator)
    or [GitHub Releases](https://github.com/galushkoart/openapi-testgen-monorepo/releases) for the latest version.

## 2) Configure the extension

Minimal template generator configuration:

```kotlin
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
}
```

## 3) Run generation

```bash
./gradlew generateOpenApiTests
```

The generated tests will be automatically added to your test source set.

## 4) Manual-only mode (optional)

To disable automatic wiring into compilation/resource processing:

```kotlin
openApiTestGenerator {
    manualOnly.set(true)
}
```

Use this when you need custom control over when and how generated sources are included.

## Next steps

- Generator configuration details: [Template generator](../how-to/generators/template-generator.md)
- Reference for all DSL fields: [Gradle plugin reference](../reference/gradle-plugin.md)
- All available settings: [Distribution settings](../reference/distribution-settings.md)
