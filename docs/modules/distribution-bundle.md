---
description: The distribution-bundle module is the shared product layer used by CLI and Gradle plugin. It bundles core, pattern-support, and generator-template modules, providing TestGenerationRunner as a high-level API for test generation execution.
---

# Module: `distribution-bundle`

`distribution-bundle` is the shared "product layer" used by both the **CLI** and the **Gradle plugin**. It bundles feature modules and exposes a stable execution entry point.

## Depends on

- `core`
- `generator-template`
- `pattern-support`

## Used by

- `cli`
- `plugin`

## Key entry points

### TestGenerationRunner

Runs a generation end-to-end with configurable modules and reporting:

1. Merges config + overrides into `TestGeneratorExecutionOptions`
2. Extracts module settings (e.g., pattern generation options)
3. Creates modules via a `ModuleFactory`
4. Generates a `GenerationReport`
5. Writes artifacts when generation succeeds or `alwaysWriteTests` is set, and returns success when artifacts were written even if the report contains errors

Use `withDefaults(reporter)` for standard wiring or the builder for customization.

#### Programmatic invocation (Kotlin)

This snippet runs generation using the same defaults as the CLI/Gradle plugin (`withDefaults`) and passes inputs via `TestGeneratorOverrides`.

```kotlin
import art.galushko.openapi.testgen.config.TestGeneratorOverrides
import art.galushko.openapi.testgen.distribution.Slf4jReporter
import art.galushko.openapi.testgen.distribution.TestGenerationRunner
import org.slf4j.LoggerFactory
import java.nio.file.Path

fun main() {
    val runner = TestGenerationRunner.withDefaults(
        reporter = Slf4jReporter(LoggerFactory.getLogger("openapi-testgen"))
    )

    val overrides = TestGeneratorOverrides(
        specFile = "openapi.yaml",
        outputDir = Path.of("build/generated"),
        generatorId = "test-suite-writer",
        generatorOptions = mapOf(
            "format" to "json",
            "outputFileName" to "test-suites.json",
        ),
    )

    runner.execute(config = null, overrides = overrides)
}
```

Add dependency:

```kotlin
dependencies {
    implementation("art.galushko.openapi.testgen:distribution-bundle:<version>")
}
```

See [Version placeholders](../getting-started/installation.md#version-placeholders) for how to choose `<version>`.

### TestGenerationReporter

Output abstraction allowing environment-specific logging:

- CLI uses `Slf4jReporter` which delegates formatting to core `ConsoleReporter`
- Gradle uses a logger adapter that routes to `project.logger`

### DistributionDefaults

Factory for standard modules, settings, and extractors:

- **Modules**: `TemplateGeneratorModule`, `PatternSupportModule`
- **Extractors**: `PatternModuleSettingsExtractor`
- **Settings**: inserts provider id `pattern` before `plain-string` in example-value order

## Extending the distribution

For embedding/integration scenarios, prefer:

- `TestGenerationRunner.builder()` to add modules or custom reporters
- `TestGenerationEngine` (from `core`) if you want finer control over orchestration

## API reference

- Dokka API reference: [`docs/api/distribution-bundle/index.html`](../api/distribution-bundle/index.html)

## Related docs

- Concepts: [Architecture](../concepts/architecture.md)
- How-to:
  - [Custom modules](../how-to/extending.md#custom-modules)
  - [Custom generators](../how-to/extending.md#custom-generators)
- Modules:
  - [Core module](core.md)
  - [Pattern support](index.md#pattern-support)
