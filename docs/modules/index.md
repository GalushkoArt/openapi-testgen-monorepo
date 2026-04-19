---
description: Module catalog for the OpenAPI Test Generator composite build, covering the dependency flow, thin-module responsibilities, and links to the remaining deep-dive module pages.
---

# Modules

This repository is a Gradle **composite build** made of small Kotlin modules. Most users interact through the **CLI** or the **Gradle plugin**, both of which depend on `distribution-bundle`.

## Dependency direction (high level)

`model` -> `example-value` -> `core` -> feature modules -> `distribution-bundle` -> `cli` / `plugin`

See also: [Architecture](../concepts/architecture.md).

## Deep-dive pages

Keep these pages when you need subsystem-level detail:

- [core](core.md)
- [pattern-value](pattern-value.md)
- [distribution-bundle](distribution-bundle.md)

## Module catalog

| Module | Responsibility | Used by / notes |
|--------|----------------|-----------------|
| `model` | Shared data types and error/reporting contracts | Foundation for the rest of the stack |
| `example-value` | Schema-derived example generation and `SchemaValueProvider` SPI | Used by `core`; extended by `pattern-value` |
| `core` | OpenAPI parsing, provider/rule orchestration, built-in `test-suite-writer` | See [core](core.md) |
| `pattern-value` | Regex-based value generation without a `core` dependency | See [pattern-value](pattern-value.md) |
| `pattern-support` | Optional pattern integration module for `core` | Enabled by `distribution-bundle` defaults |
| `generator-template` | Mustache-based source-code generation (`template`) | Enabled by `distribution-bundle` defaults |
| `distribution-bundle` | Shared runner, defaults, and module wiring | Used by CLI and plugin |
| `plugin` | Gradle plugin and typed DSL | See [Gradle plugin reference](../reference/gradle-plugin.md) |
| `cli` | Picocli command-line entry point and native/JVM packaging | See [CLI reference](../reference/cli.md) |

## `model`

- Canonical data types: `TestSuite`, `TestCase`, `Outcome`, `GenerationReport`, `GenerationError`
- Has no project-module dependencies
- Start with [Model reference](../reference/model.md) for field-level details

## `example-value`

- Owns `SchemaValueProvider`, `SchemaExampleValueGenerator`, response example extraction, and schema merging support
- Depends on `model`
- Start with [SPI](../reference/spi.md#value-providers) and [Distribution settings](../reference/distribution-settings.md#testgenerationsettingsexamplevalues)

## `pattern-value`

- Standalone regex-based value generation
- Implements the `example-value` SPI without depending on `core`
- Used indirectly through `pattern-support` or directly by embedding code
- Start with [Pattern-value module](pattern-value.md) for public API, configuration options, and standalone usage

## `pattern-support`

- Adds the `pattern` schema value provider id, pattern validation rule wiring, and `patternGeneration` settings extraction
- Lives at the boundary between optional value generation and the core engine
- Start with [Distribution settings](../reference/distribution-settings.md#testgenerationsettingspatterngeneration) and [Rules catalog](../reference/catalogs/rules-catalog.md)

## `generator-template`

- Contributes the `template` generator module
- Owns built-in RestAssured template sets and custom-template loading
- Start with [Generators](../how-to/generators.md#template-generator)

## `plugin`

- Registers `generateOpenApiTests` and exposes `openApiTestGenerator { ... }`
- Delegates execution to `TestGenerationRunner`
- Start with [Gradle integration](../getting-started/gradle-integration.md) and [Gradle plugin reference](../reference/gradle-plugin.md)

## `cli`

- Maps CLI flags to `TestGeneratorOverrides`
- Ships as an npm wrapper, JVM distribution, and optional native binary
- Start with [Getting started](../getting-started/index.md), [Installation](../getting-started/installation.md), and [CLI reference](../reference/cli.md)

