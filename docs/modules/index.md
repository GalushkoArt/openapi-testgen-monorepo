---
description: Overview of the OpenAPI Test Generator monorepo module structure, showing the dependency flow from model through core to CLI and Gradle plugin. Provides a catalog of all modules with links to detailed documentation.
---

# Modules

This repository is a Gradle **composite build** made of small, focused Kotlin modules. Most users interact through the **CLI** or the **Gradle plugin**, both of which depend on `distribution-bundle`.

## Dependency direction (high level)

`model` → `example-value` → `core` → (feature modules) → `distribution-bundle` → `cli` / `plugin`

See also: [Architecture](../concepts/architecture.md).

## Module catalog

- [model](model.md): shared data model (`TestCase`, `TestSuite`, `Outcome`, `GenerationReport`)
- [example-value](example-value.md): schema-derived example value generation (standalone library + SPI)
- [core](core.md): OpenAPI parsing + generation engine + built-in generator (`test-suite-writer`)
- [pattern-value](pattern-value.md): regex-based value generation (standalone, no `core` dependency)
- [pattern-support](pattern-support.md): integrates `pattern-value` into `core` as an optional module
- [generator-template](generator-template.md): Mustache-based code generation (`template` generator module)
- [distribution-bundle](distribution-bundle.md): shared runner + defaults used by CLI and Gradle plugin
- [plugin](plugin.md): Gradle plugin (`generateOpenApiTests`, `openApiTestGenerator { ... }`)
- [cli](cli.md): Picocli-based CLI (`openapi-testgen`)

