# Example-Value Module

Schema-based example value generation for OpenAPI schemas.

This module generates realistic example values from OpenAPI schema definitions, supporting
type-specific generation, format hints (email, date, uuid), and extensibility via the
`SchemaValueProvider` SPI.

## Features

- Type-aware value generation (string, number, boolean, array, object)
- Format support: `email`, `date`, `date-time`, `uuid`, `uri`, `hostname`
- Schema constraint awareness: `enum`, `const`, `default`, `example`
- Full example mode for populating optional object properties and non-empty arrays
- Configurable generation depth for nested schemas
- Extensible via `SchemaValueProvider` SPI
- Response example extraction with a pluggable fallback body generator (`ResponseBodyGenerator`)
- Java-friendly API: options presets with `with*` methods, `@JvmOverloads` entry points, SAM-convertible interfaces

## Usage

Use this module directly when you want schema-derived example generation without the rest of the test-generation pipeline.

```kotlin
dependencies {
    implementation("art.galushko.openapi.testgen:example-value:<version>")
}
```

When using `core` or `distribution-bundle`, example value generation is already wired for you.

### Generating values from schemas

```kotlin
val generator = SchemaExampleValueGeneratorFactory().create()
val value = generator.getExampleValue("petId", schema, openAPI)
```

Tune generation with the options presets instead of positional flags — `REQUEST_DEFAULTS` and
`RESPONSE_DEFAULTS` are static fields, and `with*` methods express deltas (also from Java):

```java
var options = SchemaExampleValueGeneratorOptions.RESPONSE_DEFAULTS.withFullExample(true);
Object body = generator.getExampleValueWithOptions("response", schema, openAPI, options);
```

### Extracting response examples

`ResponseExampleExtractor` selects the response by status code (exact → range → `default`),
negotiates media types (JSON/JWT-like → XML → other), and prefers explicit spec examples.
When no usable explicit example exists, it falls back to a body generator you can plug in
(`ResponseBodyGenerator` is SAM-convertible from Java and Kotlin):

```java
var extractor = new ResponseExampleExtractor((schema, api) -> myGenerator.generate(schema, api));
var extracted = extractor.extractExpectedResponseExampleWithMediaType(operation, openAPI, 200);
// extracted.getMediaType() is the negotiated media type; a null body with a non-null
// media type means content was declared but nothing was extractable.
```

The `ResponseExampleExtractor(SchemaExampleValueGenerator)` constructor keeps the legacy behavior:
it applies `RESPONSE_DEFAULTS` for the include/fallback flags and only honors `maxExampleDepth`
and `fullExample` from the generator's configured options.

### Merging composed schemas and resolving refs

`SchemaMerger.mergeWithSubSchemas(schema, openAPI)` flattens `allOf`/`oneOf`/`anyOf` and resolves
`$ref` against `#/components/schemas/` in one call; `SchemaTypeHelpers.resolveSchemaRef(schema, openAPI)`
is the null-tolerant standalone resolver. `ExampleValueSettings.defaults()` provides a Java-friendly
settings entry point for `SchemaExampleValueGeneratorFactory.create(settings)`.

## Documentation

- [Module Catalog](https://docs.galushko.art/openapi-test-generator/modules/#example-value)
- [Value Providers SPI](https://docs.galushko.art/openapi-test-generator/reference/spi/#value-providers)
- [API Reference (Dokka)](https://docs.galushko.art/openapi-test-generator/api/)

## Development

```bash
./gradlew :example-value:test
./gradlew :example-value:check
```
