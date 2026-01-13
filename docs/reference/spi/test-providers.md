# Test providers SPI

Providers are responsible for turning rules (and other logic) into generated `TestCase` objects.

## TestCaseProvider

Providers implement `TestCaseProvider<T>` and return `Outcome<List<TestCase>>`.

Guidelines:

- Do not mutate inputs (valid case, context, OpenAPI models).
- Preserve deterministic ordering.
- Prefer composition of smaller providers per concern.
- Use provider boundary helpers (`runProviderSafely`) so exceptions become structured failures.

