---
description: OpenAPI Test Generator automatically creates validation tests from OpenAPI specifications, ensuring your API enforces parameter types, required fields, format constraints, and security schemes.
---

# OpenAPI Test Generator

Deterministic test generation from OpenAPI specs. This repo provides a core engine, optional feature modules, a CLI, and a Gradle plugin. See [Glossary](concepts/glossary.md) for definitions of core terms.

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
# Config filename is arbitrary; examples use openapi-testgen.yaml
openapi-testgen --config-file ./openapi-testgen.yaml
```

### Gradle plugin

See [Version placeholders](getting-started/installation.md#version-placeholders) for how to choose `<version>`.

```kotlin
plugins {
    id("art.galushko.openapi-test-generator") version "<version>"
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

