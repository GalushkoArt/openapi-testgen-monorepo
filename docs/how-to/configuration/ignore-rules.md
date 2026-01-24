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

## Runtime behavior

- Path keys are matched as-is; missing paths are logged as warnings.
- Method keys are uppercased; matching is case-insensitive.
- The wildcard path (`*`) merges with path-specific rules; wildcard method entries override path-specific entries on key collisions.
- `*:*:*` (path `*`, method `*`, value `*`) is ignored and logged as a warning.
- Filtering happens after suite assembly; if all cases are filtered, the operation is recorded as "not tested".

## Alternative: target by exclusion

If `includeOperations` is not suitable (for example, you need to keep an existing config and only
exclude a few paths or tests), you can use `ignoreTestCases` to remove everything else.

!!! warning "Performance note"
    `ignoreTestCases` filters **after** generation. It does not reduce generation time.
    Prefer `includeOperations` for faster, pre-generation filtering.

Notes:
- Test case name matching is exact only (no wildcards or regex).
- CLI list values use `[]` when ignoring specific test names.

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
- [Distribution settings](../../reference/distribution-settings.md)
