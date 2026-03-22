---
description: How the generator handles OpenAPI schema composition keywords (allOf, anyOf, oneOf), including merge behavior, test case generation strategies, cycle detection, and budget controls for composed schemas.
---

# Schema composition

OpenAPI supports schema composition through `allOf`, `anyOf`, and `oneOf` keywords. This page explains how the test generator processes composed schemas and
generates test cases for them.

## Composition keywords

| Keyword | OpenAPI Semantics                                      | How Generator Handles It                           |
|---------|--------------------------------------------------------|----------------------------------------------------|
| `allOf` | Instance must validate against **all** listed schemas  | Merges all subschemas into a single unified schema |
| `anyOf` | Instance must validate against **at least one** schema | Generates test cases for each subschema variant    |
| `oneOf` | Instance must validate against **exactly one** schema  | Generates test cases for each subschema variant    |

## SchemaMerger

The `SchemaMerger` component (in `example-value` module) handles schema composition by flattening composed schemas into a single merged schema for validation
and value generation.

### Merge behavior

When merging schemas:

- **Properties**: Merged recursively; later schemas override earlier ones
- **Required fields**: Combined (union of all required arrays)
- **Constraints**: Tightest bounds win (e.g., higher `minLength`, lower `maxLength`)
- **Type**: Must be compatible across all subschemas
- **Enums**: Intersection of allowed values

### Example

Given this composed schema:

```yaml
components:
    schemas:
        Pet:
            allOf:
                -   $ref: '#/components/schemas/Animal'
                -   type: object
                    properties:
                        name:
                            type: string
                            minLength: 1
                    required:
                        - name
        Animal:
            type: object
            properties:
                species:
                    type: string
            required:
                - species
```

SchemaMerger produces a flattened schema equivalent to:

```yaml
type: object
properties:
    species:
        type: string
    name:
        type: string
        minLength: 1
required:
    - species
    - name
```

## Test case generation for compositions

### allOf schemas

For `allOf` compositions, the generator:

1. Merges all subschemas into a unified schema
2. Applies validation rules to the merged schema
3. Generates negative test cases for constraint violations

Since `allOf` requires all constraints to be satisfied, only the merged schema's rules are tested.

### anyOf and oneOf schemas

For `anyOf` and `oneOf` compositions, the generator:

1. Identifies each subschema variant
2. Generates test cases for each variant's validation rules
3. Produces test cases that violate each variant's specific constraints

This can produce more test cases because each variant's unique constraints are tested.

## Budget controls for composition

Composed schemas can lead to combinatorial explosion. Two budget settings control this:

| Setting                 | Description                                           |
|-------------------------|-------------------------------------------------------|
| `maxMergedSchemaDepth`  | Maximum recursion depth when merging composed schemas |
| `maxSchemaCombinations` | Maximum schema combinations per parameter/property    |

Defaults are documented in [Distribution settings](../reference/distribution-settings.md#budget-controls).

### When budgets are exceeded

When a budget limit is reached:

- Generation for that schema/property stops
- A `GenerationError` is recorded with context (operation, field path, limit type)
- Other schemas in the same operation continue processing (unless using `FAIL_FAST` mode)

### Configuration

```yaml
testGenerationSettings:
    maxMergedSchemaDepth: 75      # Increase for deeply nested compositions
    maxSchemaCombinations: 200    # Increase for complex anyOf/oneOf
```

See [Budget controls](architecture.md#budget-controls) for more details on tuning these limits.

## Cycle detection

Composed schemas can contain circular references (e.g., a schema that references itself through `allOf`). The generator handles cycles through:

- **Schema reference tracking**: `visitedSchemaRefs` tracks `$ref` paths during traversal
- **Structural hashing**: `SchemaStructureHasher` detects structurally equivalent schemas
- **Depth limiting**: `maxSchemaDepth` prevents infinite recursion

When a cycle is detected, the generator stops traversal for that path and continues with other properties.

## Limitations

### Unsupported patterns

Some complex composition patterns may not generate complete test coverage:

- **Discriminator-based polymorphism**: The generator processes subschemas but may not generate tests for all discriminator values
- **Conditional schemas (if/then/else)**: Not currently supported
- **Complex nested compositions**: Deep nesting of `anyOf` inside `oneOf` may hit budget limits

### Best practices

For optimal test generation with composed schemas:

1. **Keep compositions shallow**: Prefer flat schemas over deeply nested compositions
2. **Use `allOf` for inheritance**: Simpler merge semantics produce more predictable tests
3. **Limit `anyOf`/`oneOf` variants**: Each variant multiplies test cases
4. **Use ignore rules**: Skip problematic paths if generation budgets are exceeded

```yaml
testGenerationSettings:
    ignoreTestCases:
        "/complex-endpoint":
            "POST": "*"  # Skip tests for endpoints with complex compositions
```

## Related docs

- [Budget controls](architecture.md#budget-controls) - Tuning composition limits
- [Architecture](architecture.md) - SchemaMerger in the value generation stack
- [Error handling](error-handling.md) - How budget violations are reported
