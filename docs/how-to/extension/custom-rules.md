---
description: Learn how to create custom validation rules that generate negative test cases for domain-specific constraints, custom string formats, or OpenAPI extension properties not covered by built-in rules.
---

# Custom validation rules

This guide shows how to create a custom validation rule that generates additional negative test cases for your OpenAPI specifications.

## When to create a custom rule

Create a custom rule when:

- Your API has domain-specific validation not covered by built-in rules (e.g., custom string formats, business-logic constraints)
- You want to test specific edge cases unique to your service
- You need to validate extension properties (`x-*` fields) in your OpenAPI spec

Use [built-in rules](../../reference/catalogs/rules-catalog.md) for standard OpenAPI constraints like `minimum`, `maximum`, `pattern`, `enum`, etc.

## Rule types

### Schema rules (`SimpleSchemaValidationRule`)

Schema rules produce invalid values for individual schema nodes. They return `Sequence<RuleValue>` where each `RuleValue` contains:

- A description (used in the test case name)
- An invalid value to substitute into the request

Schema rules are applied to parameters (query, path, header, cookie) and request body properties.

### Auth rules (`AuthValidationRule`)

Auth rules produce complete negative test cases for authentication scenarios. They return `Sequence<TestCase>` with explicit `expectedStatusCode` (typically 401 or 403).

Use auth rules when the negative case affects multiple request fields or security configuration.

## Complete example: suffix validation rule

This example creates a rule that generates test cases for string fields with a custom `x-suffix` extension.

### Step 1: Implement the rule

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
        // Guard clause: only apply to string schemas
        if (!isStringSchema(schema)) return emptySequence()

        // Guard clause: only apply when x-suffix extension is present
        val requiredSuffix = schema.extensions?.get("x-suffix") as? String
            ?: return emptySequence()

        // Generate an invalid value (string without the required suffix)
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

### Step 2: Unit test the rule

Use the project's test utilities for consistent testing:

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

### Step 3: Register via `TestGenerationModule`

Create a module to contribute your rule:

```kotlin
package com.example.rules

import art.galushko.openapi.testgen.config.TestGenerationModule
import art.galushko.openapi.testgen.config.TestGeneratorExecutionOptions
import art.galushko.openapi.testgen.spi.SimpleSchemaValidationRule

class SuffixValidationModule : TestGenerationModule {
    override val id: String = "suffix-validation"

    override fun extraSimpleSchemaRules(
        options: TestGeneratorExecutionOptions
    ): List<SimpleSchemaValidationRule> = listOf(SuffixValidationRule())
}
```

### Step 4: Use with `TestGenerationRunner`

To run generation from Kotlin code, use `distribution-bundle`'s `TestGenerationRunner` API.
Start with the canonical embedding example in [Module: `distribution-bundle`](../../modules/distribution-bundle.md#programmatic-invocation-kotlin), then add your `TestGenerationModule` to the runner wiring (see [Custom modules](custom-modules.md)).

## Debugging custom rules

### Enable debug logging

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

### Inspect output by rule

When using the `test-suite-writer` generator with JSON output, filter test cases by rule FQCN:

```bash
# Find all test cases generated by your rule
jq '.[] | .testCases[] | select(.rule == "com.example.rules.SuffixValidationRule")' \
  generated/test-suites.json

# Count test cases per rule
jq '[.[] | .testCases[].rule] | group_by(.) | map({rule: .[0], count: length})' \
  generated/test-suites.json
```

### Common issues

**Rule not producing test cases:**

1. Check guard clauses - add logging to verify the schema meets your conditions
2. Verify the schema type using `schema.type` or `schema.types`
3. Ensure extension properties are present in your OpenAPI spec

**Rule producing wrong values:**

1. Verify the `RuleValue` description matches `getRuleName()`
2. Check that invalid values actually violate the constraint
3. Test with a minimal OpenAPI spec to isolate the issue

## CLI and Gradle plugin limitations

Custom rules require **embedded usage** with `TestGenerationRunner`. The CLI and Gradle plugin only support built-in rules from the distribution bundle.

To use custom rules with CLI/Gradle:

1. Fork the project and add your rule to `core/src/main/kotlin/.../rules/`
2. Register it in `BuiltInRules`
3. Build your custom distribution

For most use cases, embedding with `TestGenerationRunner` is simpler.

## Reference implementation

The [`pattern-support`](../../modules/pattern-support.md) module demonstrates production-quality rule implementation:

| File | Purpose |
|------|---------|
| [`InvalidPatternSchemaValidationRule.kt`](https://github.com/ArtGalushko/openapi-test-generator/blob/main/pattern-support/src/main/kotlin/art/galushko/openapi/testgen/pattern/support/InvalidPatternSchemaValidationRule.kt) | Rule implementation with guard clauses, error handling, and logging |
| [`PatternSupportModule.kt`](https://github.com/ArtGalushko/openapi-test-generator/blob/main/pattern-support/src/main/kotlin/art/galushko/openapi/testgen/pattern/support/PatternSupportModule.kt) | Module registration pattern |
| [`InvalidPatternSchemaValidationRuleTest.kt`](https://github.com/ArtGalushko/openapi-test-generator/blob/main/pattern-support/src/test/kotlin/art/galushko/openapi/testgen/pattern/support/InvalidPatternSchemaValidationRuleTest.kt) | Test patterns with parameterized tests |

## Notes

!!! note "Rule filtering uses FQCN"
    `ignoreSchemaValidationRules` and `ignoreAuthValidationRules` settings expect **fully qualified class names** (e.g., `com.example.rules.SuffixValidationRule`), not the value from `getRuleName()`.

!!! warning "Determinism required"
    Rules must produce identical output for identical inputs. Avoid randomness, non-deterministic iteration, and side effects.

## Related documentation

- Reference: [Validation rules SPI](../../reference/spi/validation-rules.md)
- Reference: [Rules catalog](../../reference/catalogs/rules-catalog.md)
- Concepts: [Determinism](../../concepts/determinism.md)
- How-to: [Custom modules](custom-modules.md)
- Module: [Pattern support](../../modules/pattern-support.md)
