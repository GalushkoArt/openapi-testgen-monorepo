---
description: Generate a baseline positive (2xx) test case per operation with includeValidCase. Covers enabling it via YAML, CLI, and Gradle, how the valid case is built, security and example-value prerequisites, generated output, and limitations.
---

# Positive testing

## Goal

Generate one baseline positive test case per operation — a request that satisfies every required
parameter, the request body schema, and the security requirements, and expects the operation's
success (2xx) status code. Positive cases complement the negative cases described in
[Negative testing](negative-testing.md): together they verify that the API rejects invalid input
*and* accepts valid input where the contract says it should.

By default the generator emits only negative test cases. The baseline valid case is always built
internally (negative providers mutate it to produce invalid variants), but it is only included in
the output when `includeValidCase` is enabled.

## Prerequisites

- A working generation setup — see [Getting started](../getting-started/index.md)
- For secured operations: `validSecurityValues` entries, unless placeholder values are acceptable
  (see [Security values below](#provide-valid-credentials-for-secured-operations))

## Enable the baseline valid case

### Enable positive testing in the YAML config file

```yaml
testGenerationSettings:
    includeValidCase: true
```

### Enable positive testing with the CLI

```bash
openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --setting includeValidCase=true
```

### Enable positive testing in the Gradle DSL

```kotlin
openApiTestGenerator {
    testGenerationSettings {
        includeValidCase.set(true)
    }
}
```

## How the valid case is built

Each operation gets a single case named `Test Valid Case`, assembled by `ValidCaseBuilder` in core:

- **Parameters**: only *required* path, query, header, and cookie parameters are populated, using
  schema-derived example values (the parameter's `schema` first, falling back to its `content`
  schema).
- **Request body**: the first supported media type is used. An explicit media-type `example` /
  `examples` entry takes precedence; otherwise the body is generated from the schema. If the body
  is required and no media type is supported, generation for the operation fails.
- **Security**: the first security requirement object is satisfied — API keys are placed in the
  declared query/header/cookie location, other schemes produce an `authorization` header.
- **Expected status**: the lowest numeric 2xx response declared for the operation; `2XX` and
  `default` responses fall back to 200. An operation without any success response fails with
  `Success status code not found in responses`.
- **Expected body**: when the success response declares content, the response example is extracted
  with media-type negotiation (explicit examples take precedence over schema-generated ones).

Because the valid case is also the baseline that negative providers mutate, an operation whose
valid case cannot be built fails as a whole — with or without `includeValidCase`.

When enabled, the valid case counts toward `maxTestCasesPerOperation`
(see [budget controls](../reference/distribution-settings.md#budget-controls)).

## Provide valid credentials for secured operations

Example values for security schemes come from `validSecurityValues`, keyed by the scheme name
under `components.securitySchemes` (not the header name):

```yaml
testGenerationSettings:
    includeValidCase: true
    validSecurityValues:
        ApiKeyAuth: test-api-key-123
        BearerAuth: valid-jwt-token
```

Without an entry for a scheme, the generated case carries a placeholder such as
`<valid_BearerAuth_placeholder>` — the suite still generates, but the request will not
authenticate against a real server until you substitute a real value. See
[Security values](configuration.md#security-values) for all configuration surfaces.

## What the output looks like

With the `test-suite-writer` generator, the valid case is emitted alongside the negative cases of
each suite (example from the core test fixtures, `POST /pets` secured by `BearerAuth` with no
`validSecurityValues` configured):

```json
{
    "name": "Test Valid Case",
    "method": "POST",
    "path": "/pets",
    "queryParams": {},
    "pathParams": {},
    "headers": [
        { "key": "authorization", "value": "<valid_BearerAuth_placeholder>" }
    ],
    "cookie": [],
    "body": {
        "petType": "cat",
        "meowVolume": 7
    },
    "requestBodyMediaType": "application/json",
    "expectedBody": {
        "data": { "meowVolume": 1, "petType": "cat" },
        "requestId": "d5a5495b-cbdc-4237-a66e-000000000000",
        "status": "success",
        "timestamp": "2025-05-05T17:32:28Z"
    },
    "responseBodyMediaType": "application/json",
    "needToComplete": true,
    "expectedStatusCode": 201
}
```

## Why generated positive tests carry a TODO note

The valid case is emitted with `needToComplete: true`: schema-derived example values satisfy the
contract's *shape*, but may not satisfy business rules (unique emails, existing IDs, referential
integrity), and the schema-generated expected body may not match what your API actually returns.
The template generator therefore adds this note to the generated test method:

```text
TODO: Review this generated case before relying on it as a fully automated test.
```

What to do about it:

- Provide realistic inputs — spec-level `example`/`examples` on media types take precedence over
  generated values, and [example value settings](../reference/distribution-settings.md#testgenerationsettingsexamplevalues)
  (`useSchemaExampleFallback`, provider templates) tune generated values.
- Provide real credentials via `validSecurityValues`.
- With the `test-suite-writer` generator, use MERGE mode with `needToComplete` in
  `protectedTestCaseFields` so your manual completion of the case survives regeneration — see
  [Merge semantics](generators.md#merge-semantics).

## Verify the output

Select the valid cases from a generated single-file suite:

```bash
jq '[.[] | .testCases[] | select(.name == "Test Valid Case")] | length' generated/test-suites.json
```

Expect one per generated operation.

## Limitations

- One baseline case per operation — this is contract-conformance smoke coverage, not
  property-based valid-input exploration.
- Optional parameters are not exercised; only required inputs are populated.
- The expected body is best-effort: prefer explicit response examples in the spec when you want
  exact assertions.

## Related docs

- [Negative testing](negative-testing.md) — the generated 4xx catalog
- [Configuration](configuration.md) — all settings surfaces and precedence
- [Distribution settings — Output options](../reference/distribution-settings.md#output-options) — `includeValidCase` default
- [Generators](generators.md) — template output and merge semantics
- [Model reference](../reference/model.md) — `TestCase` fields incl. `needToComplete`
