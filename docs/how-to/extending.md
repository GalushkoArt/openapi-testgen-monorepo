---
description: Extend the test generator with custom validation rules, test case providers, artifact generators, and modules. Includes complete Kotlin examples, registration patterns, and debugging tips.
---

# Extending the generator

The test generator can be extended with:

- **Validation rules** -- produce invalid values for schema nodes or auth scenarios
- **Providers** -- orchestrate rule application into `TestCase` objects (contributor workflow)
- **Generators** -- emit artifacts (source code, JSON, YAML) from `TestSuite`s
- **Modules** -- bundle rules, generators, and providers without reflection via `TestGenerationModule`

## Custom validation rules

### When to create a custom rule

Create a custom rule when:

- Your API has domain-specific validation not covered by built-in rules (e.g., custom string formats, business-logic constraints)
- You want to test specific edge cases unique to your service
- You need to validate extension properties (`x-*` fields) in your OpenAPI spec

Use [built-in rules](../reference/catalogs/rules-catalog.md) for standard OpenAPI constraints like `minimum`, `maximum`, `pattern`, `enum`, etc.

### Rule types

#### Schema rules (`SimpleSchemaValidationRule`)

Schema rules produce invalid values for individual schema nodes. They return `Sequence<RuleValue>` where each `RuleValue` contains:

- A description (used in the test case name)
- An invalid value to substitute into the request

Schema rules are applied to parameters (query, path, header, cookie) and request body properties.

#### Auth rules (`AuthValidationRule`)

Auth rules produce complete negative test cases for authentication scenarios. They return `Sequence<TestCase>` with explicit `expectedStatusCode` (typically 401 or 403).

Use auth rules when the negative case affects multiple request fields or security configuration.

### Complete example: suffix validation rule

This example creates a rule that generates test cases for string fields with a custom `x-suffix` extension.

#### Step 1: Implement the rule

```kotlin
package com.example.rules

import art.galushko.openapi.testgen.generation.TestGenerationContext
import art.galushko.openapi.testgen.spi.RuleValue
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema

/**
 * Produces a string that does NOT end with the required suffix.
 *
 * Applies when the schema has an `x-suffix` extension property.
 * Returns a single [RuleValue] with a value missing the required suffix.
 */
class SuffixValidationRule : SimpleSchemaValidationRule {

    override fun getRuleName(): String = "Invalid Suffix"

    override fun apply(schema: Schema<*>, context: TestGenerationContext): Sequence<RuleValue> {
        if (!isStringSchema(schema)) return emptySequence()

        val requiredSuffix = schema.extensions?.get("x-suffix") as? String
            ?: return emptySequence()

        val baseValue = "invalid_value"
        val invalidValue = if (baseValue.endsWith(requiredSuffix)) "${baseValue}_x" else baseValue

        return sequenceOf(RuleValue(getRuleName(), invalidValue))
    }

    private fun isStringSchema(schema: Schema<*>): Boolean {
        if (schema is StringSchema) return true
        val types = schema.types
        if (types != null && types.contains("string")) return true
        return schema.type == "string"
    }
}
```

Key implementation patterns:

- **Guard clauses first**: Return `emptySequence()` when the rule doesn't apply
- **Type checking**: Verify the schema type before processing
- **Extension access**: Read custom properties via `schema.extensions`
- **Deterministic output**: Same inputs must produce same outputs

#### Step 2: Unit test the rule

```kotlin
package com.example.rules

import art.galushko.openapi.testgen.generation.createBasicTestCase
import art.galushko.openapi.testgen.generation.createTestContext
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SuffixValidationRuleTest {

    private val rule = SuffixValidationRule()

    @Test
    @DisplayName("should generate invalid value for string schema with x-suffix")
    fun shouldApplyToStringSchemaWithSuffix() {
        val schema = StringSchema().apply {
            extensions = mapOf("x-suffix" to ".json")
        }
        val context = createTestContext(
            validCase = createBasicTestCase(),
            operation = Operation(),
            openAPI = OpenAPI()
        )

        val result = rule.apply(schema, context).toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().buildDescription()).isEqualTo("Invalid Suffix")
        assertThat(result.first().value as String).doesNotEndWith(".json")
    }

    @Test
    @DisplayName("should return empty sequence for schema without x-suffix")
    fun shouldNotApplyWithoutExtension() {
        val schema = StringSchema()
        val context = createTestContext()

        val result = rule.apply(schema, context).toList()

        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("should return empty sequence for non-string schema")
    fun shouldNotApplyToNonStringSchema() {
        val schema = IntegerSchema().apply {
            extensions = mapOf("x-suffix" to ".json")
        }
        val context = createTestContext()

        val result = rule.apply(schema, context).toList()

        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("should produce deterministic output")
    fun shouldBeDeterministic() {
        val schema = StringSchema().apply {
            extensions = mapOf("x-suffix" to ".json")
        }
        val context = createTestContext()

        val result1 = rule.apply(schema, context).toList()
        val result2 = rule.apply(schema, context).toList()

        assertThat(result1).isEqualTo(result2)
    }
}
```

!!! note "Test helpers are internal to this repo"
    `createTestContext()` and `createBasicTestCase()` live under `core/src/test/...` in this repository.
    If you are implementing rules in another project, either copy the minimal helper pattern or build
    a `DefaultTestGenerationContext` directly with the dependencies you need.

### Debugging custom rules

#### Enable debug logging

Use `--log-level DEBUG` with the CLI to see rule application details:

```bash
openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option outputFileName=test-suites.json \
  --log-level DEBUG
```

??? note "Building from source (contributors)"
    If you're contributing to the project or need to test local changes:

    ```bash
    ./gradlew :cli:installDist
    ./cli/build/install/openapi-testgen/bin/openapi-testgen --help
    ```

#### Inspect output by rule

When using the `test-suite-writer` generator with JSON output, filter test cases by rule FQCN:

```bash
jq '.[] | .testCases[] | select(.rule == "com.example.rules.SuffixValidationRule")' \
  generated/test-suites.json

jq '[.[] | .testCases[].rule] | group_by(.) | map({rule: .[0], count: length})' \
  generated/test-suites.json
```

#### Common issues

**Rule not producing test cases:**

1. Check guard clauses -- add logging to verify the schema meets your conditions
2. Verify the schema type using `schema.type` or `schema.types`
3. Ensure extension properties are present in your OpenAPI spec

**Rule producing wrong values:**

1. Verify the `RuleValue` description matches `getRuleName()`
2. Check that invalid values actually violate the constraint
3. Test with a minimal OpenAPI spec to isolate the issue

### CLI and Gradle plugin limitations

Custom rules require **embedded usage** with `TestGenerationRunner`. The CLI and Gradle plugin only support built-in rules from the distribution bundle.

To use custom rules with CLI/Gradle:

1. Fork the project and add your rule to `core/src/main/kotlin/.../rules/`
2. Register it in `BuiltInRules`
3. Build your custom distribution

For most use cases, embedding with `TestGenerationRunner` is simpler.

### Reference implementation

The [`pattern-support`](../modules/index.md#pattern-support) module entry in the catalog points to the production-quality wiring used in this repository:

| File | Purpose |
|------|---------|
| [`InvalidPatternSchemaValidationRule.kt`](https://github.com/GalushkoArt/openapi-testgen-monorepo/blob/main/pattern-support/src/main/kotlin/art/galushko/openapi/testgen/pattern/support/InvalidPatternSchemaValidationRule.kt) | Rule implementation with guard clauses, error handling, and logging |
| [`PatternSupportModule.kt`](https://github.com/GalushkoArt/openapi-testgen-monorepo/blob/main/pattern-support/src/main/kotlin/art/galushko/openapi/testgen/pattern/support/PatternSupportModule.kt) | Module registration pattern |
| [`InvalidPatternSchemaValidationRuleTest.kt`](https://github.com/GalushkoArt/openapi-testgen-monorepo/blob/main/pattern-support/src/test/kotlin/art/galushko/openapi/testgen/pattern/support/InvalidPatternSchemaValidationRuleTest.kt) | Test patterns with parameterized tests |

!!! note "Rule filtering uses FQCN"
    `ignoreSchemaValidationRules` and `ignoreAuthValidationRules` settings expect **fully qualified class names** (e.g., `com.example.rules.SuffixValidationRule`), not the value from `getRuleName()`.

---

## Custom providers

!!! warning "Contributor workflow"
    Adding a new `TestCaseProvider` currently requires changing `core` wiring (it cannot be contributed via `TestGenerationModule`).
    Rules and generators *can* be contributed via `TestGenerationModule`, so prefer those extension points when possible.
    See [Development setup](../contributing/development-setup.md) if you plan to change `core`.

Providers turn rules (and other logic) into generated `TestCase` objects. In `core`, providers are orchestrated in a fixed order per operation:

1. auth
2. parameters
3. request body

### When to add a provider (vs a rule)

Add a provider when you want a new class of negative cases that is not naturally represented as a schema/auth rule.

If you only need additional schema cases, prefer adding a rule instead (see [Custom validation rules](#custom-validation-rules)).

### 1) Implement a provider

Implement `TestCaseProvider<T>` (typically `T = Operation`). Providers return `Outcome<List<TestCase>>` and should be pure/deterministic:

- Do not mutate inputs (`validCase`, `OpenAPI`, `Operation`)
- Return stable ordering
- Use provider boundary helpers (`runProviderSafely`) so exceptions become structured failures

### 2) Wire it into generation

In this repo, provider wiring happens in `TestGeneratorConfigurer.createTestSuiteGenerator(...)` via `ProviderOrchestrator`.

To add a provider:

1. Add your provider implementation under `core/src/main/kotlin/.../providers/`.
2. Decide the execution position (auth -> parameters -> body is intentional).
3. Update `TestGeneratorConfigurer` to include your provider in the orchestrator list.

### 3) Add tests

Add focused tests under `core/src/test/kotlin/.../providers/` and assert:

- generated case names (exact strings)
- expected status codes (usually 400, auth often 401/403)
- stable ordering and deterministic output

---

## Custom generators

Generators emit artifacts from generated `TestSuite`s (source code, JSON/YAML, etc.).

### 1) Implement `ArtifactGenerator` + `ArtifactGeneratorFactory`

The factory validates options and creates a new generator instance per execution:

```kotlin
import art.galushko.openapi.testgen.generator.ArtifactGeneratorFactory
import art.galushko.openapi.testgen.model.TestSuite
import art.galushko.openapi.testgen.spi.ArtifactGenerator
import java.io.File

public object MyGeneratorFactory : ArtifactGeneratorFactory {
    override val id: String = "my-generator"
    override val description: String = "Writes my custom artifacts"

    override fun create(outputDir: File, options: Map<String, Any?>): ArtifactGenerator {
        return object : ArtifactGenerator {
            override fun generateTests(testSuite: TestSuite) {
                outputDir.mkdirs()
                // ... write files ...
            }
        }
    }
}
```

### 2) Register via `TestGenerationModule`

```kotlin
import art.galushko.openapi.testgen.config.TestGenerationModule
import art.galushko.openapi.testgen.generator.ArtifactGeneratorFactory

public class MyGeneratorModule : TestGenerationModule {
    override val id: String = "my-generator-module"
    override fun artifactGeneratorFactories(): List<ArtifactGeneratorFactory> = listOf(MyGeneratorFactory)
}
```

### 3) Use it

- **Embedding**: add the module when executing (see [distribution-bundle](../modules/distribution-bundle.md)).
- **CLI / Gradle plugin**: currently use `DistributionDefaults.modules(...)`. To make a new generator available there, it must be added to the distribution (contributor change).

---

## Custom modules

`TestGenerationModule` is the primary mechanism for adding **optional features without reflection**. Modules can contribute:

- `ArtifactGeneratorFactory` implementations (new generators)
- `SchemaValueProvider` implementations (new example value providers)
- extra schema rules (`SimpleSchemaValidationRule`)
- extra auth rules (`AuthValidationRule`)

### Implement `TestGenerationModule`

```kotlin
import art.galushko.openapi.testgen.config.TestGenerationModule
import art.galushko.openapi.testgen.config.TestGeneratorExecutionOptions
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import art.galushko.openapi.testgen.generator.ArtifactGeneratorFactory
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule

public class MyModule(
    private val provider: SchemaValueProvider,
) : TestGenerationModule {
    override val id: String = "my-module"

    override fun schemaValueProviders(options: TestGeneratorExecutionOptions): Map<String, SchemaValueProvider> =
        mapOf("my-provider" to provider)

    override fun artifactGeneratorFactories(): List<ArtifactGeneratorFactory> = emptyList()

    override fun extraSimpleSchemaRules(options: TestGeneratorExecutionOptions): List<SimpleSchemaValidationRule> = emptyList()
}
```

### Optional: add settings extraction

If your module needs its own settings blob, implement `ModuleSettingsExtractor` and add it to execution (typically via `TestGenerationRunner.builder().addModuleExtractor(...)`).

!!! note "Module-owned settings"
    `ModuleSettingsExtractor` is the mechanism for parsing module-owned settings blobs. It is separate from `TestGenerationModule` and runs before core settings parsing.

### Wire the module

- **Embedding**: use `TestGenerationRunner.builder()` and add your module (see [distribution-bundle](../modules/distribution-bundle.md)).
- **CLI / Gradle plugin**: the default distribution modules are defined by `DistributionDefaults.modules(...)`.

---

## Wiring and determinism

These requirements apply to all extension types (rules, generators, modules):

- Validate inputs early and fail with actionable error messages.
- Keep output deterministic (stable ordering, no timestamps, no randomness).
- Sort keys and class lists when serializing maps/sets.
- Module ids must be stable and unique.

!!! warning "Determinism required"
    Rules must produce identical output for identical inputs. Avoid randomness, non-deterministic iteration, and side effects.

## Related docs

- Reference: [Validation rules SPI](../reference/spi.md#validation-rules)
- Reference: [Test providers SPI](../reference/spi.md#test-providers)
- Reference: [Generators SPI](../reference/spi.md#generators)
- Reference: [Rules catalog](../reference/catalogs/rules-catalog.md)
- How-to: [Generators](generators.md)
- Concepts: [Architecture](../concepts/architecture.md)
- Concepts: [Determinism](../concepts/architecture.md#determinism-guarantees)
- Module: [Pattern support](../modules/index.md#pattern-support)
- Module: [distribution-bundle](../modules/distribution-bundle.md)
