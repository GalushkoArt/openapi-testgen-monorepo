# Test-suite-writer generator (`test-suite-writer`)

This page describes the built-in `test-suite-writer` generator and its options.

For core entry points and extension context, see the [core module](../../modules/core.md).
For distribution defaults and wiring, see [distribution settings](../../reference/distribution-settings.md).
For configuration via YAML, see [YAML config](../configuration/yaml-config.md).

## Overview

The `generator` package defines the artifact generation infrastructure and the built-in
`test-suite-writer` generator. Template-based code generation lives in the `generator-template`
module and is wired via `TestGenerationModule`.

## ArtifactGeneratorFactory

Factories implement `ArtifactGeneratorFactory`:

- `id` must be unique and stable.
- `description` is user-facing.
- `create(outputDir, options)` should validate options and return a fresh generator instance.

## ArtifactGeneratorRegistry

`ArtifactGeneratorRegistry`:

- Registers built-in factories from `BuiltInGenerators`.
- Accepts extra factories via constructor injection.
- Rejects duplicate IDs.
- Exposes `availableIds()` and `availableGenerators()` in sorted order.

`ArtifactGeneratorConfigurer` is a convenience for creating a generator from built-ins only.

## BuiltInGenerators and GeneratorIds

Built-in generators are listed explicitly in `BuiltInGenerators` (no reflection). IDs are defined
in `GeneratorIds`:

- `test-suite-writer`
- `template` (provided by `generator-template` module)

## TestSuiteWriter (built-in)

`TestSuiteWriter` writes test suites as JSON or YAML. It is **stateful** and **not thread-safe**.
Create a new instance per generation run.

Output modes:

- `SINGLE_FILE`: aggregate all suites into a single file keyed by operation name.
- `MULTIPLE_FILES`: write one file per suite (prefix + operation name + extension).

Merge behavior:

- `writeMode = MERGE` loads existing suites and merges by test case name.
- `preventOverwriteSuites` blocks suite-level replacement (default true when unset).
- `preventOverwriteCases` preserves existing test case fields during merge (default true).
- `protectedTestCaseFields` lists fields to preserve when merging cases; when empty and
  `preventOverwriteCases` is true, existing cases are kept as-is.

Writes are atomic: data is written to a temp file and moved into place.

## TestSuiteWriter options

Parsed by `transformAndValidateWriterOptions` from a map:

- `outputMode`: `SINGLE_FILE` or `MULTIPLE_FILES` (default `SINGLE_FILE`; invalid values fall back).
- `outputFileName`: required for `SINGLE_FILE` (ignored for `MULTIPLE_FILES`).
- `format`: `JSON` or `YAML` (default `JSON`; invalid values fall back).
- `indent`: indentation string for JSON output (default 4 spaces).
- `writeMode`: `MERGE` (default) or `OVERWRITE` (invalid strings fall back to `MERGE`).
- `preventOverwriteSuites`: boolean (default `true` when unset or invalid string).
- `preventOverwriteCases`: boolean (default `true` when unset or invalid string).
- `protectedTestCaseFields`: list or comma-separated string (default empty).
- `fileNamePrefix`: prefix for per-suite files (default empty).

Invalid types for `writeMode`, `preventOverwriteSuites`, `preventOverwriteCases`, or
`protectedTestCaseFields` throw `IllegalArgumentException`. Invalid strings generally fall back to
defaults.

## Extending generators

To add a custom generator:

1. Implement `ArtifactGeneratorFactory` and `ArtifactGenerator`.
2. Register the factory via `TestGenerationModule.artifactGeneratorFactories`.
3. Pass your module into `TestGenerationEngine.createArtifactGenerator` or `generateReport`.

## Related docs

- Template generator module: [generator-template](../../modules/generator-template.md)
