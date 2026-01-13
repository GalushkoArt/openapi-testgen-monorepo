# Release process

This guide covers the complete release workflow for OpenAPI Test Generator.

## Overview

A release involves:

1. Preparing the codebase (version bump, changelog, checks)
2. Publishing library modules to Maven Central
3. Publishing the Gradle plugin to the Gradle Plugin Portal
4. Creating a GitHub release (optional)

## Prerequisites

Before your first release, complete the one-time setup in [Publishing artifacts](publishing.md):

- Maven Central Portal account with verified namespace
- Gradle Plugin Portal account with API keys
- GPG signing key uploaded to keyservers
- Credentials configured in `~/.gradle/gradle.properties` or environment

## Release checklist

### 1. Prepare the release

Update the version number:

```bash
# Edit gradle/libs.versions.toml
# Change: openapi-testgen = "0.8.0" → "0.9.0"
```

Update the changelog:

```bash
# Edit docs/changelog/CHANGELOG.md
# Add release notes under a new version heading
```

### 2. Run verification checks

```bash
# Run all tests and quality checks
./gradlew check

# Verify binary API compatibility
./gradlew apiCheck

# Build documentation (optional but recommended)
./gradlew dokkaHtmlMultiModule
```

All checks must pass before proceeding.

### 3. Commit and tag

```bash
git add -A
git commit -m "Release v0.9.0"
git tag -a v0.9.0 -m "Release v0.9.0"
git push origin master --tags
```

### 4. Publish to Maven Central

```bash
for module in model example-value core generator-template pattern-value pattern-support distribution-bundle cli plugin; do
  ./gradlew -p "$module" publishAllPublicationsToMavenCentralRepository
done
```

After upload completes:

1. Log in to [central.sonatype.com](https://central.sonatype.com/)
2. Go to "Deployments"
3. Verify staged artifacts
4. Click "Publish" to release

!!! warning "Wait for sync"
    Artifacts may take up to 30 minutes to appear on Maven Central after publishing.

### 5. Publish to Gradle Plugin Portal

```bash
./gradlew -p plugin publishPlugins
```

The plugin appears on [plugins.gradle.org](https://plugins.gradle.org/plugin/art.galushko.openapi-test-generator) within minutes.

### 6. Create GitHub release (optional)

1. Go to the repository releases page
2. Create a new release from the tag
3. Copy release notes from changelog
4. Attach CLI native binaries if built

## Post-release

After a successful release:

1. Bump version to next snapshot (e.g., `0.10.0-SNAPSHOT`) for continued development
2. Announce the release (if applicable)

## Hotfix releases

For urgent fixes on a released version:

```bash
# Create hotfix branch from tag
git checkout -b hotfix/0.9.1 v0.9.0

# Apply fixes, then follow normal release process
# with version 0.9.1
```

## Dry run

To verify the release process without publishing:

```bash
# Build all artifacts locally
./gradlew publishToMavenLocal

# Check ~/.m2/repository/art/galushko/ for artifacts
ls -la ~/.m2/repository/art/galushko/
```

## Troubleshooting

See [Publishing artifacts - Troubleshooting](publishing.md#troubleshooting) for common issues.
