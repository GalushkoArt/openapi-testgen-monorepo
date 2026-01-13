# Ignore rules

Filter generated test cases by path, HTTP method, or test case name using the ignore configuration.

## Configuration location

- YAML: `testGenerationSettings.ignoreTestCases`
- Gradle: `testGenerationSettings { ignoreTestCases.putAll(...) }`
- CLI: `--setting ignoreTestCases.*=...`

## Ignore config structure

The ignore configuration uses a hierarchical pattern:

- Path key → `"*"`: ignore the entire path
- Path key → method → `"*"`: ignore all tests for a specific HTTP method
- Path key → method → list of names: ignore specific test cases by name (supports wildcards)

## Examples

### Ignore entire path

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/api/internal/*":
      "*": [ "*" ]
```

### Ignore specific method

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/api/users":
      "DELETE": [ "*" ]
```

### Ignore specific test cases

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/api/users":
      "GET": [ "Invalid Query role parameter" ]
```

### Wildcard path for all paths

```yaml
testGenerationSettings:
  ignoreTestCases:
    "*":
      "OPTIONS": [ "*" ]
```

### Combine multiple filters

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/health":
      "*": [ "*" ]
    "/api/admin/*":
      "*": [ "*" ]
    "/api/pets/{petId}":
      "GET": [ "Invalid*" ]
      "DELETE": [ "*" ]
```

## Runtime behavior

- Path keys are matched as-is; missing paths are logged as warnings.
- Method keys are uppercased; matching is case-insensitive.
- The wildcard path (`*`) merges with path-specific rules; wildcard method entries override path-specific entries on key collisions.
- Filtering happens after suite assembly; if all cases are filtered, the operation is recorded as "not tested".

## Related docs

- [YAML config](yaml-config.md)
- [Distribution settings](../../reference/distribution-settings.md)
