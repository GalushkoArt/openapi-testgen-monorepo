---
description: The pattern-support module integrates pattern-value into core as an optional feature module. It contributes a schema value provider for pattern-based values, a validation rule for pattern violations, and a settings extractor for pattern generation configuration.
---

# Module: `pattern-support`

`pattern-support` integrates `pattern-value` into `core` as an **optional feature module**. It contributes:

- a schema value provider id `"pattern"` (used by `ExampleValueSettings.providers`)
- an additional schema validation rule for pattern violations
- a settings extractor that reads `patternGeneration` config into `PatternGenerationOptions`

## Depends on

- `core`
- `pattern-value`

## Used by

- `distribution-bundle` (enabled by default via `DistributionDefaults`)

## Key types

- `PatternSupportModule`: `TestGenerationModule` implementation
- `PatternModuleSettingsExtractor`: extracts `patternGeneration` settings from config/overrides
- `PatternGenerationOptions`: options object passed into the module

## Settings

`patternGeneration` is a **module setting** (not a field on `TestGenerationSettings`). It is provided as a raw map under `testGenerationSettings.patternGeneration` and extracted before core settings parsing.

See:

- Reference: [Distribution settings](../reference/distribution-settings.md#testgenerationsettingspatterngeneration)
- How-to: [Configure generation via YAML](../how-to/configuration/yaml-config.md)

## Related docs

- Concepts: [Architecture](../concepts/architecture.md)
- Modules: [pattern-value](pattern-value.md), [example-value](example-value.md)
- Reference: [Rules catalog](../reference/catalogs/rules-catalog.md)

## Scope and responsibilities (contributors)

`pattern-support` integrates regex-based pattern generation into core by:

- Contributing a schema example provider id `pattern`.
- Contributing a negative schema rule `InvalidPatternSchemaValidationRule`.
- Parsing module settings from `testGenerationSettings.patternGeneration`.

It depends on `core` and `pattern-value`.

## Configuration

Module settings key: `patternGeneration`.

Example YAML:

```yaml
testGenerationSettings:
  patternGeneration:
    defaultMinLength: 3
    spaceChars: " \t\n\r"
    anyPrintableChars: "abcdefghijklmnopqrstuvwxyz"
```

## Provider and rule wiring

`PatternSupportModule` contributes:

- `SchemaValueProvider` id `pattern` (valid pattern matches).
- `InvalidPatternSchemaValidationRule` (non-matching pattern values).

To use the provider, include `pattern` in `ExampleValueSettings.providers` or rely on a
distribution that inserts it into the default provider order.

## API reference

- Dokka API reference: [`docs/api/pattern-support/index.html`](../api/pattern-support/index.html)
