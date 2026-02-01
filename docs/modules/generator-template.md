---
description: The generator-template module provides Mustache-based code generation for producing Java and Kotlin test source files. It includes built-in template sets for RestAssured and supports custom templates and variables.
---

# Module: `generator-template`

`generator-template` provides the `template` generator: **Mustache-based code generation** for source-code tests (Java/Kotlin).

It is contributed as an explicit feature module (`TemplateGeneratorModule`) rather than being built-in to `core`.

## Depends on

- `core`

## Used by

- `distribution-bundle` (enabled by default via `DistributionDefaults`)

## Key types

- `TemplateGeneratorModule`: `TestGenerationModule` that registers the `template` generator
- `TemplateArtifactGenerator` (internal): renders templates and writes files

## Configuration

See:

- How-to: [Template generator](../how-to/generators/template-generator.md)
- How-to: [Custom templates](../how-to/generators/custom-templates.md)
- Reference: [Generator options](../reference/catalogs/generator-options.md)

## Scope and responsibilities (contributors)

`generator-template` provides Mustache-based code generation for test suites. It contributes a
single generator id `template` and loads templates from
`generator-template/src/main/resources/templates`.

## Key types

### TemplateGeneratorModule

`TemplateGeneratorModule` implements `TestGenerationModule` and registers the `template` generator
via `TemplateArtifactGeneratorFactory`.

### TemplateArtifactGenerator

`TemplateArtifactGenerator`:

- Renders test classes and methods from Mustache templates.
- Derives `className` from `operationName` (operationId) or from HTTP method + path when missing, then normalizes it into a valid identifier.
- Writes output files using `outputFileNamePattern`.
- Supports custom template directories via `customTemplateDir`.
- Uses `templateVariables` to pass user-defined values into templates.

## Templates

Built-in template sets:

- `restassured-java`
- `restassured-kotlin`

Each set provides a `class.mustache` and `method.mustache` template.

## API reference

- Dokka API reference: [`docs/api/generator-template/index.html`](../api/generator-template/index.html)

## Related docs

- Core module: [core](core.md)
- How-to: [Template generator](../how-to/generators/template-generator.md)
