# Providers

## Overview
Providers generate negative test cases for a single OpenAPI operation. Each provider receives a
`TestGenerationContext` that includes the parsed spec, the current operation, and a baseline valid
`TestCase`. Provider execution is orchestrated by `ProviderOrchestrator` and merged via
`OutcomeAggregator` (see [architecture](../../concepts/architecture.md)).

For core entry points and extension context, see the [core module](../../modules/core.md).
For provider interfaces and extension points, see the [Test providers SPI](../spi/test-providers.md).
For user-facing configuration (ignore rules, budgets), see [YAML config](../../how-to/configuration/yaml-config.md).

Providers operate at two levels:
- Operation-level providers decide which OpenAPI element to target (parameters, request body, auth).
- Element-level providers (parameter/request body) apply rules to build invalid values.

All providers are executed inside `runProviderSafely`, which converts exceptions (including
`BudgetExceededException`) into `Outcome.Failure` results with error context.

## Default execution order
The default order is defined in `TestGeneratorConfigurer.createTestSuiteGenerator`:

1. `AuthTestCaseProviderForOperation`
2. `ParameterTestCaseProviderForOperation`
3. `RequestBodyTestCaseProviderForOperation`

The order matters for deterministic output ordering in generated test suites.

## Provider catalog

### Operation-level providers
- `AuthTestCaseProviderForOperation`
  - Applies ordered `AuthValidationRule` instances to generate auth-negative cases.
  - Skips when neither the operation nor the root OpenAPI document defines security.
  - Expected status codes: 401/403 from built-in auth rules.

- `ParameterTestCaseProviderForOperation`
  - Iterates over operation parameters and invokes parameter providers for each.
  - Merges outcomes across parameters and providers.
  - Built-in providers generate `expectedStatusCode = 400` cases.

- `RequestBodyTestCaseProviderForOperation`
  - Applies request-body providers to the operation request body when present.
  - Skips when request body is missing or content is empty after dereference.
  - Built-in providers generate `expectedStatusCode = 400` cases.

### Parameter providers
- `MissedRequiredParameterTestProvider`
  - Generates a case that omits a required query/header/cookie parameter.
  - Path parameters are excluded (always required by design).
  - **Applies to Headers:** Required non-security header parameters (e.g., `X-Request-ID`, `X-Correlation-ID`)
  - **Does NOT apply to:** Security scheme headers (handled by `AuthTestCaseProviderForOperation`)
  - **Test Case Pattern:** `Missed Required {Location} Parameter {name}`
  - **Expected Status Code:** 400
  - **Example Output:**
    ```json
    {
      "name": "Missed Required Header Parameter X-Request-ID",
      "expectedStatusCode": 400,
      "rule": "art.galushko.openapi.testgen.providers.parameter.MissedRequiredParameterTestProvider"
    }
    ```
  - **See also:** [Query Parameter Validation Tests](../../how-to/negative-testing/query-parameters.md), [Header Parameter Validation Tests](../../how-to/negative-testing/header-parameters.md)

- `ParameterSchemaValidationTestProvider`
  - Applies schema validation rules to parameter schemas (including composed schemas).
  - Supports query/path/header/cookie parameters.
  - **See also:** [Path Parameter Validation Tests](../../how-to/negative-testing/path-parameters.md), [Query Parameter Validation Tests](../../how-to/negative-testing/query-parameters.md), [Header Parameter Validation Tests](../../how-to/negative-testing/header-parameters.md)

### Request body providers
- `MissedRequiredRequestBodyTestProvider`
  - Generates a case with a missing required request body.
  - **Test Case Name:** `Required Request Body is missing`
  - **Expected Status Code:** 400
  - **Applies When:** `requestBody.required: true`
  - **See also:** [Request Body Schema Tests](../../how-to/negative-testing/request-body-schema.md)

- `RequestBodySchemaValidationTestProvider`
  - Applies schema validation rules to the request body schema.
  - Uses the first supported media type from `Consts.supportedMediaTypes`
    (`application/json`, `application/xml`, `application/x-www-form-urlencoded`).
  - **Test Case Pattern:** `Incorrect Request Body: {rule description}`
  - **Expected Status Code:** 400
  - **Applies When:** Request body has a supported media type and schema constraints
  - **See also:** [Request Body Schema Tests](../../how-to/negative-testing/request-body-schema.md)

## Negative-case semantics and status codes
- Parameter and request body providers generate invalid inputs with `expectedStatusCode = 400`.
- Auth rules generate `expectedStatusCode = 401` or `403` depending on the rule:
  - 401 Unauthorized: missing/invalid security values.
  - 403 Forbidden: insufficient or incorrect OAuth2/OpenID scopes.
For scenario-specific guides, see [negative testing](../../how-to/negative-testing/index.md).

## Ignore settings
- `TestGenerationSettings.ignoreSchemaValidationRules` and `ignoreAuthValidationRules` filter the
  rule lists before providers run.
- `TestGenerationSettings.includeOperations` filters paths/methods before generation via
  `IncludeOperationsHandler` in `TestGenerationProcessor`.
- `TestGenerationSettings.ignoreTestCases` filters test cases after suite assembly via
  `IgnoreConfigHandler` (path/method/test-case-level filters).

## Budget and error handling
- `maxSchemaCombinations` limits composed-schema expansion via `CombinationBudget`.
- `maxSchemaDepth` limits recursion for composed rules (array/object traversal).
- When provider-level budgets are exceeded, providers return `Outcome.Failure` with a
  `GenerationError` rather than throwing.
- `maxTestCasesPerOperation` is enforced by `TestCaseBudgetValidator` in the generator layer and
  throws `BudgetExceededException`.

## Extension points
Providers are wired explicitly in `TestGeneratorConfigurer`. To add, remove, or reorder providers,
implement a custom `TestSuiteGenerator` or supply an alternative wiring layer in a custom engine.
Rules remain the primary extension point via `TestGenerationModule.extraSimpleSchemaRules` and
`extraAuthRules`.
