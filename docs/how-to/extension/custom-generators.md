---
description: Create custom artifact generators that emit source code, JSON, YAML, or other outputs from generated test suites. Covers implementing ArtifactGenerator and ArtifactGeneratorFactory interfaces and registering them via modules.
---

# Custom generators

Generators emit artifacts from generated `TestSuite`s (source code, JSON/YAML, etc.).

## 1) Implement `ArtifactGenerator` + `ArtifactGeneratorFactory`

The factory validates options and creates a new generator instance per execution:

```kotlin
import art.galushko.openapi.testgen.generator.ArtifactGeneratorFactory
import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.spi.ArtifactGenerator
import java.io.File

public object MyGeneratorFactory : ArtifactGeneratorFactory {
    override val id: String = "my-generator"
    override val description: String = "Writes my custom artifacts"

    override fun create(outputDir: File, options: Map<String, Any?>): ArtifactGenerator {
        // Validate options early and fail with actionable messages.
        return object : ArtifactGenerator {
            override fun generateTests(testSuite: TestSuite) {
                // Keep output deterministic (stable ordering, no timestamps).
                outputDir.mkdirs()
                // ... write files ...
            }
        }
    }
}
```

## 2) Register via `TestGenerationModule`

Create a module that contributes your factory:

```kotlin
import art.galushko.openapi.testgen.config.TestGenerationModule
import art.galushko.openapi.testgen.generator.ArtifactGeneratorFactory

public class MyGeneratorModule : TestGenerationModule {
    override val id: String = "my-generator-module"
    override fun artifactGeneratorFactories(): List<ArtifactGeneratorFactory> = listOf(MyGeneratorFactory)
}
```

## 3) Use it

- **Embedding**: add the module when executing (see [distribution-bundle](../../modules/distribution-bundle.md)).
- **CLI / Gradle plugin**: currently use `DistributionDefaults.modules(...)`. To make a new generator available there, it must be added to the distribution (contributor change).

## Option validation and determinism

- Validate generator options early and fail with actionable error messages.
- Keep output deterministic (stable ordering, no timestamps, no randomness).
  - Sort keys and class lists when serializing maps/sets.
  - Avoid filesystem-dependent ordering.

## Related docs

- Reference: [Generators SPI](../../reference/spi/generators.md)
- Catalog: [Generator options](../../reference/catalogs/generator-options.md)

