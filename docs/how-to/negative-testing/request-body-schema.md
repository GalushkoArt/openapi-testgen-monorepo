# Request Body Schema Validation Tests

This guide explains how OpenAPI Test Generator creates negative tests for request bodies and how to run
and inspect the generated output.

## Types of Request Body Tests

The generator produces two categories of request body tests:

| Category | Provider | Expected Status | When Generated |
|----------|----------|-----------------|----------------|
| Missing required body | `MissedRequiredRequestBodyTestProvider` | 400 | When `requestBody.required: true` |
| Schema violations | `RequestBodySchemaValidationTestProvider` | 400 | When request body schemas have constraints |

!!! note "Provider vs rule in output"
    The missing-body case is created by `MissedRequiredRequestBodyTestProvider`, so the `rule` field
    contains that provider class. For schema violations, the provider applies schema rules, so the `rule`
    field contains a rule class name (for example, `MissedRequiredObjectPropertiesSchemaValidationRule`),
    not the provider name.

## Example OpenAPI Spec

From `samples/openapi.yaml`:

```yaml
paths:
  /orders:
    post:
      operationId: createOrder
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/NewOrder'
```

Schema excerpt:

```yaml
components:
  schemas:
    OrderItem:
      type: object
      required: [ sku, quantity, price ]
      additionalProperties: false
      properties:
        sku: { type: string }
        quantity: { type: integer, minimum: 1 }
        price: { type: number, minimum: 0 }
    NewOrder:
      type: object
      required: [ userId, items ]
      additionalProperties: false
      properties:
        userId: { type: string }
        items:
          type: array
          minItems: 1
          items: { $ref: '#/components/schemas/OrderItem' }
```

## Missing Required Request Body

When `requestBody.required: true`, the generator emits a single test case that omits the body entirely.
Excerpt from `samples/java-spring-file-writer/src/test/resources/openapi-test-suites.json`:

```json
{
  "name": "Required Request Body is missing",
  "method": "POST",
  "path": "/orders",
  "headers": [
    { "key": "X-API-Key", "value": "test-api-key-123" }
  ],
  "body": null,
  "expectedBody": {
    "code": "bad_request",
    "message": "Invalid input"
  },
  "expectedStatusCode": 400,
  "rule": "art.galushko.openapi.testgen.providers.body.MissedRequiredRequestBodyTestProvider"
}
```

!!! tip "Expected body"
    The `expectedBody` field comes from the default error response defined in `samples/openapi.yaml`.
    Your actual error responses will vary based on your API's error schema.

## Schema Constraint Violations

`RequestBodySchemaValidationTestProvider` applies schema rules to the resolved body schema and generates
tests for each constraint violation. Fixture-backed examples for `createOrder`:

- `Incorrect Request Body: Missed Required Object Properties items`<br>
  Body: `{"userId":"a"}`<br>
  Rule: `MissedRequiredObjectPropertiesSchemaValidationRule`
- `Incorrect Request Body: Missed Required Object Properties userId`<br>
  Body: `{"items":[{"price":0,"quantity":1,"sku":"a"}]}`<br>
  Rule: `MissedRequiredObjectPropertiesSchemaValidationRule`
- `Incorrect Request Body: Object Property items Below Min Items Array`<br>
  Body: `{"items":[],"userId":"a"}`<br>
  Rule: `ObjectItemSchemaValidationRule`
- `Incorrect Request Body: Object Property items Array Item Object Property price Out Of Minimum Boundary Number`<br>
  Body: `{"items":[{"price":-1,"quantity":1,"sku":"a"}],"userId":"a"}`<br>
  Rule: `ObjectItemSchemaValidationRule`

Excerpt showing a nested array item violation:

```json
{
  "name": "Incorrect Request Body: Object Property items Array Item Object Property price Out Of Minimum Boundary Number",
  "method": "POST",
  "path": "/orders",
  "headers": [
    { "key": "X-API-Key", "value": "test-api-key-123" }
  ],
  "body": {
    "items": [
      { "price": -1, "quantity": 1, "sku": "a" }
    ],
    "userId": "a"
  },
  "expectedBody": {
    "code": "bad_request",
    "message": "Invalid input"
  },
  "expectedStatusCode": 400,
  "rule": "art.galushko.openapi.testgen.rules.composed.ObjectItemSchemaValidationRule"
}
```

Fixture source: `samples/java-spring-file-writer/src/test/resources/openapi-test-suites.json`.

## Running the Generator

### CLI

```bash
./gradlew :cli:installDist

./cli/build/install/openapi-testgen/bin/openapi-testgen \
  --spec-file samples/openapi.yaml \
  --output-dir ./build/generated \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json \
  --setting 'validSecurityValues.ApiKeyAuth=test-api-key-123' \
  --setting 'includeOperations./orders[]=POST'
```

!!! note "Security scheme names"
    `validSecurityValues` keys are security scheme names (for the sample spec, `ApiKeyAuth`), not
    header names. The generator maps the scheme to `X-API-Key` internally.

### Gradle

```kotlin
openApiTestGenerator {
    specFile.set(file("samples/openapi.yaml"))
    outputDir.set(file("build/generated-tests"))
    generator.set("test-suite-writer")
    generatorOptions.putAll(
        mapOf(
            "format" to "json",
            "outputFileName" to "test-suites.json"
        )
    )
    testGenerationSettings {
        validSecurityValues.put("ApiKeyAuth", "test-api-key-123")
        includeOperations.put("/orders", listOf("POST"))
    }
}
```

## Inspecting Output

```bash
# Missing request body cases
jq '.. | objects | select(.name? | strings | contains("Required Request Body is missing"))' \
  build/generated/test-suites.json

# Request body schema violations
jq '.. | objects | select(.name? | strings | startswith("Incorrect Request Body"))' \
  build/generated/test-suites.json
```

## Media Type Behavior

Request body validation uses the first supported media type from `Consts.supportedMediaTypes`
(`application/json`, `application/xml`, `application/x-www-form-urlencoded`). If your request body
only defines unsupported media types, no request body tests are generated.

## Budget Controls for Complex Schemas

Deep or highly nested schemas can generate many test cases. Use budget settings to control test volume:

```yaml
testGenerationSettings:
  maxSchemaDepth: 50
  maxMergedSchemaDepth: 50
  maxSchemaCombinations: 100
  maxTestCasesPerOperation: 1000
```

See [Budget controls](../../concepts/budget-controls.md) for details.

## Filtering Request Body Tests

Test case name lists require exact matches (wildcards are not evaluated).

### Exclude specific request body tests

```yaml
testGenerationSettings:
  ignoreTestCases:
    "/orders":
      "POST":
        - "Required Request Body is missing"
        - "Incorrect Request Body: Missed Required Object Properties items"
        - "Incorrect Request Body: Object Property items Below Min Items Array"
```

### Target only request body operations

Use [Include Operations](../configuration/include-operations.md) to scope generation:

```yaml
testGenerationSettings:
  includeOperations:
    "/orders":
      - POST
```

### Disable a specific schema rule

```yaml
testGenerationSettings:
  ignoreSchemaValidationRules:
    - art.galushko.openapi.testgen.rules.schema.MissedRequiredObjectPropertiesSchemaValidationRule
```

!!! note "Schema rules are shared"
    Ignoring a schema rule affects request bodies and parameters. If you disable a rule, its tests will
    disappear everywhere it applies.

## Applicable Rules

Request bodies can trigger the same schema rules as parameters. For nested objects and arrays, the
`rule` field contains a composed rule class that wraps the underlying simple rule.

| Constraint | Simple Rule | Composed Rule (nested) |
|------------|-------------|------------------------|
| Missing required property | `MissedRequiredObjectPropertiesSchemaValidationRule` | `ObjectItemSchemaValidationRule` |
| `minItems` / `maxItems` | `BelowMinItemsArraySchemaValidationRule` / `AboveMaxItemsArraySchemaValidationRule` | `ObjectItemSchemaValidationRule` |
| `minimum` / `maximum` | `OutOfMinimumBoundaryNumberSchemaValidationRule` / `OutOfMaximumBoundaryNumberSchemaValidationRule` | `ObjectItemSchemaValidationRule` |
| Wrong primitive type | `InvalidTypeValidationRule` | `ObjectItemSchemaValidationRule` |
| Integer constraints | `IntegerBreakingSchemaValidationRule` | `ObjectItemSchemaValidationRule` |
| String length | `OutOfMinimumLengthStringSchemaValidationRule` / `OutOfMaximumLengthStringSchemaValidationRule` | `ObjectItemSchemaValidationRule` |
| `format: email` | `WrongEmailFormatSchemaValidationRule` | `ObjectItemSchemaValidationRule` |
| `format: uuid` | `WrongUuidFormatSchemaValidationRule` | `ObjectItemSchemaValidationRule` |
| Invalid enum value | `InvalidEnumValueSchemaValidationRule` | `ObjectItemSchemaValidationRule` |
| Invalid pattern | `InvalidPatternSchemaValidationRule` (pattern-support module) | `ObjectItemSchemaValidationRule` |

## Related Documentation

- [Providers Catalog](../../reference/catalogs/providers-catalog.md) - Request body providers
- [Rules Catalog](../../reference/catalogs/rules-catalog.md) - Schema validation rules
- [Test Suite Writer](../generators/test-suite-writer.md) - Output format details
- [Template Generator](../generators/template-generator.md) - RestAssured output
