---
description: Filter out unwanted test cases by path, HTTP method, or test case name using the ignore configuration. Useful for skipping internal endpoints, deprecated operations, or specific validation scenarios.
---

# Ignore rules

Filter generated test cases by path, HTTP method, or test case name using the ignore configuration.

## Configuration location

- YAML: `testGenerationSettings.ignoreTestCases`
- Gradle: `testGenerationSettings { ignoreTestCases.putAll(...) }`
- CLI: `--setting ignoreTestCases.<path>.<method>[]=...` or `--setting ignoreTestCases.<path>=*`

## Ignore config structure

The ignore configuration uses a hierarchical pattern:

- Path key → `"*"`: ignore the entire path
- Path key → method → `"*"`: ignore all tests for a specific HTTP method
- Path key → method → list of names: ignore specific test cases by name (exact match only)

Notes:
- Path keys are matched exactly (no globbing). Use `*` as the wildcard path for all paths.
- Test case names are exact matches (no wildcards or regex).
- For the authoritative wildcard/merge behavior and warnings, see [Distribution settings](../../reference/distribution-settings.md#ignore-configuration).

## Examples

### Ignore entire path

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/api/internal": "*"
```

### Ignore specific method

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/api/users":
      "DELETE": "*"
```

### Ignore specific test cases

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/api/users":
      "GET":
        - "Missing required query param: role"
```

### Wildcard path for all paths

```yaml
testGenerationSettings:
  ignoreTestCases:
    "*":
      "OPTIONS": "*"
```

### Combine multiple filters

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/health": "*"
    "/api/admin": "*"
    "*":
      "OPTIONS": "*"
    "/api/pets/{petId}":
      "GET":
        - "Missing required path param: petId"
      "DELETE": "*"
```

## Alternative: target by exclusion

If `includeOperations` is not suitable (for example, you need to keep an existing config and only
exclude a few paths or tests), you can use `ignoreTestCases` to remove everything else.

!!! warning "Performance note"
    Path-level and method-level ignores can skip work before generation, but ignoring specific test case names happens after suite generation.
    Prefer `includeOperations` for inclusion-based targeting on large specs.

Notes:
- Test case name matching is exact only (no wildcards or regex).
- CLI list values use `[]` when ignoring specific test names (see [CLI reference - Settings](../../reference/cli.md#settings)).

Example: keep only GET `/users/{userId}` by ignoring other paths:

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/users": "*"
    "/orders": "*"
    # /users/{userId} GET is NOT ignored (tests are generated)
```

CLI list syntax for specific test case names:

```bash
openapi-testgen \
  --setting 'ignoreTestCases./users/{userId}.GET[]=No security values provided'
```

Recommended approach for targeting operations:

!!! tip "Recommended approach"
    Use [`includeOperations`](include-operations.md) to target specific operations directly.

## Related docs

- [YAML config](yaml-config.md)
- [Include operations](include-operations.md)
- [Distribution settings](../../reference/distribution-settings.md#ignore-configuration) - Ignore configuration semantics
- [CLI reference](../../reference/cli.md#settings) - CLI settings syntax
- [Gradle plugin reference](../../reference/gradle-plugin.md#test-generation-settings) - Gradle testGenerationSettings DSL
