---
description: Release history documenting notable changes, new features, breaking changes, and fixes for each version following Semantic Versioning.
---

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
