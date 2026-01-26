# npm Installation

Install the OpenAPI Test Generator CLI via npm for easy integration with JavaScript/TypeScript projects.

## Quick Start

=== "npm"

    ```bash
    # Global installation
    npm install -g @openapi-testgen/cli

    # Run directly with npx (no installation)
    npx @openapi-testgen/cli --help

    # Project dependency
    npm install --save-dev @openapi-testgen/cli
    ```

=== "pnpm"

    ```bash
    # Global installation
    pnpm add -g @openapi-testgen/cli

    # Run directly with pnpm dlx
    pnpm dlx @openapi-testgen/cli --help

    # Project dependency
    pnpm add -D @openapi-testgen/cli
    ```

=== "yarn"

    ```bash
    # Global installation
    yarn global add @openapi-testgen/cli

    # Run directly with yarn dlx (Yarn 2+)
    yarn dlx @openapi-testgen/cli --help

    # Project dependency
    yarn add -D @openapi-testgen/cli
    ```

=== "bun"

    ```bash
    # Global installation
    bun add -g @openapi-testgen/cli

    # Run directly with bunx
    bunx @openapi-testgen/cli --help

    # Project dependency
    bun add -D @openapi-testgen/cli
    ```

## Requirements

The npm package supports two execution modes:

### Native Binary (Recommended)

Native binaries are automatically used when available for your platform:

| Platform    | Package                             | Requirements                                   |
|-------------|-------------------------------------|------------------------------------------------|
| Linux x64   | `@openapi-testgen/cli-linux-x64`    | Any Linux (statically linked)                  |
| Linux ARM64 | `@openapi-testgen/cli-linux-arm64`  | glibc-based Linux (Ubuntu, Debian, RHEL, etc.) |
| macOS ARM64 | `@openapi-testgen/cli-darwin-arm64` | macOS 11+ (Apple Silicon)                      |
| Windows x64 | `@openapi-testgen/cli-win32-x64`    | Windows 10+                                    |

Native packages are installed as optional dependencies and provide:

- Instant startup (no JVM warmup)
- Smaller memory footprint
- No Java requirement

!!! note "Linux ARM64 Compatibility"
The Linux ARM64 binary requires glibc and may not work on older distributions or musl-based systems (Alpine Linux). If incompatibility is detected, the CLI
automatically falls back to the JAR version.

### JAR Fallback

If no native binary is available or compatible, the CLI automatically falls back to the JAR-based distribution:

- **Requires**: Java 21 or later
- **Works on**: Any platform with Java support (macOS x64, Linux ARMv7, Alpine, etc.)

The fallback happens automatically in these cases:

1. **No native package** for your platform (e.g., macOS x64, Linux ARMv7)
2. **musl-based systems** like Alpine Linux (the `libc` field in package.json prevents installation)
3. **glibc version mismatch** on Linux ARM64 (detected at runtime)

## Installation Options

### Global Installation

Install globally to use `openapi-testgen` from anywhere:

=== "npm"

    ```bash
    npm install -g @openapi-testgen/cli
    ```

=== "pnpm"

    ```bash
    pnpm add -g @openapi-testgen/cli
    ```

=== "yarn"

    ```bash
    yarn global add @openapi-testgen/cli
    ```

=== "bun"

    ```bash
    bun add -g @openapi-testgen/cli
    ```

Verify installation:

```bash
openapi-testgen --version
```

### Project Dependency

Add to your project for CI/CD integration:

=== "npm"

    ```bash
    npm install --save-dev @openapi-testgen/cli
    ```

=== "pnpm"

    ```bash
    pnpm add -D @openapi-testgen/cli
    ```

=== "yarn"

    ```bash
    yarn add -D @openapi-testgen/cli
    ```

=== "bun"

    ```bash
    bun add -D @openapi-testgen/cli
    ```

Use in `package.json` scripts:

```json
{
    "scripts": {
        "generate-suites": "openapi-testgen --spec-file api.yaml --output-dir test-data --generator test-suite-writer"
    }
}
```

### One-off Execution

Run without installing:

=== "npm"

    ```bash
    npx @openapi-testgen/cli --spec-file api.yaml --output-dir tests
    ```

=== "pnpm"

    ```bash
    pnpm dlx @openapi-testgen/cli --spec-file api.yaml --output-dir tests
    ```

=== "yarn"

    ```bash
    yarn dlx @openapi-testgen/cli --spec-file api.yaml --output-dir tests
    ```

=== "bun"

    ```bash
    bunx @openapi-testgen/cli --spec-file api.yaml --output-dir tests
    ```

## Platform-Specific Installation

### Force Native Binary

Install only the native package for your platform (smallest download):

```bash
# Linux x64
npm install -g @openapi-testgen/cli-linux-x64

# Linux ARM64
npm install -g @openapi-testgen/cli-linux-arm64

# macOS ARM64 (Apple Silicon)
npm install -g @openapi-testgen/cli-darwin-arm64

# Windows x64
npm install -g @openapi-testgen/cli-win32-x64
```

### Force JAR Version

Skip native packages and use JAR only (requires Java 21+):

```bash
npm install -g @openapi-testgen/cli --omit=optional
```

## Troubleshooting

### "Java not found" Error

The JAR fallback requires Java 21+. Install Java or use a native package:

```bash
# Check Java version
java -version

# Install Java 21 (example using SDKMAN)
sdk install java 21-tem

# Or install the native package for your platform
npm install -g @openapi-testgen/cli-linux-x64
npm install -g @openapi-testgen/cli-linux-arm64
```

### Permission Denied (Unix)

Ensure the launcher script is executable:

```bash
chmod +x $(npm root -g)/@openapi-testgen/cli/bin/openapi-testgen
```

### Slow Startup

Native binaries start faster. Install the native package for your platform:

```bash
# Linux
npm install -g @openapi-testgen/cli-linux-x64

# Linux ARM64
npm install -g @openapi-testgen/cli-linux-arm64

# macOS (Apple Silicon)
npm install -g @openapi-testgen/cli-darwin-arm64

# Windows
npm install -g @openapi-testgen/cli-win32-x64
```

### Alpine Linux / musl Systems

The native Linux binaries require glibc. On Alpine Linux or other musl-based systems, the native package won't be installed and the CLI uses the JAR fallback
automatically:

```bash
# Install (native package is skipped automatically on musl)
npm install -g @openapi-testgen/cli

# Ensure Java 21+ is installed
apk add openjdk21
```

If you want to explicitly skip native packages:

```bash
npm install -g @openapi-testgen/cli --omit=optional
```

### glibc Version Mismatch (Linux ARM64)

On Linux ARM64, if you see a warning like this when running the CLI:

```
Warning: Native binary is not compatible with this system (likely glibc version mismatch).
Falling back to JAR-based CLI (requires Java 21+)...
```

This means your system's glibc version is older than what the native binary was built against. The CLI will automatically use the JAR fallback. To ensure it
works:

```bash
# Install Java 21+
# Ubuntu/Debian
sudo apt install openjdk-21-jre

# RHEL/CentOS/Fedora
sudo dnf install java-21-openjdk

# Verify Java version
java -version
```

Alternatively, upgrade your Linux distribution to get a newer glibc version.

### Package Manager Issues

If you encounter issues with a specific package manager:

```bash
# Clear cache and reinstall
npm cache clean --force
npm install -g @openapi-testgen/cli

# Or try a different package manager
pnpm add -g @openapi-testgen/cli
```

## Verifying Installation

```bash
# Check version
openapi-testgen --version

# Show help
openapi-testgen --help

# Test with a spec file
openapi-testgen --spec-file petstore.yaml --output-dir ./tests --generator test-suite-writer
```

## Updating

=== "npm"

    ```bash
    npm update -g @openapi-testgen/cli
    ```

=== "pnpm"

    ```bash
    pnpm update -g @openapi-testgen/cli
    ```

=== "yarn"

    ```bash
    yarn global upgrade @openapi-testgen/cli
    ```

=== "bun"

    ```bash
    bun update -g @openapi-testgen/cli
    ```

## Uninstalling

=== "npm"

    ```bash
    npm uninstall -g @openapi-testgen/cli
    ```

=== "pnpm"

    ```bash
    pnpm remove -g @openapi-testgen/cli
    ```

=== "yarn"

    ```bash
    yarn global remove @openapi-testgen/cli
    ```

=== "bun"

    ```bash
    bun remove -g @openapi-testgen/cli
    ```

## Next Steps

- [First test suite](first-test-suite.md) - Generate your first tests
- [CLI reference](../reference/cli.md) - Full command-line options
- [YAML configuration](../how-to/configuration/yaml-config.md) - Configure via file
