# Value providers SPI

Schema-derived example values are generated via the `example-value` module.

## SchemaValueProvider

`SchemaValueProvider` is a functional interface:

```kotlin
fun provide(schema: Schema<*>, variationIndex: Int): Any?
```

Guidelines:

- Return `null` when not applicable.
- Keep outputs deterministic for identical inputs.
- Use `variationIndex` to generate deterministic variations when uniqueness is required.

See also: [example-value module](../../modules/example-value.md)

