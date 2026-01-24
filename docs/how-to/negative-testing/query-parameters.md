# Query Parameter Validation Tests

This guide explains how OpenAPI Test Generator creates negative tests for query parameters,
including both missing required parameters and invalid values.

## Types of Query Parameter Tests

The generator produces two categories of query parameter tests:

| Category | Provider/Rule | Expected Status | When Generated |
|----------|---------------|-----------------|----------------|
| Missing required | `MissedRequiredParameterTestProvider` | 400 | When `required: true` |
| Invalid values | `ParameterSchemaValidationTestProvider` | 400 | When schema constraints exist |

!!! note "Provider vs rule in output"
    The missing-required case is created by `MissedRequiredParameterTestProvider`. For invalid values,
    the provider applies schema rules, so the generated `rule` field contains a rule class name (for
    example, `MissedRequiredObjectPropertiesSchemaValidationRule`), not the provider name.

## Missing Required Query Parameters

When a query parameter has `required: true`, the generator creates a test case that omits
the parameter entirely, expecting the API to return a 400 Bad Request.

### Example OpenAPI Spec

From `core/src/test/resources/oas/circular.openapi.yaml`:

```yaml
paths:
  /persons:
    get:
      operationId: listPersons
      parameters:
        - name: person
          in: query
          required: true
          schema:
            $ref: '#/components/schemas/Person'
```

### Generated Test Case

```json
{
  "name": "Missed Required Query Parameter person",
  "method": "GET",
  "path": "/persons",
  "queryParams": {},
  "expectedStatusCode": 400,
  "rule": "art.galushko.openapi.testgen.providers.parameter.MissedRequiredParameterTestProvider"
}
```

!!! note "Test Case Naming Pattern"
    Missing required parameter tests follow the pattern:
    `Missed Required {ParameterLocation} Parameter {parameterName}`

    Examples:

    - `Missed Required Query Parameter person`
    - `Missed Required Header Parameter X-Request-ID`
    - `Missed Required Cookie Parameter session`

## Complex Object Query Parameters

When a query parameter references an object schema with required properties, the generator
produces tests for each missing required property. For the `Person` schema:

```yaml
components:
  schemas:
    Person:
      type: object
      required: [name, age]
      properties:
        name:
          type: string
          minLength: 1
          maxLength: 100
        age:
          type: integer
          minimum: 0
          maximum: 150
        address:
          $ref: '#/components/schemas/Address'
```

The generator creates these test cases:

- `Invalid Query person parameter: Missed Required Object Properties age`
- `Invalid Query person parameter: Missed Required Object Properties name`
- `Invalid Query person parameter: Object Property address Missed Required Object Properties city`
- `Invalid Query person parameter: Object Property address Missed Required Object Properties street`

## Invalid Query Parameter Values

Object property constraints generate invalid-value tests. For the `Person` schema above:

- `Invalid Query person parameter: Object Property age Integer Breaking` - Non-integer value
- `Invalid Query person parameter: Object Property age Invalid Type` - Wrong type
- `Invalid Query person parameter: Object Property age Out Of Maximum Boundary Number` - Exceeds max
- `Invalid Query person parameter: Object Property age Out Of Minimum Boundary Number` - Below min
- `Invalid Query person parameter: Object Property name Out Of Maximum Length String` - Too long
- `Invalid Query person parameter: Object Property name Out Of Minimum Length String` - Too short

Fixture-backed example (invalid type for `age`):

```json
{
  "name": "Invalid Query person parameter: Object Property age Invalid Type",
  "method": "GET",
  "path": "/persons",
  "queryParams": {
    "person": {
      "age": "abc",
      "name": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
  },
  "expectedStatusCode": 400,
  "rule": "art.galushko.openapi.testgen.rules.composed.ObjectItemSchemaValidationRule"
}
```

Fixture source: `core/src/test/resources/oas/circular-generated-test-suites-by-name.json`.

## Running the Generator

### CLI

```bash
./gradlew :cli:installDist

./cli/build/install/openapi-testgen/bin/openapi-testgen \
  --spec-file core/src/test/resources/oas/circular.openapi.yaml \
  --output-dir ./build/generated \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json
```

### Gradle

```kotlin
openApiTestGenerator {
    specFile.set(file("core/src/test/resources/oas/circular.openapi.yaml"))
    outputDir.set(file("build/generated-tests"))
    generator.set("test-suite-writer")
    generatorOptions.putAll(
        mapOf(
            "format" to "json",
            "outputFileName" to "test-suites.json"
        )
    )
}
```

## Inspecting Generated Output

Find query parameter tests in the generated JSON:

```bash
# Find all missing required query parameter tests
jq '.. | objects | select(.name? | strings | contains("Missed Required Query"))' \
  build/generated/test-suites.json

# Find all invalid query parameter tests
jq '.. | objects | select(.name? | strings | startswith("Invalid Query"))' \
  build/generated/test-suites.json
```

Example output from the fixture:

```json
{
  "name": "Missed Required Query Parameter person",
  "method": "GET",
  "path": "/persons",
  "queryParams": {},
  "pathParams": {},
  "headers": [],
  "cookie": [],
  "securityValues": {
    "queryParams": {},
    "headers": [],
    "cookie": [],
    "other": {}
  },
  "body": null,
  "expectedBody": null,
  "needToComplete": false,
  "expectedStatusCode": 400,
  "rule": "art.galushko.openapi.testgen.providers.parameter.MissedRequiredParameterTestProvider"
}
```

Fixture source: `core/src/test/resources/oas/circular-generated-test-suites-by-name.json`.

## Filtering Query Parameter Tests

Test case name lists require exact matches (wildcards are not evaluated).

### Exclude missing parameter tests for a path

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/persons":
      "GET":
        - "Missed Required Query Parameter person"
```

### Exclude specific validation tests

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/persons":
      "GET":
        - "Missed Required Query Parameter person"
        - "Invalid Query person parameter: Missed Required Object Properties age"
        - "Invalid Query person parameter: Missed Required Object Properties name"
```

### Target only query parameter operations

Use [includeOperations](../configuration/include-operations.md) to generate tests for specific paths:

```yaml
testGenerationSettings:
  includeOperations:
    "/persons":
      - "GET"
```

## Related Documentation

- [Providers Catalog](../../reference/catalogs/providers-catalog.md) - MissedRequiredParameterTestProvider details
- [Rules Catalog](../../reference/catalogs/rules-catalog.md) - Schema validation rules
- [Ignore Rules](../configuration/ignore-rules.md) - Filtering test cases
- [Include Operations](../configuration/include-operations.md) - Targeted generation
