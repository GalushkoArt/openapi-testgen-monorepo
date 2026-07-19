<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/assets/wordmark-dark.svg">
    <img src="docs/assets/wordmark.svg" alt="openapi-testgen" width="380">
  </picture>
</p>

# OpenAPI Test Generator

[![CI](https://github.com/GalushkoArt/openapi-testgen-monorepo/actions/workflows/ci.yml/badge.svg)](https://github.com/GalushkoArt/openapi-testgen-monorepo/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/GalushkoArt/openapi-testgen-monorepo/branch/main/graph/badge.svg)](https://codecov.io/gh/GalushkoArt/openapi-testgen-monorepo)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/art.galushko.openapi.testgen/core)](https://central.sonatype.com/artifact/art.galushko.openapi.testgen/core)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/art.galushko.openapi-test-generator)](https://plugins.gradle.org/plugin/art.galushko.openapi-test-generator)
[![npm](https://img.shields.io/npm/v/@openapi-testgen/cli)](https://www.npmjs.com/package/@openapi-testgen/cli)

Automatically generate test cases from your OpenAPI or Swagger specifications to validate **API contract compliance**. The generator creates negative test cases that verify your controllers properly enforce parameter validation, request body constraints, and authentication as defined in your API spec — plus an optional baseline positive (2xx) case per operation (`includeValidCase`). These tests validate infrastructure-level behavior (input validation, schema enforcement, security) - not business logic.

**[Documentation](https://docs.galushko.art/openapi-test-generator/)** |
**[Getting Started](https://docs.galushko.art/openapi-test-generator/getting-started/)** |
**[API Reference](https://docs.galushko.art/openapi-test-generator/api/)**

## What It Does

- **Parameter validation tests**: Missing required params, invalid types, out-of-range values, enum violations
- **Request body tests**: Schema validation, nested object constraints, array limits
- **Authentication tests**: Missing credentials, invalid tokens, wrong security schemes
- **Positive baseline tests**: Optional valid (2xx) request per operation via `includeValidCase`
- **Deterministic output**: Same spec always produces the same tests (reproducible builds)

## Current Scope

- OpenAPI 3.0.x, OpenAPI 3.1.x, and Swagger 2.0 input specs are supported.
- Generation currently processes operations under `paths`.
- OpenAPI `webhooks` are parsed but are not yet converted into generated test suites.
- For webhook-only specs (no `paths`), generation completes successfully with zero suites.

## See It in Action

Given this OpenAPI spec with a `page` query parameter (`minimum: 1`):

```yaml
paths:
  /users:
    get:
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            minimum: 1
```

Run the generator:

```bash
openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json
```

Generated output (`test-suites.json`):

```json
{
    "listUsers": {
        "path": "/users",
        "method": "GET",
        "operationName": "listUsers",
        "testCases": [
            {
                "name": "Invalid Query page parameter: Out Of Minimum Boundary Number",
                "method": "GET",
                "path": "/users",
                "queryParams": { "page": 0 },
                "headers": [{ "key": "X-API-Key", "value": "test-api-key-123" }],
                "expectedStatusCode": 400
            },
            {
                "name": "Invalid Query page parameter: Integer Breaking",
                "method": "GET",
                "path": "/users",
                "queryParams": { "page": 1.5 },
                "headers": [{ "key": "X-API-Key", "value": "test-api-key-123" }],
                "expectedStatusCode": 400
            }
        ]
    }
}
```

These tests catch APIs that accept `page=0` (below minimum) or `page=1.5` (non-integer) when the spec says `integer, minimum: 1`. Without these tests, clients might receive wrong data instead of a 400 error.

## Installation

### Gradle Plugin

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("art.galushko.openapi-test-generator") version "<version>"
}
```

See the [installation guide](https://docs.galushko.art/openapi-test-generator/getting-started/installation/#version-placeholders) for where to look up `<version>`.

[View on Gradle Plugin Portal](https://plugins.gradle.org/plugin/art.galushko.openapi-test-generator)

### CLI (npm)

The easiest way to install the CLI:

```bash
npm install -g @openapi-testgen/cli
openapi-testgen --help
```

Native binaries are automatically used when available. Falls back to the bundled JAR (requires Java 21+) on unsupported platforms.

Also available via pnpm, yarn, or bun. See [Installation](https://docs.galushko.art/openapi-test-generator/getting-started/installation/#cli-via-npm) for platform notes and npm-wrapper behavior.

### CLI (Manual)

Download from [GitHub Releases](https://github.com/GalushkoArt/openapi-testgen-monorepo/releases):

- **Native binary** (fastest, no JVM required): `openapi-testgen-<version>-<platform>.zip`
- **JVM distribution** (portable): `openapi-testgen-<version>.zip`
  - Contains launcher scripts and `openapi-testgen-<version>-all.jar`

Or build from source:

```bash
./gradlew :cli:installDist
./cli/build/install/openapi-testgen/bin/openapi-testgen --help
```

## Quick Start

### Using the Gradle Plugin

```kotlin
openApiTestGenerator {
    specFile.set("src/main/resources/openapi.yaml")
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

```bash
./gradlew generateOpenApiTests
```

### Using the CLI

Generate Rest-Assured Java tests:

```bash
openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./generated-tests \
  --generator template \
  --generator-option templateSet=restassured-java \
  --generator-option templateVariables.package=com.example.generated \
  --generator-option templateVariables.baseUrl=http://localhost:8080
```

Generate JSON test suites for data-driven frameworks:

```bash
openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./test-data \
  --generator test-suite-writer \
  --generator-option outputFileName=tests.json
```

## Output Formats

| Generator           | Output                          | Use Case                                                |
|---------------------|---------------------------------|---------------------------------------------------------|
| `template`          | Java/Kotlin/custom test classes | Rest-Assured, Spring MockMvc, custom mustache templates |
| `test-suite-writer` | JSON/YAML test suites           | Data-driven frameworks, custom runners                  |

## Configuration

All settings work identically across the CLI (`--setting`, `--config-file`), the Gradle DSL, and a
shared YAML config file — for example security credentials for protected endpoints, operation
filtering, budget limits for complex specs, and the optional positive baseline case:

```yaml
specFile: "openapi.yaml"
outputDir: "./build/generated"
generator: "template"
generatorOptions:
    templateSet: "restassured-java"
    templateVariables:
        package: "com.example.tests"
        baseUrl: "http://localhost:8080"
testGenerationSettings:
    includeValidCase: true
    validSecurityValues:
        ApiKeyAuth: "test-key"
```

Use with CLI: `openapi-testgen --config-file config.yaml`

- [Configuration guide](https://docs.galushko.art/openapi-test-generator/how-to/configuration/) — config file, operation filtering, ignore rules, security values
- [Distribution settings](https://docs.galushko.art/openapi-test-generator/reference/distribution-settings/) — every key, default, and precedence rule (including budget controls)

## Architecture

The generator uses a **provider-rule** architecture:

- **Providers** orchestrate test generation for different aspects (parameters, body, auth)
- **Rules** encode specific OpenAPI constraints (min/max, required, patterns)
- **Generators** output test artifacts (code or data files)

```
OpenAPI Spec → Parser → Providers → Rules → Test Cases → Generator → Output
```

All wiring is explicit and reflection-free, enabling GraalVM native compilation.

## Module Map

| Module                | Purpose                                   |
|-----------------------|-------------------------------------------|
| `model`               | Core data types (`TestCase`, `TestSuite`) |
| `example-value`       | Schema-based example value generation     |
| `core`                | Test generation engine                    |
| `generator-template`  | Mustache-based code generation            |
| `pattern-value`       | Regex pattern-based value generation      |
| `pattern-support`     | Pattern module integration                |
| `distribution-bundle` | Bundled distribution for CLI/plugin       |
| `plugin`              | Gradle plugin                             |
| `cli`                 | Command-line interface                    |

## Extension Points

Extend the generator with custom components:

- **Rules**: `SimpleSchemaValidationRule`, `AuthValidationRule`
- **Providers**: `TestCaseProvider<T>`
- **Generators**: `ArtifactGeneratorFactory`, `ArtifactGenerator`
- **Modules**: `TestGenerationModule`
- **Templates**: Custom Mustache templates for code generation

See the [SPI documentation](https://docs.galushko.art/openapi-test-generator/reference/spi/) and the [Custom templates guide](https://docs.galushko.art/openapi-test-generator/how-to/generators/#custom-mustache-templates).

## Samples

Working examples in the `samples/` directory:

- [Java + Spring + Rest-Assured](samples/java-spring-rest-assured/README.md)
- [Kotlin + Spring + Rest-Assured](samples/kotlin-spring-rest-assured/README.md)
- [Java + Spring + File Writer](samples/java-spring-file-writer/README.md)

## Documentation

- [Getting Started Guide](https://docs.galushko.art/openapi-test-generator/getting-started/)
- [Configuration Guide](https://docs.galushko.art/openapi-test-generator/how-to/configuration/)
- [Generators Guide](https://docs.galushko.art/openapi-test-generator/how-to/generators/)
- [Architecture Concepts](https://docs.galushko.art/openapi-test-generator/concepts/architecture/)
- [API Reference (Dokka)](https://docs.galushko.art/openapi-test-generator/api/)

## Building from Source

Requirements:

- Java 21
- Kotlin 2.3.x

```bash
# Run all checks (aggregates every included build's `check` plus the samples)
./gradlew check

# Build CLI distribution
./gradlew :cli:installDist

# Build native binary (requires GraalVM)
./gradlew :cli:nativeCompile
```

## Contributing

Contributions are welcome! Please start with [Development setup](https://docs.galushko.art/openapi-test-generator/contributing/development-setup/) and [Publishing artifacts](https://docs.galushko.art/openapi-test-generator/contributing/publishing/) for:

- Development setup
- Code style guidelines
- Testing requirements
- Pull request process

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [swagger-parser](https://github.com/swagger-api/swagger-parser) for OpenAPI parsing
- [Mustache.java](https://github.com/spullara/mustache.java) for template rendering
- [regexp-gen](https://github.com/Cornutum/regexp-gen) for regex-based value generation
