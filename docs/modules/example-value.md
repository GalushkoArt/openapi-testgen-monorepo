---
description: The example-value module is a standalone library for generating deterministic example values from OpenAPI schemas. It provides the SchemaValueProvider SPI and built-in providers for enums, UUIDs, emails, dates, and other common types.
---

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

Generates example values for parameters and request bodies. Response examples are handled by `ResponseExampleExtractor`, which uses schema-derived fallback when explicit examples are missing:

- Uses `schema.example` when present (variationIndex = 0).
- Merges composed schemas via `SchemaMerger` before generating values.
- Handles arrays/objects by recursing into item/property schemas.
- Stops recursion based on `maxExampleDepth`.

### ResponseExampleExtractor

Resolves expected response examples from OpenAPI operations:

- Response lookup: exact status code -> range (e.g., `2XX`) -> `default`.
- Media type priority: JSON-like (`application/json`, `application/*+json`) -> `application/xml` -> other types (alphabetical).
- Example selection: `MediaType.example` -> `MediaType.examples`; schema-derived fallback is applied only for JSON-like media types.

Response fallback uses response-specific defaults regardless of `ExampleValueSettings` flags:

- `includeOptionalExampleProperties = true`
- `includeWriteOnly = false`
- `useSchemaExampleFallback = true`

`maxExampleDepth` from `ExampleValueSettings` still applies.

This replaces the removed `SchemaExampleValueGenerator.extractExpectedResponseExample` method.

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

Example value generation is configured via `testGenerationSettings.exampleValues` (YAML/Gradle) or `--setting exampleValues.*` (CLI).
The canonical key list, defaults, and examples live in [Distribution settings → testGenerationSettings.exampleValues](../reference/distribution-settings.md#testgenerationsettingsexamplevalues).
This module page focuses on behavior and the `SchemaValueProvider` SPI.

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
