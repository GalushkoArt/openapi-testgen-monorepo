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

For OAuth2 and OpenID Connect security schemes, the generator automatically populates `authorizationScopes` in `securityValues.other`. This provides structured access to scope information for test generators and custom integrations.

### Example output

Given an OpenAPI spec with OAuth2 security:

```yaml
components:
  securitySchemes:
    oauth2:
      type: oauth2
      flows:
        clientCredentials:
          tokenUrl: https://example.com/token
          scopes:
            read: Read access
            write: Write access
security:
  - oauth2: [read, write]
```

The generated test cases will include:

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

### Using scope metadata

The `authorizationScopes` field allows programmatic access to OAuth2/OpenID Connect scopes without parsing the Authorization header placeholder:

```kotlin
val scopes = testCase.securityValues.other["authorizationScopes"] as? List<Map<String, Any>>
scopes?.forEach { entry ->
    val schemeName = entry["name"] as String
    val schemeType = entry["type"] as String  // "oauth2" or "openidconnect"
    val scopeList = entry["scopes"] as List<String>
    // Use scope information for token generation, assertions, etc.
}
```

!!! tip "Scope-based test generation"
    Auth validation rules like `InsufficientScopesAuthValidationRule` and `IncorrectScopesAuthValidationRule` modify scopes to generate negative test cases. The `authorizationScopes` field reflects these modifications, making it easy to see exactly which scopes are being tested.

