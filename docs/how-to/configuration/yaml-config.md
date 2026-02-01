---
description: Configure test generation using a YAML config file. Shows file structure, precedence, and examples for CLI and Gradle plugin usage, with links to the canonical settings and option references.
---

# Configure generation via YAML

You can provide a YAML config file that mirrors execution options (used by both CLI and Gradle plugin).

The config filename is arbitrary; examples use `openapi-testgen.yaml`.

## Example config

```yaml
specFile: "openapi.yaml"
outputDir: "./build/generated"
generator: "test-suite-writer"
generatorOptions:
  outputFileName: "openapi-test-suites.json"
  writeMode: "MERGE"
testGenerationSettings:
  maxSchemaDepth: 50
  maxMergedSchemaDepth: 50
  maxSchemaCombinations: 100
  maxTestCasesPerOperation: 1000
  maxErrors: 100
  errorMode: "COLLECT_ALL"
  validSecurityValues:
    ApiKeyAuth: "test-key"
alwaysWriteTests: false
logLevel: "INFO"
```

## Using the config

### CLI

```bash
openapi-testgen \
  --config-file ./openapi-testgen.yaml \
  --spec-file ./src/test/resources/openapi.yaml
```

### Gradle plugin

```kotlin
openApiTestGenerator {
    configFile.set("openapi-testgen.yaml")
}
```

## Overrides and merge behavior

- Overrides win over config values.
- Nested maps are deep-merged; lists/arrays are replaced.

See [Distribution settings](../../reference/distribution-settings.md) for defaults and precedence.

## Config schema (overview)

Top-level keys you’ll typically use:

- `specFile` (required)
- `outputDir` (required)
- `generator` (required): `template` or `test-suite-writer`
- `generatorOptions` (optional): generator-specific options
- `testGenerationSettings` (optional): core generation settings (budgets, filters, security values, example generation)
- `alwaysWriteTests` (optional)
- `logLevel` (optional)

For the authoritative list of keys, defaults, and precedence rules, see [Distribution settings](../../reference/distribution-settings.md).

## Generator options

`generatorOptions` is generator-specific.

See [Generator options](../../reference/catalogs/generator-options.md) for supported keys and exact behavior.

## Common next steps

- Target a subset of endpoints: [Include operations](include-operations.md)
- Provide credentials for auth schemes: [Security values](security-values.md)
- Exclude noisy endpoints/cases: [Ignore rules](ignore-rules.md)
- Add the valid (2xx) baseline case: [Positive testing](positive-testing.md)

## Related docs

- [Budget controls](../../concepts/budget-controls.md) - Tuning budget limits
- [CLI reference](../../reference/cli.md) - Flags and nested `--setting` / `--generator-option` syntax
- [Gradle plugin reference](../../reference/gradle-plugin.md) - DSL fields and task wiring
