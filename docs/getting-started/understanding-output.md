# Understanding output

The output depends on the selected generator (`template` or `test-suite-writer`).

## Template generator (`template`)

- Produces source files (Java or Kotlin) under your configured `outputDir`.
- Template sets are under `generator-template/src/main/resources/templates/`.
- Common template variables:
  - `package`: optional package declaration
  - `baseUrl`: used by built-in RestAssured templates

See: [Template generator](../how-to/generators/template-generator.md)

## Test suite writer (`test-suite-writer`)

- Produces JSON or YAML containing `TestSuite` and `TestCase` structures.
- By default, aggregates all suites into a single file (`outputMode=SINGLE_FILE`).

See:

- [Test-suite-writer generator](../how-to/generators/test-suite-writer.md)
- [Model reference](../reference/model/test-suite.md)

## Valid case inclusion

By default, only negative test cases (expecting 4xx responses) are generated.
To include a positive test case that validates successful requests, enable the
`includeValidCase` setting:

```yaml
testGenerationSettings:
    includeValidCase: true
```

The valid case will:
- Be named "Test Valid Case"
- Have `expectedStatusCode` set to the first 2xx response defined in the spec (200, 201, 204, etc.)
- Include only required parameters with valid example values
- Appear in each test suite's test case list (order may be normalized by writers)

