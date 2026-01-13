# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OpenAPI Test Generator is a Kotlin-based monorepo that automatically generates test cases (both negative and positive) from OpenAPI specifications. It consists of:

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

### Build and test a specific module
```bash
./gradlew :core:test
./gradlew :plugin:test
./gradlew :cli:test
```

### Run a single test
```bash
./gradlew :core:test --tests "ValidCaseBuilderTest"
./gradlew :core:test --tests "*.ParameterSchemaValidationTestProviderTest.should generate test cases for all rules*"
```

### Build and run the CLI
```bash
# Build distribution
./gradlew :cli:installDist

# Run with help
./cli/build/install/openapi-testgen/bin/openapi-testgen --help

# Generate tests from OpenAPI spec (template generator)
./cli/build/install/openapi-testgen/bin/openapi-testgen \
  --spec-file path/to/openapi.yaml \
  --output-dir ./generated \
  --generator template \
  --generator-option templateSet=restassured-java \
  --generator-option package=com.example.generated

# Generate JSON test suites (test-suite-writer generator)
./cli/build/install/openapi-testgen/bin/openapi-testgen \
  --spec-file path/to/openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option format=json

# Using a YAML config file
./cli/build/install/openapi-testgen/bin/openapi-testgen \
  --config-file openapi-testgen.yaml \
  --spec-file path/to/openapi.yaml  # CLI args override config file

# With settings overrides
./cli/build/install/openapi-testgen/bin/openapi-testgen \
  --spec-file path/to/openapi.yaml \
  --output-dir ./generated \
  --generator template \
  --setting maxSchemaDepth=15 \
  --setting validSecurityValues.X-API-Key=test-key
```

### Build native image (GraalVM required)
```bash
# Build native binary
./gradlew :cli:nativeCompile
./cli/build/native/nativeCompile/openapi-testgen --help
```

### Code quality
```bash
./gradlew detekt              # Run linter
./gradlew apiCheck            # Verify binary compatibility
./gradlew dependencyAnalysis  # Check unused dependencies
```

## Architecture

For a deep-dive on module responsibilities, data flow, and extension points, see `docs/concepts/architecture.md`.

### Test Generation Flow

1. **Parse OpenAPI**: `ParseOptions().apply { isResolveFully = true }` ensures full schema resolution
2. **Generate test suites**: `TestSuiteGenerator` iterates operations and delegates to providers
3. **Apply rules**: Each provider composes multiple rules to derive test cases
4. **Emit artifacts**: `ArtifactGenerator` implementations output Java/Kotlin tests or JSON/YAML files

### Key Components

#### Configuration Layer (`core/src/main/kotlin/.../config/`)
- `TestGenerationEngine`: Shared facade for wiring core components (used by CLI and Gradle plugin)
- `TestGenerationSettings`: Typed settings container (`ignoreTestCases`, `ignoreSchemaValidationRules`, `maxSchemaDepth`, `maxSchemaCombinations`, `maxTestCasesPerOperation`, etc.)
- `TestGeneratorExecutionOptions`: Fully resolved options for a single execution (combines config files and environment-specific sources)
- `TestGeneratorExecutionOptionsFactory`: Merges declarative config with overrides into execution options

This layer centralizes orchestration logic, avoiding duplication between CLI and plugin.

#### Distribution Layer (`distribution-bundle/src/main/kotlin/.../distribution/`)
- `TestGenerationRunner`: Builder-pattern entry point for test generation execution; encapsulates common workflow
- `TestGenerationReporter`: Interface for output customization (CLI uses SLF4J, plugin uses Gradle logger)
- `TestGenerationResult`: Sealed class for handling success/failure (CLI returns exit codes, plugin throws exceptions)
- `DistributionDefaults`: Factory for standard modules, extractors, and settings with pattern provider enabled
- `Slf4jReporter`: Default reporter implementation using SLF4J

The distribution module simplifies CLI and plugin by bundling `core`, `pattern-support`, and `generator-template` into a single dependency with a high-level API.

#### Generation Layer (`core/src/main/kotlin/.../generation/`)
- `TestGenerationContext`: Interface providing shared context during generation (OpenAPI model, valid case, helpers)
- `DefaultTestGenerationContext`: Standard implementation with depth tracking and budget controls
- `TestSuiteGenerator`: Interface for test suite generation strategies
- `DefaultTestSuiteGenerator`: Standard implementation using `ValidCaseBuilder` and `ProviderOrchestrator`

#### Test Data Layer (`core/src/main/kotlin/.../testdata/`)
- `ValidCaseBuilder`: Constructs baseline valid `TestCase` from operation parameters, body, and security
- `BasicTestDataProvider`: Provides basic type-appropriate test values
- `SecurityValueProvider`: Provides security scheme values from `validSecurityValues` config
- `SchemaExampleValueGenerator`: Derives values from schema examples, patterns, or defaults

#### Providers (`core/src/main/kotlin/.../providers/`)
- `ParameterTestCaseProviderForOperation`: Generates tests for query/path/header/cookie parameters
- `RequestBodyTestCaseProviderForOperation`: Generates tests for request body validation
- `AuthTestCaseProviderForOperation`: Generates tests for authentication/security

Each provider accepts a valid baseline test case and returns a list of derived negative test cases (typically expecting 400/401/403 status codes).

#### Rules (`core/src/main/kotlin/.../rules/`)
- `schema/SimpleSchemaValidationRule`: Base for simple scalar rules (min/max, pattern, enum, etc.)
- `ArrayItemSchemaValidationRule`: Composed rule for arrays (delegates to nested rules)
- `ObjectItemSchemaValidationRule`: Composed rule for objects (delegates to nested rules)
- `AuthValidationRule`: Security scheme validation rules

Rules return `Sequence<RuleValue>` for deterministic, lazy evaluation. All rules are sorted by class name for stable output.

#### Orchestration (`core/src/main/kotlin/.../generation/orchestration/`)
- `ProviderOrchestrator`: Executes providers in fixed order (auth → parameters → request body)
- `OutcomeAggregator`: Merges provider results, accumulating test cases and errors
- `TestCaseBudgetValidator`: Enforces `maxTestCasesPerOperation` limit by throwing `BudgetExceededException`

#### Generators (`core/src/main/kotlin/.../generator/`)
- `TemplateArtifactGenerator`: Mustache-based code generation (Java/Kotlin tests) - in `generator-template` module
- `TestSuiteWriter`: JSON/YAML emission for data-driven frameworks
- `ArtifactGeneratorRegistry`: Registry for generator factories by id
- `BuiltInGenerators`: Factory providing built-in generators (`test-suite-writer`)

Options are validated via `transformAndValidateOptions(...)` functions that fail fast with clear error messages.

#### Budget Controls
- `maxSchemaDepth`: Limits recursion in nested schemas (default: 50)
- `maxSchemaCombinations`: Limits allOf/anyOf/oneOf explosion (default: 100)
- `maxTestCasesPerOperation`: Caps test cases per operation (default: 1000)

Budget violations produce `GenerationError` with `BudgetExceededException` context.

### Monorepo Structure

Composite build defined in `settings.gradle.kts`:
```kotlin
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

## Documentation

Documentation lives under `docs/` and is built with MkDocs (Material theme). See `.cursor/rules/13-documentation.mdc` for full authoring guidance.

### Structure (Diataxis Framework)

| Directory | Purpose | Content type |
|-----------|---------|--------------|
| `getting-started/` | Tutorials | Step-by-step learning guides |
| `how-to/` | Task guides | Problem → Solution walkthroughs |
| `concepts/` | Explanation | Architecture, design rationale |
| `reference/` | Lookup | Settings, SPI, catalogs |
| `modules/` | Module docs | Per-module technical details |
| `samples/` | Examples | Sample project walkthroughs |
| `contributing/` | Contributor guides | Setup, style, release process |
| `api/` | Dokka output | Auto-generated (do not edit) |

### Preview Commands

```bash
# Install Python dependencies
python -m pip install -r docs/requirements.txt

# Preview with regenerated Dokka API docs
./gradlew docsServe

# Preview without regenerating Dokka (faster iteration)
mkdocs serve
```

### Key Conventions

- **File naming**: Lowercase with hyphens (`my-page.md`)
- **No frontmatter**: Start directly with `# Page Title`
- **Active voice**: "Configure X" not "X can be configured"
- **Code blocks**: Always specify language (` ```kotlin `, ` ```yaml `)
- **Admonitions**: Use `!!! note`, `!!! warning`, `!!! tip` for callouts
- **Diagrams**: Use Mermaid for flowcharts and architecture
- **Cross-references**: Link instead of duplicating content

### Adding/Updating Documentation

1. Create or edit markdown files under `docs/`
2. Add new pages to `mkdocs.yml` under `nav:`
3. Run `mkdocs serve` to verify rendering and links
4. Update code and docs in the same PR

### Docs Acceptance Criteria

- Examples are runnable and realistic (no `foo`/`bar` placeholders)
- Links resolve (`mkdocs serve` shows no broken links)
- New pages appear in `mkdocs.yml` navigation
- Consistent terminology with `docs/concepts/glossary.md`
- Reference catalogs updated when adding rules/providers/generators

## Adding New Features

### New Validation Rule

1. Extend `SimpleSchemaValidationRule` (or `AuthValidationRule` for security)
2. Implement `getRuleName()` and `apply(schema, openAPI)` → `Sequence<RuleValue>`
3. Register in `BuiltInRules.simpleSchemaValidationRules()` (or `authValidationRules()`)
4. Add focused unit tests under `core/src/test/kotlin/.../rules/`
5. Ensure deterministic ordering and no side effects

### New Test Provider

1. Implement `TestCaseProvider<Operation>`
2. Accept rules via constructor; compose them in `provideTestCases(...)`
3. Set `expectedStatusCode` appropriately (typically 400 for validation errors)
4. Add tests under `core/src/test/kotlin/.../providers/`
5. Register in `TestGeneratorConfigurer.createTestSuiteGenerator(...)`

### New Code Generator Backend

1. Implement `ArtifactGenerator` interface
2. Create `ArtifactGeneratorFactory` implementation with `id`, `description`, and `create(...)` method
3. Create `transformAndValidateXxxOptions(...)` function for option parsing
4. Prefer Mustache templates over string templating
5. Register factory in `BuiltInGenerators.all()`
6. Add end-to-end test ensuring deterministic outputs

### Ignore Configuration

Filter generated test cases via `testGenerationSettings`:
```kotlin
testGenerationSettings {
    // Skip specific rules
    ignoreSchemaValidationRules.addAll("OutOfMinimumLengthString", "InvalidEnumValue")
    ignoreAuthValidationRules.add("MissingSecurityValues")

    // Skip test cases by path/operation/name pattern
    ignoreTestCases.putAll(mapOf(
        "/internal/*" to mapOf("*" to listOf("*")),  // Skip all tests for /internal/ paths
        "/pets/{petId}" to mapOf(
            "GET" to listOf("Invalid*"),  // Skip "Invalid*" tests for GET /pets/{petId}
            "DELETE" to listOf("*")       // Skip all tests for DELETE /pets/{petId}
        )
    ))
}
```

### New TestGenerationModule

To create a feature module (like `pattern-support` or `generator-template`):

1. Implement `TestGenerationModule`:
```kotlin
public class MyModule : TestGenerationModule {
    override val id: String = "my-module"
    override val description: String = "Adds custom functionality"

    override fun createArtifactGeneratorFactories() = listOf(MyGeneratorFactory())
    override fun createSchemaValueProviders() = listOf(MyValueProvider())
    override fun createSimpleSchemaValidationRules() = listOf(MyRule())
    override fun createModuleSettingsExtractor() = MySettingsExtractor()
}
```

2. Register in CLI/plugin when wiring `TestGenerationEngine`
3. Add tests under `<module>/src/test/kotlin`
4. Document module settings via `ModuleSettingsExtractor`

### Acceptance Criteria (for any change)

- `./gradlew :core:check :plugin:check` passes (Java 21, Kotlin 2.2)
- No detekt violations in changed files
- Configuration cache compatibility preserved (no heavy work at configuration time)
- Public API changes explicitly annotated and covered by `apiCheck`
- Deterministic output ordering (sort by class name, keys)

## Engineering Principles

From `.cursor/rules/12-solid-dry-kiss.mdc`:

- **SOLID**: Keep classes focused; use extension points (`getRules`, `createTestGenerator`) for new features
- **DRY**: Extract common transformations into helpers; centralize option parsing
- **KISS**: Prefer simple data flows, fail fast with precise errors, avoid premature generalization
- **YAGNI**: Don't add functionality until it's actually needed; resist speculative features and "just in case" options
- **Separation of Concerns**: Keep parsing (OpenAPI), generation (providers/rules), and output (generators) separate; don't mix business logic with I/O
- **Principle of Least Surprise**: Follow naming conventions; default behaviors should be safe and intuitive; error messages should be actionable
- **Dependency Direction**: Dependencies flow inward (`plugin` → `core` → `model`); high-level modules depend on low-level, never reverse
- **Determinism**: Sort collections by class name or key; validate inputs early with `require`/`requireNotNull`
- **Testing posture**: Unit-test rules/providers in isolation; snapshot/end-to-end test generators

## Common Pitfalls

1. **Mutation**: Providers and rules must not mutate inputs; keep transformations pure
2. **Ordering**: Always sort classes and collection keys for stable outputs
3. **Null safety**: Never use `!!`; validate explicitly or use safe defaults
4. **Configuration time**: Avoid expensive I/O or parsing during Gradle configuration phase
5. **Test isolation**: No network, no file I/O (except controlled fixtures), no shared mutable state

## Debugging

### Common Issues

**Test generation produces no test cases:**
- Check if `ignoreTestCases` or `ignoreSchemaValidationRules` filters everything
- Verify the OpenAPI spec has operations with parameters or request bodies
- Check logs for `BudgetExceededException` or parsing errors

**Schema errors:**
- Enable debug logging: `--log-level debug` (CLI) or `logLevel.set("debug")` (Gradle)
- Check for circular references in schemas (logged as warnings)
- Verify `$ref` paths resolve correctly

**Budget exceeded:**
- Increase limits: `maxSchemaDepth`, `maxSchemaCombinations`, `maxTestCasesPerOperation`
- Simplify complex allOf/anyOf/oneOf compositions
- Use `ignoreTestCases` to skip problematic operations

### Useful Commands
```bash
# Run with verbose output
./gradlew :core:test --info

# Run single test with debug
./gradlew :core:test --tests "TestGeneratorTest" --debug-jvm

# Check for unused dependencies
./gradlew dependencyAnalysis

# Verify API compatibility
./gradlew apiCheck
./gradlew apiDump  # Update API files after intentional changes
```

## Dependencies

- **Java 21** (toolchain)
- **Kotlin 2.2.10**
- **OpenAPI parser**: `io.swagger.parser.v3:swagger-parser-v3` - parses and resolves OpenAPI specs
- **Mustache templates**: `com.github.spullara.mustache.java:compiler` - template rendering (generator-template module)
- **Regex generation**: `org.cornutum.regexp:regexp-gen` - generates strings matching patterns (pattern-value module)
- **Jackson**: JSON/YAML serialization with Kotlin module
- **Picocli**: CLI argument parsing (cli module)
- **Testing**: JUnit 5, AssertJ, Allure, Mockito
- **Linting**: detekt with ktlint rules
- **GraalVM**: Native Build Tools plugin (CLI); reflection config may be needed for OpenAPI parser/Jackson
