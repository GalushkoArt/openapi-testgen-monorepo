# OpenAPI Test Generator CLI

Command-line interface for generating API tests from OpenAPI and Swagger specifications —
JSON/YAML test suites or ready-to-run Java/Kotlin RestAssured sources, from any build system.

## Installation

```bash
# npm (recommended) — native binary when available, JAR fallback (Java 21+)
npm install -g @openapi-testgen/cli

# or run without installing
npx @openapi-testgen/cli --help
```

Native binaries and JVM distributions are also on
[GitHub Releases](https://github.com/GalushkoArt/openapi-testgen-monorepo/releases). See
[Installation](https://docs.galushko.art/openapi-test-generator/getting-started/installation/) for
platform notes, artifact names, and npm-wrapper behavior.

## Quick Start

Generate JSON test suites from a spec:

```bash
openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./test-data \
  --generator test-suite-writer \
  --generator-option outputFileName=tests.json
```

Swap `--generator template --generator-option templateSet=restassured-java` to emit executable
RestAssured tests instead.

## Documentation

Everything else — the full flag list, nested `--setting`/`--generator-option` syntax, the YAML
config file, and generator options — lives on the docs site:

| Topic | Page |
|-------|------|
| All flags and nested option syntax | [CLI reference](https://docs.galushko.art/openapi-test-generator/reference/cli/) |
| Setting keys, defaults, and precedence | [Distribution settings](https://docs.galushko.art/openapi-test-generator/reference/distribution-settings/) |
| Config file, filtering, security values | [Configuration](https://docs.galushko.art/openapi-test-generator/how-to/configuration/) |
| Template and test-suite-writer output | [Generators](https://docs.galushko.art/openapi-test-generator/how-to/generators/) |
| First run, quick starts | [Getting started](https://docs.galushko.art/openapi-test-generator/getting-started/) |
| Native-image internals and reflection config | [Development setup](https://docs.galushko.art/openapi-test-generator/contributing/development-setup/#native-image-considerations) |

## Development

```bash
# Build from source
./gradlew :cli:installDist          # JVM distribution
./gradlew :cli:shadowJar            # fat JAR
./gradlew :cli:nativeCompile        # native binary (requires GraalVM)

# Tests
./gradlew :cli:test                 # unit tests
./gradlew :cli:testFatJar           # smoke tests
./gradlew :cli:testNative           # requires prior nativeCompile
./gradlew :cli:testDistributions
```
