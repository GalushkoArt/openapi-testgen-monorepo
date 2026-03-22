# Distribution-Bundle Module

Unified entry point for OpenAPI test generation.

This module bundles `core`, `pattern-support`, and `generator-template` into a single dependency
with a high-level API. It's the foundation for both the CLI and Gradle plugin, providing
`TestGenerationRunner` for simplified execution.

## Features

- Single dependency for all standard test generation features
- `TestGenerationRunner` with builder-pattern API
- Pre-configured defaults (template + pattern support)
- Pluggable reporting via `TestGenerationReporter`
- Structured results via `TestGenerationResult` sealed class

## Usage

For CLI and Gradle plugin usage, this module is an internal dependency. For custom tooling:

```kotlin
dependencies {
    implementation("art.galushko.openapi.testgen:distribution-bundle:<version>")
}
```

```kotlin
import org.slf4j.LoggerFactory

val runner = TestGenerationRunner.withDefaults(
    reporter = Slf4jReporter(LoggerFactory.getLogger("openapi-testgen"))
)

val result = runner.execute(
    config = null,
    overrides = TestGeneratorOverrides(
        specFile = "openapi.yaml",
        outputDir = Path.of("build/generated"),
        generatorId = "test-suite-writer",
        generatorOptions = mapOf(
            "format" to "json",
            "outputFileName" to "test-suites.json",
        ),
    ),
)

println(result)
```

## Components

| Component | Description |
|-----------|-------------|
| `TestGenerationRunner` | Builder-pattern entry point |
| `TestGenerationReporter` | Interface for output customization |
| `TestGenerationResult` | Sealed class for success/failure handling |
| `DistributionDefaults` | Factory for standard modules and settings |
| `Slf4jReporter` | Default SLF4J-based reporter |

## Documentation

- [Module Overview](https://docs.galushko.art/openapi-test-generator/modules/distribution-bundle/)
- [Getting Started](https://docs.galushko.art/openapi-test-generator/getting-started/)
- [Distribution Settings](https://docs.galushko.art/openapi-test-generator/reference/distribution-settings/)
- [API Reference (Dokka)](https://docs.galushko.art/openapi-test-generator/api/)

## Development

```bash
./gradlew :distribution-bundle:test
./gradlew :distribution-bundle:check
```
