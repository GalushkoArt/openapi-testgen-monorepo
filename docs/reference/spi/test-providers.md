---
description: Reference documentation for the TestCaseProvider SPI. Providers transform validation rules into generated TestCase objects while maintaining deterministic ordering and immutability.
---

# Test providers SPI

Providers are responsible for turning rules (and other logic) into generated `TestCase` objects.

## TestCaseProvider

`TestCaseProvider<T>` consumes a specific OpenAPI element (the generic `T`) and the current `TestGenerationContext`, and returns an `Outcome<List<TestCase>>`.

### Interface

```kotlin
public interface TestCaseProvider<T> {
    public fun provideTestCases(spec: T, context: TestGenerationContext): Outcome<List<TestCase>>
}
```

### Typical `T`

- `Operation` for top-level providers that run per operation.
- `Parameter` for parameter-focused providers (delegated to by an operation provider).
- `RequestBody` for request-body-focused providers (delegated to by an operation provider).

### Wiring notes

Core runs operation-level providers in a fixed order (auth → parameters → request body) via `ProviderOrchestrator`.
Most extensions should add new *rules* rather than new providers; adding a new provider typically requires embedding and custom wiring.

Guidelines:

- Do not mutate inputs (valid case, context, OpenAPI models).
- Preserve deterministic ordering.
- Prefer composition of smaller providers per concern.
- Use provider boundary helpers (`runProviderSafely`) so exceptions become structured failures.

## Related documentation

- Reference: [Providers catalog](../catalogs/providers-catalog.md)
- Reference: [Validation rules SPI](validation-rules.md)
- How-to: [Custom providers](../../how-to/extension/custom-providers.md)

