---
description: Reference documentation for the ArtifactGenerator and ArtifactGeneratorFactory SPIs. Generators emit test artifacts (code files, JSON/YAML) from generated test suites.
---

# Generators SPI

Generators emit artifacts based on generated test suites.

## ArtifactGenerator

`ArtifactGenerator` writes artifacts for a `TestSuite` (or a list of suites).

### Interface

```kotlin
public interface ArtifactGenerator {
    public fun generateTests(testSuite: TestSuite)
    public fun generateTests(testSuites: List<TestSuite>)
}
```

Guidelines:

- Keep output deterministic (stable ordering, no timestamps).
- Validate inputs early and fail with actionable error messages.
- Localize I/O; avoid global state.

## ArtifactGeneratorFactory

Factories create generator instances and validate generator options.

### Interface

```kotlin
public interface ArtifactGeneratorFactory {
    public val id: String
    public val description: String
    public fun create(outputDir: File, options: Map<String, Any?>): ArtifactGenerator
}
```

Guidelines:

- Use a stable, unique generator id.
- Validate options up-front.
- Create a fresh generator instance per execution run.

## Registration and wiring

Generators are selected by `id` via `ArtifactGeneratorRegistry`. Register factories by contributing them from a `TestGenerationModule` and passing that module to
the engine wiring (CLI, Gradle plugin, or embedding code).

See:

- Reference: [Generator options catalog](../catalogs/generator-options.md)
- How-to: [Custom generators](../../how-to/extension/custom-generators.md)
- How-to: [Custom modules](../../how-to/extension/custom-modules.md)

