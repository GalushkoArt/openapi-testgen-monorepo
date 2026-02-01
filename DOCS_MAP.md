# Documentation Map

Maps documentation pages to `mkdocs.yml` navigation. Keep in sync when adding/moving/renaming pages.

Keep entries lightweight: path + one-line purpose (no multi-paragraph content summaries).

Run `python3 skills/project-docs/scripts/sync_docs_map.py --check` to verify sync status.

---

## Repository Documentation (Outside MkDocs)

| File | Purpose |
|------|---------|
| `README.md` | Project entry point, installation, quick starts |
| `CLAUDE.md` | Contributor guide for AI assistants |
| `AGENTS.md` | Repository guidelines and automation instructions |

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

- `docs/index.md` - OpenAPI Test Generator automatically creates validation tests from OpenAPI specifications, ensuring your API enforces parameter types, required fields, format constraints, and security schemes.

### Getting Started

- `docs/getting-started/index.md` - Introduction to OpenAPI Test Generator for first-time users. Covers the main generators (template and test-suite-writer) and points to installation, CLI tutorials, and Gradle integration guides.
- `docs/getting-started/installation.md` - Install the OpenAPI Test Generator CLI or Gradle plugin. Covers npm installation, GitHub release downloads, native binaries, building from source, and Gradle plugin portal setup.
- `docs/getting-started/npm-installation.md` - Comprehensive guide for installing the OpenAPI Test Generator CLI via npm, pnpm, yarn, or bun. Covers native binary support, JAR fallback, platform-specific options, and troubleshooting common issues.
- `docs/getting-started/first-test-suite.md` - Step-by-step tutorial for generating your first test suite using the CLI. Demonstrates both template-based test generation (RestAssured Java) and JSON/YAML test suite output.
- `docs/getting-started/gradle-integration.md` - Tutorial for integrating OpenAPI test generation into Gradle builds. Shows how to apply the plugin, configure the extension, run generation, and optionally use manualOnly mode.
- `docs/getting-started/end-to-end-workflow.md` - Complete tutorial from writing a small OpenAPI spec to generating JSON test suites, generating executable RestAssured tests, and running them locally and in CI.
- `docs/getting-started/understanding-output.md` - Explains the output formats produced by OpenAPI Test Generator. Covers template generator output (Java/Kotlin source files), test-suite-writer output (JSON/YAML), and the includeValidCase setting for positive test cases.

### Samples

- `docs/samples/index.md` - Overview of sample projects demonstrating OpenAPI Test Generator integration with different generators, languages, and configurations including Spring Boot, RestAssured, and data-driven testing approaches.
- `docs/samples/java-spring-file-writer.md` - Demonstrates the test-suite-writer generator for outputting test suites as JSON or YAML files. Shows MERGE mode for preserving manual edits and protected fields when regenerating.
- `docs/samples/java-spring-rest-assured.md` - Demonstrates the template generator with RestAssured to produce executable Java test classes. Shows automatic build integration, template variables, and multiple generation tasks.
- `docs/samples/kotlin-spring-rest-assured.md` - Demonstrates the template generator with Kotlin and custom Mustache templates for full control over generated test code. Includes three generation tasks with different configurations.

### How-to

- `docs/how-to/index.md` - Goal-oriented guides for common tasks including configuration, negative testing, generators, extension points, and integration with CI/CD and testing frameworks.

#### Configuration

- `docs/how-to/configuration/yaml-config.md` - Configure test generation using a YAML config file. Shows file structure, precedence, and examples for CLI and Gradle plugin usage, with links to the canonical settings and option references.
- `docs/how-to/configuration/include-operations.md` - Generate tests for specific API operations using includeOperations. Filter by path and HTTP method to target individual endpoints, improving generation speed for large OpenAPI specifications.
- `docs/how-to/configuration/ignore-rules.md` - Filter out unwanted test cases by path, HTTP method, or test case name using the ignore configuration. Useful for skipping internal endpoints, deprecated operations, or specific validation scenarios.
- `docs/how-to/configuration/security-values.md` - Provide valid authentication credentials for test generation using validSecurityValues. Includes YAML, CLI, and Gradle examples and links to the canonical TestCase security metadata schema.
- `docs/how-to/configuration/positive-testing.md` - Enable positive test case generation to verify that valid requests return 2xx responses. By default, only negative test cases (4xx) are generated.

#### Negative Testing

- `docs/how-to/negative-testing/index.md` - Overview of negative testing guides for OpenAPI Test Generator. Links to scenario-focused documentation for generating tests that validate parameter constraints, request body schemas, and authentication requirements.
- `docs/how-to/negative-testing/generating.md` - Step-by-step guide to running negative test generation using the CLI or Gradle plugin. Covers JSON suite generation, executable test output, security values configuration, and CI/CD integration.
- `docs/how-to/negative-testing/path-parameters.md` - Learn how OpenAPI Test Generator creates negative tests for path parameters. Covers schema violations, format validation (UUID, email), pattern-based validation, and filtering options.
- `docs/how-to/negative-testing/query-parameters.md` - Learn how OpenAPI Test Generator creates negative tests for query parameters, including missing required parameters and invalid values. Covers complex object parameters, filtering options, and output inspection.
- `docs/how-to/negative-testing/header-parameters.md` - Learn how OpenAPI Test Generator creates negative tests for header parameters. Covers both regular required headers (expecting 400) and security headers like Authorization and API keys (expecting 401/403).
- `docs/how-to/negative-testing/request-body-schema.md` - Learn how OpenAPI Test Generator creates negative tests for request bodies. Covers missing required bodies, schema constraint violations, nested object and array validation, and budget controls for complex schemas.

#### Generators

- `docs/how-to/generators/template-generator.md` - Configure the template generator to render Mustache-based Java or Kotlin tests from OpenAPI specifications. Covers generator options, template variables, and output control settings.
- `docs/how-to/generators/custom-templates.md` - Create custom Mustache templates for the template generator to support different test frameworks, languages, or code styles. Includes template directory structure, available variables, and example templates.
- `docs/how-to/generators/test-suite-writer.md` - Configure the test-suite-writer generator to output test suites as JSON or YAML files. Covers output modes, merge behavior, and output structure, with links to the canonical option catalog.

#### Extension

- `docs/how-to/extension/custom-rules.md` - Learn how to create custom validation rules that generate negative test cases for domain-specific constraints, custom string formats, or OpenAPI extension properties not covered by built-in rules.
- `docs/how-to/extension/custom-providers.md` - Understand how to create custom test case providers that orchestrate negative test generation for OpenAPI operations. Covers the provider contract, wiring into generation, and when to prefer rules over providers.
- `docs/how-to/extension/custom-generators.md` - Create custom artifact generators that emit source code, JSON, YAML, or other outputs from generated test suites. Covers implementing ArtifactGenerator and ArtifactGeneratorFactory interfaces and registering them via modules.
- `docs/how-to/extension/custom-modules.md` - Learn how to create custom TestGenerationModule implementations to extend the test generator with artifact generators, schema value providers, and validation rules without using reflection.

#### Integration

- `docs/how-to/integration/ci-cd.md` - Add OpenAPI Test Generator to your CI/CD pipeline using the Gradle plugin or CLI. Covers job patterns for splitting generation and tests, caching, environment-specific configuration, and troubleshooting.
- `docs/how-to/integration/restassured.md` - Configure OpenAPI Test Generator to produce RestAssured-based tests using the built-in Java and Kotlin template sets. Includes a Gradle example and links to the canonical generator reference.
- `docs/how-to/integration/spring-boot.md` - Enable Spring Boot test annotations in generated tests by setting the springBootTest template variable. Works with the built-in RestAssured templates.

- `docs/how-to/troubleshooting.md` - Solutions for common issues including missing test output, budget exceeded errors, CLI/npm problems, and platform-specific compatibility.

### Concepts

- `docs/concepts/index.md` - Overview of conceptual documentation explaining the design rationale and internal workings of the OpenAPI Test Generator.
- `docs/concepts/architecture.md` - System architecture of OpenAPI Test Generator, covering module dependencies, data flow, core abstractions, and extension points. This is the primary reference for understanding how the generator is structured and how its components interact.
- `docs/concepts/provider-rule-model.md` - Explains the provider-rule architecture that separates concerns between what to test (providers) and how to generate invalid values (rules). Covers the valid case baseline, provider responsibilities, and deterministic composition.
- `docs/concepts/test-generation-flow.md` - Step-by-step walkthrough of the test generation pipeline, from parsing the OpenAPI spec through provider execution to artifact output. Covers the high-level stages and output types.
- `docs/concepts/schema-composition.md` - Explains how the generator handles OpenAPI schema composition keywords (allOf, anyOf, oneOf), including merge behavior, test case generation strategies, cycle detection, and budget controls for composed schemas.
- `docs/concepts/determinism.md` - Explains the determinism guarantees of the generator, ensuring stable and reproducible output for the same inputs. Covers ordering rules for providers, rules, modules, and writers.
- `docs/concepts/budget-controls.md` - Documents the budget limits that prevent runaway generation from complex or deeply nested schemas. Covers maxSchemaDepth, maxSchemaCombinations, maxTestCasesPerOperation, and how to tune them.
- `docs/concepts/error-handling.md` - Describes the error handling strategy using explicit result types (Outcome, GenerationReport) to support best-effort behavior without hiding failures. Covers error modes, error aggregation, and the alwaysWriteTests option.
- `docs/concepts/glossary.md` - Definitions of key terms and concepts used throughout the OpenAPI Test Generator documentation, including providers, rules, outcomes, budgets, and core data types.

### Reference

- `docs/reference/index.md` - Index of reference documentation including catalogs, CLI flags, Gradle DSL, distribution settings, and SPI interfaces.

#### Catalogs

- `docs/reference/catalogs/generator-options.md` - Reference for generator options available in the test-suite-writer and template generators. Covers output modes, file formats, merge behavior, and template customization.
- `docs/reference/catalogs/rules-catalog.md` - Complete catalog of built-in validation rules for OpenAPI test generation, including schema rules (array, boundary, enum, type, string, object, date), pattern module rules, and authentication rules. Covers rule registration, ordering, settings, and extension points.
- `docs/reference/catalogs/providers-catalog.md` - Catalog of test case providers that generate negative test cases for OpenAPI operations. Includes operation-level providers for auth, parameters, and request bodies, along with their execution order, expected status codes, and extension points.

- `docs/reference/distribution-settings.md` - Comprehensive settings reference shared by CLI and Gradle plugin. Covers budget controls, security configuration, include/ignore filters, example value providers, and module settings.

#### Model

- `docs/reference/model/test-suite.md` - Reference documentation for the model module's key types including TestSuite, SecurityValues, and KeyValuePair. Explains how these data classes are used across the system for test generation and serialization.
- `docs/reference/model/test-case.md` - Reference documentation for the TestCase data class, which represents a single generated test scenario including request inputs, security values, and expected outcomes.
- `docs/reference/model/errors.md` - Reference documentation for error handling types in the model module, including Outcome, GenerationError, ErrorContext, GenerationReport, and BudgetExceededException.

#### SPI

- `docs/reference/spi/index.md` - Overview of the core SPI (Service Provider Interface) for extending the OpenAPI Test Generator with custom validation rules, test providers, generators, and value providers.
- `docs/reference/spi/validation-rules.md` - Reference documentation for the validation rules SPI, including SchemaValidationRule, SimpleSchemaValidationRule, and AuthValidationRule interfaces. Explains how to implement custom rules that generate negative test cases.
- `docs/reference/spi/test-providers.md` - Reference documentation for the TestCaseProvider SPI. Providers transform validation rules into generated TestCase objects while maintaining deterministic ordering and immutability.
- `docs/reference/spi/generators.md` - Reference documentation for the ArtifactGenerator and ArtifactGeneratorFactory SPIs. Generators emit test artifacts (code files, JSON/YAML) from generated test suites.
- `docs/reference/spi/value-providers.md` - Reference documentation for the SchemaValueProvider SPI in the example-value module. Value providers generate schema-derived example values for test data.

- `docs/reference/api.md` - Links to Dokka-generated API documentation for all modules including core, model, example-value, generator-template, pattern-value, pattern-support, distribution-bundle, plugin, and cli.
- `docs/reference/cli.md` - Command-line interface reference for openapi-testgen. Covers options, nested flag syntax, usage examples, and CI integration patterns.
- `docs/reference/gradle-plugin.md` - Gradle plugin reference for OpenAPI Test Generator. Documents extension properties, testGenerationSettings DSL, task registration, and automatic source set wiring.

### Modules

- `docs/modules/index.md` - Overview of the OpenAPI Test Generator monorepo module structure, showing the dependency flow from model through core to CLI and Gradle plugin. Provides a catalog of all modules with links to detailed documentation.
- `docs/modules/model.md` - The model module contains the canonical data structures used across all modules, including TestSuite, TestCase, Outcome, and error reporting types. It has no dependencies and is the foundation of the module hierarchy.
- `docs/modules/example-value.md` - The example-value module is a standalone library for generating deterministic example values from OpenAPI schemas. It provides the SchemaValueProvider SPI and built-in providers for enums, UUIDs, emails, dates, and other common types.
- `docs/modules/core.md` - The core module is the generation engine that parses OpenAPI specifications, builds test suites using providers and rules, enforces budget controls, and produces artifacts through generators. It provides the TestGenerationEngine facade and built-in test-suite-writer generator.
- `docs/modules/pattern-value.md` - The pattern-value module provides standalone regex-based value generation. It wraps a regex generator to produce deterministic strings that match or violate schema patterns, implementing the example-value SPI without depending on core.
- `docs/modules/pattern-support.md` - The pattern-support module integrates pattern-value into core as an optional feature module. It contributes a schema value provider for pattern-based values, a validation rule for pattern violations, and a settings extractor for pattern generation configuration.
- `docs/modules/generator-template.md` - The generator-template module provides Mustache-based code generation for producing Java and Kotlin test source files. It includes built-in template sets for RestAssured and supports custom templates and variables.
- `docs/modules/distribution-bundle.md` - The distribution-bundle module is the shared product layer used by CLI and Gradle plugin. It bundles core, pattern-support, and generator-template modules, providing TestGenerationRunner as a high-level API for test generation execution.
- `docs/modules/plugin.md` - The plugin module provides Gradle integration via the art.galushko.openapi-test-generator plugin. It registers the generateOpenApiTests task and exposes a typed DSL for configuring test generation within Gradle builds.
- `docs/modules/cli.md` - The cli module provides the openapi-testgen command-line interface built on Picocli. It supports JVM distribution and optional GraalVM native image builds for running test generation from the terminal.

### Contributing

- `docs/contributing/index.md` - Index of contributor guides covering development setup, code style, testing, documentation, publishing, and release process for the OpenAPI Test Generator project.
- `docs/contributing/development-setup.md` - Setup guide for contributors including project structure, convention plugins, build commands, working with rules/providers/generators, and native image considerations.
- `docs/contributing/code-style.md` - Kotlin coding conventions for the project including naming, null safety, immutability, formatting with Detekt, and SLF4J logging patterns.
- `docs/contributing/testing-guide.md` - Testing conventions using JUnit 5, AssertJ, and Allure. Covers determinism requirements, fixture usage, and common test commands.
- `docs/contributing/documentation-guide.md` - Guide for writing and maintaining project documentation using MkDocs. Covers frontmatter requirements, Diataxis structure, local preview, style guidelines, and contribution workflow.
- `docs/contributing/publishing.md` - Publish library modules to Maven Central and the Gradle plugin to the Plugin Portal. Covers account setup, credential configuration, signing, and troubleshooting upload issues.
- `docs/contributing/npm-publishing.md` - Publish CLI packages to npm including the main JAR-based package and platform-specific native binaries. Covers build scripts, version management, and CI integration.
- `docs/contributing/release-process.md` - Complete release workflow checklist covering version bumps, verification checks, Maven Central and Gradle Plugin Portal publishing, GitHub releases, and hotfix procedures.

### Changelog

- `docs/changelog/CHANGELOG.md` - Release history documenting notable changes, new features, breaking changes, and fixes for each version following Semantic Versioning.
