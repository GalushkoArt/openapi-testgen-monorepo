---
description: Minimal Gradle plugin setup for wiring OpenAPI-generated tests into a build, with notes on automatic wiring and manual-only mode.
---

# Gradle integration

Use the Gradle plugin when you want generation wired into your build instead of running the CLI separately.

## 1) Apply the plugin

```kotlin
plugins {
    id("art.galushko.openapi-test-generator") version "<version>"
}
```

See [Version placeholders](installation.md#version-placeholders) for where to look up `<version>`.

## 2) Configure generation

Minimal template-generator setup:

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

Minimal JSON/YAML writer setup:

```kotlin
openApiTestGenerator {
    specFile.set("src/test/resources/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests"))
    generator.set("test-suite-writer")
    generatorOptions.put("outputFileName", "test-suites.json")
}
```

## 3) Run generation

```bash
./gradlew generateOpenApiTests
```

Default wiring:

- `template` output is added to the test source set
- `test-suite-writer` output is wired into test resources

## 4) Disable automatic wiring when needed

```kotlin
openApiTestGenerator {
    manualOnly.set(true)
}
```

Use `manualOnly` when you want to control task ordering or consume generated output from a custom location.

## Next steps

- [Generators](../how-to/generators.md) for generator-specific options
- [Distribution settings](../reference/distribution-settings.md) for defaults and settings semantics
- [Gradle plugin reference](../reference/gradle-plugin.md) for the full DSL surface
