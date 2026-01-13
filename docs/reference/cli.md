# CLI reference

Command: `openapi-testgen`

## Usage

```bash
openapi-testgen [options]
```

## Options

- `--help`, `-h`: show help
- `--version`, `-V`: show version
- `--config-file <path>`: path to YAML config file
- `--spec-file <path>`: path to OpenAPI spec file (YAML/JSON)
- `--output-dir <path>`: output directory for generated files
- `--generator <id>`: generator id (e.g. `template`, `test-suite-writer`)
- `--generator-option <key=value>`: generator option (repeatable). Supports dot notation for nested maps and `[]` for lists.
- `--setting <key.nested.path=value>`: test generation setting (repeatable). Supports dot notation for nested maps and `[]` for lists.
- `--always-write-test`: write artifacts even if generation fails (default: false)
- `--log-level <level>`: log level for generator logs (ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF; default: INFO)

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

## Exit codes

- `0`: success
- `1`: failure

## Distribution defaults

See [Distribution settings](distribution-settings.md) for the shared defaults and precedence
used by the CLI and Gradle plugin.
