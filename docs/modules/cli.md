# Module: `cli`

`cli` provides the `openapi-testgen` command-line interface built on Picocli.

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

# CLI module

## Overview

`cli` provides the `openapi-testgen` command-line interface for running generation from the terminal.

It supports JVM distribution, a fat JAR, and an optional GraalVM native image build.

## Related documentation

- Reference: [CLI flags](../reference/cli.md)
- Getting started: [first test suite](../getting-started/first-test-suite.md)

