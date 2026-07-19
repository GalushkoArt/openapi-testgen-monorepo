---
description: Publish CLI packages to npm including the main JAR-based package and platform-specific native binaries. Covers build scripts, version management, and CI integration.
---

# npm Publishing

This guide covers publishing CLI packages to npm.

## Prerequisites

- npm account with access to `@openapi-testgen` organization
- npm authentication configured (`npm login`)
- Release preflight passing (`./scripts/release-preflight.sh <version>`)
- `jq` installed for JSON manipulation

## Package Structure

The CLI is published as multiple npm packages:

| Package                             | Description                        | Size  |
|-------------------------------------|------------------------------------|-------|
| `@openapi-testgen/cli`              | Main package with JAR and launcher | ~15MB |
| `@openapi-testgen/cli-linux-x64`    | Native Linux binary                | ~25MB |
| `@openapi-testgen/cli-linux-arm64`  | Native Linux ARM64 binary          | ~25MB |
| `@openapi-testgen/cli-darwin-arm64` | Native macOS binary                | ~25MB |
| `@openapi-testgen/cli-win32-x64`    | Native Windows binary              | ~25MB |

The main package includes optional dependencies for native packages. npm automatically installs the correct native package based on the user's platform.

## Building Packages

### Main CLI Package (JAR-based)

```bash
# Build fat JAR and prepare npm package
./npm/scripts/build-packages.sh

# Output: cli/build/npm/cli/
```

### Native Packages (requires native binaries)

Native packages are usually built by the manual release-candidate workflow. To build one platform locally, stage the native binary into the same artifact layout used by the workflow:

```bash
# Build and test fat jar and native binary (requires GraalVM 21)
./gradlew :cli:testDistributions

# Example for local Linux x64. Use native-linux-arm64, native-darwin-arm64,
# or native-win32-x64 for other platforms.
mkdir -p cli/build/native-artifacts/native-linux-x64
cp cli/build/native/nativeCompile/openapi-testgen cli/build/native-artifacts/native-linux-x64/

# Prepare npm packages from staged artifacts
NATIVE_DIR="cli/build/native-artifacts" FAT_JAR_DIR="./cli/build/libs" ./npm/scripts/build-packages.sh <version>

# Output: cli/build/npm/cli-linux-x64/, cli/build/npm/cli-linux-arm64/, cli/build/npm/cli-darwin-arm64/, etc.
```

### From release-candidate artifacts

Run the manual **Release Candidate** workflow from GitHub Actions and download `npm-tarballs-<version>`.

```bash
# Extract the workflow artifact to ./tarballs/

# Validate without publishing
./scripts/publish-npm-tarballs.sh <version> --tarball-dir ./tarballs --dry-run

# Publish after manual review
./scripts/publish-npm-tarballs.sh <version> --tarball-dir ./tarballs --yes
```

## Version Management

Package versions are derived from `gradle/libs.versions.toml`:

```toml
[versions]
openapi-testgen = "X.Y.Z"
```

The build script (`npm/scripts/build-packages.sh`) automatically updates `package.json` files with the correct version.

To override the version:

```bash
./npm/scripts/build-packages.sh 1.0.0
```

## Publishing Steps

### 1. Verify Build

```bash
# Run all checks
./scripts/release-preflight.sh <version>

# Build npm packages
./npm/scripts/build-packages.sh

# Verify package structure
./npm/scripts/verify-packages.sh

# Create tarballs for the packages that exist locally
mkdir -p cli/build/npm-tarballs
for pkg in cli-linux-x64 cli-linux-arm64 cli-darwin-arm64 cli-win32-x64 cli; do
  if [[ -d "cli/build/npm/$pkg" ]]; then
    npm pack "cli/build/npm/$pkg" --pack-destination cli/build/npm-tarballs
  fi
done
```

### 2. Authenticate with npm

```bash
npm login --scope=@openapi-testgen
```

### 3. Publish Native Packages First

Native packages must be published before the main package (they are dependencies):

```bash
./scripts/publish-npm-tarballs.sh <version> --tarball-dir cli/build/npm-tarballs --dry-run
```

### 4. Publish All Packages

```bash
./scripts/publish-npm-tarballs.sh <version> --tarball-dir cli/build/npm-tarballs --yes
```

The script publishes native packages first and the main package last. Without `--yes`, it prints the publish plan and exits.

## Testing Locally

### Verdaccio (Local Registry)

Test the full installation flow locally:

```bash
# Start Verdaccio
docker run -d -p 4873:4873 --name verdaccio verdaccio/verdaccio

# Configure npm to use local registry
npm config set registry http://localhost:4873
npm config set //localhost:4873/:_authToken "test-token"

# Publish packages
cd cli/build/npm
for pkg in cli-linux-x64 cli-linux-arm64 cli-darwin-arm64 cli-win32-x64 cli; do
  if [[ -d "$pkg" ]]; then
    cd "$pkg" && npm publish --access public && cd ..
  fi
done

# Test installation
npm install -g @openapi-testgen/cli --registry http://localhost:4873
openapi-testgen --version

# Cleanup
npm config delete registry
npm config delete //localhost:4873/:_authToken
docker stop verdaccio && docker rm verdaccio
```

### Dry Run

```bash
npm publish --dry-run --access public
```

## Release-candidate workflow

The manual release-candidate workflow (`release-candidate.yml`) builds release-ready npm tarballs without publishing them:

1. Builds native binaries on all platforms
2. Prepares npm packages
3. Creates packed tarballs ready for publishing
4. Tests installation on multiple platforms and Node versions

### Workflow artifacts

| Artifact                 | Contents                      | Purpose               |
|--------------------------|-------------------------------|-----------------------|
| `npm-tarballs-<version>` | Ready-to-publish `.tgz` files | Direct manual publish |

### Publishing with the manual workflow

Run **Manual Release Publication** with target `npm`, the release version, and the successful Release Candidate run id. The job verifies that the candidate belongs to the same commit, downloads `npm-tarballs-<version>`, and publishes native packages before the main package.

For local recovery, download the artifact from the Release Candidate workflow, extract it, and run:

```bash
cd npm-tarballs

../scripts/publish-npm-tarballs.sh <version> --tarball-dir . --dry-run
../scripts/publish-npm-tarballs.sh <version> --tarball-dir . --yes
```

## Troubleshooting

### Version Conflict

If version already exists on npm:

1. Bump version in `gradle/libs.versions.toml`
2. Rebuild packages: `./npm/scripts/build-packages.sh`
3. Republish

### Authentication Failure

```bash
# Re-authenticate
npm login --scope=@openapi-testgen

# Verify authentication
npm whoami
```

### Package Too Large

The fat JAR is ~100MB uncompressed but compresses to ~15MB. If the package is too large:

1. Check for accidentally included files
2. Verify `.npmignore` or `files` field in `package.json`
3. Run `npm pack --dry-run` to see included files

### Native Binary Issues

If native binary doesn't work after installation:

1. Check platform compatibility (`os`, `cpu`, `libc` fields)
2. Verify binary is executable (`chmod +x`)
3. Test binary directly: `./cli/build/npm/cli-linux-x64/bin/openapi-testgen --version` (or `cli-linux-arm64`)

## Package Metadata

### Main Package (`@openapi-testgen/cli`)

```json
{
    "name": "@openapi-testgen/cli",
    "bin": {
        "openapi-testgen": "./bin/openapi-testgen"
    },
    "files": [
        "bin/",
        "lib/",
        "scripts/"
    ],
    "engines": {
        "node": ">=18.0.0"
    },
    "optionalDependencies": {
        "@openapi-testgen/cli-linux-x64": "X.Y.Z",
        "@openapi-testgen/cli-linux-arm64": "X.Y.Z",
        "@openapi-testgen/cli-darwin-arm64": "X.Y.Z",
        "@openapi-testgen/cli-win32-x64": "X.Y.Z"
    }
}
```

### Native Packages

```json
{
    "name": "@openapi-testgen/cli-linux-x64",
    "os": [
        "linux"
    ],
    "cpu": [
        "x64"
    ],
    "libc": [
        "glibc"
    ],
    "bin": {
        "openapi-testgen": "./bin/openapi-testgen"
    },
    "files": [
        "bin/"
    ]
}
```

## See Also

- [Publishing artifacts](publishing.md) - Maven Central and Gradle Plugin Portal
- [Publishing artifacts](publishing.md#release-checklist) - Full release checklist
- [Installation](../getting-started/installation.md#cli-via-npm) - User installation guide
