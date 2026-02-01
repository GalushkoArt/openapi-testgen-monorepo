# OpenAPI Test Generator

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/art.galushko.openapi.testgen/core)](https://central.sonatype.com/artifact/art.galushko.openapi.testgen/core)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/art.galushko.openapi-test-generator)](https://plugins.gradle.org/plugin/art.galushko.openapi-test-generator)
[![npm](https://img.shields.io/npm/v/@openapi-testgen/cli)](https://www.npmjs.com/package/@openapi-testgen/cli)

Automatically generate test cases from your OpenAPI specifications to validate **API contract compliance**. The generator creates negative test cases that verify your controllers properly enforce parameter validation, request body constraints, and authentication as defined in your OpenAPI spec. These tests validate infrastructure-level behavior (input validation, schema enforcement, security) - not business logic.

**[Documentation](https://docs.galushko.art/openapi-test-generator/)** |
**[Getting Started](https://docs.galushko.art/openapi-test-generator/getting-started/)** |
**[API Reference](https://docs.galushko.art/openapi-test-generator/api/)**

## What It Does

- **Parameter validation tests**: Missing required params, invalid types, out-of-range values, enum violations
- **Request body tests**: Schema validation, nested object constraints, array limits
- **Authentication tests**: Missing credentials, invalid tokens, wrong security schemes
- **Deterministic output**: Same spec always produces the same tests (reproducible builds)

## Installation

### Gradle Plugin

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("art.galushko.openapi-test-generator") version "0.9.2"
}
```

[View on Gradle Plugin Portal](https://plugins.gradle.org/plugin/art.galushko.openapi-test-generator)

### CLI (npm)

The easiest way to install the CLI:

```bash
npm install -g @openapi-testgen/cli
openapi-testgen --help
```

Native binaries are automatically used when available. Falls back to JAR (requires Java 21+) on unsupported platforms.

Also available via pnpm, yarn, or bun. See [npm Installation](https://docs.galushko.art/openapi-test-generator/getting-started/npm-installation/) for details.

### CLI (Manual)

Download from [GitHub Releases](https://github.com/GalushkoArt/openapi-testgen-monorepo/releases):

- **Native binary** (fastest, no JVM required): `openapi-testgen-<version>-<platform>.zip`
- **Fat JAR** (portable): `openapi-testgen-<version>.jar`

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
  --generator-option templateVariables.package=com.example.generated
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

### YAML Config File

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
    maxTestCasesPerOperation: 500
    validSecurityValues:
        ApiKeyAuth: "test-key"
```

Use with CLI: `openapi-testgen --config-file config.yaml`

### Budget Controls

Prevent runaway generation on complex specs:

| Setting                    | Default | Description                           |
|----------------------------|---------|---------------------------------------|
| `maxSchemaDepth`           | 50      | Maximum nested schema depth           |
| `maxSchemaCombinations`    | 100     | Limit for allOf/anyOf/oneOf expansion |
| `maxTestCasesPerOperation` | 1000    | Cap per API operation                 |

### Filtering

Skip specific test cases or rules:

```kotlin
testGenerationSettings {
    ignoreTestCases.putAll(
        mapOf(
            "/internal/*" to mapOf("*" to listOf("*")),           // Skip all /internal/ tests
            "/pets/{id}" to mapOf("DELETE" to listOf("*")),       // Skip DELETE tests
        )
    )
    ignoreSchemaValidationRules.add("InvalidEnumValue")       // Skip specific rule
}
```

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

See the [SPI documentation](https://docs.galushko.art/openapi-test-generator/reference/spi/) and [Custom templates guide](https://docs.galushko.art/openapi-test-generator/how-to/generators/custom-templates/).

## Samples

Working examples in the `samples/` directory:

- [Java + Spring + Rest-Assured](samples/java-spring-rest-assured/README.md)
- [Kotlin + Spring + Rest-Assured](samples/kotlin-spring-rest-assured/README.md)
- [Java + Spring + File Writer](samples/java-spring-file-writer/README.md)

## Documentation

- [Getting Started Guide](https://docs.galushko.art/openapi-test-generator/getting-started/)
- [How-To Guides](https://docs.galushko.art/openapi-test-generator/how-to/)
- [Configuration Reference](https://docs.galushko.art/openapi-test-generator/reference/)
- [Architecture Concepts](https://docs.galushko.art/openapi-test-generator/concepts/architecture/)
- [API Reference (Dokka)](https://docs.galushko.art/openapi-test-generator/api/)

## Building from Source

Requirements:

- Java 21
- Kotlin 2.2.x

```bash
# Run all tests
./gradlew check

# Build CLI distribution
./gradlew :cli:installDist

# Build native binary (requires GraalVM)
./gradlew :cli:nativeCompile
```

## Contributing

Contributions are welcome! Please see the [Contributing Guide](https://docs.galushko.art/openapi-test-generator/contributing/) for:

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
