---
description: Reference for the model module's core data types - TestCase, TestSuite, SecurityValues, KeyValuePair, and error handling types (Outcome, GenerationError, GenerationReport, BudgetExceededException).
---

# Model Reference

The `model` module contains the canonical data classes shared across the system. It is data-only and has no dependency on `core`. It is consumed by `core`,
`example-value`, generators, and reporting layers.

See also: [Test-suite-writer generator](../how-to/generators.md#test-suite-writer-generator) and the [core module](../modules/core.md).

## TestCase

`TestCase` represents a single generated test scenario.

### Key fields

- Request identity: `path`, `method`, `name`
- Inputs: `queryParams`, `pathParams`, `headers`, `cookie`, `body`, `requestBodyMediaType`
- Security: `securityValues` (kept separate from request fields)
- Expectations: `expectedStatusCode`, `expectedBody`, `responseBodyMediaType`, `needToComplete`

`expectedBody` is populated when a response example is available in the OpenAPI spec. It can be a map, list, string, number, or boolean depending on the example shape and media type. Explicit examples are preferred; if none exist, schema-derived fallbacks may be used for prioritized structured media types such as JSON-like, JWT-like, XML-like, YAML, and form-urlencoded content.
`requestBodyMediaType` is populated with the selected request body content type when a body example is generated.
`responseBodyMediaType` is populated with the content type used to extract `expectedBody`.

!!! tip "externalValue workaround"
    Examples using `externalValue` are not resolved because they require fetching external resources. If your spec uses `externalValue`, add an inline `example` or `examples` entry as a fallback:
    ```yaml
    responses:
      '200':
        content:
          application/json:
            examples:
              external:
                externalValue: 'https://example.com/response.json'
              inline:  # Fallback for test generation
                value:
                  id: 1
                  name: "example"
    ```

- Provenance: `rule` (fully qualified rule or provider class name)

`expectedStatusCode = 0` means "unspecified".

### Rule attribution

The `rule` field contains the **fully qualified class name (FQCN)** of the rule or provider that generated the test case. This enables:

#### Filtering test cases by rule

Use `ignoreSchemaValidationRules` or `ignoreAuthValidationRules` settings to skip test cases from specific rules:

```yaml
testGenerationSettings:
  ignoreSchemaValidationRules:
    - art.galushko.openapi.testgen.rules.schema.OutOfMinimumLengthStringSchemaValidationRule
    - com.example.rules.SuffixValidationRule
```

These ignore settings apply only to schema/auth rules. Provider-generated test cases can be filtered by name with `ignoreTestCases`.

#### Analyzing test case distribution

With the `test-suite-writer` generator, inspect which rules produced which test cases:

```bash
# Find test cases by rule
jq '.[] | .testCases[] | select(.rule == "art.galushko.openapi.testgen.rules.schema.InvalidEnumValueSchemaValidationRule")' \
  generated/test-suites.json
```

Provider examples use their FQCNs too:

```bash
jq '.[] | .testCases[] | select(.rule == "art.galushko.openapi.testgen.providers.parameter.MissedRequiredParameterTestProvider")' \
  generated/test-suites.json

# Count test cases per rule
jq '[.[] | .testCases[].rule] | group_by(.) | map({rule: .[0], count: length})' \
  generated/test-suites.json
```

#### Debugging custom rules

When implementing custom rules, the `rule` field helps verify your rule is being applied:

```kotlin
assertThat(testCase.rule)
    .isEqualTo("com.example.rules.SuffixValidationRule")
```

### Structure

```kotlin
data class TestCase(
    val name: String,
    val method: String,
    val path: String,
    val queryParams: Map<String, Any> = emptyMap(),
    val pathParams: Map<String, Any> = emptyMap(),
    val headers: List<KeyValuePair<String, Any>> = emptyList(),
    val cookie: List<KeyValuePair<String, Any>> = emptyList(),
    val securityValues: SecurityValues = SecurityValues(),
    val body: Any? = null,
    val requestBodyMediaType: String? = null,
    val expectedBody: Any? = null,
    val responseBodyMediaType: String? = null,
    val needToComplete: Boolean = false,
    val expectedStatusCode: Int = 0,
    val rule: String? = null,
)
```

## TestSuite

`TestSuite` groups cases for one operation. `operationName` is the stable identifier used by `test-suite-writer` for keys and filenames. When `operationName`
is null or blank, writers may skip the suite.

## SecurityValues

`SecurityValues` tracks security-related request data separately from regular parameters. This separation allows auth rules to reason about security material
independently.

```kotlin
data class SecurityValues(
    val queryParams: Map<String, Any> = emptyMap(),
    val headers: List<KeyValuePair<String, Any>> = emptyList(),
    val cookie: List<KeyValuePair<String, Any>> = emptyList(),
    val other: Map<String, Any> = emptyMap(),
)
```

### The `other` field

The `other` field contains additional security-related metadata for extensions. Known keys:

#### `authorizationScopes`

Present when OAuth2 or OpenID Connect schemes are used. Each entry contains:

- `name`: Security scheme name (e.g., `"oauth2"`, `"oidc"`)
- `type`: Scheme type (`"oauth2"` or `"openidconnect"`)
- `scopes`: List of scope strings (may be empty)

Example JSON output:

```json
{
  "securityValues": {
    "headers": [{"key": "authorization", "value": "<oauth2:[read,write]>"}],
    "other": {
      "authorizationScopes": [
        {"name": "oauth2", "type": "oauth2", "scopes": ["read", "write"]}
      ]
    }
  }
}
```

!!! note "When is `authorizationScopes` present?"
    - **Present**: When ANY OAuth2 or OpenID Connect scheme exists (including empty scopes)
    - **Absent**: When only API key or HTTP schemes are used
    - Entries are sorted by `name` for deterministic output

This metadata enables test generators to programmatically access scope information without parsing the Authorization header placeholder string.

## KeyValuePair

`KeyValuePair` preserves ordering for headers and cookies, where duplicates may exist.

## Serialization expectations

- Data classes are designed to preserve JSON shape for generators.
- Header/cookie ordering is preserved via `List<KeyValuePair<...>>`.
- `TestCase` preserves selected body media types via `requestBodyMediaType` and `responseBodyMediaType`.
- `rule` and `operationName` are used as stable identifiers for output and traceability.

## How core uses these models

- `ValidCaseBuilder` creates the baseline `TestCase`.
- Providers derive invalid `TestCase` instances by changing a single field or security value.
- `TestSuiteWriter` uses `TestSuite.operationName` as the suite key or filename.
- Reporting aggregates `TestSuite` values into a `GenerationReport`.

## Error Types

### Outcome

`Outcome<T>` is the result type used during generation:

- `Outcome.Success`: full result with no errors.
- `Outcome.PartialSuccess`: result plus errors (best-effort generation).
- `Outcome.Failure`: errors only, no usable result.

Providers should return `Outcome` rather than throwing, using `runProviderSafely`.

### GenerationError

`GenerationError` captures a provider failure:

- `providerClass`: fully qualified provider class name.
- `message`: human-readable error description.
- `context`: where the error occurred.
- `exceptionText`: optional stack trace for unexpected failures.

### ErrorContext

`ErrorContext` provides hierarchical context for error reporting:

- `Operation`: path, method, and optional operation id.
- `Parameter`: operation + parameter name/location + optional `$ref`.
- `RequestBody`: operation + optional `$ref`.

### GenerationReport

`GenerationReport` aggregates results across operations:

- `successfulSuites`: generated test suites.
- `errors`: all collected `GenerationError` instances.
- `summary`: `GenerationSummary` with per-outcome operation lists.
- `hasErrors`: convenience flag.

`OperationInfo` is the per-operation metadata stored in the summary lists.

### ErrorHandlingConfig and ErrorMode

`ErrorHandlingConfig` controls report building:

- `ErrorMode.FAIL_FAST`: stop on the first error.
- `ErrorMode.COLLECT_ALL`: collect errors up to `maxErrors`.

### BudgetExceededException

`BudgetExceededException` is thrown when a hard limit is exceeded:

- `BudgetType.SCHEMA_COMBINATIONS` -> `testGenerationSettings.maxSchemaCombinations`.
- `BudgetType.TEST_CASES_PER_OPERATION` -> `testGenerationSettings.maxTestCasesPerOperation`.

Some budget violations are converted to `Outcome.Failure` at provider boundaries, while others (such as per-operation test case limits) may propagate unless
callers handle them explicitly.

See the [core module](../modules/core.md) for where these errors are handled.

## Related docs

- Concepts: [Error handling](../concepts/error-handling.md)
- Concepts: [Architecture](../concepts/architecture.md)
- SPI: [Validation rules](spi.md#validation-rules)
- How-to: [Custom validation rules](../how-to/extending.md#custom-validation-rules)
- Modules: [model](../modules/index.md#model), [core](../modules/core.md)
