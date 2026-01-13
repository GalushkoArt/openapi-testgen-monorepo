# Module: `distribution-bundle`

`distribution-bundle` is the shared "product layer" used by both the **CLI** and the **Gradle plugin**. It bundles feature modules and exposes a stable execution entry point.

## Depends on

- `core`
- `generator-template`
- `pattern-support`

## Used by

- `cli`
- `plugin`

## Key entry points

### TestGenerationRunner

Runs a generation end-to-end with configurable modules and reporting:

1. Merges config + overrides into `TestGeneratorExecutionOptions`
2. Extracts module settings (e.g., pattern generation options)
3. Creates modules via a `ModuleFactory`
4. Generates a `GenerationReport`
5. Writes artifacts when generation succeeds or `alwaysWriteTests` is set

Use `withDefaults(reporter)` for standard wiring or the builder for customization.

### TestGenerationReporter

Output abstraction allowing environment-specific logging:

- CLI uses `Slf4jReporter` which delegates formatting to core `ConsoleReporter`
- Gradle uses a logger adapter that routes to `project.logger`

### DistributionDefaults

Factory for standard modules, settings, and extractors:

- **Modules**: `TemplateGeneratorModule`, `PatternSupportModule`
- **Extractors**: `PatternModuleSettingsExtractor`
- **Settings**: inserts provider id `pattern` before `plain-string` in example-value order

## Extending the distribution

For embedding/integration scenarios, prefer:

- `TestGenerationRunner.builder()` to add modules or custom reporters
- `TestGenerationEngine` (from `core`) if you want finer control over orchestration

## API reference

- Dokka API reference: [`docs/api/distribution-bundle/index.html`](../api/distribution-bundle/index.html)

## Related docs

- Concepts: [Architecture](../concepts/architecture.md)
- How-to:
  - [Custom modules](../how-to/extension/custom-modules.md)
  - [Custom generators](../how-to/extension/custom-generators.md)
- Modules:
  - [Core module](core.md)
  - [Pattern support](pattern-support.md)
