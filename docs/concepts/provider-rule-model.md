# Provider-rule model

The generator is built around a provider-rule model that keeps responsibilities separated and makes output deterministic.

## The baseline: a valid case

For each OpenAPI operation, core builds a baseline *valid* `TestCase` (the “valid case”). Providers use it as the starting point to derive negative cases.

This baseline includes only what’s required (required params, first supported request body media type, security values).

## Providers

Providers decide **what to vary** for a given operation:

- Auth provider: derives auth-negative cases (missing/invalid credentials, scope variations).
- Parameter provider: derives parameter-negative cases (missing required params, schema violations).
- Request body provider: derives request-body-negative cases (missing body, schema violations).

Providers return `Outcome<List<TestCase>>` and must be pure (no mutation of inputs).

## Rules

Rules decide **how to vary** a specific schema/security constraint.

- **Schema rules** return `Sequence<RuleValue>` (lazy, deterministic). A provider turns each `RuleValue` into a negative `TestCase`.
- **Auth rules** return `Sequence<TestCase>` because auth variations often span multiple request fields and expected status codes (401/403).

## Deterministic composition

Determinism comes from:

- Fixed provider execution order (auth → parameters → request body)
- Stable rule ordering (sorted deterministically)
- Stable suite/case ordering when aggregating and writing

See: [Determinism](determinism.md)

