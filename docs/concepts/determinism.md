# Determinism

The generator is designed to produce stable outputs for the same inputs.

## What “deterministic” means here

Given the same OpenAPI spec and the same configuration, generation should produce:

- The same set of test cases
- In a stable order
- With stable names and expected status codes
- With deterministic merging/writing behavior

## Deterministic ordering rules

- **Providers**: execute in a fixed order (auth → parameters → request body).
- **Rules**: ordered deterministically; ignore filters are applied after sorting.
- **Modules**: sorted by module id and then class name.
- **Writers**: suite and case ordering is stable; merges preserve deterministic ordering.

## Common pitfalls

- Unordered maps/sets in configuration can cause unstable output if iterated directly.
  Prefer stable iteration order or explicit sorting at boundaries.
- Time/randomness must not affect test case names or values.

