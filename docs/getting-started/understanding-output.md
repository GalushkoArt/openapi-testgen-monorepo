---
description: Explains the output formats produced by OpenAPI Test Generator. Covers template generator output (Java/Kotlin source files), test-suite-writer output (JSON/YAML), and the includeValidCase setting for positive test cases.
---

# Understanding output

The output depends on the selected generator (`template` or `test-suite-writer`).

## Template generator (`template`)

- Produces source files (Java or Kotlin) under your configured `outputDir`.
- Template sets are under `generator-template/src/main/resources/templates/`.
- Common template variables:
  - `package`: optional package declaration
  - `baseUrl`: used by built-in RestAssured templates

See: [Template generator](../how-to/generators/template-generator.md)

## Test-suite-writer generator (`test-suite-writer`)

- Produces JSON or YAML containing `TestSuite` and `TestCase` structures.
- By default, aggregates all suites into a single file (`outputMode=SINGLE_FILE`).

See:

- [Test-suite-writer generator](../how-to/generators/test-suite-writer.md)
- [Model reference](../reference/model/test-suite.md)

## Valid case inclusion

By default, suites include only negative test cases (expecting 4xx responses).
To include a baseline positive test per operation, enable `testGenerationSettings.includeValidCase`.

See: [Include positive test cases](../how-to/configuration/positive-testing.md) and [Distribution settings → Output options](../reference/distribution-settings.md#output-options)
