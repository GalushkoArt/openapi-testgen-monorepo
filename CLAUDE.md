# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OpenAPI Test Generator is a Kotlin-based monorepo that automatically generates test cases (both negative and positive) from OpenAPI specifications. It consists of:

- **build-logic**: Convention plugins for centralized build configuration (Kotlin/JVM, quality, publishing)
- **model**: Core data models (`TestCase`, `TestSuite`)
- **example-value**: Standalone example value generation from OpenAPI schemas (`SchemaExampleValueGenerator`, `SchemaValueProvider` SPI)
- **core**: Test generation logic (providers, rules, generators)
- **generator-template**: Mustache-based template generator module (`TemplateGeneratorModule`)
- **pattern-value**: Regex-based example value generation (no core dependency)
- **pattern-support**: Pattern module integration (module + rule + settings extractor)
- **distribution-bundle**: Bundles core, pattern-support, and generator-template; provides `TestGenerationRunner` for simplified execution
- **plugin**: Gradle plugin for build integration
- **cli**: Command-line interface with optional GraalVM native image support
- **samples**: Example usage projects (Java/Kotlin Spring + RestAssured and file-writer)

The system uses a **provider-rule architecture**: `TestCaseProvider` implementations orchestrate test case generation for different aspects (parameters, request bodies, auth), while `SchemaValidationRule` and `AuthValidationRule` implementations encode specific OpenAPI constraints (min/max, required fields, security).

## Build Commands

### Run all checks (lint, tests, API compatibility)
```bash
./gradlew check
```

## Architecture

For a deep-dive on module responsibilities, data flow, and extension points, see `docs/concepts/architecture.md`.

### Monorepo Structure

Composite build defined in `settings.gradle.kts`:
```kotlin
includeBuild("build-logic")
includeBuild("model")
includeBuild("example-value")
includeBuild("core")
includeBuild("generator-template")
includeBuild("pattern-value")
includeBuild("pattern-support")
includeBuild("distribution-bundle")
includeBuild("plugin")
includeBuild("cli")

include(
    "samples:java-spring-rest-assured",
    "samples:java-spring-file-writer",
    "samples:kotlin-spring-rest-assured",
)
```

Dependency flow:
- `example-value` depends on `model`
- `core` depends on `model` and `example-value`
- `pattern-value` depends on `example-value`
- `pattern-support` depends on `core` and `pattern-value`
- `generator-template` depends on `core`
- `distribution` bundles `core`, `pattern-support`, and `generator-template`
- `plugin` and `cli` depend on `distribution` (single dependency for all test generation features)
- Samples consume the plugin via `id("art.galushko.openapi-test-generator")`

### Manual Wiring & GraalVM Support

- **Rules and generators** are explicitly wired at compile time via `BuiltInRules`, `BuiltInGenerators`, and `TestGenerationModule`
- **No reflection for discovery**: rule/generator discovery is manual and deterministic (`ManualRuleRegistry`, `ArtifactGeneratorRegistry`)
- **Extensibility**: Custom rules/generators are added via constructor injection or `TestGenerationModule`
- **Deterministic**: Rules, generators, and modules are sorted for stable output ordering
- **GraalVM native image**: CLI has native support; core avoids reflection for discovery, but Mustache templates use reflection and the OpenAPI parser/Jackson can require reflection config (see `cli/README.md`)

### Build Infrastructure (Convention Plugins)

The `build-logic` module contains precompiled script plugins that centralize build configuration:

| Plugin | Purpose |
|--------|---------|
| `testgen.kotlin-base` | Kotlin/JVM toolchain (Java 21), compiler options (Kotlin 2.2, JSR305 strict), Dokka documentation |
| `testgen.quality` | Detekt linting, Kover coverage, binary compatibility validation, dependency analysis, JUnit 5 test config |
| `testgen.library` | Combines `kotlin-base` + `quality` + Maven Central publishing |
| `testgen.library-with-allure` | Extends `library` with Allure test reporting |

Modules apply convention plugins instead of configuring each tool individually:
```kotlin
// Example: core/build.gradle.kts
plugins {
    id("testgen.library-with-allure")
}

testgenQuality {
    koverMinCoverage = 95
}

dependencies {
    api(libs.testgen.model)
    // ...
}
```

Key features:
- **Centralized detekt config**: Single `build-logic/config/detekt.yml` shared across all modules
- **Version catalog**: All modules use the root `gradle/libs.versions.toml` via `settings-conventions.gradle.kts`
- **Shared settings**: `gradle/settings-base.gradle.kts` configures repositories and build cache


## Kotlin Style & Conventions

See `.cursor/rules/01-kotlin-style.mdc` for full details. Key points:

- **Naming**: PascalCase for classes, camelCase for functions/variables, UPPER_SNAKE_CASE for constants
- **Null safety**: Avoid `!!`; use `requireNotNull`, safe calls, or null-object defaults
- **Immutability**: Prefer `data class` and immutable collections
- **Error handling**: Use `require`/`check` for validation; never swallow exceptions
- **Logging**: SLF4J with parameterized messages (no string concatenation)
- **Tests**: JUnit 5 + AssertJ + Allure; deterministic, isolated, no network I/O

## Testing

### Test Structure

- Tests mirror source structure under `core/src/test/kotlin`
- Use `@DisplayName` for clarity; add Allure metadata (`@Epic`, `@Feature`) where helpful
- Follow AAA (Arrange-Act-Assert) discipline
- Extend base classes when applicable:
    - `ValidationRuleTest` for rule tests
    - `TestProviderTest` for provider tests

### Assertions

Prefer AssertJ fluent assertions:
```kotlin
// For rule results
assertThat(rule.apply(schema, openAPI))
    .usingRecursiveComparison()
    .isEqualTo(expected)

// For test case lists
assertThat(provider.provideTestCases(validCase, spec, openAPI))
    .containsExactlyInAnyOrderElementsOf(expectedCases)
```

Use custom conditions:
```kotlin
assertThat(results).has(Conditions.ruleAppliedTo(rule, expected))
assertThat(results).has(Conditions.correctAppliedTo(expected))
```

**IMPORTANT - Precise Assertions:**
- **Strings**: Use exact matching (`.isEqualTo(expected)`), NOT partial matching (`.contains(substring)`)
- **Collections**: Always verify exact size (`.hasSize(n)`)
- **Error messages**: Match complete error strings, not substrings
- Precise assertions catch regressions and unexpected side effects. Prefer compare results with expected values via `isEqualTo` or `containsExactlyInAnyOrder`.

```kotlin
// GOOD: Precise string assertions
assertThat(error.message).isEqualTo("Invalid API key: must be 32 characters")

// BAD: Vague partial matching
assertThat(error.message).contains("Invalid")

// GOOD: Exact collection contents
assertThat(testCases).containsExactlyInAnyOrder(case1, case2, case3)

// BAD: Not exact contents
assertThat(testCases).contains(case1, case2, case3)
```

### Parameterized Tests

Provide inputs via `@MethodSource`:
```kotlin
@ParameterizedTest(name = "{0}")
@MethodSource("schemaProvider")
fun `should handle various schemas`(scenario: String, schema: Schema, expected: List<RuleValue>) {
    // test body
}

companion object {
    @JvmStatic
    fun schemaProvider() = Stream.of(
        Arguments.of("Scenario 1", schema1, expected1),
        Arguments.of("Scenario 2", schema2, expected2)
    )
}
```

### Integration Tests

Parse specs with full resolution:
```kotlin
val openAPI = OpenAPIV3Parser().read("src/test/resources/openapi.yaml", null, ParseOptions().apply {
    isResolveFully = true
})
```

Compare generated suites against snapshot JSON:
```kotlin
val mapper = ObjectMapper().registerKotlinModule()
val expected = mapper.readValue<List<TestSuite>>(File("src/test/resources/openapi-generated-test-suits.json"))
assertThat(capturedSuites).usingRecursiveComparison().isEqualTo(expected)
```

## Adding New Features

### Acceptance Criteria (for any change)

- `./gradlew :core:check :plugin:check` passes (Java 21, Kotlin 2.2)
- No detekt violations in changed files
- Configuration cache compatibility preserved (no heavy work at configuration time)
- Public API changes explicitly annotated and covered by `apiCheck`
- Deterministic output ordering (sort by class name, keys)

## Engineering Principles

- **SOLID**: Keep classes focused; use extension points (`getRules`, `createTestGenerator`) for new features
- **DRY**: Extract common transformations into helpers; centralize option parsing
- **KISS**: Prefer simple data flows, fail fast with precise errors, avoid premature generalization
- **YAGNI**: Don't add functionality until it's actually needed; resist speculative features and "just in case" options
- **Separation of Concerns**: Keep parsing (OpenAPI), generation (providers/rules), and output (generators) separate; don't mix business logic with I/O
- **Principle of Least Surprise**: Follow naming conventions; default behaviors should be safe and intuitive; error messages should be actionable
- **Dependency Direction**: Dependencies flow inward (`plugin` → `core` → `model`); high-level modules depend on low-level, never reverse
- **Determinism**: Sort collections by class name or key; validate inputs early with `require`/`requireNotNull`
- **Testing posture**: Unit-test rules/providers in isolation; snapshot/end-to-end test generators
