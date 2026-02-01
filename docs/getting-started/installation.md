---
description: Install the OpenAPI Test Generator CLI or Gradle plugin. Covers npm installation, GitHub release downloads, native binaries, building from source, and Gradle plugin portal setup.
---

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

## Version placeholders

This documentation uses placeholders like `<version>` in examples.

Where to find a version:

- **CLI releases**: use the release tag / filename from [GitHub Releases](https://github.com/GalushkoArt/openapi-testgen-monorepo/releases).
- **Gradle plugin**: use the version shown in the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/art.galushko.openapi-test-generator).
- **Maven dependencies**: use the version published to Maven Central for the artifact you depend on (for example, `distribution-bundle`).

## CLI

### npm (Recommended)

```bash
npm install -g @openapi-testgen/cli
openapi-testgen --help
```

The npm package uses a native binary when available and falls back to a Java 21+ JAR on unsupported platforms.

For pnpm/yarn/bun commands, project dependency setup, and troubleshooting, see [npm Installation](npm-installation.md).

### Download from GitHub

Download the latest release from the [GitHub Releases page](https://github.com/GalushkoArt/openapi-testgen-monorepo/releases):

- **JVM distribution** (`openapi-testgen-<version>.zip`): Fat JAR, cross-platform, requires Java 21+
- **Native binary** (`openapi-testgen-<version>-<platform>.zip`): Standalone executable, no Java required
    - `linux-amd64`: Linux x86_64
    - `linux-arm64`: Linux arm_64
    - `macos-arm64`: macOS Apple Silicon
    - `windows-amd64`: Windows x86_64

#### JVM distribution

```bash
# Download and extract
unzip openapi-testgen-<version>.zip

# Run (requires Java 21+)
java -jar openapi-testgen-<version>-all.jar --help
```

#### Native binary

```bash
# Download and make executable (Linux/macOS)
unzip openapi-testgen-<version>-linux-amd64.zip
cd openapi-testgen-<version>-linux-amd64
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
    id("art.galushko.openapi-test-generator") version "<version>"
}
```

Or using legacy plugin application:

```kotlin
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("art.galushko.openapi.testgen:plugin:<version>")
    }
}

apply(plugin = "art.galushko.openapi-test-generator")
```

Then configure `openApiTestGenerator { ... }`.
See: [Gradle integration](gradle-integration.md) and [Gradle plugin reference](../reference/gradle-plugin.md).
