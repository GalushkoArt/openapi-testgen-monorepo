---
description: Release history documenting notable changes, new features, breaking changes, and fixes for each version following Semantic Versioning.
---

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 0.12.0

### Added

- **New schema validation rules**: `Null For Required Property` (objects with a required, non-nullable property set to `null`; skips `nullable: true`/3.1 `"null"` types — also when declared by a `oneOf`/`anyOf`/`allOf` branch, resolved through `$ref` — and composes through nested objects and arrays), `Unexpected Additional Property` (injects an undeclared property when `additionalProperties: false`), and `Wrong Int64 Format` (out-of-64-bit-range value for `format: int64`). New `overrideBasicTestData` keys: `outOfInt64RangeValue`, `unexpectedAdditionalPropertyValue`
- Snapshot tests support `UPDATE_SNAPSHOTS=true` to regenerate golden files (`core` and `cli`)
- **Swagger 2.0 input support**: CLI, Gradle plugin, fat JAR, native binary, and core generation now accept Swagger 2.0 YAML or JSON specs by normalizing them to the existing OpenAPI 3 model pipeline
- **example-value Java-friendly API**: `ResponseExampleExtractor` accepts a SAM-convertible `ResponseBodyGenerator` so consumers can plug their own fallback body generator while keeping the library's explicit-example precedence; `SchemaTypeHelpers.resolveSchemaRef` (null-tolerant `$ref` resolver) and `SchemaMerger.mergeWithSubSchemas(input, openAPI)` remove `$ref`-resolution boilerplate; `SchemaExampleValueGeneratorOptions` gains `with*` methods; `ExampleValueSettings.defaults()` and a `@JvmOverloads` sweep (generator, factory, merger) make the module usable from Java without positional-argument ceremony. Java interop is pinned by a dedicated Java test suite
- **Documentation rework**: new how-to guides for positive testing and FAQ, module deep-dive pages
  for `generator-template` and `pattern-support`, symptom-indexed troubleshooting (headings quote
  exact error messages), a "choose your path" decision table in getting started, and intent-based
  snippet headings; `cli/` and `plugin/` READMEs became compact landing pages linking to the
  canonical site reference; the docs site now serves `llms.txt` (mkdocs-llmstxt); `context7.json`
  gained project metadata and agent-facing usage rules; `AGENTS.md` is now the canonical
  tool-neutral agent guide imported by `CLAUDE.md`
- **Version compatibility checks**:
    - `checkJacksonCompatibility` runs in every module's `check`: it fails when a Jackson 3 artifact (`tools.jackson*`, not fully supported by the swagger modules) appears on a runtime classpath, or when any resolved `com.fasterxml.jackson*` module drifts from the version-catalog pins (e.g. a swagger bump dragging in a newer Jackson transitively)
    - Consumer compatibility matrix (`scripts/compat-check.sh`, i.e. `publishAllToMavenLocal` + `:plugin:compatibilityTest`): consumes the published plugin from Maven Local in real consumer projects across Gradle 8.5 / 8.14.5 / 9.6.1 and with consumer-controlled Jackson versions (an older Jackson requested on the buildscript classpath, and Jackson forced down to swagger-parser's own build version 2.21.1). Runs in CI (`Consumer Compatibility` job), in the release-candidate build, and in `scripts/release-preflight.sh`
    - Root `publishAllToMavenLocal` aggregate task

### Changed

- Parser failures now report the detected OpenAPI/Swagger version where possible; Swagger versions other than 2.0 fail with an explicit unsupported-version message
- CLI native-image metadata now covers the Swagger 2.0 conversion path: the concrete OpenAPI schema subclasses instantiated by the converter, the v1 model/property classes it serializes reflectively (`io.swagger.models.*`), and the v1 parser's service registration; the native smoke fixture exercises body parameters, `definitions` composition (`allOf`/`$ref`), formData, and `securityDefinitions`
- Swagger 2.0 parsing honors the configured SnakeYAML parser limits (`ParserSettings`): the limits are applied to the v1 parser used by the conversion path, and version detection reads the spec through swagger-parser's own deserializer, so a large spec parsed with raised limits is routed to the Swagger 2.0 pipeline instead of failing as an unknown version
- Generated artifacts are now written atomically (temp file + atomic move) via the new `AtomicFileWriter` in core; write failures now fail the generation run instead of being logged and swallowed (applies to both the template generator and the test-suite writer)
- Root `./gradlew check` (and `build`) now aggregates every included build's `check`, so CI, release scripts, and contributors all run the same single entry point
- Config-file loading and log-level validation are now shared between the CLI and the Gradle plugin via `TestGenerationExecution` and `LogLevelResolver` in distribution-bundle; invalid levels are rejected before generation and the CLI error message reads "Invalid log level ..." instead of "Invalid --log-level ..."
- **BREAKING (Gradle plugin)**: `OpenApiTestGeneratorTask.configFile` is now a `RegularFileProperty` (was `Property<String>`). The `openApiTestGenerator { configFile.set("...") }` extension DSL is unchanged; only directly registered tasks must now pass a file, e.g. `configFile.set(layout.projectDirectory.file("config.yaml"))`
- The Gradle plugin is configuration-cache compatible: task wiring is lazy (no `afterEvaluate`), extension-to-task copying is explicit instead of reflection-based, and the task no longer touches `Task.project` at execution time; the monorepo build now runs with `org.gradle.configuration-cache=true`
- The Gradle plugin content-tracks local spec files declared in either the DSL or YAML config, so editing the spec re-triggers generation instead of reporting UP-TO-DATE
- Kotlin test-source wiring now honors `manualOnly` (previously the generation task was always attached for Kotlin projects)
- Test tasks now receive the AspectJ weaver (for `@Step`/allure-assertj) through a configuration-cache-safe argument provider in build-logic instead of allure-gradle's built-in one, which fails task validation under the configuration cache (aspectjweaver 1.9.25.1)
- `ResponseExampleExtractor` now returns the negotiated media type (with a `null` body) when a response declares content but no example can be extracted, instead of dropping both; `mediaType != null && body == null` means "content declared, nothing extractable"
- **BREAKING (example-value)**: `SchemaExampleValueGeneratorOptions.REQUEST_DEFAULTS`/`RESPONSE_DEFAULTS` are now static fields (`@JvmField`) instead of companion getters. Kotlin sources compile unchanged; Java sources that called `Companion.getREQUEST_DEFAULTS()`/`getRESPONSE_DEFAULTS()` must switch to the static fields, and binaries compiled against 0.11.0 need a recompile
- **Gradle 9.6.1** (wrapper, was 8.13): required by current plugin releases, which ship Kotlin 2.2+ metadata that Gradle 8.x's embedded Kotlin cannot read
- **Dependency updates** (verified by the full aggregate `check` plus the new consumer compatibility matrix):
    - Kotlin 2.2.10 → 2.3.21 (2.4.0 is out but dependency-analysis' kotlin-metadata-jvm reads metadata ≤ 2.3)
    - Jackson 2.19.4 → 2.22.1, deliberately staying on the 2.x line: Jackson 3 moved to the `tools.jackson` group and is not fully supported by swagger-core/swagger-parser; jackson-annotations follows its new patchless scheme (2.22)
    - swagger-core 2.2.41 → 2.2.52; swagger-parser 2.1.36 → 2.1.42 (pinned: 2.1.43+ silently breaks local `$defs` refs inside OAS 3.1 schemas that declare `$id` — generated negative test cases vanish; caught by the cli golden suites, see swagger-parser#2338/#2331)
    - JUnit 5.14.4 (JUnit 6 deferred), AssertJ 3.27.7, Allure java 2.35.3 / generator 2.44.0 / Gradle plugin 4.1.0, aspectjweaver 1.9.25.1, SLF4J 2.0.18, Logback 1.5.38, Dokka 2.2.0, Kover 0.9.8, dependency-analysis 3.9.0, vanniktech maven-publish 0.37.0 (new `publishToMavenCentral()` API), GraalVM buildtools 1.1.4, shadow 9.5.1, plugin-publish 1.3.1, ben-manes versions 0.54.0
    - Samples: Spring Boot 3.5.16, openapi-generator 7.23.0, rest-assured 5.5.7, Kotlin 2.2.21; the sample `GlobalExceptionHandler`s now also handle `HandlerMethodValidationException` (Spring 6.2 routes handler parameter validation through it)
- `OpenApiTestGeneratorTask` declares itself `@DisableCachingByDefault` (Gradle 9's `validatePlugins` requires an explicit cacheability statement; the task stays non-cacheable because the default test-suite-writer `MERGE` mode and the template generator's `SKIP_IF_EXISTS` mode read the existing output directory, so a build-cache restore would replace user-preserved edits instead of merging them). `generatorOptions` is typed `MapProperty<String, Any>` instead of `MapProperty<String, Any?>` (Gradle 9 nullability bounds; null values were never accepted at runtime)
- Publication signing is skipped when no `signingInMemoryKey` property is configured, so `publishAllToMavenLocal` works without GPG keys; CI releases still sign
- Gradle plugin `logLevel` extension property now carries a `@Deprecated` annotation (the deprecation itself was announced in 0.12.0), so IDEs warn at the declaration site

### Deprecated

- Gradle plugin `logLevel` property: inside the Gradle daemon SLF4J is bound to Gradle's own backend, so the property has no effect; use `--info`/`--debug` instead. The value is still validated and invalid levels fail the task

### Fixed

- **Template-generated code now compiles when values contain `/` or `$`**: the string-literal escaper emitted the JSON-only `\/` escape (illegal in Java/Kotlin source) and left `$` unescaped (triggering Kotlin string-template interpolation). `/` is now left as-is, `$` and form feed are emitted as unicode escapes (`\u0024`, `\u000c`) valid in both languages
- Gradle plugin: a `generator` declared only in the YAML config file was shadowed by the extension's empty-string default and generation failed with `Unknown generator: ''`; the empty default is now treated as unset, so the config-file value applies
- Gradle plugin: config-file-only builds no longer fail while resolving the absent DSL `specFile`; the config-resolved local spec is now content-tracked for up-to-date checks
- Documentation dependency installation is configuration-cache safe, so release-candidate and docs deployment builds run with the repository's default cache settings
- The file-writer sample's secondary YAML output now lives under `build/`, avoiding a Gradle implicit-dependency error caused by declaring the project source tree as generated output
- CLI `--log-level` help text now lists all accepted levels — `ALL` and `OFF` were missing from the description although both were already accepted

### Documentation

- Added a supported-specifications reference with Swagger 2.0 normalization behavior and known multipart/file-upload limits
- The changelog now lives at the repository root (`CHANGELOG.md`); docs builds copy it into the site
- Contributor entry points added at the root: `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`, GitHub issue/PR templates
- README gained CI and coverage badges

### Infrastructure

- New manual publication workflow: each dispatch publishes exactly one target (Maven Central staging, Gradle Plugin Portal, npm, or final GitHub Release), validates a successful release-candidate run for the same commit, and keeps the tag/GitHub Release last
- New `Docs Deploy` workflow publishes the MkDocs site to GitHub Pages on release (or manually)
- Dependabot enabled for Gradle (version catalog) and GitHub Actions
- Release publication guards: publishing must be dispatched from `main`, the npm target requires a release candidate whose npm test matrix actually ran, packaging scripts only accept the fat JAR whose file name matches the release version, the npm CLI smoke test asserts the reported `--version`, and the post-publish npm registry check retries before failing
- Kover XML coverage is uploaded to Codecov from CI
- Root aggregate `publishAllToMavenCentral` task removes the hand-maintained module list from the publish path
- pattern-support reuses `SchemaTypeHelpers` (ref resolution, string-type checks) from example-value instead of private duplicates
- Drift-guard tests pin `BasicTestDataProvider` override keys, `TestSuiteWriter` protected-field names, and the Gradle plugin's extension-to-task wiring to their sources of truth
- The `model` module gained a test suite (coverage floor 70%); the `distribution-bundle` floor rose from 70% to 90%

## 0.11.0

### Added

- **Full schema examples**: `ExampleValueSettings.fullExample` and `SchemaExampleValueGeneratorOptions.fullExample`
  can now generate schema-derived examples that populate every declared object property and produce non-empty arrays when schema constraints allow it

### Changed

- **BREAKING**: `ExampleValueSettings` and `SchemaExampleValueGeneratorOptions` data-class constructor,
  `copy`, and `componentN` signatures changed to include `fullExample`; use named arguments where possible

### Documentation

- Added `fullExample` to the distribution settings, CLI, Gradle plugin, and example-value module docs

## 0.10.0

### Added

- **Media type tracking**: `TestCase` now includes `requestBodyMediaType` and `responseBodyMediaType` fields, preserving the content types used during test case generation
- **Multi-media-type request body testing**: `RequestBodySchemaValidationTestProvider` now generates test cases for all supported request body media types (JSON, JWT, XML, YAML, form-urlencoded, and `+json`/`+jwt`/`+xml` suffixes), with automatic deduplication of identical cases
- **JWT media type support**: Request-body generation now supports `application/jwt` and `+jwt` media types; response schema fallback also treats JWT-like media types as JSON-like
- **`text/json` media type support**: Request-body generation now supports `text/json` across valid-case selection and request-body schema validation; response schema fallback now also treats `text/json` as JSON-like
- **Dynamic Content-Type and Accept headers**: Template generator now emits `Content-Type` from the actual request body media type and `Accept` from the response media type (previously hardcoded to `application/json`)
- **Parser settings**: New `ParserSettings` configuration for SnakeYAML parser limits (`yamlCodePointLimit`, `yamlMaxAliasesForCollections`, `yamlAllowRecursiveKeys`, `yamlNestingDepthLimit`) to handle large or complex YAML specs
- **CLI `--parser-setting` flag**: Configure parser settings from the command line (e.g., `--parser-setting yamlCodePointLimit=10000000`)
- **Gradle `parserSettings { }` DSL**: Type-safe parser settings configuration in the Gradle plugin extension
- **`ExtractedResponseExample`**: New public data class pairing response body with its media type
- **`ResponseExampleExtractor.extractExpectedResponseExampleWithMediaType()`**: New methods returning both response body and media type
- **`MediaTypePrioritizer`**: Promoted to public API for deterministic media type ordering
- **Context7 integration**: Added `context7.json` for documentation retrieval optimization

### Changed

- **BREAKING**: `TestCase` constructor signature changed — two new fields (`requestBodyMediaType`, `responseBodyMediaType`) inserted between `body` and `expectedBody`. Code using positional constructor arguments or destructuring must be updated; named-parameter `copy()` calls are unaffected
- **BREAKING**: `Consts.APPLICATION_JSON`, `Consts.APPLICATION_XML`, `Consts.APPLICATION_XWWW_FORM_URLENCODED`, and `Consts.supportedMediaTypes` removed from core. Media type constants moved to `example-value` module `MediaTypeHelper`
- Auth validation rules (`InvalidSecurityValuesAuthValidationRule`, `InsufficientScopesAuthValidationRule`, `IncorrectScopesAuthValidationRule`) now populate `responseBodyMediaType` in generated test cases
- `ValidCaseBuilder` now tracks and populates `requestBodyMediaType` and `responseBodyMediaType`
- Parameter schema resolution now supports OpenAPI parameter `content` schemas when `schema` is absent (used in both `ParameterSchemaValidationTestProvider` and required-parameter handling in `ValidCaseBuilder`)
- When both parameter `schema` and `content` are defined, generation now logs a warning and applies only `schema` (deterministic precedence)
- Cycle detection for nested schema traversal now uses `$ref` history plus schema-instance identity checks instead of structural hashing, preventing unrelated structurally identical schemas from being treated as cycles
- CLI native image reflection config expanded with `allPublicMethods` on `Schema` and additional OpenAPI model classes for forward compatibility
- CLI native image agent tracing now runs against 3 fixture specs (baseline, OpenAPI 3.0 exclusive bounds, OpenAPI 3.1 advanced features)
- **JSON-first built-in RestAssured templates**: Built-in `restassured-java` and `restassured-kotlin` templates now stay focused on JSON-like payloads for automatic body assertions while still emitting `Content-Type`/`Accept` from the selected request/response media types
- **Generic non-JSON template fallback**: Non-JSON scalar or string request bodies are emitted as raw literals, while non-JSON structured request/response bodies now fall back to TODO guidance with placeholder/request-preview code instead of specialized XML/form/JWT assertion helpers
- **Request-body media type preservation**: `RequestBodySchemaValidationTestProvider` now keeps the concrete `requestBodyMediaType` on generated negative test cases for each supported content entry instead of inheriting or dropping it

### Fixed

- **`ValidCaseBuilder` media-content handling**: Structured header/cookie parameter values resolved from OpenAPI `content` are now preserved instead of being stringified, and required request bodies can now be generated from media-type `example`/`examples` entries even when no schema is defined
- Fixed false-positive `CYCLE_DETECTED` skips for sibling properties that share identical `anyOf` structure (for example `to`/`cc`/`bcc`), so all eligible branches now generate test cases
- **`alwaysWriteTests` execution contract**: CLI and Gradle execution now return success when artifacts were written because `alwaysWriteTests=true`, while preserving generation errors in the report/log output
- **Parser settings isolation**: `OpenApiSpecParser` now snapshots and restores swagger-parser global YAML settings after each parse, preventing one run from leaking parser limits into later runs
- **Deterministic `MULTIPLE_FILES` writes**: `TestSuiteWriter` now deduplicates repeated suite names per batch and fails fast when distinct operation names sanitize to the same output filename
- **Template literal escaping**: Built-in Java/Kotlin templates now embed generated request/response strings without HTML-escaping artifacts in comments or string literals

### Documentation

- Updated providers catalog: `RequestBodySchemaValidationTestProvider` now documents multi-media-type processing and supported types
- Updated providers catalog with parameter schema resolution precedence (`schema` first, `content` fallback) and warning behavior when both are present
- Added `IncorrectScopesAuthValidationRule` to providers catalog status code table
- Updated `TestCase` model reference with `requestBodyMediaType` and `responseBodyMediaType` fields
- Updated CLI reference with `--parser-setting` flag and examples
- Updated Gradle plugin reference with `parserSettings {}` DSL
- Updated distribution settings reference with `parserSettings` section
- Updated provider-rule and query-parameter guides to document parameter `content` schema support and schema-vs-content precedence
- Consolidated documentation to reduce duplication across negative-testing, CI/CD, and getting-started guides
- Updated generator docs to describe the simplified JSON-first built-in template behavior and the TODO/manual-completion fallback for non-JSON structured bodies
- Updated CLI, distribution-settings, error-handling, and troubleshooting docs to clarify the restored `alwaysWriteTests` success semantics
- Refreshed CLI smoke fixtures for corrected request-body media type tracking in generated JSON output

## 0.9.2

### Added

- **`--prefer-jar` flag** (npm): Force JAR execution, bypassing native binary detection for troubleshooting or consistency

### Fixed

- **TestSuiteWriter merge logic**: Correctly returns updated test case when no protected fields are configured (previously returned existing case unchanged)
- **securityValues field protection**: Added `securityValues` to the list of mergeable fields in TestSuiteWriter
- **protectedTestCaseFields validation**: Invalid field names now produce a helpful error message instead of being silently ignored

### Documentation

- Added README files for platform-specific native binary npm packages
- Expanded troubleshooting with `--prefer-jar` usage
- Improved generator options documentation

## 0.9.1

A minor release focused on improving Linux binary compatibility across distributions.

### Changed

- **Linux x64 native binary**: Statically linked with musl libc for maximum portability (works on any Linux distribution without glibc dependency)
- **Linux ARM64 native binary**: Now built on Ubuntu 22.04 (glibc 2.35) for broader compatibility with older distributions

### Added

- **Automatic JAR fallback**: CLI now detects glibc incompatibility at runtime and automatically falls back to JAR-based execution
- **Installation-time warnings**: postinstall script warns Linux ARM64 users on non-glibc systems (musl/Alpine) about JAR fallback requirement
- **Improved error messages**: Clear guidance when native binary fails due to glibc version mismatch

### Documentation

- Added platform compatibility details to npm installation guide
- Added troubleshooting section for glibc and native binary issues

## 0.9.0

### Added

- **Response body example extraction**: Valid test cases now include `expectedBody` populated from OpenAPI response examples
- **Named example support**: New `extractExpectedResponseExample(operation, openAPI, statusCode, exampleName)` overload for selecting specific named examples
- **Enhanced response resolution**: Response lookup now follows OpenAPI priority: exact status code → range (e.g., `2XX`) → `default`
- **Media type prioritization**: Response examples prefer JSON-like media types (`application/json` > `+json` suffix > `application/xml` > `+xml` suffix > others)
- **Schema-derived fallback**: When no explicit example exists, response examples are derived from schema with:
    - Optional properties with explicit examples included
    - `writeOnly` properties excluded
    - `schema.examples` and `schema.default` used as fallbacks
- **`findSuccessStatusCode` helper**: New `SchemaTypeHelpers.findSuccessStatusCode(operation)` for finding the first success status code
- **`resolveExampleRef` helper**: New `SchemaTypeHelpers.resolveExampleRef(example, openAPI)` for resolving example `$ref` references
- **`needToComplete` flag**: Valid test cases are marked with `needToComplete = true` to indicate they may need manual completion
- **`includeOperations` whitelist filtering**: New configuration (CLI `--include-operation`, Gradle DSL) to generate tests only for selected operations
- **Test suite merge controls**: New `preventOverwriteCases` and `protectedTestCaseFields` options for preserving existing test cases/fields
- **OAuth2/OpenID scope metadata**: `securityValues.other.authorizationScopes` now provides structured scope info for auth test generation
- **CLI npm distribution**: Platform-specific npm packages for cross-platform CLI installation

### Changed

- **BREAKING**: `SchemaTypeHelpers.tryGetResponseFromRef` removed; use `SchemaTypeHelpers.resolveResponseByStatus` (exact → range → default)
- **BREAKING**: `SchemaExampleValueGenerator.extractExpectedResponseExample` removed; use `ResponseExampleExtractor`
- `ValidCaseBuilder` now delegates to `SchemaTypeHelpers.findSuccessStatusCode` for status code resolution (DRY improvement)
- Exception handling in response example fallback now distinguishes between expected (`IllegalStateException`) and unexpected (`RuntimeException`) errors
- Response example fallback now respects `maxExampleDepth` and applies schema-derived values only for JSON-like media types, preferring explicit examples when present
- **Build**: Consistent Kotlin 2.2 language version (`languageVersion.set(KOTLIN_2_2)`) across all modules
- **Build**: Centralized Gradle configuration via build-logic convention plugins; modules migrated and per-module detekt configs/baselines removed
- **Template generator**: `className` derivation now treats any non-alphanumeric character as a word separator, not just `_`, `-`, ` `, and `:`

### Fixed

- Response example extraction now correctly handles parameterized media types (e.g., `application/json; charset=utf-8`)
- Named example selection now falls back to default extraction when the named example has no usable value (e.g., `externalValue`)
- Circular schema references in response examples no longer cause infinite loops
- **Date/DateTime validation rules**: Fixed sequence construction to use `listOf().asSequence()` for single-element sequences

## 0.8.0

Initial public release.

---

## Version History

For detailed commit history, see the [git log](https://github.com/GalushkoArt/openapi-testgen-monorepo/commits/main).
