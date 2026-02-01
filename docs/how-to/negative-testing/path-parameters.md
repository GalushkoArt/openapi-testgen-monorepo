---
description: Learn how OpenAPI Test Generator creates negative tests for path parameters. Covers schema violations, format validation (UUID, email), pattern-based validation, and filtering options.
---

# Path Parameter Validation Tests

This guide explains how OpenAPI Test Generator creates negative tests for path parameters
and how to inspect the generated output.

## How Path Parameters Are Tested

Path parameters are always required in OpenAPI (they're part of the URL path). The generator
creates test cases for:

- **Schema violations**: Invalid values that don't match the parameter's schema constraints
  (wrong type, out of range, invalid format, pattern mismatches)
- **Format violations**: Values that don't match specified formats (uuid, email, date, etc.)

The `ParameterSchemaValidationTestProvider` orchestrates path parameter test generation,
delegating to individual `SchemaValidationRule` implementations.

!!! note "Path Parameters Cannot Be Omitted"
    Unlike query or header parameters, path parameters cannot be missing—they are embedded
    in the URL path. The generator only tests invalid *values*, not missing parameters.

## Example OpenAPI Spec

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

This defines a path parameter with a regex pattern constraint. The generator produces test
cases with values that violate this pattern.

## Invalid Path Parameter Test Cases

Using the `samples/openapi.yaml` spec, the generator produces:

| Test Case Name                                 | Invalid Value | Expected Status | Rule                                 |
|------------------------------------------------|---------------|-----------------|--------------------------------------|
| Invalid Path userId parameter: Invalid Pattern | `"AE."`       | 400             | `InvalidPatternSchemaValidationRule` |

For UUID-formatted path parameters (e.g., `format: uuid`), the core fixtures include:

| Test Case Name                                   | Invalid Value                            | Expected Status | Rule                                  |
|--------------------------------------------------|------------------------------------------|-----------------|---------------------------------------|
| Invalid Path userId parameter: Wrong UUID Format | `"8e258b27-c787-49ef-9539-11461b251ffg"` | 400             | `WrongUuidFormatSchemaValidationRule` |

The sample spec (`samples/openapi.yaml`) does not include a UUID path parameter. Use a spec with
`format: uuid` to reproduce this case.

## Inspecting Output

The generated JSON contains the test case structure (excerpt):

```json
{
    "name": "Invalid Path userId parameter: Invalid Pattern",
    "method": "GET",
    "path": "/users/{userId}",
    "pathParams": {
        "userId": "AE."
    },
    "headers": [
        {
            "key": "X-API-Key",
            "value": "test-api-key-123"
        }
    ],
    "body": null,
    "expectedStatusCode": 400,
    "rule": "art.galushko.openapi.testgen.pattern.support.InvalidPatternSchemaValidationRule"
}
```

Key fields:

- `pathParams`: Contains the invalid value for the path parameter
- `expectedStatusCode`: 400 for parameter validation failures
- `rule`: Fully qualified class name of the rule that generated this test

Fields like `queryParams`, `cookie`, `securityValues`, `expectedBody`, and `needToComplete` are omitted here.
See [Test Suite Writer](../generators/test-suite-writer.md) for full output details.

## Format Violations

When a path parameter uses `format: uuid`, the generator emits a wrong-UUID case (excerpt from
`core/src/test/resources/oas/openapi-generated-test-suites.json`). This fixture uses `DELETE /users/{userId}`;
your operation and method may differ.

```json
{
    "name": "Invalid Path userId parameter: Wrong UUID Format",
    "method": "DELETE",
    "path": "/users/{userId}",
    "pathParams": {
        "userId": "8e258b27-c787-49ef-9539-11461b251ffg"
    },
    "expectedStatusCode": 400,
    "rule": "art.galushko.openapi.testgen.rules.schema.WrongUuidFormatSchemaValidationRule"
}
```

The invalid UUID `8e258b27-c787-49ef-9539-11461b251ffg` has a `g` character (invalid hex).

## Pattern-Based Validation

When path parameters have a `pattern` constraint, the `pattern-support` module generates
invalid values using `InvalidPatternSchemaValidationRule`.

For the pattern `^[a-z0-9_]+$` (lowercase alphanumeric with underscores):

- Valid: `user_123`, `abc`
- Invalid: `AE.` (uppercase and dot violate the pattern)

!!! note "Pattern Module"
    Pattern-based validation requires the `pattern-support` module, which is included
    in the default distribution bundle. See [pattern-support module](../../modules/pattern-support.md)
    for details and limitations.

## Filtering Path Parameter Tests

### Exclude Specific Tests

To exclude specific path parameter tests (exact name match only):

```yaml
testGenerationSettings:
    ignoreTestCases:
        "/users/{userId}":
            "GET":
                - "Invalid Path userId parameter: Invalid Pattern"
```

### Target Specific Operations

Use `includeOperations` to generate tests only for specific paths:

```yaml
testGenerationSettings:
    includeOperations:
        "/users/{userId}":
            - GET
```

See [Include Operations](../configuration/include-operations.md) for details.

### Exclude a Rule Entirely

To disable pattern validation across all operations:

```yaml
testGenerationSettings:
    ignoreSchemaValidationRules:
        - art.galushko.openapi.testgen.pattern.support.InvalidPatternSchemaValidationRule
```

See [Ignore Rules](../configuration/ignore-rules.md) for more filtering options.

## Applicable Rules

Path parameters can trigger these schema validation rules:

| Constraint              | Rule                                                                                                | Example             |
|-------------------------|-----------------------------------------------------------------------------------------------------|---------------------|
| `pattern`               | `InvalidPatternSchemaValidationRule`                                                                | Regex violation     |
| `format: uuid`          | `WrongUuidFormatSchemaValidationRule`                                                               | Invalid UUID        |
| `format: email`         | `WrongEmailFormatSchemaValidationRule`                                                              | Invalid email       |
| `minLength`/`maxLength` | `OutOfMinimumLengthStringSchemaValidationRule` / `OutOfMaximumLengthStringSchemaValidationRule`     | Too short/long      |
| `minimum`/`maximum`     | `OutOfMinimumBoundaryNumberSchemaValidationRule` / `OutOfMaximumBoundaryNumberSchemaValidationRule` | Out of range        |
| `enum`                  | `InvalidEnumValueSchemaValidationRule`                                                              | Not in enum         |
| `type: integer`         | `IntegerBreakingSchemaValidationRule`                                                               | Decimal for integer |

## Related Documentation

- [Running Test Generator](generating.md) - How to run the test generator
- [Providers Catalog](../../reference/catalogs/providers-catalog.md) - `ParameterSchemaValidationTestProvider` details
- [Rules Catalog](../../reference/catalogs/rules-catalog.md) - All schema validation rules
- [Test Suite Writer](../generators/test-suite-writer.md) - Output format details
- [Include Operations](../configuration/include-operations.md) - Target specific paths/methods
- [Ignore Rules](../configuration/ignore-rules.md) - Exclude rules or test cases
