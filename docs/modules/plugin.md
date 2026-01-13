# Module: `plugin`

`plugin` provides the Gradle plugin integration (`art.galushko.openapi-test-generator`). It registers one task (`generateOpenApiTests`) and wires it into the build depending on `generator` and `manualOnly`.

## Depends on

- `distribution-bundle`

## Key types

- `OpenApiTestGeneratorPlugin`: plugin entry point
- `OpenApiTestGeneratorTask`: task implementation that delegates to `TestGenerationRunner`
- `TestGeneratorExtension`: Gradle DSL entry point (`openApiTestGenerator { ... }`)
- `TestGenerationSettingsExtension`: typed nested DSL (`testGenerationSettings { ... }`)

## Related docs

- Reference:
  - [Gradle plugin reference](../reference/gradle-plugin.md)
- Getting started:
  - [Gradle integration](../getting-started/gradle-integration.md)

# Plugin module

## Overview

`plugin` provides the Gradle plugin `art.galushko.openapi-test-generator`. It registers a single task, `generateOpenApiTests`, and exposes a typed DSL via the `openApiTestGenerator { ... }` extension.

## Key behaviors

- Registers `generateOpenApiTests` in group `verification`.
- Supports an optional YAML config file (`configFile`) and typed overrides in Gradle DSL.
- Wires generation into compilation/resource processing when `manualOnly` is `false`.
  Wiring differs by generator:
  - `template`: adds `outputDir` to test sources
  - `test-suite-writer`: adds `outputDir` to test resources

## API reference

- Dokka API reference: [`docs/api/plugin/index.html`](../api/plugin/index.html)

## Related documentation

- Reference: [Gradle plugin DSL](../reference/gradle-plugin.md)
- Getting started: [Gradle integration](../getting-started/gradle-integration.md)

