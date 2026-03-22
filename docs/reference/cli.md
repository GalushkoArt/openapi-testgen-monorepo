---
description: Command-line interface reference for openapi-testgen. Covers options, nested flag syntax, usage examples, and CI integration patterns.
---

# CLI reference

Command: `openapi-testgen`

## Installation

See:

- [Installation](../getting-started/installation.md) - npm, native, JVM, and source-build installation paths

## Usage

```bash
openapi-testgen [options]
```

## Operation scope

- Generation currently targets operations from `paths`.
- `webhooks` are parsed but not generated as test suites.
- If the spec contains only `webhooks` and no `paths`, the command succeeds and reports zero generated suites.

## Core options {#core-options}

- `--help`, `-h`: show help
- `--version`, `-V`: show version
- `--config-file <path>`: path to YAML config file
- `--spec-file <path>`: path to OpenAPI spec file (YAML/JSON)
- `--output-dir <path>`: output directory for generated files
- `--generator <id>`: generator id (e.g. `template`, `test-suite-writer`)
- `--always-write-test`: write artifacts even if generation reports errors; when artifacts are written, the command exits successfully and the report still includes the errors (default: false)
- `--log-level <level>`: log level for generator logs (ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF; default: INFO)

## npm wrapper options {#npm-options}

When installed via npm, the wrapper script provides additional options:

- `--prefer-jar`: Force JAR execution, skipping native binary detection in the npm wrapper (requires Java 21+)

## Generator options {#generator-options}

- `--generator-option <key=value>`: generator option (repeatable). Supports dot notation for nested maps and `[]` for lists.

## Settings {#settings}

- `--setting <key.nested.path=value>`: test generation setting (repeatable). Supports dot notation for nested maps and `[]` for lists.
- `--parser-setting <key.nested.path=value>`: parser setting (repeatable). Supports dot notation for nested maps and `[]` for lists.

Parser settings are resolved under `parserSettings`:

- SnakeYAML limits, for example `yamlCodePointLimit=10000000`

## Nested option examples

### Template variables

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./build/generated-tests \
  --generator template \
  --generator-option templateVariables.package=com.example.generated \
  --generator-option templateVariables.baseUrl=http://localhost:8080
```

### List values via `[]`

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./build/generated-tests \
  --generator template \
  --setting exampleValues.providers[]=enum \
  --setting exampleValues.providers[]=const
```

### Example values options

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./build/generated-tests \
  --generator template \
  --setting exampleValues.includeOptionalExampleProperties=true \
  --setting exampleValues.includeWriteOnly=false \
  --setting exampleValues.useSchemaExampleFallback=true
```

### Parser settings and parse options

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./build/generated-tests \
  --generator test-suite-writer \
  --parser-setting yamlCodePointLimit=10000000
```

### Target operations with includeOperations

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./build/generated-tests \
  --generator test-suite-writer \
  --generator-option outputFileName=generated.json \
  --setting 'includeOperations./users/{userId}[]=GET'
```

## Worked example: target a single operation

See [Include operations](../how-to/configuration.md#include-operations) for a full walkthrough (YAML + CLI + expected output excerpt).

## CI integration

### Using in shell scripts

```bash
#!/bin/bash
set -e

openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option outputFileName=test-suites.json
```

### Environment variables

YAML config files do not expand environment variables by themselves. Use CLI flags for overrides:

```bash
API_TEST_KEY=my-secret-key openapi-testgen \
  --config-file openapi-testgen.yaml \
  --setting "validSecurityValues.ApiKeyAuth=${API_TEST_KEY}"
```

Or use a wrapper script:

```bash
#!/bin/bash
openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option outputFileName=test-suites.json \
  --setting "validSecurityValues.ApiKeyAuth=${API_TEST_KEY:-default-key}"
```

## Exit codes

- `0`: success
- `1`: failure

## See also

- [Getting started](../getting-started/index.md) - Fastest path to useful output
- [End-to-end workflow](../getting-started/end-to-end-workflow.md) - Complete workflow from spec to tests
- [Include operations](../how-to/configuration.md#include-operations) - Target specific operations
- [Ignore rules](../how-to/configuration.md#ignore-rules) - Filter by exclusion
- [CI/CD integration](../how-to/ci-cd.md) - CI job wiring patterns
- [Test-suite-writer](../how-to/generators.md#test-suite-writer-generator) - Output format details

## Distribution defaults

See [Distribution settings](distribution-settings.md) for the shared defaults and precedence
used by the CLI and Gradle plugin.
