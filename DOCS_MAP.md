# Documentation Map

Maps documentation pages to `mkdocs.yml` navigation. Keep in sync when adding/moving/renaming pages.

Keep entries lightweight: path + one-line purpose (no multi-paragraph content summaries).

Run `python3 skills/project-docs/scripts/sync_docs_map.py --check` to verify sync status.

---

## Repository Documentation (Outside MkDocs)

| File | Purpose |
|------|---------|
| `README.md` | Project entry point, installation, quick starts |
| `CLAUDE.md` | Claude Code entry point; imports `AGENTS.md` plus Claude-specific notes |
| `AGENTS.md` | Canonical repository guidelines for AI agents and automation |

### Module READMEs

| File | Purpose |
|------|---------|
| `cli/README.md` | CLI installation and usage |
| `plugin/README.md` | Gradle plugin configuration |
| `distribution-bundle/README.md` | Programmatic API (`TestGenerationRunner`) |
| `core/README.md` | Provider-rule engine overview |
| `example-value/README.md` | Schema value generation |
| `pattern-value/README.md` | Regex-based value generation |
| `pattern-support/README.md` | Pattern module integration |
| `generator-template/README.md` | Mustache template generator |
| `model/README.md` | Core data types |
| `npm/cli/README.md` | npm package README |

### Sample READMEs

| File | Purpose |
|------|---------|
| `samples/java-spring-file-writer/README.md` | JSON/YAML test suite generation |
| `samples/java-spring-rest-assured/README.md` | Java RestAssured tests |
| `samples/kotlin-spring-rest-assured/README.md` | Kotlin RestAssured tests |

---

## Site Documentation (`docs/`)

### Home

- `docs/index.md` - Entry point for OpenAPI Test Generator documentation. Start here for installation, quick starts, task guides, concepts, samples, and contributor docs.

### Getting Started

- `docs/getting-started/index.md` - Quick-start guide for generating your first OpenAPI-derived tests with the CLI or Gradle plugin, including what the generated output looks like.
- `docs/getting-started/installation.md` - Install the OpenAPI Test Generator CLI or Gradle plugin. Covers npm, GitHub releases, JVM/native distributions, platform notes, and source builds.
- `docs/getting-started/gradle-integration.md` - Minimal Gradle plugin setup for wiring OpenAPI-generated tests into a build, with notes on automatic wiring and manual-only mode.
- `docs/getting-started/end-to-end-workflow.md` - Complete tutorial from writing a small OpenAPI spec to generating JSON test suites, generating executable RestAssured tests, and running them locally and in CI.

### Samples

- `docs/samples/index.md` - Overview of sample projects demonstrating OpenAPI Test Generator integration with different generators, languages, and configurations including Spring Boot, RestAssured, and data-driven testing approaches.
- `docs/samples/java-spring-file-writer.md` - Demonstrates the test-suite-writer generator for outputting test suites as JSON or YAML files. Shows MERGE mode for preserving manual edits and protected fields when regenerating.
- `docs/samples/java-spring-rest-assured.md` - Demonstrates the template generator with RestAssured to produce executable Java test classes. Shows automatic build integration, template variables, and multiple generation tasks.
- `docs/samples/kotlin-spring-rest-assured.md` - Demonstrates the template generator with Kotlin and custom Mustache templates for full control over generated test code. Includes three generation tasks with different configurations.

### How-to

- `docs/how-to/configuration.md` - Configure test generation using YAML config files, CLI flags, and Gradle DSL. Covers operation filtering, ignore rules, security values, and positive testing.
- `docs/how-to/negative-testing.md` - Generate negative test cases that verify your API rejects invalid input. Covers path, query, header, and request body validation with OpenAPI examples, generated output, and filtering options.
- `docs/how-to/positive-testing.md` - Generate a baseline positive (2xx) test case per operation with includeValidCase. Covers enabling it via YAML, CLI, and Gradle, how the valid case is built, security and example-value prerequisites, generated output, and limitations.
- `docs/how-to/generators.md` - Configure built-in generators (template and test-suite-writer), create custom Mustache templates, and integrate with RestAssured and Spring Boot.
- `docs/how-to/extending.md` - Extend the test generator with custom validation rules, test case providers, artifact generators, and modules. Includes complete Kotlin examples, registration patterns, and debugging tips.
- `docs/how-to/ci-cd.md` - GitHub Actions and GitLab CI workflow examples, job patterns for splitting generation and tests, caching, artifact retention, and environment-specific overrides.
- `docs/how-to/troubleshooting.md` - Symptom-indexed solutions for generation errors, unexpected output, Gradle-specific issues, and CLI/npm platform problems. Headings quote the exact error messages so you can search for what you see.
- `docs/how-to/faq.md` - Frequently asked questions about OpenAPI Test Generator - installation, Gradle and CLI usage, security values, filtering, positive tests, customization, budgets, and running generated tests.

### Concepts

- `docs/concepts/architecture.md` - System architecture including module dependencies, data flow, provider-rule model, budget controls, determinism guarantees, and extension points. The primary reference for understanding how the generator is structured.
- `docs/concepts/schema-composition.md` - How the generator handles OpenAPI schema composition keywords (allOf, anyOf, oneOf), including merge behavior, test case generation strategies, cycle detection, and budget controls for composed schemas.
- `docs/concepts/error-handling.md` - Error handling strategy using explicit result types (Outcome, GenerationReport) to support best-effort behavior without hiding failures. Covers error modes, error aggregation, and the alwaysWriteTests option.
- `docs/concepts/glossary.md` - Definitions of key terms and concepts used throughout the documentation, including providers, rules, outcomes, budgets, and core data types.

### Reference

#### Catalogs

- `docs/reference/catalogs/rules-catalog.md` - Complete catalog of built-in validation rules for OpenAPI test generation, including schema rules (array, boundary, enum, type, string, object, date), pattern module rules, and authentication rules. Covers rule registration, ordering, settings, and extension points.
- `docs/reference/catalogs/providers-catalog.md` - Reference catalog listing all built-in providers with their execution order, expected status codes, budget settings, and extension points.

- `docs/reference/distribution-settings.md` - Comprehensive settings reference shared by CLI and Gradle plugin. Covers budget controls, security configuration, include/ignore filters, example value providers, and module settings.
- `docs/reference/supported-specifications.md` - Supported OpenAPI and Swagger specification versions, normalization behavior, and known compatibility limits for generation.
- `docs/reference/model.md` - Reference for the model module's core data types - TestCase, TestSuite, SecurityValues, KeyValuePair, and error handling types (Outcome, GenerationError, GenerationReport, BudgetExceededException).
- `docs/reference/spi.md` - Core SPI (Service Provider Interface) for extending the generator with custom validation rules, test providers, generators, and value providers. Includes interface definitions, registration, and implementation checklist.
- `docs/reference/api.md` - Entry point for the Dokka-generated API reference published under this site for all public modules.
- `docs/reference/cli.md` - Command-line interface reference for openapi-testgen. Covers options, nested flag syntax, usage examples, and CI integration patterns.
- `docs/reference/gradle-plugin.md` - Gradle plugin reference for OpenAPI Test Generator. Documents extension properties, testGenerationSettings DSL, task registration, and automatic source set wiring.

### Modules

- `docs/modules/index.md` - Module catalog for the OpenAPI Test Generator composite build, covering the dependency flow, thin-module responsibilities, and links to the remaining deep-dive module pages.
- `docs/modules/core.md` - The core module is the generation engine that parses OpenAPI specifications, builds test suites using providers and rules, enforces budget controls, and produces artifacts through generators. It provides the TestGenerationEngine facade and built-in test-suite-writer generator.
- `docs/modules/example-value.md` - The example-value module generates example values from OpenAPI schemas, extracts response examples with media-type negotiation, and merges composed schemas. It owns the SchemaValueProvider SPI and offers a Java-friendly API with presets, withers, and SAM-convertible interfaces.
- `docs/modules/pattern-value.md` - The pattern-value module generates string values from OpenAPI schema regex patterns using the Cornutum regexp-gen library. It implements the SchemaValueProvider SPI from example-value and has no dependency on core.
- `docs/modules/pattern-support.md` - The pattern-support module bridges pattern-value into the core engine, contributing the `pattern` schema value provider, the InvalidPattern negative rule, and the patternGeneration settings extractor. Covers wiring, configuration ownership, and manual embedding.
- `docs/modules/generator-template.md` - The generator-template module contributes the Mustache-based `template` generator that renders test suites as Java or Kotlin source files. Covers the module's role, template resolution, output naming, write modes, and how embedders enable it.
- `docs/modules/distribution-bundle.md` - The distribution-bundle module is the shared product layer used by CLI and Gradle plugin. It bundles core, pattern-support, and generator-template modules, providing TestGenerationRunner as a high-level API for test generation execution.

### Contributing

- `docs/contributing/development-setup.md` - Setup guide for contributors including project structure, convention plugins, build commands, code style, testing conventions, and native image considerations.
- `docs/contributing/documentation-guide.md` - Guide for writing and maintaining project documentation using MkDocs. Covers frontmatter requirements, Diataxis structure, local preview, style guidelines, and contribution workflow.
- `docs/contributing/publishing.md` - Publish library modules to Maven Central, the Gradle plugin to the Plugin Portal, and npm artifacts, including the release checklist and troubleshooting guidance.
- `docs/contributing/npm-publishing.md` - Publish CLI packages to npm including the main JAR-based package and platform-specific native binaries. Covers build scripts, version management, and CI integration.

### Changelog

- `docs/changelog/CHANGELOG.md` - Release history documenting notable changes, new features, breaking changes, and fixes for each version following Semantic Versioning.
