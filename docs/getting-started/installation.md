---
description: Install the OpenAPI Test Generator CLI or Gradle plugin. Covers npm, GitHub releases, JVM/native distributions, platform notes, and source builds.
---

# Installation

This project supports two primary entry points:

- **CLI**: `openapi-testgen`
- **Gradle plugin**: `art.galushko.openapi-test-generator`

## Requirements

### Runtime

- **Java 21** or later for the JVM CLI distribution and the Gradle plugin
- **Node.js 18+** for the npm wrapper package
- Native binaries have no Java requirement

### Supported specifications

- **OpenAPI 3.0.x**
- **OpenAPI 3.1.x**

### Building from source

- Java 21
- Kotlin 2.2.x

## Version placeholders

This documentation uses `<version>` in examples.

Where to find it:

- **CLI release artifacts**: [GitHub Releases](https://github.com/GalushkoArt/openapi-testgen-monorepo/releases)
- **Gradle plugin version**: [Gradle Plugin Portal](https://plugins.gradle.org/plugin/art.galushko.openapi-test-generator)
- **Published modules**: Maven Central under `art.galushko.openapi.testgen`

## CLI via npm

### Quick install

=== "npm"

    ```bash
    npm install -g @openapi-testgen/cli
    openapi-testgen --help
    ```

=== "pnpm"

    ```bash
    pnpm add -g @openapi-testgen/cli
    pnpm dlx @openapi-testgen/cli --help
    ```

=== "yarn"

    ```bash
    yarn global add @openapi-testgen/cli
    yarn dlx @openapi-testgen/cli --help
    ```

=== "bun"

    ```bash
    bun add -g @openapi-testgen/cli
    bunx @openapi-testgen/cli --help
    ```

### Runtime behavior

The npm package is a wrapper that prefers a native binary when one is available and compatible, then falls back to the bundled JAR when it is not.

Native packages:

| Platform    | Package                             | Notes                                            |
|-------------|-------------------------------------|--------------------------------------------------|
| Linux x64   | `@openapi-testgen/cli-linux-x64`    | Statically linked                                |
| Linux ARM64 | `@openapi-testgen/cli-linux-arm64`  | Requires glibc                                   |
| macOS ARM64 | `@openapi-testgen/cli-darwin-arm64` | Apple Silicon                                    |
| Windows x64 | `@openapi-testgen/cli-win32-x64`    | Native executable                                |

JAR fallback behavior:

- Requires Java 21+
- Applies when no native package exists for the platform
- Applies on musl-based systems such as Alpine Linux
- Applies when the Linux ARM64 binary is present but not compatible with the host glibc

### npm troubleshooting

#### Alpine Linux / musl

The native Linux binaries require glibc. On Alpine and other musl-based systems, install Java 21+ and let the wrapper use the JAR fallback:

```bash
npm install -g @openapi-testgen/cli
apk add openjdk21
```

#### Linux ARM64 glibc mismatch

If you see a warning like this:

```text
Warning: Native binary is not compatible with this system (likely glibc version mismatch).
Falling back to JAR-based CLI (requires Java 21+)...
```

install Java 21+ or upgrade to a newer glibc-based distribution.

#### Force JAR execution from the npm wrapper

`--prefer-jar` is handled by the npm wrapper package, not by the raw CLI binary or JVM distribution.

```bash
openapi-testgen --prefer-jar --help
```

Use it when you want to bypass native-binary detection for debugging or consistency across environments.

## CLI via GitHub Releases

Download from [GitHub Releases](https://github.com/GalushkoArt/openapi-testgen-monorepo/releases):

- **JVM distribution**: `openapi-testgen-<version>.zip`
- **Native distributions**: `openapi-testgen-<version>-<platform>.zip`

JVM distribution contents:

- launcher scripts under `bin/`
- the fat JAR `openapi-testgen-<version>-all.jar`

Run the extracted JVM distribution:

```bash
unzip openapi-testgen-<version>.zip
./openapi-testgen-<version>/bin/openapi-testgen --help
```

Run the fat JAR directly:

```bash
java -jar openapi-testgen-<version>-all.jar --help
```

Run a native distribution:

```bash
unzip openapi-testgen-<version>-linux-amd64.zip
cd openapi-testgen-<version>-linux-amd64
chmod +x openapi-testgen
./openapi-testgen --help
```

## Build from source

```bash
# Installable JVM distribution
./gradlew :cli:installDist
./cli/build/install/openapi-testgen/bin/openapi-testgen --help

# Fat JAR
./gradlew :cli:shadowJar
java -jar cli/build/libs/openapi-testgen-*-all.jar --help

# Native image (requires GraalVM)
./gradlew :cli:nativeCompile
./cli/build/native/nativeCompile/openapi-testgen --help
```

See the [CLI reference](../reference/cli.md) for flags and nested option syntax.

## Gradle plugin

The plugin is published to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/art.galushko.openapi-test-generator).

Apply it with the plugins DSL:

```kotlin
plugins {
    id("art.galushko.openapi-test-generator") version "<version>"
}
```

Or with legacy plugin application:

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

## Next steps

- [Getting started](index.md) for the shortest path to useful output
- [Gradle integration](gradle-integration.md) for build setup
- [CLI reference](../reference/cli.md) for exact flags
- [Gradle plugin reference](../reference/gradle-plugin.md) for DSL fields
