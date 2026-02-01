---
description: Documents the budget limits that prevent runaway generation from complex or deeply nested schemas. Covers maxSchemaDepth, maxSchemaCombinations, maxTestCasesPerOperation, and how to tune them.
---

# Budget controls

OpenAPI schemas can be deeply nested or contain combinatorial compositions (`allOf`/`anyOf`/`oneOf`). Budget controls cap worst-case behavior and keep generation tractable.

## Key budgets

| Budget | Description |
|--------|-------------|
| `maxSchemaDepth` | Recursion limit when traversing schemas for validation |
| `maxMergedSchemaDepth` | Recursion limit when merging composed schemas into a flattened view |
| `maxSchemaCombinations` | Cap on composed-schema combinations (prevents `anyOf`/`oneOf` explosion) |
| `maxTestCasesPerOperation` | Maximum test cases generated for a single operation |
| `maxErrors` | Cap on collected errors when using `COLLECT_ALL` mode |

Defaults and types are documented in [Distribution settings](../reference/distribution-settings.md#budget-controls).

## What happens when a budget is exceeded

Some budget violations are converted into structured generation errors (via provider boundaries), while others may stop generation for a specific operation depending on where the limit is enforced.

When a budget is exceeded, the error includes:

- The operation path and method
- The current count vs. the limit
- Suggested solutions (increase limit, simplify schema, or use ignore rules)

## How to tune budgets

- **Increase budgets** when your schemas are legitimately complex and you need more coverage.
- **Decrease budgets** in CI when you prefer faster feedback.
- Consider using [ignore rules](../how-to/configuration/ignore-rules.md) for problematic operations instead of globally increasing limits.

For configuration keys, defaults, and where to set them (YAML vs CLI vs Gradle), see:

- Reference: [Distribution settings](../reference/distribution-settings.md#budget-controls)
- How-to: [Configure generation via YAML](../how-to/configuration/yaml-config.md)
