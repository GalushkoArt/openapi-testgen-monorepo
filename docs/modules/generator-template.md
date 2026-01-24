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

- [Template generator how-to](../how-to/generators/template-generator.md)
- [Generator options catalog](../reference/catalogs/generator-options.md)

# Generator-Template Module

This document is non-normative and targets contributors. For core wiring, see
[test-suite-writer generator](../how-to/generators/test-suite-writer.md) and [architecture](../concepts/architecture.md).
For generator selection and configuration examples, see [template generator](../how-to/generators/template-generator.md).

## Scope and responsibilities

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

## Generator options

Options are parsed by `transformAndValidateTemplateOptions`:

- `templateSet`: template set id (default `restassured-java`).
- `classTemplatePath`: Mustache class template path (default `templates/{{templateSet}}/class.mustache`).
- `customTemplateDir`: optional filesystem directory for custom templates.
- `templateVariables`: map of custom variables available in templates.
- `outputFileExtension`: file extension (default inferred from templateSet).
- `outputFileNamePattern`: output naming pattern (default `{{className}}.{{outputFileExtension}}`).
- `writeMode`: `OVERWRITE` or `SKIP_IF_EXISTS`.
- `fileHeaderComment`: optional header string prepended by templates.

Template variables used by the built-in templates include:

- `classSuffix` (defaults to `Test` in code).
- `methodPrefix`, `methodSuffix` (defaults to empty strings).

## Templates

Built-in template sets:

- `restassured-java`
- `restassured-kotlin`

Each set provides a `class.mustache` and `method.mustache` template.

## API reference

- Dokka API reference: [`docs/api/generator-template/index.html`](../api/generator-template/index.html)

## Related docs

- Core module: [core](core.md)
- Template generator how-to: [template generator](../how-to/generators/template-generator.md)
