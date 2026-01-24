# Installation

This project supports two primary entry points:

- **CLI**: `openapi-testgen`
- **Gradle plugin**: `art.galushko.openapi-test-generator`

## Requirements

### Runtime

- **Java 21** or later (required for JVM CLI distribution and Gradle plugin)
- Native binaries have no runtime requirements

### Supported specifications

- **OpenAPI 3.0.x** and **3.1.x**

### Building from source

- Java 21
- Kotlin 2.2.x

## CLI

### npm (Recommended)

The easiest way to install the CLI:

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

Native binaries are automatically used when available. Falls back to JAR (requires Java 21+) on unsupported platforms.

For detailed options (native binaries, project dependencies, troubleshooting), see [npm Installation](npm-installation.md).

### Download from GitHub

Download the latest release from the [GitHub Releases page](https://github.com/GalushkoArt/openapi-testgen-monorepo/releases):

- **JVM distribution** (`openapi-testgen-<version>.zip`): Cross-platform, requires Java 21+
- **Native binary** (`openapi-testgen-<version>-<platform>`): Standalone executable, no Java required
    - `linux-amd64`: Linux x86_64
    - `linux-arm64`: Linux arm_64
    - `macos-arm64`: macOS Apple Silicon
    - `windows-amd64`: Windows x86_64

#### JVM distribution

```bash
# Download and extract
unzip openapi-testgen-0.9.0.zip
cd openapi-testgen-0.9.0

# Run
./openapi-testgen --help
```

#### Native binary

```bash
# Download and make executable (Linux/macOS)
unzip openapi-testgen-0.9.0-linux-amd64.zip
cd openapi-testgen-0.9.0-linux-amd64
chmod +x openapi-testgen
./openapi-testgen --help
```

### Build from source

For development or custom builds:

```bash
# JVM distribution
./gradlew :cli:installDist
./cli/build/install/openapi-testgen/bin/openapi-testgen --help

# Native image (requires GraalVM)
./gradlew :cli:nativeCompile
./cli/build/native/nativeCompile/openapi-testgen --help
```

See the [CLI reference](../reference/cli.md) for complete usage and options.

## Gradle plugin

The plugin is published to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/art.galushko.openapi-test-generator).

### Apply the plugin

Using the plugins DSL (recommended):

```kotlin
plugins {
    id("art.galushko.openapi-test-generator") version "0.9.0"
}
```

Or using legacy plugin application:

```kotlin
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("art.galushko.openapi.testgen:plugin:0.9.0")
    }
}

apply(plugin = "art.galushko.openapi-test-generator")
```

Then configure `openApiTestGenerator { ... }`.
See: [Gradle integration](gradle-integration.md) and [Gradle plugin reference](../reference/gradle-plugin.md).
