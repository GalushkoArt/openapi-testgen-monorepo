# Model Module

Core data types for the OpenAPI Test Generator.

This module defines the fundamental data structures used throughout the test generation pipeline:
`TestCase`, `TestSuite`, and error handling types.

## Key Types

| Type | Description |
|------|-------------|
| `TestCase` | Single test case with request details and expected response |
| `TestSuite` | Collection of test cases for an API operation |
| `GenerationError` | Structured error with context for debugging |
| `GenerationReport` | Aggregated results from a generation run |

## Usage

This module is a transitive dependency of `core` and `distribution-bundle`. You typically don't
need to depend on it directly unless building custom tooling.

```kotlin
dependencies {
    implementation("art.galushko.openapi:testgen-model:0.9.1")
}
```

## Documentation

- [Module Overview](https://docs.galushko.art/openapi-test-generator/modules/model/)
- [TestSuite Reference](https://docs.galushko.art/openapi-test-generator/reference/model/test-suite/)
- [TestCase Reference](https://docs.galushko.art/openapi-test-generator/reference/model/test-case/)
- [Error Handling](https://docs.galushko.art/openapi-test-generator/reference/model/errors/)
- [API Reference (Dokka)](https://docs.galushko.art/openapi-test-generator/api/)

## Development

```bash
./gradlew :model:test
./gradlew :model:check
```
