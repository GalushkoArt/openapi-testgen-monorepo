---
description: Solutions for common issues including missing test output, budget exceeded errors, CLI/npm problems, and platform-specific compatibility.
---

# Troubleshooting

## No tests generated

- Check your OpenAPI spec contains operations with parameters and/or request bodies.
- Check ignore filters are not excluding everything: `testGenerationSettings.ignoreTestCases`, `ignoreSchemaValidationRules`, `ignoreAuthValidationRules`.
- If your spec is webhook-only (defines `webhooks` but no `paths`), generation currently returns zero suites by design.

### Webhook-only spec produces no suites

Current generation logic targets `paths` operations. OpenAPI `webhooks` are parsed but not yet converted into test suites.

Expected behavior for webhook-only specs:

- CLI/Gradle command succeeds
- summary contains zero operations and zero test cases
- no suite artifact is written by `test-suite-writer`

Workaround: if you need generated suites now, model the webhook consumer endpoints under `paths` in a generation-specific spec.

## Budget exceeded

Symptoms often include `BudgetExceededException` or partial generation with many errors.

Try increasing:

- `maxSchemaDepth` / `maxMergedSchemaDepth`
- `maxSchemaCombinations`
- `maxTestCasesPerOperation`

Or skip problematic operations via ignore config.

## Output not written when errors occur

By default, artifacts are written only on success.

- CLI: use `--always-write-test`
- YAML/Gradle: set `alwaysWriteTests: true` / `alwaysWriteTests.set(true)`

With `alwaysWriteTests` enabled, the generator writes whatever artifacts it can and the CLI/Gradle task stays successful when output is written. Inspect the report/log output for the remaining generation errors.

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

