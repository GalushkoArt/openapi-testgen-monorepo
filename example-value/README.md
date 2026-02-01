# Example-Value Module

Schema-based example value generation for OpenAPI schemas.

This module generates realistic example values from OpenAPI schema definitions, supporting
type-specific generation, format hints (email, date, uuid), and extensibility via the
`SchemaValueProvider` SPI.

## Features

- Type-aware value generation (string, number, boolean, array, object)
- Format support: `email`, `date`, `date-time`, `uuid`, `uri`, `hostname`
- Schema constraint awareness: `enum`, `const`, `default`, `example`
- Configurable generation depth for nested schemas
- Extensible via `SchemaValueProvider` SPI

## Usage

This module can be used standalone or as part of the full test generation pipeline.

### Standalone

```kotlin
dependencies {
    implementation("art.galushko.openapi:testgen-example-value:0.9.2")
}
```

```kotlin
val generator = SchemaExampleValueGeneratorFactory().create(
    options = SchemaExampleValueGeneratorOptions(),
    providers = listOf(EnumValueProvider(), ConstValueProvider(), PlainStringValueProvider())
)

val exampleValue = generator.generate(schema, openAPI)
```

### With Core

When using `core` or `distribution-bundle`, example value generation is automatically configured.

## Documentation

- [Module Overview](https://docs.galushko.art/openapi-test-generator/modules/example-value/)
- [Value Providers SPI](https://docs.galushko.art/openapi-test-generator/reference/spi/value-providers/)
- [API Reference (Dokka)](https://docs.galushko.art/openapi-test-generator/api/)

## Development

```bash
./gradlew :example-value:test
./gradlew :example-value:check
```
