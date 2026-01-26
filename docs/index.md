# OpenAPI Test Generator

Deterministic test generation from OpenAPI specs. This repo provides a core engine, optional feature modules, a CLI, and a Gradle plugin.

Generated tests validate that your API correctly enforces the contract defined in your OpenAPI specification - parameter types, required fields, format constraints, and security schemes. This is **infrastructure-level validation** (input validation, schema enforcement, authentication), not business logic testing.

## Choose your path

- **New here?** Start with [Getting started](getting-started/index.md).
- **Need to solve a problem?** Jump to [How-to guides](how-to/index.md).
- **Want to understand how it works?** See [Concepts](concepts/index.md).
- **Looking up specific keys/types?** Use [Reference](reference/index.md).
- **Working on the codebase?** See [Modules](modules/index.md) and [Contributing](contributing/index.md).

## Quick start

### CLI

```bash
openapi-testgen --help
openapi-testgen --config-file open-api-test-generation-config.yaml
```

### Gradle plugin

```kotlin
plugins {
    id("art.galushko.openapi-test-generator") version "0.9.1"
}

openApiTestGenerator {
    specFile.set("src/test/resources/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests"))
    generator.set("template")
    generatorOptions.putAll(
        mapOf(
            "templateSet" to "restassured-java",
            "templateVariables" to mapOf(
                "package" to "com.example.generated",
                "baseUrl" to "http://localhost:8080",
            ),
        )
    )
}
```

