---
description: Enable positive test case generation to verify that valid requests return 2xx responses. By default, only negative test cases (4xx) are generated.
---

# Include positive test cases

By default, the generator produces only negative test cases that validate error handling (4xx responses).
To also test that valid requests succeed with 2xx responses, enable the `includeValidCase` option.

## Configuration

### YAML
```yaml
testGenerationSettings:
    includeValidCase: true
```

### CLI
```bash
openapi-testgen --setting includeValidCase=true ...
```

### Gradle
```kotlin
openApiTestGenerator {
    testGenerationSettings {
        includeValidCase.set(true)
    }
}
```

## What you get

Enabling `includeValidCase` adds a single baseline positive test case (named "Test Valid Case") to each generated suite.
The expected status code is derived from the first 2xx response defined in the OpenAPI spec for the operation.

See: [Distribution settings → Output options](../../reference/distribution-settings.md#output-options)
