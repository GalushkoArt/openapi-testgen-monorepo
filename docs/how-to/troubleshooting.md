---
description: Symptom-indexed solutions for generation errors, unexpected output, Gradle-specific issues, and CLI/npm platform problems. Headings quote the exact error messages so you can search for what you see.
---

# Troubleshooting

This page is indexed by symptom: section headings quote the exact error message or observed
behavior. Search the page for the text you see in your build output.

## Diagnosing

- **CLI**: rerun with `--log-level DEBUG` to see resolved options and per-operation decisions.
- **Gradle**: rerun with `--info` (or `--debug`); the plugin's `logLevel` property has
  [no effect inside Gradle](#openapitestgeneratorloglevel-has-no-effect-inside-gradle).
- Generation always ends with a report summarizing successful, partial, and failed operations plus
  collected errors — read it before changing settings. See
  [Error handling](../concepts/error-handling.md).

## Generation errors

### `Generator must be provided either via overrides or config`

No generator id reached the run. Set `--generator` (CLI), `generator.set(...)` (Gradle DSL), or
`generator:` in the YAML config file. The id must be `template` or `test-suite-writer` (plus any
custom generator ids you registered).

### `Unknown generator: '<id>'. Available: template, test-suite-writer`

The generator id has a typo, or a custom generator's module was not registered. If the id in the
message is empty (`Unknown generator: ''`) and you declared `generator:` only in the YAML config
file of a Gradle build: versions before 0.12.0 shadowed the config-file value with the extension's
empty default — upgrade, or set `generator` in the Gradle DSL directly.

### `Invalid log level '<level>'. Expected one of ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF.`

The `--log-level` flag (or `logLevel` config value) is validated before generation in both the CLI
and the Gradle plugin. Fix the value; levels are case-insensitive uppercase names.

### `specFile must be configured via overrides or config file`

Same family as the generator error above: `--spec-file` / `specFile.set(...)` / `specFile:` is
missing. The companion message `outputDir must be configured via overrides or config file` has the
same fix with `--output-dir` / `outputDir`.

### `Budget exceeded for <METHOD> <path>`

The exception message names the exhausted budget (`schema combinations` or
`test cases per operation`) and the setting that raises it. Raise the relevant limits:

```yaml
testGenerationSettings:
    maxSchemaDepth: 100
    maxMergedSchemaDepth: 100
    maxSchemaCombinations: 500
    maxTestCasesPerOperation: 5000
```

Or as a one-off CLI override (semicolons separate multiple `--setting` values):

```bash
openapi-testgen --setting 'maxSchemaCombinations=500;maxTestCasesPerOperation=5000' ...
```

Alternatives: simplify deeply nested `oneOf`/`anyOf`/`allOf` structures, or exclude the offending
operations via [include/ignore filters](configuration.md#ignore-rules). Defaults and semantics:
[Budget controls](../reference/distribution-settings.md#budget-controls).

### `templateSet cannot be blank` and other template option errors

The `template` generator validates its options up front:

- `Cannot identify file extension. Please specify 'outputFileExtension' option in generatorOptions` —
  the template set name contains neither "java" nor "kotlin", so the extension cannot be inferred.
- `Invalid 'templateVariables' option: '...'. Expected map.` — `templateVariables` must be a map,
  not a string (in the CLI use dot notation: `--generator-option templateVariables.package=...`).
- `Invalid 'writeMode' option: '...'. Supported values: OVERWRITE, SKIP_IF_EXISTS` — check spelling.
- `Custom template not found: <dir>/<path>` — `customTemplateDir` is set but the template file is
  missing under it. The directory must contain the full relative template path.

See [Template generator options](generators.md#template-generator-options) for the option table.

### `Unsupported Swagger version '<x>' in <spec>. Only Swagger 2.0 is supported.`

Swagger 1.x specs are rejected. Convert the spec to Swagger 2.0 or OpenAPI 3.x — see
[Supported specifications](../reference/supported-specifications.md).

### `Parsed unknown OpenAPI/Swagger version model is null`

The parser could not read the spec at all. The accompanying `Unable to read location ...` detail
usually means the path is wrong or not readable from the working directory (in Gradle, paths
resolve against the project directory).

### YAML spec too large to parse

Very large specs hit SnakeYAML's ~3 MB code-point limit. Raise it with a parser setting:

```bash
openapi-testgen --parser-setting yamlCodePointLimit=10000000 ...
```

Gradle: `parserSettings { yamlCodePointLimit.set(10_000_000) }`. See
[parserSettings](../reference/distribution-settings.md#parsersettings).

## Unexpected output

### No tests generated

- Check your OpenAPI spec contains operations with parameters and/or request bodies.
- Check ignore filters are not excluding everything: `testGenerationSettings.ignoreTestCases`,
  `ignoreSchemaValidationRules`, `ignoreAuthValidationRules`.
- Check `includeOperations` — when set, only listed path/method pairs generate.
- If your spec is webhook-only (defines `webhooks` but no `paths`), generation currently returns
  zero suites by design (see below).

### Webhook-only spec produces no suites

Current generation logic targets `paths` operations. OpenAPI `webhooks` are parsed but not yet
converted into test suites.

Expected behavior for webhook-only specs:

- CLI/Gradle command succeeds
- summary contains zero operations and zero test cases
- no suite artifact is written by `test-suite-writer`

Workaround: if you need generated suites now, model the webhook consumer endpoints under `paths`
in a generation-specific spec.

### Generated requests contain `<valid_..._placeholder>` values

Placeholders such as `<valid_BearerAuth_placeholder>` or `<valid_ApiKeyAuth_api_key_placeholder>`
mean no `validSecurityValues` entry was configured for that security scheme. The key must match
the scheme name under `components.securitySchemes` (e.g. `ApiKeyAuth`), **not** the header name
(e.g. `X-API-Key`). See [Security values](configuration.md#security-values).

### Generated tests contain a `TODO: Review this generated case` comment

The test case was emitted with `needToComplete: true` — this is the baseline positive case, whose
schema-derived values satisfy the contract's shape but not necessarily your business rules. See
[Positive testing](positive-testing.md#why-generated-positive-tests-carry-a-todo-note).

### Generated tests do not compile

The template generator emits sources, not dependencies. RestAssured template sets need
RestAssured, JUnit 5, and (for the Spring variants) Spring Boot test dependencies on the test
classpath — see [Step 3: Add test dependencies](../reference/gradle-plugin.md#step-3-add-test-dependencies)
and [RestAssured integration](generators.md#restassured-integration).

### Far too many test cases

Deeply composed schemas multiply cases. Lower `maxTestCasesPerOperation`, use
`includeOperations` to target the operations you care about, or disable rules you do not need —
see [Disable a rule](../reference/catalogs/rules-catalog.md#disable-a-rule-by-fully-qualified-class-name).

### `Test Generation failed due to errors. Enable 'always write tests' to force writing tests anyway.`

By default, artifacts are written only on success.

- CLI: use `--always-write-test`
- YAML/Gradle: set `alwaysWriteTests: true` / `alwaysWriteTests.set(true)`

With `alwaysWriteTests` enabled, the generator writes whatever artifacts it can and the CLI/Gradle
task stays successful when output is written. Inspect the report/log output for the remaining
generation errors.

## Gradle issues

### `openApiTestGenerator.logLevel has no effect inside Gradle`

Inside the Gradle daemon, SLF4J is bound to Gradle's own logging backend, so the deprecated
`logLevel` property cannot change verbosity — the task warns and continues. Use `--info` or
`--debug` on the Gradle command line instead. The value is still validated (an invalid level fails
the task).

### Multiple `--setting` values are not applied

Separators differ by flag: multiple `--setting` values in a single flag are separated by
**semicolons** (`;`); `--generator-option` and `--parser-setting` use **commas** (`,`). Quote the
whole value in shells: `--setting 'maxErrors=10;maxSchemaDepth=5'`.

### Task is UP-TO-DATE but the spec changed

Local spec files are content-tracked, so edits re-trigger generation. Remote spec URIs are tracked
by their string value only — a changed remote document behind the same URI does not invalidate the
task. Run with `--rerun-tasks` or use a local copy of the spec.

## CLI / npm issues

### Force JAR execution

If you installed the CLI via npm and experience issues with the native binary, use the npm-wrapper flag `--prefer-jar` to bypass native execution and run the bundled JAR directly:

```bash
openapi-testgen --prefer-jar --spec-file api.yaml --output-dir tests --generator test-suite-writer
```

This is useful when:

- Native binary crashes or behaves unexpectedly
- You need consistent behavior across different environments
- Debugging platform-specific issues

Requires Java 21+.

### Native binary not working on Linux

If the CLI outputs errors about missing `GLIBC_X.XX` symbols or falls back to JAR unexpectedly:

**Linux x64**: The binary is statically linked and should work on any Linux distribution.

**Linux ARM64**: The binary requires glibc (not musl). If your glibc version is too old, the CLI automatically falls back to JAR:

```
Warning: Native binary is not compatible with this system (likely glibc version mismatch).
Falling back to JAR-based CLI (requires Java 21+)...
```

Solutions:

1. **Install Java 21+** for the JAR fallback to work
2. **Upgrade your distribution** to get a newer glibc
3. **Use Docker** with a compatible base image

### Java not found

The JAR fallback requires Java 21+:

```bash
# Check Java version
java -version

# Install Java 21
# Ubuntu/Debian
sudo apt install openjdk-21-jre

# macOS (Homebrew)
brew install openjdk@21

# Windows (Chocolatey)
choco install temurin21
```

### Alpine Linux / musl

Native binaries require glibc. On musl-based systems (Alpine), install Java 21+:

```bash
apk add openjdk21
```

See [Installation](../getting-started/installation.md#cli-via-npm) for platform details and npm-wrapper behavior.

## Related docs

- [Configuration](configuration.md) — settings surfaces and precedence
- [Distribution settings](../reference/distribution-settings.md) — defaults for every key
- [Error handling](../concepts/error-handling.md) — error modes and the generation report
- [CLI reference](../reference/cli.md) and [Gradle plugin reference](../reference/gradle-plugin.md)
