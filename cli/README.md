# OpenAPI Test Generator CLI

Command-line interface for generating API tests from OpenAPI specifications.

## Installation

### Download Release

Download from [GitHub Releases](https://github.com/GalushkoArt/openapi-testgen-monorepo/releases):

| Format  | File                                          | Use Case                    |
|---------|-----------------------------------------------|-----------------------------|
| Native  | `openapi-testgen-<version>-linux-amd64.zip`   | Fastest startup, no JVM     |
| Native  | `openapi-testgen-<version>-linux-arm64.zip`   | Linux native binary for ARM |
| Native  | `openapi-testgen-<version>-macos-arm64.zip`   | macOS native binary         |
| Native  | `openapi-testgen-<version>-windows-amd64.zip` | Windows native binary       |
| Fat JAR | `openapi-testgen-<version>.zip`               | Portable, requires JVM      |

### Build from Source

```bash
# JVM distribution
./gradlew :cli:installDist
./cli/build/install/openapi-testgen/bin/openapi-testgen --help

# Fat JAR
./gradlew :cli:shadowJar
java -jar cli/build/libs/openapi-testgen-*-all.jar --help

# Native binary (requires GraalVM)
./gradlew :cli:nativeCompile
./cli/build/native/nativeCompile/openapi-testgen --help
```

## Quick Start

Generate Rest-Assured Java tests:

```bash
openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./generated-tests \
  --generator template \
  --generator-option templateSet=restassured-java \
  --generator-option templateVariables.package=com.example.generated
```

Generate JSON test suites:

```bash
openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./test-data \
  --generator test-suite-writer \
  --generator-option outputFileName=tests.json
```

## Options

| Option                | Description                                       |
|-----------------------|---------------------------------------------------|
| `--spec-file`         | Path to OpenAPI spec (required unless in config)  |
| `--output-dir`        | Output directory (required unless in config)      |
| `--generator`         | Generator: `template` or `test-suite-writer`      |
| `--generator-option`  | Generator option (repeatable): `key=value`        |
| `--config-file`       | YAML configuration file                           |
| `--setting`           | Test generation setting (repeatable): `key=value` |
| `--always-write-test` | Write output even on errors                       |
| `--log-level`         | TRACE, DEBUG, INFO, WARN, ERROR, OFF              |
| `--help`              | Show help                                         |
| `--version`           | Show version                                      |

## Configuration File

```yaml
specFile: "openapi.yaml"
outputDir: "./generated"
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

```bash
openapi-testgen --config-file config.yaml
```

CLI flags override config file values. Nested maps are deep-merged; lists are replaced.

## Test Generation Settings

Use `--setting` for typed settings:

```bash
# Error handling
--setting errorMode=FAIL_FAST
--setting maxErrors=100

# Security values
--setting validSecurityValues.ApiKeyAuth=test-key

# Budget controls
--setting maxSchemaDepth=30
--setting maxTestCasesPerOperation=500

# Example value providers
--setting exampleValues.providers[]=enum
--setting exampleValues.providers[]=const
--setting exampleValues.providers[]=pattern

# Pattern generation
--setting patternGeneration.defaultMinLength=10
```

## Generators

### Template Generator

```bash
openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./generated \
  --generator template \
  --generator-option templateSet=restassured-java \
  --generator-option templateVariables.package=com.example \
  --generator-option templateVariables.baseUrl=http://localhost:8080
```

Template sets: `restassured-java`, `restassured-kotlin`

For custom templates:

```bash
--generator-option customTemplateDir=./templates \
--generator-option classTemplatePath=my-template.mustache
```

### Test Suite Writer

```bash
openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option outputFileName=tests.json \
  --generator-option writeMode=MERGE
```

Output formats: `.json`, `.yaml` (determined by filename)

## Native Image

The native binary provides instant startup and requires no JVM. Pre-built binaries are available
in [GitHub Releases](https://github.com/GalushkoArt/openapi-testgen-monorepo/releases).

Build locally (requires GraalVM with `native-image`):

```bash
./gradlew :cli:nativeCompile
```

### Architecture

- Rule and generator wiring is explicit (no reflection for discovery)
- Reflection config is pre-generated for: OpenAPI parser, Jackson, Mustache, Logback
- Config files: `src/main/resources/META-INF/native-image/`

Regenerate reflection config after dependency changes:

```bash
./gradlew :cli:regenerateNativeImageConfig
```

## Documentation

- [Getting Started](https://docs.galushko.art/openapi-test-generator/getting-started/)
- [CLI Reference](https://docs.galushko.art/openapi-test-generator/reference/cli/)
- [Distribution Settings](https://docs.galushko.art/openapi-test-generator/reference/distribution-settings/)
- [Generator Options](https://docs.galushko.art/openapi-test-generator/reference/catalogs/generator-options/)
- [API Reference (Dokka)](https://docs.galushko.art/openapi-test-generator/api/)

## Development

```bash
# Unit tests
./gradlew :cli:test

# Smoke tests
./gradlew :cli:testFatJar
./gradlew :cli:testNative  # requires prior nativeCompile
./gradlew :cli:testDistributions
```
