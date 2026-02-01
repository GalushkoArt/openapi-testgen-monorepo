---
description: Provide valid authentication credentials for test generation using validSecurityValues. Includes YAML, CLI, and Gradle examples and links to the canonical TestCase security metadata schema.
---

# Configure security values

The generator uses `validSecurityValues` to build a baseline valid case (and to generate auth-related negative cases).

## YAML config

```yaml
testGenerationSettings:
  validSecurityValues:
    ApiKeyAuth: "test-api-key-123"
```

## CLI override

`--setting` keys map directly to `TestGenerationSettings` fields:

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./build/generated \
  --generator template \
  --setting validSecurityValues.ApiKeyAuth=test-api-key-123
```

## Gradle DSL

```kotlin
openApiTestGenerator {
    testGenerationSettings {
        validSecurityValues.put("ApiKeyAuth", "test-api-key-123")
    }
}
```

## OAuth2/OpenID Connect scope metadata

For OAuth2 and OpenID Connect schemes, generated test cases can include structured scope metadata in `securityValues.other.authorizationScopes`.

See [TestCase](../../reference/model/test-case.md#authorizationscopes) for the canonical schema, examples, and presence rules.

## Related docs

- [CLI reference](../../reference/cli.md#settings) - CLI settings syntax
- [Gradle plugin reference](../../reference/gradle-plugin.md#test-generation-settings) - Gradle testGenerationSettings DSL
- [Distribution settings](../../reference/distribution-settings.md) - All settings reference

