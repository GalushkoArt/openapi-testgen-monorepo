# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
