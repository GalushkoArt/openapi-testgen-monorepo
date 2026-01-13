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

## 3) Wire the module

- **Embedding**: use `TestGenerationRunner.builder()` and add your module (see [distribution-bundle](../../modules/distribution-bundle.md)).
- **CLI / Gradle plugin**: the default distribution modules are defined by `DistributionDefaults.modules(...)`.

## Related docs

- Concepts: [Architecture](../../concepts/architecture.md)
- API: [`TestGenerationModule`](../../api/core/index.html)

# Add custom modules

Modules are the primary extension mechanism. A module can contribute:

- Artifact generators
- Schema value providers
- Schema validation rules
- Auth validation rules
- Module-owned settings extraction

## Implement a module

```kotlin
import art.galushko.openapi.testgen.config.TestGenerationModule
import art.galushko.openapi.testgen.config.TestGeneratorExecutionOptions
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import art.galushko.openapi.testgen.generator.ArtifactGeneratorFactory
import art.galushko.openapi.testgen.spi.AuthValidationRule
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule

class MyModule : TestGenerationModule {
    override val id: String = "my-module"

    override fun artifactGeneratorFactories(): List<ArtifactGeneratorFactory> = emptyList()

    override fun schemaValueProviders(options: TestGeneratorExecutionOptions): Map<String, SchemaValueProvider> = emptyMap()

    override fun extraSimpleSchemaRules(options: TestGeneratorExecutionOptions): List<SimpleSchemaValidationRule> = emptyList()

    override fun extraAuthRules(options: TestGeneratorExecutionOptions): List<AuthValidationRule> = emptyList()
}
```

## Wire the module in

- **Embedded usage**: pass modules to `TestGenerationEngine`.
- **CLI/Gradle**: use the distribution runner’s builder to supply additional modules (or provide a custom distribution wiring in your own app/plugin).

## Determinism requirements

- Module ids must be stable and unique.
- Avoid non-deterministic iteration; sort where needed.
- Validate settings early and fail with actionable errors.

!!! note
    Module-owned settings extraction is configured via `ModuleSettingsExtractor` (not via `TestGenerationModule`).
    In CLI/Gradle use cases this is usually handled by the distribution wiring.

