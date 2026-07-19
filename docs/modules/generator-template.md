---
description: The generator-template module contributes the Mustache-based `template` generator that renders test suites as Java or Kotlin source files. Covers the module's role, template resolution, output naming, write modes, and how embedders enable it.
---

# Module: `generator-template`

`generator-template` contributes the `template` artifact generator: it renders each generated
`TestSuite` as a source file (Java, Kotlin, or anything else a Mustache template can express).
Built-in template sets target RestAssured with JUnit 5.

Most users configure this module indirectly through `generatorOptions` in the CLI, Gradle plugin,
or YAML config — that surface is documented in [Generators](../how-to/generators.md#template-generator).
This page covers the module's role in the architecture and what embedders need to wire it.

## When to use

- Automatically active when using the CLI or Gradle plugin (`distribution-bundle` registers it by
  default) — select it with `generator: template`.
- Pass it explicitly when embedding `core` directly and you want source-code output instead of the
  built-in `test-suite-writer` data files.

## Depends on / used by

- **Depends on**: `core` (SPI types, generator registry contracts) and, transitively, `model`
- **Used by**: `distribution-bundle` (registered in `DistributionDefaults.modules()` alongside
  `pattern-support`)

## Installation

Only needed for direct embedding — CLI and Gradle plugin users get it transitively:

```kotlin
dependencies {
    implementation("art.galushko.openapi.testgen:generator-template:<version>")
}
```

## Key types

| Type | Visibility | Role |
|------|------------|------|
| `TemplateGeneratorModule` | public `object` | `TestGenerationModule` with id `template`; contributes the generator factory |
| `TemplateArtifactGeneratorFactory` | internal | Creates the generator for generator id `template` |
| `TemplateArtifactGenerator` | internal | Renders one output file per `TestSuite` from the class template |

The public surface is deliberately small: embedders interact with the module object and the
generator options map; rendering internals stay internal.

## How templates are resolved

- The class template path defaults to `templates/{{templateSet}}/class.mustache`, with
  `{{templateSet}}` substituted by the `templateSet` option (`restassured-java` by default).
- When `customTemplateDir` is set, templates are loaded from that filesystem directory; a missing
  file fails fast with `Custom template not found`. Without it, templates load from the classpath,
  where the built-in sets live (`templates/restassured-java/`, `templates/restassured-kotlin/`,
  each with `class.mustache` and `method.mustache`).
- Compiled templates are cached per path for the generator's lifetime.

## How output files are named and written

- Class name: the suite's `operationName` (falling back to the path segments) converted to
  PascalCase, plus the `classSuffix` template variable (default `Test`) — e.g. operation
  `createPet` becomes `CreatePetTest`.
- File name: the `outputFileNamePattern` option, default `{{className}}.{{outputFileExtension}}`.
  `outputFileExtension` is inferred from the template set name (`java` or `kt`) and must be set
  explicitly for custom sets that name neither language.
- `writeMode` is `OVERWRITE` by default; `SKIP_IF_EXISTS` preserves existing files.
- Files are written atomically (temp file + atomic move), so failed runs never leave partial output.
- Test cases flagged `needToComplete` (the [positive baseline case](../how-to/positive-testing.md))
  render with a TODO review note in the generated method.

For the full option table (`templateSet`, `customTemplateDir`, `classTemplatePath`,
`templateVariables`, and friends), see
[Template generator options](../how-to/generators.md#template-generator-options) — that table is
canonical and not repeated here.

## Enable the module when embedding core

```kotlin
import art.galushko.openapi.testgen.config.TestGenerationEngine
import art.galushko.openapi.testgen.config.TestGeneratorExecutionOptionsFactory
import art.galushko.openapi.testgen.config.TestGeneratorOverrides
import art.galushko.openapi.testgen.generator.template.TemplateGeneratorModule
import java.nio.file.Path

val options = TestGeneratorExecutionOptionsFactory.fromConfig(
    config = null,
    overrides = TestGeneratorOverrides(
        specFile = "openapi.yaml",
        outputDir = Path.of("build/generated-tests"),
        generatorId = "template",
        generatorOptions = mapOf(
            "templateSet" to "restassured-java",
            "templateVariables" to mapOf(
                "package" to "com.example.generated",
                "baseUrl" to "http://localhost:8080",
            ),
        ),
    ),
)

val modules = listOf(TemplateGeneratorModule)
val report = TestGenerationEngine.generateReport(options, modules)
val generator = TestGenerationEngine.createArtifactGenerator(options, modules)
generator.generateTests(report.successfulSuites)
```

When using `TestGenerationRunner` from `distribution-bundle` instead, the module is already
registered — see [Distribution-bundle](distribution-bundle.md).

## Testing

```bash
./gradlew :generator-template:test
./gradlew :generator-template:check
```

## API reference

- Dokka API reference: [`docs/api/generator-template/index.html`](../api/generator-template/index.html)

## Related docs

- How-to: [Generators — template generator](../how-to/generators.md#template-generator) (options table)
- How-to: [Custom Mustache templates](../how-to/generators.md#custom-mustache-templates)
- How-to: [Positive testing](../how-to/positive-testing.md) (the `needToComplete` TODO note)
- Reference: [SPI — ArtifactGenerator](../reference/spi.md#artifactgenerator)
- Modules: [Module catalog](index.md)
