---
description: Generate negative test cases that verify your API rejects invalid input. Covers path, query, header, and request body validation with OpenAPI examples, generated output, and filtering options.
---

# Negative testing

Generate negative test cases that verify your API rejects invalid input according to your OpenAPI spec.
This page is the catalog of generated negative cases by input location (path, query, header, request body, auth).
For the complementary baseline valid (2xx) case, see [Positive testing](positive-testing.md).

## What gets tested

| Category | What it validates | Expected status | Example test case |
|----------|-------------------|-----------------|-------------------|
| Path parameters | Pattern, format, type violations | 400 | `Invalid Path userId parameter: Invalid Pattern` |
| Query parameters | Missing required, out-of-range, wrong type | 400 | `Invalid Query page parameter: Out Of Minimum Boundary Number` |
| Header parameters | Missing required non-security headers | 400 | `Missed Required Header Parameter X-Request-ID` |
| Request body | Missing body, missing properties, constraint violations | 400 | `Incorrect Request Body: Missed Required Object Properties items` |
| Authentication | Missing credentials, invalid tokens | 401/403 | `No security values provided` |

## Running and verifying

### Choose an output type

- **Executable tests**: use the `template` generator (Gradle plugin recommended). Generated sources are compiled and run by `./gradlew test`.
- **Data-driven suites**: use the `test-suite-writer` generator (CLI or Gradle). Output is `.json` / `.yaml` consumed by your own runner.

### CLI: generate JSON suites

Prerequisite: have `openapi-testgen` available (install it or run via `npx`). See [Installation](../getting-started/installation.md).

For the full CLI command and output walkthrough, see [Getting started](../getting-started/index.md).
To target a specific operation, see [Include operations](configuration.md#include-operations).

#### Provide security values (avoid placeholders)

`validSecurityValues` keys are **security scheme names** (from `components.securitySchemes`), not header names:

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json \
  --setting 'validSecurityValues.ApiKeyAuth=test-api-key-123'
```

#### Verify output

After generating, verify the output using `jq`. See [Getting started](../getting-started/index.md) for a full output example.

```bash
jq 'keys' generated/test-suites.json
jq '[.[] | .testCases[]] | length' generated/test-suites.json
```

### Gradle plugin

For Gradle plugin setup and configuration, see [Gradle integration](../getting-started/gradle-integration.md).

To use environment variables for security values and base URLs in CI, see [CI/CD integration](ci-cd.md#environment-specific-configuration).

---

## Path parameters

Path parameters are always required in OpenAPI (they're part of the URL path). The generator creates test cases for:

- **Schema violations**: Invalid values that don't match the parameter's schema constraints (wrong type, out of range, invalid format, pattern mismatches)
- **Format violations**: Values that don't match specified formats (uuid, email, date, etc.)

The `ParameterSchemaValidationTestProvider` orchestrates path parameter test generation, delegating to individual `SchemaValidationRule` implementations.

!!! note "Path Parameters Cannot Be Omitted"
    Unlike query or header parameters, path parameters cannot be missing---they are embedded
    in the URL path. The generator only tests invalid *values*, not missing parameters.

### Example OpenAPI spec

```yaml
paths:
    /users/{userId}:
        get:
            operationId: getUser
            parameters:
                -   name: userId
                    in: path
                    required: true
                    schema:
                        type: string
                        pattern: '^[a-z0-9_]+$'
```

### Invalid path parameter test cases

Using the `samples/openapi.yaml` spec, the generator produces:

| Test Case Name                                 | Invalid Value | Expected Status | Rule                                 |
|------------------------------------------------|---------------|-----------------|--------------------------------------|
| Invalid Path userId parameter: Invalid Pattern | `"AE."`       | 400             | `InvalidPatternSchemaValidationRule` |

For UUID-formatted path parameters (e.g., `format: uuid`), the core fixtures include:

| Test Case Name                                   | Invalid Value                            | Expected Status | Rule                                  |
|--------------------------------------------------|------------------------------------------|-----------------|---------------------------------------|
| Invalid Path userId parameter: Wrong UUID Format | `"8e258b27-c787-49ef-9539-11461b251ffg"` | 400             | `WrongUuidFormatSchemaValidationRule` |

### Inspecting output

```json
{
    "name": "Invalid Path userId parameter: Invalid Pattern",
    "method": "GET",
    "path": "/users/{userId}",
    "pathParams": { "userId": "AE." },
    "headers": [{ "key": "X-API-Key", "value": "test-api-key-123" }],
    "body": null,
    "expectedStatusCode": 400,
    "rule": "art.galushko.openapi.testgen.pattern.support.InvalidPatternSchemaValidationRule"
}
```

Key fields: `pathParams` (the invalid value), `expectedStatusCode` (400), `rule` (FQCN of the rule).

### Format violations

When a path parameter uses `format: uuid`, the generator emits a wrong-UUID case:

```json
{
    "name": "Invalid Path userId parameter: Wrong UUID Format",
    "method": "DELETE",
    "path": "/users/{userId}",
    "pathParams": { "userId": "8e258b27-c787-49ef-9539-11461b251ffg" },
    "expectedStatusCode": 400,
    "rule": "art.galushko.openapi.testgen.rules.schema.WrongUuidFormatSchemaValidationRule"
}
```

The invalid UUID `8e258b27-c787-49ef-9539-11461b251ffg` has a `g` character (invalid hex).

### Pattern-based validation

When path parameters have a `pattern` constraint, the `pattern-support` module generates invalid values using `InvalidPatternSchemaValidationRule`.

For the pattern `^[a-z0-9_]+$` (lowercase alphanumeric with underscores):

- Valid: `user_123`, `abc`
- Invalid: `AE.` (uppercase and dot violate the pattern)

!!! note "Pattern Module"
    Pattern-based validation requires the `pattern-support` module, which is included
    in the default distribution bundle. See [pattern-support in the module catalog](../modules/index.md#pattern-support).

### Applicable rules (path parameters)

| Constraint              | Rule                                                                                                | Example             |
|-------------------------|-----------------------------------------------------------------------------------------------------|---------------------|
| `pattern`               | `InvalidPatternSchemaValidationRule`                                                                | Regex violation     |
| `format: uuid`          | `WrongUuidFormatSchemaValidationRule`                                                               | Invalid UUID        |
| `format: email`         | `WrongEmailFormatSchemaValidationRule`                                                              | Invalid email       |
| `minLength`/`maxLength` | `OutOfMinimumLengthStringSchemaValidationRule` / `OutOfMaximumLengthStringSchemaValidationRule`     | Too short/long      |
| `minimum`/`maximum`     | `OutOfMinimumBoundaryNumberSchemaValidationRule` / `OutOfMaximumBoundaryNumberSchemaValidationRule` | Out of range        |
| `enum`                  | `InvalidEnumValueSchemaValidationRule`                                                              | Not in enum         |
| `type: integer`         | `IntegerBreakingSchemaValidationRule`                                                               | Decimal for integer |

---

## Query parameters

The generator produces two categories of query parameter tests:

| Category         | Provider/Rule                           | Expected Status | When Generated                |
|------------------|-----------------------------------------|-----------------|-------------------------------|
| Missing required | `MissedRequiredParameterTestProvider`   | 400             | When `required: true`         |
| Invalid values   | `ParameterSchemaValidationTestProvider` | 400             | When constraints are defined in `schema` or `content` |

!!! note "Provider vs rule in output"
    The missing-required case is created by `MissedRequiredParameterTestProvider`. For invalid values,
    the provider applies schema rules, so the generated `rule` field contains a rule class name (for
    example, `MissedRequiredObjectPropertiesSchemaValidationRule`), not the provider name.

!!! note "Parameter schema source"
    Parameter constraints can come from `parameter.schema` or `parameter.content[*].schema`.
    If both are present, the generator logs a warning and uses only `parameter.schema`.

### Missing required query parameter

When a query parameter has `required: true`, the generator creates a test case that omits the parameter entirely, expecting the API to return a 400 Bad Request.

```yaml
paths:
    /persons:
        get:
            operationId: listPersons
            parameters:
                -   name: person
                    in: query
                    required: true
                    schema:
                        $ref: '#/components/schemas/Person'
```

Generated test case:

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

### Complex object query parameters

When a query parameter references an object schema with required properties, the generator produces tests for each missing required property. For the `Person` schema:

```yaml
components:
    schemas:
        Person:
            type: object
            required: [ name, age ]
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

### Invalid query parameter values

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
        "person": { "age": "abc", "name": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" }
    },
    "expectedStatusCode": 400,
    "rule": "art.galushko.openapi.testgen.rules.composed.ObjectItemSchemaValidationRule"
}
```

---

## Header parameters

The generator produces different test cases depending on the header type:

| Header Type                               | Provider/Rule                           | Expected Status | Example                            |
|-------------------------------------------|-----------------------------------------|-----------------|------------------------------------|
| Required non-security                     | `MissedRequiredParameterTestProvider`   | 400             | `X-Request-ID`, `X-Correlation-ID` |
| Required security                         | `AuthTestCaseProviderForOperation`      | 401/403         | `Authorization`, `X-API-Key`       |
| Invalid value (non-security header param) | `ParameterSchemaValidationTestProvider` | 400             | Wrong format, out of range         |

Security header validation (missing or invalid values) is generated by auth rules, not schema rules.

### Missing required header (non-security)

```yaml
paths:
    /users:
        get:
            operationId: listUsers
            parameters:
                -   name: X-Request-ID
                    in: header
                    required: true
                    schema:
                        type: string
                        format: uuid
```

**Missing required header:**

```json
{
    "name": "Missed Required Header Parameter X-Request-ID",
    "method": "GET",
    "path": "/users",
    "headers": [{ "key": "authorization", "value": "<valid_BearerAuth_placeholder>" }],
    "expectedStatusCode": 400,
    "rule": "art.galushko.openapi.testgen.providers.parameter.MissedRequiredParameterTestProvider"
}
```

**Invalid UUID format:**

```json
{
    "name": "Invalid Header X-Request-ID parameter: Wrong UUID Format",
    "method": "GET",
    "path": "/users",
    "headers": [
        { "key": "authorization", "value": "<valid_BearerAuth_placeholder>" },
        { "key": "X-Request-ID", "value": "8e258b27-c787-49ef-9539-11461b251ffg" }
    ],
    "expectedStatusCode": 400,
    "rule": "art.galushko.openapi.testgen.rules.schema.WrongUuidFormatSchemaValidationRule"
}
```

### Security headers (auth)

Security headers defined in `securitySchemes` are handled by `AuthTestCaseProviderForOperation` and produce different expected status codes.

```yaml
components:
    securitySchemes:
        BearerAuth:
            type: http
            scheme: bearer
            bearerFormat: JWT
        ApiKeyAuth:
            type: apiKey
            in: header
            name: X-API-Key
security:
    -   BearerAuth: [ ]
    -   ApiKeyAuth: [ ]
```

**No security values provided:**

```json
{
    "name": "No security values provided",
    "method": "GET",
    "path": "/users",
    "headers": [{ "key": "X-Request-ID", "value": "d5a5495b-cbdc-4237-a66e-000000000000" }],
    "expectedStatusCode": 401,
    "rule": "art.galushko.openapi.testgen.rules.auth.AllSecurityMissedAuthValidationRule"
}
```

**Missing one required scheme (AND security):**

```json
{
    "name": "Missing Authorization header security",
    "method": "GET",
    "path": "/secure-and",
    "headers": [{ "key": "X-API-Key", "value": "<valid_apiKeyHeader_api_key_placeholder>" }],
    "expectedStatusCode": 401,
    "rule": "art.galushko.openapi.testgen.rules.auth.MissingSecurityValuesAuthValidationRule"
}
```

**Invalid Authorization header:**

```json
{
    "name": "Invalid Authorization header security",
    "method": "GET",
    "path": "/users",
    "headers": [
        { "key": "X-Request-ID", "value": "d5a5495b-cbdc-4237-a66e-000000000000" },
        { "key": "authorization", "value": "bearer some_really_invalid_authorization_header" }
    ],
    "expectedStatusCode": 401,
    "rule": "art.galushko.openapi.testgen.rules.auth.InvalidSecurityValuesAuthValidationRule"
}
```

!!! note "Security Placeholders"
    When auth headers are required but you haven't configured valid values, the generator
    uses placeholders like `<valid_BearerAuth_placeholder>`. Configure valid values using
    [Security values](configuration.md#security-values) to get realistic test data.

### Distinguishing header types in output

| Rule Contains                             | Header Type                | Expected Status |
|-------------------------------------------|----------------------------|-----------------|
| `MissedRequiredParameterTestProvider`     | Non-security required      | 400             |
| `SchemaValidationRule`                    | Any (schema violation)     | 400             |
| `AllSecurityMissedAuthValidationRule`     | Security (all missing)     | 401             |
| `InvalidSecurityValuesAuthValidationRule` | Security (invalid)         | 401             |
| `MissingSecurityValuesAuthValidationRule` | Security (partial missing) | 401             |

---

## Request body schema

The generator produces two categories of request body tests:

| Category              | Provider                                  | Expected Status | When Generated                             |
|-----------------------|-------------------------------------------|-----------------|--------------------------------------------|
| Missing required body | `MissedRequiredRequestBodyTestProvider`   | 400             | When `requestBody.required: true`          |
| Schema violations     | `RequestBodySchemaValidationTestProvider` | 400             | When request body schemas have constraints |

!!! note "Provider vs rule in output"
    The missing-body case is created by `MissedRequiredRequestBodyTestProvider`, so the `rule` field
    contains that provider class. For schema violations, the `rule` field contains a rule class name
    (for example, `MissedRequiredObjectPropertiesSchemaValidationRule`), not the provider name.

### Example OpenAPI spec

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

### Missing required request body

When `requestBody.required: true`, the generator emits a single test case that omits the body entirely:

```json
{
    "name": "Required Request Body is missing",
    "method": "POST",
    "path": "/orders",
    "headers": [{ "key": "X-API-Key", "value": "test-api-key-123" }],
    "body": null,
    "requestBodyMediaType": "application/json",
    "expectedBody": { "code": "bad_request", "message": "Invalid input" },
    "responseBodyMediaType": "application/json",
    "expectedStatusCode": 400,
    "rule": "art.galushko.openapi.testgen.providers.body.MissedRequiredRequestBodyTestProvider"
}
```

### Schema constraint violations

`RequestBodySchemaValidationTestProvider` applies schema rules to the resolved body schema. Fixture-backed examples for `createOrder`:

- `Incorrect Request Body: Missed Required Object Properties items` --- Body: `{"userId":"a"}` --- Rule: `MissedRequiredObjectPropertiesSchemaValidationRule`
- `Incorrect Request Body: Missed Required Object Properties userId` --- Body: `{"items":[{"price":0,"quantity":1,"sku":"a"}]}` --- Rule: `MissedRequiredObjectPropertiesSchemaValidationRule`
- `Incorrect Request Body: Object Property items Below Min Items Array` --- Body: `{"items":[],"userId":"a"}` --- Rule: `ObjectItemSchemaValidationRule`
- `Incorrect Request Body: Object Property items Array Item Object Property price Out Of Minimum Boundary Number` --- Body: `{"items":[{"price":-1,"quantity":1,"sku":"a"}],"userId":"a"}` --- Rule: `ObjectItemSchemaValidationRule`

Excerpt showing a nested array item violation:

```json
{
    "name": "Incorrect Request Body: Object Property items Array Item Object Property price Out Of Minimum Boundary Number",
    "method": "POST",
    "path": "/orders",
    "headers": [{ "key": "X-API-Key", "value": "test-api-key-123" }],
    "body": { "items": [{ "price": -1, "quantity": 1, "sku": "a" }], "userId": "a" },
    "requestBodyMediaType": "application/json",
    "expectedBody": { "code": "bad_request", "message": "Invalid input" },
    "responseBodyMediaType": "application/json",
    "expectedStatusCode": 400,
    "rule": "art.galushko.openapi.testgen.rules.composed.ObjectItemSchemaValidationRule"
}
```

### Media type behavior

Request body schema validation processes all supported media types in `requestBody.content` that have schemas. Supported media types include `application/json`, `text/json`, `application/jwt`, `application/xml`, `text/xml`, `application/x-www-form-urlencoded`, and `+json` / `+jwt` / `+xml` suffixes. Unsupported media types are skipped and logged as warnings.

### Budget controls for complex schemas

Deep or highly nested schemas can generate many test cases:

```yaml
testGenerationSettings:
    maxSchemaDepth: 50
    maxMergedSchemaDepth: 50
    maxSchemaCombinations: 100
    maxTestCasesPerOperation: 1000
```

See [Budget controls](../concepts/architecture.md#budget-controls) for details.

### Applicable rules (request bodies)

Request bodies can trigger the same schema rules as parameters. For nested objects and arrays, the `rule` field contains a composed rule class that wraps the underlying simple rule.

| Constraint                | Simple Rule                                                                                         | Composed Rule (nested)           |
|---------------------------|-----------------------------------------------------------------------------------------------------|----------------------------------|
| Missing required property | `MissedRequiredObjectPropertiesSchemaValidationRule`                                                | `ObjectItemSchemaValidationRule` |
| `minItems` / `maxItems`   | `BelowMinItemsArraySchemaValidationRule` / `AboveMaxItemsArraySchemaValidationRule`                 | `ObjectItemSchemaValidationRule` |
| `minimum` / `maximum`     | `OutOfMinimumBoundaryNumberSchemaValidationRule` / `OutOfMaximumBoundaryNumberSchemaValidationRule` | `ObjectItemSchemaValidationRule` |
| Wrong primitive type      | `InvalidTypeValidationRule`                                                                         | `ObjectItemSchemaValidationRule` |
| Integer constraints       | `IntegerBreakingSchemaValidationRule`                                                               | `ObjectItemSchemaValidationRule` |
| String length             | `OutOfMinimumLengthStringSchemaValidationRule` / `OutOfMaximumLengthStringSchemaValidationRule`     | `ObjectItemSchemaValidationRule` |
| `format: email`           | `WrongEmailFormatSchemaValidationRule`                                                              | `ObjectItemSchemaValidationRule` |
| `format: uuid`            | `WrongUuidFormatSchemaValidationRule`                                                               | `ObjectItemSchemaValidationRule` |
| Invalid enum value        | `InvalidEnumValueSchemaValidationRule`                                                              | `ObjectItemSchemaValidationRule` |
| Invalid pattern           | `InvalidPatternSchemaValidationRule` (pattern-support module)                                       | `ObjectItemSchemaValidationRule` |

!!! note "Ignoring composed rules"
    For nested properties, the `rule` field contains the composed rule class (`ObjectItemSchemaValidationRule` or `ArrayItemSchemaValidationRule`), not the underlying simple rule. When using `ignoreSchemaValidationRules`, use the fully qualified composed rule class name.

---

## Filtering and targeting tests

All filtering options apply across parameter and request body test categories.

### Target specific operations

Use [`includeOperations`](configuration.md#include-operations) to generate tests for specific paths before generation:

```yaml
testGenerationSettings:
    includeOperations:
        "/users/{userId}": [ GET ]
        "/orders": [ POST ]
```

### Exclude specific test cases

Use `ignoreTestCases` to exclude tests by exact name (no wildcards):

```yaml
testGenerationSettings:
    ignoreTestCases:
        "/users/{userId}":
            "GET":
                - "Invalid Path userId parameter: Invalid Pattern"
        "/orders":
            "POST":
                - "Required Request Body is missing"
```

### Disable schema validation rules

```yaml
testGenerationSettings:
    ignoreSchemaValidationRules:
        - art.galushko.openapi.testgen.pattern.support.InvalidPatternSchemaValidationRule
```

!!! note "Schema rules are shared"
    Ignoring a schema rule affects request bodies and parameters everywhere it applies.

### Disable auth validation rules

```yaml
testGenerationSettings:
    ignoreAuthValidationRules:
        - "art.galushko.openapi.testgen.rules.auth.AllSecurityMissedAuthValidationRule"
```

See [Ignore rules](configuration.md#ignore-rules) for the full ignore configuration reference.

## Inspecting generated output

```bash
# Path parameter tests
jq '.. | objects | select(.name? | strings | startswith("Invalid Path"))' generated/test-suites.json

# Missing required query/header parameter tests
jq '.. | objects | select(.name? | strings | contains("Missed Required"))' generated/test-suites.json

# Request body schema violations
jq '.. | objects | select(.name? | strings | startswith("Incorrect Request Body"))' generated/test-suites.json
```

## Related docs

- [Configuration](configuration.md) - YAML config, ignore rules, security values
- [Providers catalog](../reference/catalogs/providers-catalog.md) - Provider details
- [Rules catalog](../reference/catalogs/rules-catalog.md) - All schema and auth validation rules
- [Test-suite-writer](generators.md#test-suite-writer-generator) - Output format details
- [CLI reference](../reference/cli.md)
- [Gradle plugin reference](../reference/gradle-plugin.md)
