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

## Usage

Use this module directly when you want schema-derived example generation without the rest of the test-generation pipeline.

```kotlin
dependencies {
    implementation("art.galushko.openapi.testgen:example-value:<version>")
}
```

When using `core` or `distribution-bundle`, example value generation is already wired for you.

## Documentation

- [Module Catalog](https://docs.galushko.art/openapi-test-generator/modules/#example-value)
- [Value Providers SPI](https://docs.galushko.art/openapi-test-generator/reference/spi/#value-providers)
- [API Reference (Dokka)](https://docs.galushko.art/openapi-test-generator/api/)

## Development

```bash
./gradlew :example-value:test
./gradlew :example-value:check
```
