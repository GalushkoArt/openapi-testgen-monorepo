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

