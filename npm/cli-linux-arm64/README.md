# @openapi-testgen/cli-linux-arm64

Native Linux ARM64 binary for [@openapi-testgen/cli](https://www.npmjs.com/package/@openapi-testgen/cli).

## Installation

This package is automatically installed as an optional dependency when you install `@openapi-testgen/cli` on Linux ARM64 with glibc.

For standalone installation:

```bash
npm install -g @openapi-testgen/cli-linux-arm64
```

## Requirements

- Linux ARM64
- glibc 2.35+ (Ubuntu 22.04+, Debian 12+, RHEL 9+, etc.)
- Node.js 18+

This binary requires glibc and will not work on musl-based systems (Alpine Linux). On incompatible systems, `@openapi-testgen/cli` automatically falls back to JAR-based execution.

## Usage

```bash
openapi-testgen --help
```

## Troubleshooting

If you see a glibc version mismatch warning, the main CLI package will automatically fall back to JAR execution. Ensure Java 21+ is installed for the fallback to work.

## Documentation

- [CLI Reference](https://docs.galushko.art/openapi-test-generator/reference/cli/)
- [Installation Guide](https://docs.galushko.art/openapi-test-generator/getting-started/installation/#cli-via-npm)

## License

MIT
