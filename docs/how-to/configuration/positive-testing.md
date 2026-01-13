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

Each test suite will include a "Test Valid Case" entry with a 2xx expected status:

```json
{
  "name": "Test Valid Case",
  "method": "POST",
  "path": "/pets",
  "body": { "name": "example", "tag": "example" },
  "expectedStatusCode": 201
}
```

This test verifies that:
- The API accepts valid input
- Required parameters are documented correctly
- The success response code (2xx) matches the spec

!!! note
    This test is not included in the default test suites.

    Also, generating a set of valid test cases for different request parameters is not in the scope of this project because these cases are tighly coupled to the business logic.
