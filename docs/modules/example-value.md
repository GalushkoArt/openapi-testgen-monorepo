# Module: `example-value`

`example-value` is a **standalone library** for generating deterministic example values from OpenAPI schemas. It provides the **`SchemaValueProvider` SPI** and the `SchemaExampleValueGenerator` orchestration used by `core` to build valid baselines and schema-derived values.

This module depends only on `model` (and the OpenAPI parser types it needs) and does not depend on `core`.

## Depends on

- `model`

## Used by

- `core`
- `pattern-value` (implements the SPI)

## Key types

### SchemaExampleValueGenerator

Generates example values for parameters, request bodies, and responses:

- Uses `schema.example` when present (variationIndex = 0).
- Merges composed schemas via `SchemaMerger` before generating values.
- Handles arrays/objects by recursing into item/property schemas.
- Stops recursion based on `maxExampleDepth`.

### SchemaExampleValueGeneratorFactory

Creates configured generator instances using `ExampleValueSettings` and extra providers.

- Missing provider ids are skipped with a warning.
- If all configured providers are missing, the default provider order is used as a fallback.

### SchemaValueProvider (SPI)

Providers return a value or null for a given schema. Providers are ordered; the first non-null value wins. `variationIndex` is used to generate deterministic variations.

### SchemaMerger

Merges composed schemas (allOf/anyOf/oneOf) with budget controls.

## Built-in providers (ids)

Built-ins are registered in this order by default:

`enum`, `const`, `uuid`, `email`, `date`, `date-time`, `plain-string`, `number`, `boolean`

The `pattern` provider is contributed by the `pattern-support` module and is inserted by distribution defaults before `plain-string`.

## Configuration (ExampleValueSettings)

Location in config:

- YAML: `testGenerationSettings.exampleValues`
- Gradle plugin: `openApiTestGenerator { testGenerationSettings { exampleValues.putAll(...) } }`
- CLI: `--setting exampleValues.*=...`

Key fields:

- `providers`: ordered list of provider ids (first provider that returns a value wins).
- `maxExampleDepth`: max recursion depth for schema traversal.
- `uuid.template`: template string for UUIDs (must include `%s`).
- `email.template`: template string for emails (must include `%s`).
- `date.startDate`: start date (`YYYY-MM-DD`).
- `dateTime.startDate`: start date (`YYYY-MM-DD`).
- `dateTime.timeSuffixTemplate`: time suffix template (must include `%s`).
- `plainString.validChars`: characters used for plain string generation (must be non-empty).

Example configuration:

```yaml
testGenerationSettings:
  exampleValues:
    providers:
      - enum
      - const
      - uuid
      - email
      - date
      - date-time
      - plain-string
      - number
      - boolean
    maxExampleDepth: 50
    uuid:
      template: "d5a5495b-cbdc-4237-a66e-%s"
    email:
      template: "test%s@example.com"
    date:
      startDate: "2025-05-05"
    dateTime:
      startDate: "2025-05-05"
      timeSuffixTemplate: "%sT17:32:28Z"
    plainString:
      validChars: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
```

## SPI contract (SchemaValueProvider)

`SchemaValueProvider` is a functional interface:

```kotlin
fun provide(schema: Schema<*>, variationIndex: Int): Any?
```

Guidelines:

- Return null when the provider does not apply.
- Do not mutate the schema or the OpenAPI model.
- Keep outputs deterministic for identical inputs.
- Use `variationIndex` to produce deterministic variations (e.g. for `uniqueItems`).

## Extension points

Standalone usage:

- Pass providers to `SchemaExampleValueGeneratorFactory(extraProviders = ...)`.
- Add provider ids to `ExampleValueSettings.providers`.

Core integration:

- Contribute providers via `TestGenerationModule.schemaValueProviders` and pass the module to core wiring.
- Ensure provider ids do not collide with built-in ids.

## API reference

- Dokka API reference: [`docs/api/example-value/index.html`](../api/example-value/index.html)

## Related docs

- Reference:
  - [Value providers SPI](../reference/spi/value-providers.md)
- Concepts:
  - [Budget controls](../concepts/budget-controls.md)
  - [Schema composition](../concepts/schema-composition.md)
- Modules:
  - [pattern-value](pattern-value.md)
  - [pattern-support](pattern-support.md)
  - [core](core.md)
