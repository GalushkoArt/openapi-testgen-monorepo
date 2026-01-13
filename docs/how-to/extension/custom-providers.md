# Custom providers

Providers turn rules (and other logic) into generated `TestCase` objects. In `core`, providers are orchestrated in a fixed order per operation:

1. auth
2. parameters
3. request body

## Current limitation (important)

At the moment, `TestGenerationModule` **does not contribute providers**. Adding a new provider is a **contributor workflow**: it requires changing `core` wiring.

## 1) Implement a provider

Implement `TestCaseProvider<T>` (typically `T = Operation`) and keep it pure/deterministic:

- Do not mutate inputs (`validCase`, `OpenAPI`, `Operation`)
- Return stable ordering
- Use provider boundary helpers (`runProviderSafely`) so exceptions become structured failures

## 2) Wire it into generation

In this repo, provider wiring happens in `TestGeneratorConfigurer.createTestSuiteGenerator(...)` via `ProviderOrchestrator`.

To add a provider:

1. Add your provider implementation under `core/src/main/kotlin/.../providers/`.
2. Decide the execution position (auth → parameters → body is intentional).
3. Update `TestGeneratorConfigurer` to include your provider in the orchestrator list.

## 3) Add tests

Add focused tests under `core/src/test/kotlin/.../providers/` and assert:

- generated case names (exact strings)
- expected status codes (usually 400, auth often 401/403)
- stable ordering and deterministic output

## Related docs

- Concepts: [Provider-rule model](../../concepts/provider-rule-model.md)
- Reference: [Test providers SPI](../../reference/spi/test-providers.md)

# Add custom providers

Providers orchestrate negative test case generation for an OpenAPI operation (auth, parameters, request body).

## When to add a provider

Add a provider when you want a new class of negative cases that is not naturally represented as a schema/auth rule.

## Provider contract

Providers implement `TestCaseProvider<T>` and return `Outcome<List<TestCase>>`.

Guidelines:

- Be pure: do not mutate the valid case or OpenAPI models.
- Be deterministic: preserve stable ordering and stable naming.
- Use `runProviderSafely` (providers are expected to surface failures as `Outcome.Failure`).

## Wiring providers

The default provider order is fixed in core wiring. To add/reorder providers:

- Provide a custom wiring layer (custom `TestSuiteGenerator`), or
- Wrap engine creation to supply a different `TestGeneratorConfigurer` / orchestrator.

If you only need additional schema cases, prefer adding a rule instead.
See: [Custom rules](custom-rules.md)

