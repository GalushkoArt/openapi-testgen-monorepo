---
description: The pattern-value module generates string values from OpenAPI schema regex patterns using the Cornutum regexp-gen library. It implements the SchemaValueProvider SPI from example-value and has no dependency on core.
---

# Module: `pattern-value`

`pattern-value` generates string values that match (or deliberately do not match) ECMA-262 regular expression patterns declared in OpenAPI string schemas. It is a **standalone** module with no dependency on `core`.

Most users do not interact with this module directly --- it is wired automatically through `pattern-support` and `distribution-bundle`. Direct use is appropriate when you need regex-based value generation outside of the test-generation pipeline.

## Depends on

- `example-value` (for the `SchemaValueProvider` SPI)

## Used by

- `pattern-support` (registers the provider and the `InvalidPattern` rule in the core engine)

## Public API

The module exposes three public classes.

### PatternValueGenerator

The central class. It wraps the [Cornutum regexp-gen](https://github.com/cornutum/regexp-gen) library (v3.0.0) and provides two operations:

| Method | Purpose |
|--------|---------|
| `generateValidValue(pattern, minLength?, maxLength?, variationIndex)` | Returns a string that **matches** the pattern within the given length bounds, or `null` on failure |
| `generateInvalidValue(pattern, minLength?, maxLength?)` | Returns a string that does **not** match the pattern, or `null` when the pattern accepts all strings |

Both methods return `null` (rather than throwing) when generation is impossible and log a warning via SLF4J.

**Determinism.** Valid-value generation uses `variationIndex` as the random seed, so the same inputs always produce the same output. Invalid-value generation uses a fixed seed.

**Length negotiation.** When a schema declares `minLength` / `maxLength` alongside a `pattern`, the generator reconciles the constraints:

- The effective minimum is the greater of the pattern's intrinsic minimum and the schema `minLength`, falling back to `PatternGenerationOptions.defaultMinLength`.
- The effective maximum is the lesser of the pattern's intrinsic maximum and the schema `maxLength`.
- If the effective minimum exceeds the effective maximum, generation returns `null`.

### PatternValueProvider

Implements `SchemaValueProvider` from `example-value`. It delegates to `PatternValueGenerator` and activates only for string schemas that have a non-null `pattern` property.

```kotlin
val provider = PatternValueProvider()          // uses default generator
val value = provider.provide(stringSchema, 0)  // returns String? matching the pattern
```

### PatternGenerationOptions

An immutable `data class` controlling generator behavior.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `defaultMinLength` | `Int` | `3` | Fallback minimum length when neither the schema nor the pattern specifies one |
| `spaceChars` | `String` | `" \t\f\n\r\u00a0"` | Characters that match `\s`; also affects `\S` |
| `anyPrintableChars` | `String?` | `null` | Characters that match `.` (dot); `null` uses the library default (Latin-1 printable) |

Validation rules:

- `defaultMinLength` must be non-negative.
- `spaceChars` must not be empty.
- `anyPrintableChars`, if non-null, must not be empty.

`PatternGenerationOptions.fromMap(map)` parses options from an untyped `Map<String, Any?>`, which is the format used by YAML config and the Gradle plugin DSL (the `testGenerationSettings.patternGeneration` block). See [Distribution settings](../reference/distribution-settings.md#testgenerationsettingspatterngeneration) for the YAML reference.

## Standalone usage

Add the dependency directly (no `core` or `distribution-bundle` required):

```kotlin
dependencies {
    implementation("art.galushko.openapi.testgen:pattern-value:<version>")
}
```

Generate values programmatically:

```kotlin
val generator = PatternValueGenerator(
    PatternGenerationOptions(defaultMinLength = 5)
)

// Valid value matching the pattern
val valid = generator.generateValidValue("^[A-Z]{2}\\d{4}$", null, null, 0)
// valid == "SX7531"  (deterministic for seed 0)

// Invalid value that does NOT match the pattern
val invalid = generator.generateInvalidValue("^[A-Z]{3}$", null, null)
```

Or use the SPI adapter:

```kotlin
val provider = PatternValueProvider(generator)
val value = provider.provide(schema, variationIndex = 0)
```

## Library limitations

The underlying regexp-gen library does not support every regex feature:

- **Negative lookahead / lookbehind** (`(?!...)`, `(?<!...)`)
- **Word boundary assertions** (`\b`, `\B`)
- **Backreferences**
- **Catch-all patterns** (`.*`) --- cannot produce a non-matching string

When these are encountered, the generator returns `null` and logs a warning.

## Integration with the test-generation pipeline

When using `distribution-bundle` (CLI or Gradle plugin), pattern generation is enabled automatically:

1. `DistributionDefaults` inserts the `pattern` provider id before `plain-string` in the example-value provider order.
2. `PatternSupportModule` creates a `PatternValueProvider` (for valid example values) and an `InvalidPatternSchemaValidationRule` (for negative test cases).
3. `PatternModuleSettingsExtractor` reads the `patternGeneration` block from `testGenerationSettings`.

You do not need to add `pattern-value` as a direct dependency when using `distribution-bundle`, `cli`, or the Gradle plugin.

## Testing

```bash
./gradlew :pattern-value:test
./gradlew :pattern-value:check   # includes detekt + coverage (90% minimum)
```

## API reference

- Dokka API reference: [`docs/api/pattern-value/index.html`](../api/pattern-value/index.html)

## Related docs

- Concepts: [Example value generation](../concepts/architecture.md#example-value-generation)
- Reference: [Distribution settings -- patternGeneration](../reference/distribution-settings.md#testgenerationsettingspatterngeneration)
- Reference: [SPI -- SchemaValueProvider](../reference/spi.md#schemavalueprovider)
- Reference: [Rules catalog](../reference/catalogs/rules-catalog.md) (for `InvalidPatternSchemaValidationRule`)
- Modules: [Pattern support](index.md#pattern-support)
- Modules: [Module catalog](index.md)
