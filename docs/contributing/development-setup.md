# Development Guide

This guide targets contributors and maintainers. For a system-level view, see
[architecture](../concepts/architecture.md). For core internals, see the [core module](../modules/core.md).

## Project structure

This repository is a Gradle composite build with these key modules:

- `model`: shared data types (`TestCase`, `TestSuite`, error types).
- `example-value`: schema-derived example value generation (SPI + built-ins).
- `core`: test generation engine (providers, rules, generators).
- `generator-template`: Mustache-based code generator.
- `pattern-value`: regex-based value generation (no core dependency).
- `pattern-support`: pattern integration module (rule + provider + settings).
- `distribution-bundle`: default wiring for CLI and Gradle plugin.
- `plugin`: Gradle plugin for build integration.
- `cli`: command-line interface (JVM and native).
- `samples`: runnable example projects.

## Build and test

Common commands:

```bash
./gradlew check
./gradlew :core:test
./gradlew :plugin:test
./gradlew :cli:test
./gradlew detekt
./gradlew apiCheck
```

CLI distribution builds:

```bash
./gradlew :cli:installDist
./gradlew :cli:shadowJar
./gradlew :cli:nativeCompile
```

## Working on generators, rules, and providers

- Rules: implement `SimpleSchemaValidationRule` or `AuthValidationRule` and register via
  `TestGenerationModule` (see [SPI](../reference/spi/index.md) and [rules catalog](../reference/catalogs/rules-catalog.md)).
- Providers: implement `TestCaseProvider<T>` and wire via a custom `TestSuiteGenerator` or
  `TestGeneratorConfigurer` (see [providers catalog](../reference/catalogs/providers-catalog.md)).
- Generators: implement `ArtifactGeneratorFactory` and register via a module (see
  [generators docs](../how-to/generators/test-suite-writer.md)).

When generator output changes, update snapshot fixtures under `core/src/test/resources` and run
module tests.

## Configuration defaults

If you add module settings or change defaults, update:

- `DistributionDefaults` in `distribution-bundle`
- Documentation in `docs/` (index and affected module docs)

## Native image considerations

The CLI native image configuration is maintained under `cli/src/main/resources/META-INF/native-image/`.
After adding dependencies that rely on reflection or resources, regenerate configs:

```bash
./gradlew :cli:regenerateNativeImageConfig
```

See the [CLI reference](../reference/cli.md) for details.

## Documentation maintenance

- Keep [docs/index.md](../index.md) in sync with new or renamed docs.
- Ensure cross-links remain valid and reflect module boundaries.
- When adding new extension points, update [SPI docs](../reference/spi/index.md).
