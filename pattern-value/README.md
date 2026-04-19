# Pattern-Value Module

Regex-based example value generation for OpenAPI schema patterns.

This module generates string values that match `schema.pattern` regular expressions. It implements
the `SchemaValueProvider` SPI from `example-value` and can be used standalone or integrated via
`pattern-support`.

## Features

- Generates strings matching arbitrary regex patterns
- Configurable length constraints and character sets
- Thread-safe, deterministic generation
- No dependency on `core` (can be used standalone)

## Usage

### Standalone

```kotlin
dependencies {
    implementation("art.galushko.openapi.testgen:pattern-value:<version>")
}
```

```kotlin
val generator = PatternValueGenerator(
    options = PatternGenerationOptions(
        defaultMinLength = 5,
    )
)

val value = generator.generateValidValue("^[A-Z]{2}\\d{4}$", minLength = null, maxLength = null, variationIndex = 0)
```

### As Value Provider

```kotlin
val provider = PatternValueProvider(generator)
val value = provider.provide(schema, context)
```

### With Core (via pattern-support)

When using `distribution-bundle`, pattern generation is automatically included. For custom
wiring, add `pattern-support` which registers the provider and adds the `InvalidPattern` rule.

## Configuration

| Option | Default | Description |
|--------|---------|-------------|
| `defaultMinLength` | 3 | Minimum generated string length when the schema does not define one |
| `spaceChars` | ` \t\f\n\r\u00a0` | Characters for `\s` matching |
| `anyPrintableChars` | ASCII printable | Characters for `.` matching |

## Documentation

- [Module Catalog](https://docs.galushko.art/openapi-test-generator/modules/#pattern-value)
- [Pattern Support Module](https://docs.galushko.art/openapi-test-generator/modules/#pattern-support)
- [Value Providers SPI](https://docs.galushko.art/openapi-test-generator/reference/spi/#value-providers)
- [API Reference (Dokka)](https://docs.galushko.art/openapi-test-generator/api/)

## Development

```bash
./gradlew :pattern-value:test
./gradlew :pattern-value:check
```
