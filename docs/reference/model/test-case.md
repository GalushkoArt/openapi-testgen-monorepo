---
description: Reference documentation for the TestCase data class, which represents a single generated test scenario including request inputs, security values, and expected outcomes.
---

# TestCase

`TestCase` represents a single generated test scenario.

## Key fields

- Request identity: `path`, `method`, `name`
- Inputs: `queryParams`, `pathParams`, `headers`, `cookie`, `body`
- Security: `securityValues` (kept separate from request fields)
- Expectations: `expectedStatusCode`, `expectedBody`, `needToComplete`

`expectedBody` is populated when a response example is available in the OpenAPI spec. It can be a map, list, string, number, or boolean depending on the example shape and media type. Schema-derived fallbacks are applied only for JSON-like media types; other media types require explicit examples.

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

## Rule attribution

The `rule` field contains the **fully qualified class name (FQCN)** of the rule or provider that generated the test case. This enables:

### Filtering test cases by rule

Use `ignoreSchemaValidationRules` or `ignoreAuthValidationRules` settings to skip test cases from specific rules:

```yaml
testGenerationSettings:
  ignoreSchemaValidationRules:
    - art.galushko.openapi.testgen.rules.schema.OutOfMinimumLengthStringSchemaValidationRule
    - com.example.rules.SuffixValidationRule
```

These ignore settings apply only to schema/auth rules. Provider-generated test cases can be filtered by name with `ignoreTestCases`.

### Analyzing test case distribution

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

### Debugging custom rules

When implementing custom rules, the `rule` field helps verify your rule is being applied:

```kotlin
assertThat(testCase.rule)
    .isEqualTo("com.example.rules.SuffixValidationRule")
```

## Structure

```kotlin
data class TestCase(
    val name: String,
    val method: String,
    val path: String,
    val expectedStatusCode: Int = 0,
    val expectedBody: Any? = null,
    val needToComplete: Boolean = false,
    val queryParams: Map<String, Any> = emptyMap(),
    val pathParams: Map<String, Any> = emptyMap(),
    val headers: List<KeyValuePair<String, Any>> = emptyList(),
    val cookie: List<KeyValuePair<String, Any>> = emptyList(),
    val body: Any? = null,
    val rule: String? = null,
    val securityValues: SecurityValues = SecurityValues(),
)
```

## SecurityValues

`SecurityValues` tracks security-related request data separately from regular parameters. This separation allows auth rules to reason about security material independently.

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

## Related documentation

- [TestSuite](test-suite.md)
- [Errors](errors.md)
- [Validation rules SPI](../spi/validation-rules.md)
- [Custom validation rules](../../how-to/extension/custom-rules.md)
