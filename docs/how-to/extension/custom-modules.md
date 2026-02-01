---
description: Learn how to create custom TestGenerationModule implementations to extend the test generator with artifact generators, schema value providers, and validation rules without using reflection.
---

# Custom modules

`TestGenerationModule` is the primary mechanism for adding **optional features without reflection**. Modules can contribute:

- `ArtifactGeneratorFactory` implementations (new generators)
- `SchemaValueProvider` implementations (new example value providers)
- extra schema rules (`SimpleSchemaValidationRule`)
- extra auth rules (`AuthValidationRule`)

## 1) Implement `TestGenerationModule`

```kotlin
import art.galushko.openapi.testgen.config.TestGenerationModule
import art.galushko.openapi.testgen.config.TestGeneratorExecutionOptions
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import art.galushko.openapi.testgen.generator.ArtifactGeneratorFactory
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule

public class MyModule(
    private val provider: SchemaValueProvider,
) : TestGenerationModule {
    override val id: String = "my-module"

    override fun schemaValueProviders(options: TestGeneratorExecutionOptions): Map<String, SchemaValueProvider> =
        mapOf("my-provider" to provider)

    override fun artifactGeneratorFactories(): List<ArtifactGeneratorFactory> = emptyList()

    override fun extraSimpleSchemaRules(options: TestGeneratorExecutionOptions): List<SimpleSchemaValidationRule> = emptyList()
}
```

## 2) (Optional) Add settings extraction

If your module needs its own settings blob, implement `ModuleSettingsExtractor` and add it to execution (typically via `TestGenerationRunner.builder().addModuleExtractor(...)`).

!!! note "Module-owned settings"
    `ModuleSettingsExtractor` is the mechanism for parsing module-owned settings blobs. It is separate from `TestGenerationModule` and runs before core settings parsing.

## 3) Wire the module

- **Embedding**: use `TestGenerationRunner.builder()` and add your module (see [distribution-bundle](../../modules/distribution-bundle.md)).
- **CLI / Gradle plugin**: the default distribution modules are defined by `DistributionDefaults.modules(...)`.

## Determinism requirements

- Module ids must be stable and unique.
- Avoid non-deterministic iteration; sort where needed.
- Validate settings early and fail with actionable errors.

## Related docs

- Concepts: [Architecture](../../concepts/architecture.md)
- API: [TestGenerationModule](../../api/core/index.html)

