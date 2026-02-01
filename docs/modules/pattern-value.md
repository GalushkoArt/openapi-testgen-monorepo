---
description: The pattern-value module provides standalone regex-based value generation. It wraps a regex generator to produce deterministic strings that match or violate schema patterns, implementing the example-value SPI without depending on core.
---

# Module: `pattern-value`

`pattern-value` is a **standalone regex-based value generation** module. It wraps a regex generator to produce deterministic strings that match (or intentionally do not match) `schema.pattern`.

Important: it has **no `core` dependency**. It implements the `example-value` SPI.

## Depends on

- `example-value`

## Used by

- `pattern-support`

## Related docs

- Modules: [pattern-support](pattern-support.md), [example-value](example-value.md)
- Reference: [Value providers SPI](../reference/spi/value-providers.md)

## Scope and responsibilities (contributors)

`pattern-value` provides regex-based value generation for `schema.pattern`. It implements the
`SchemaValueProvider` SPI from `example-value` and has no dependency on `core`.

## Key types

This module centers around `PatternGenerationOptions`, `PatternValueGenerator`, and `PatternValueProvider`.

### PatternGenerationOptions

Configuration for pattern generation:

- `defaultMinLength`: fallback minimum length when neither schema nor pattern specifies one.
- `spaceChars`: characters considered whitespace for `\s` (and `\S`).
- `anyPrintableChars`: characters for `.` and negated character classes; null uses library defaults.

Options can be parsed from an untyped map via `PatternGenerationOptions.fromMap`.

### PatternValueGenerator

Wraps the Cornutum `regexp-gen` library to generate:

- Valid values that match a pattern (seeded by `variationIndex` for determinism).
- Invalid values that do not match a pattern (uses length hints only).

Generation returns null when the pattern cannot be processed or non-matching strings are
infeasible (e.g., `.*`).

### PatternValueProvider

Implements `SchemaValueProvider` for string schemas with `pattern`. It delegates to
`PatternValueGenerator` and respects `minLength` and `maxLength`.

## Usage

Standalone usage:

```kotlin
val generator = PatternValueGenerator(PatternGenerationOptions(defaultMinLength = 5))
val valid = generator.generateValidValue("^[A-Z]{3}$", minLength = null, maxLength = null, variationIndex = 0)
```

Core integration is provided by the `pattern-support` module.

## API reference

- Dokka API reference: [`docs/api/pattern-value/index.html`](../api/pattern-value/index.html)
