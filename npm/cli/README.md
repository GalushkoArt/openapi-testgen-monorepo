# @openapi-testgen/cli

Generate test cases from OpenAPI specifications to validate API contract compliance.

## Installation

```bash
# Global installation
npm install -g @openapi-testgen/cli

# Run directly with npx
npx @openapi-testgen/cli --help

# Project dependency
npm install --save-dev @openapi-testgen/cli
```

## Requirements

The package supports two execution modes:

### Native Binary (Recommended)

Native binaries are automatically used when available for your platform:

| Platform    | Package                             | Requirements                                  |
|-------------|-------------------------------------|-----------------------------------------------|
| Linux x64   | `@openapi-testgen/cli-linux-x64`    | Any Linux (static binary)                     |
| Linux ARM64 | `@openapi-testgen/cli-linux-arm64`  | glibc-based Linux; auto JAR fallback on musl   |
| macOS ARM64 | `@openapi-testgen/cli-darwin-arm64` | macOS 11+                                     |
| Windows x64 | `@openapi-testgen/cli-win32-x64`    | Windows 10+                                   |

Native packages are installed as optional dependencies. No Java required.

### JAR Fallback

If no native binary is available for your platform, the CLI falls back to the JAR-based distribution:

- **Requires**: Java 21 or later
- **Works on**: Any platform with Java support

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

## Usage in package.json

```json
{
    "scripts": {
        "generate-tests": "openapi-testgen --spec-file api.yaml --output-dir tests"
    }
}
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

## Documentation

- [Getting Started](https://docs.galushko.art/openapi-test-generator/getting-started/)
- [CLI Reference](https://docs.galushko.art/openapi-test-generator/reference/cli/)
- [npm Installation Guide](https://docs.galushko.art/openapi-test-generator/getting-started/npm-installation/)

## License

MIT
