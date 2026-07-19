---
description: The example-value module generates example values from OpenAPI schemas, extracts response examples with media-type negotiation, and merges composed schemas. It owns the SchemaValueProvider SPI and offers a Java-friendly API with presets, withers, and SAM-convertible interfaces.
---

# Module: `example-value`

`example-value` derives example values from OpenAPI schemas: type- and format-aware value generation, response example extraction with media-type negotiation, and `allOf`/`anyOf`/`oneOf` schema merging. It owns the `SchemaValueProvider` SPI that `pattern-value` and custom providers implement.

Most users do not interact with this module directly --- `core` and `distribution-bundle` wire it automatically. Direct use is appropriate when you need schema-derived example generation, response example extraction, or schema flattening outside of the test-generation pipeline (the module is consumed standalone by external projects, including Java ones).

## Depends on

- `model` (error/reporting contracts used by `CombinationBudget`)

## Used by

- `core` (valid-case building, response expectations, schema flattening)
- `pattern-value` (implements the `SchemaValueProvider` SPI)
- `pattern-support` (reuses `SchemaTypeHelpers`)

## Public API

### SchemaExampleValueGenerator

Generates synthetic example values from schemas. Providers are tried in order; the first non-null value wins.

| Method                                                                           | Purpose                                                                |
|----------------------------------------------------------------------------------|------------------------------------------------------------------------|
| `getExampleValue(name, schema, openAPI, variationIndex = 0)`                     | Example value for a schema; schema-level `example` wins at variation 0 |
| `getExampleValueWithOptions(name, schema, openAPI, options, variationIndex = 0)` | Same, with per-call options                                            |
| `getExampleArrayValues(name, schema, openAPI)`                                   | Array satisfying `minItems`/`uniqueItems`                              |
| `getExampleObject(name, schema, openAPI)`                                        | Object with required properties populated                              |

Create instances via `SchemaExampleValueGeneratorFactory().create(settings)` --- the factory resolves the built-in providers (`enum`, `const`, `uuid`, `email`, `date`, `date-time`, `plain-string`, `number`, `boolean`) in the order configured by `ExampleValueSettings.providers` and accepts extra providers keyed by id.

### SchemaExampleValueGeneratorOptions

Immutable options with Java-friendly presets and withers:

| Property                           | Default | Description                                              |
|------------------------------------|---------|----------------------------------------------------------|
| `maxExampleDepth`                  | `50`    | Recursion depth limit                                    |
| `includeOptionalExampleProperties` | `false` | Include optional properties that carry explicit examples |
| `includeWriteOnly`                 | `true`  | Include `writeOnly` properties (disable for responses)   |
| `useSchemaExampleFallback`         | `false` | Fall back to `schema.examples` / `schema.default`        |
| `fullExample`                      | `false` | Populate every declared property and non-empty arrays    |

`REQUEST_DEFAULTS` and `RESPONSE_DEFAULTS` are static fields; `with*` methods express deltas without positional `copy`:

```java
var options = SchemaExampleValueGeneratorOptions.RESPONSE_DEFAULTS.withFullExample(true);
```

### ResponseExampleExtractor

Extracts expected response examples from operations using a defined selection order: response by status (exact -> range like `2XX` -> `default`), media types by priority (JSON/JWT-like -> XML -> other), explicit spec examples first (including `$ref`'d and named examples), schema-derived fallback otherwise.

The fallback slot is pluggable via the SAM-convertible `ResponseBodyGenerator`:

```java
var extractor = new ResponseExampleExtractor((schema, api) -> myGenerator.generate(schema, api));
var extracted = extractor.extractExpectedResponseExampleWithMediaType(operation, openAPI, 200);
```

The generator is invoked only when no usable explicit example exists, so explicit-example precedence stays in one tested place. `ExtractedResponseExample` carries `body` and `mediaType`; a null `body` with a non-null `mediaType` means content was declared but nothing was extractable.

The legacy `ResponseExampleExtractor(SchemaExampleValueGenerator)` constructor applies `RESPONSE_DEFAULTS` for the include/fallback flags and only honors `maxExampleDepth` and `fullExample` from the generator's configured options --- use the `ResponseBodyGenerator` constructor to keep full control.

### SchemaMerger

Flattens composed schemas (`allOf`/`anyOf`/`oneOf`) with "meet" constraint semantics (tightest bounds win, required union, enum intersection). See [Schema composition](../concepts/schema-composition.md) for the merge rules.

```kotlin
// Resolves $ref against #/components/schemas/ and starts a fresh traversal
val merged = SchemaMerger().mergeWithSubSchemas(schema, openAPI)
```

The full-control overload `mergeWithSubSchemas(input, depth, visitedRefs, resolve)` remains for callers that manage traversal state; `getSchemaFlatCombinations` expands `oneOf`/`anyOf` into flat combinations under an optional `CombinationBudget`.

### SchemaTypeHelpers

Static helpers for `$ref` dereferencing (`resolveSchemaRef` is the null-tolerant entry point, `tryGetSchemaFromRef` the non-null variant), response resolution by status, example `$ref` resolution, and schema type checks (`isObject`, `isArray`, `isNumber`, ...). All are `@JvmStatic`.

### ExampleValueSettings

Configuration for the factory: provider order, generation flags, and per-provider settings (uuid/email/date/date-time/plain-string templates). `ExampleValueSettings.defaults()` is the Java-friendly entry point; `fromMap(map)` parses the `testGenerationSettings.exampleValues` YAML/DSL block. See [Distribution settings](../reference/distribution-settings.md#testgenerationsettingsexamplevalues) for the config reference.

## Standalone usage

```kotlin
dependencies {
    implementation("art.galushko.openapi.testgen:example-value:<version>")
}
```

```kotlin
val generator = SchemaExampleValueGeneratorFactory().create()
val value = generator.getExampleValue("petId", schema, openAPI)
```

From Java, the same pipeline reads naturally thanks to `@JvmOverloads` entry points and SAM conversion:

```java
SchemaExampleValueGenerator generator = new SchemaExampleValueGeneratorFactory().create();
Object value = generator.getExampleValue("petId", schema, openAPI);
ResponseExampleExtractor extractor = new ResponseExampleExtractor((s, api) -> generator.getExampleValue("response", s, api));
```

## Extending via the SPI

Implement `SchemaValueProvider` and register it with the factory under a unique id:

```kotlin
val factory = SchemaExampleValueGeneratorFactory(
    extraProviders = mapOf("my-format" to MyFormatValueProvider()),
)
```

Provider ids are open-world; add the id to `ExampleValueSettings.providers` to control ordering. See [SPI -- Value providers](../reference/spi.md#value-providers).

## Testing

```bash
./gradlew :example-value:test
./gradlew :example-value:check   # includes detekt + coverage (95% minimum) + apiCheck
```

## API reference

- Dokka API reference: [`docs/api/example-value/index.html`](../api/example-value/index.html)

## Related docs

- Concepts: [Schema composition](../concepts/schema-composition.md)
- Concepts: [Example value generation](../concepts/architecture.md#example-value-generation)
- Reference: [Distribution settings -- exampleValues](../reference/distribution-settings.md#testgenerationsettingsexamplevalues)
- Reference: [SPI -- Value providers](../reference/spi.md#value-providers)
- Modules: [Pattern-value](pattern-value.md)
- Modules: [Module catalog](index.md)
