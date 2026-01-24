# Negative Testing

Scenario-focused guides for generating negative test cases from OpenAPI specs.

Each guide includes:

- A minimal OpenAPI snippet for the scenario
- CLI and Gradle examples that run against existing samples
- Fixture-backed output excerpts (real test case names)
- Expected status code explanation

## Parameter Validation

- [Path Parameters](path-parameters.md) - Tests for invalid path parameter values (pattern violations, wrong formats)
- [Query Parameters](query-parameters.md) - Tests for missing required parameters and invalid query values
- [Header Parameters](header-parameters.md) - Tests for missing required headers and security header validation

## Request Body Validation

- [Request Body Schema](request-body-schema.md) - Tests for missing request bodies and schema violations

## Targeted Generation

Use [Include Operations](../configuration/include-operations.md) to generate tests only for specific
paths and methods.

## Related Configuration

- [Ignore Rules](../configuration/ignore-rules.md) - Exclude specific rules or test cases
- [YAML Config](../configuration/yaml-config.md) - Full configuration reference
