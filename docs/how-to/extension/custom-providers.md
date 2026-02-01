---
description: Understand how to create custom test case providers that orchestrate negative test generation for OpenAPI operations. Covers the provider contract, wiring into generation, and when to prefer rules over providers.
---

# Custom providers

!!! warning "Contributor workflow"
    Adding a new `TestCaseProvider` currently requires changing `core` wiring (it cannot be contributed via `TestGenerationModule`).
    Rules and generators *can* be contributed via `TestGenerationModule`, so prefer those extension points when possible.
    See [Development setup](../../contributing/development-setup.md) if you plan to change `core`.

Providers turn rules (and other logic) into generated `TestCase` objects. In `core`, providers are orchestrated in a fixed order per operation:

1. auth
2. parameters
3. request body

## Current limitation (important)

At the moment, `TestGenerationModule` **does not contribute providers**. Adding a new provider requires changing `core` wiring.
See [Development setup](../../contributing/development-setup.md) if you plan to contribute this change.

## When to add a provider (vs a rule)

Add a provider when you want a new class of negative cases that is not naturally represented as a schema/auth rule.

If you only need additional schema cases, prefer adding a rule instead (see [Custom rules](custom-rules.md)).

## 1) Implement a provider

Implement `TestCaseProvider<T>` (typically `T = Operation`). Providers return `Outcome<List<TestCase>>` and should be pure/deterministic:

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

