---
description: The cli module provides the openapi-testgen command-line interface built on Picocli. It supports JVM distribution and optional GraalVM native image builds for running test generation from the terminal.
---

# Module: `cli`

`cli` provides the `openapi-testgen` command-line interface built on Picocli.
It supports JVM distribution (fat JAR) and an optional GraalVM native image build.

## Depends on

- `distribution-bundle`

## Key types

- `GenerateCommand` (Picocli): maps flags to `TestGeneratorOverrides`
- `KeyValueParser`: parses nested `--setting` / `--generator-option` structures (`a.b[]=x`)

## API reference

- Dokka API reference: [`docs/api/cli/index.html`](../api/cli/index.html)

## Related docs

- Reference: [CLI reference](../reference/cli.md)
- Getting started: [First test suite (CLI)](../getting-started/first-test-suite.md)

