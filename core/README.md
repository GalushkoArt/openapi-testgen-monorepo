# Core Module

The deterministic test generation engine for OpenAPI specifications.

This module contains the core logic for generating test cases from OpenAPI specs, including
providers, rules, orchestration, and the generation pipeline. It implements a provider-rule
architecture that ensures deterministic, reproducible output.

## Features

- **Providers**: Generate test cases for parameters, request bodies, and authentication
- **Rules**: Encode OpenAPI constraints (min/max, required, enum, pattern, etc.)
- **Orchestration**: Deterministic execution with budget controls
- **Generators**: Pluggable output backends (template-based code, JSON/YAML data)

## Architecture

```
OpenAPI Spec → Parser → ValidCaseBuilder → Providers → Rules → TestCases → Generator
```

Key components:
- `TestSuiteGenerator` - Main entry point for generation
- `ProviderOrchestrator` - Executes providers in deterministic order
- `TestGenerationContext` - Shared context with depth tracking and budgets
- `ArtifactGenerator` - Output backend interface

## Usage

Most users should depend on `distribution-bundle` instead, which bundles core with standard
modules and provides a simplified API.

```kotlin
dependencies {
    implementation("art.galushko.openapi:testgen-core:0.9.1")
}
```

For direct usage, wire components via `TestGenerationEngine`:

```kotlin
val engine = TestGenerationEngine(
    modules = listOf(/* custom modules */),
    settingsExtractors = listOf(/* settings extractors */),
    testGenerationSettings = TestGenerationSettings()
)
val generator = engine.createTestSuiteGenerator()
val suites = generator.generate(openAPI)
```

## Extension Points

| SPI | Purpose |
|-----|---------|
| `SimpleSchemaValidationRule` | Custom schema validation rules |
| `AuthValidationRule` | Custom authentication rules |
| `TestCaseProvider<T>` | Custom test case generation logic |
| `ArtifactGeneratorFactory` | Custom output formats |
| `TestGenerationModule` | Bundle rules, providers, and generators |

## Documentation

- [Module Overview](https://docs.galushko.art/openapi-test-generator/modules/core/)
- [Architecture](https://docs.galushko.art/openapi-test-generator/concepts/architecture/)
- [Rules Catalog](https://docs.galushko.art/openapi-test-generator/reference/catalogs/rules-catalog/)
- [Providers Catalog](https://docs.galushko.art/openapi-test-generator/reference/catalogs/providers-catalog/)
- [SPI Reference](https://docs.galushko.art/openapi-test-generator/reference/spi/)
- [API Reference (Dokka)](https://docs.galushko.art/openapi-test-generator/api/)

## Development

```bash
./gradlew :core:test
./gradlew :core:check

# Run specific test
./gradlew :core:test --tests "ValidCaseBuilderTest"
```
