---
description: Publish library modules to Maven Central, the Gradle plugin to the Plugin Portal, and npm artifacts, including the release checklist and troubleshooting guidance.
---

# Publishing artifacts

This project publishes library modules to Maven Central, the Gradle plugin to the Gradle Plugin Portal, npm CLI packages, and GitHub release assets.

Every publication is manual. The **Release Candidate** workflow builds and tests artifacts without publishing. The **Manual Release Publication** workflow publishes exactly one selected target per dispatch and requires the successful release-candidate run id for the same commit. Local scripts remain available for recovery and dry runs.

## Prerequisites

- Java 21
- GPG signing key (ASCII-armored)
- Maven Central Portal account with verified namespace
- Gradle Plugin Portal account with API keys

## Account setup

### Maven Central (Central Portal)

1. **Create account**: Register at [central.sonatype.com](https://central.sonatype.com/)
2. **Verify namespace**: Go to Namespaces and claim `art.galushko` (requires domain or GitHub verification)
3. **Generate user token**: Visit [central.sonatype.com/account](https://central.sonatype.com/account) and create credentials
4. **Save credentials**: Copy the username and password (cannot be retrieved later)

### Gradle Plugin Portal

1. **Create account**: Register at [plugins.gradle.org](https://plugins.gradle.org/)
2. **Get API keys**: Log in, go to your profile, and open the "API Keys" tab
3. **Copy credentials**: Use the provided snippet or note the key and secret values

### GPG signing key

Maven Central requires all artifacts to be signed.

```bash
# Generate a new key (if needed)
gpg --gen-key

# List keys to find your KEY_ID
gpg --list-secret-keys --keyid-format=long

# Export ASCII-armored private key
gpg --export-secret-keys --armor KEY_ID > private-key.asc

# Upload public key to keyserver (required for verification)
gpg --keyserver keyserver.ubuntu.com --send-keys KEY_ID
```

## Credentials configuration

The project uses the [vanniktech maven-publish plugin](https://github.com/vanniktech/gradle-maven-publish-plugin) which expects specific credential names.

### Environment variables (recommended for CI)

```bash
# Maven Central Portal credentials
export ORG_GRADLE_PROJECT_mavenCentralUsername=your-portal-username
export ORG_GRADLE_PROJECT_mavenCentralPassword=your-portal-password

# GPG signing (ASCII-armored private key)
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(cat private-key.asc)"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=your-gpg-passphrase

# Gradle Plugin Portal
export GRADLE_PUBLISH_KEY=your-plugin-portal-key
export GRADLE_PUBLISH_SECRET=your-plugin-portal-secret
```

### Gradle properties file

Alternatively, add to `~/.gradle/gradle.properties`:

```properties
# Maven Central Portal
mavenCentralUsername=your-portal-username
mavenCentralPassword=your-portal-password
# GPG signing (use \n for newlines in the key)
signingInMemoryKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
signingInMemoryKeyPassword=your-gpg-passphrase
# Gradle Plugin Portal
gradle.publish.key=your-plugin-portal-key
gradle.publish.secret=your-plugin-portal-secret
```

## Prepare a release

1. Update version in `gradle/libs.versions.toml` (key: `openapi-testgen`)
2. Update `CHANGELOG.md`
3. Run the release preflight:

```bash
./scripts/release-preflight.sh <version> --allow-dirty
```

The preflight validates the version catalog, changelog section, module checks, sample builds, `:cli:testFatJar`, and `docsBuild`. By default it also requires a clean working tree; use `--allow-dirty` while checking local release edits before the release commit. `docsBuild` is the current docs verification task and already runs Dokka generation through `dokkaHtmlAll`.

For metadata-only validation, use:

```bash
./scripts/release-preflight.sh <version> --skip-gradle --allow-dirty
```

To include the local native-image smoke test, run with GraalVM available:

```bash
./scripts/release-preflight.sh <version> --include-native --allow-dirty
```

## Manual release-candidate workflow

Use the **Release Candidate** workflow in GitHub Actions when you need signed-off release artifacts without publishing anything.

Inputs:

| Input            | Description                                                            |
|------------------|------------------------------------------------------------------------|
| `version`        | Must match `gradle/libs.versions.toml`                                 |
| `run_npm_matrix` | Runs npm install checks across platforms/package managers when enabled |

The workflow:

1. Validates release metadata with `./scripts/release-preflight.sh <version> --skip-gradle`
2. Runs module checks, sample builds, docs build, and fat-JAR smoke tests
3. Builds native binaries on Linux x64, Linux ARM64, macOS ARM64, and Windows x64
4. Packages GitHub release ZIPs and `SHA256SUMS`
5. Packages npm tarballs
6. Optionally tests npm install flows

Artifacts to download:

| Artifact                              | Contents                                                |
|---------------------------------------|---------------------------------------------------------|
| `github-release-assets-<version>`     | GitHub release ZIPs plus `SHA256SUMS`                   |
| `npm-tarballs-<version>`              | Ready-to-publish npm `.tgz` files                       |

!!! note "Manual boundary"
    The workflow intentionally stops at artifact upload. A maintainer still reviews artifacts, creates the GitHub release, uploads ZIPs, and publishes Maven, Plugin Portal, and npm packages manually.

## Release checklist

### 1. Prepare and verify

- Update `gradle/libs.versions.toml`
- Update `CHANGELOG.md`
- Run `./scripts/release-preflight.sh <version> --allow-dirty`

### 2. Commit and merge

```bash
git add -A
git commit -m "Release <version>"
git push origin HEAD
```

After review, merge the release preparation to `main`. Do not create the tag yet; the final manual publication job creates it only after every registry is public.

```bash
git checkout main
git pull
```

### 3. Build and inspect the release candidate

Run the manual **Release Candidate** workflow from the release commit on `main`. Record its numeric workflow run id; every publication dispatch verifies that the run succeeded for the exact publication commit.

From the `github-release-assets-<version>` workflow artifact, inspect:

- `openapi-testgen-<version>.zip`
- `openapi-testgen-<version>-linux-amd64.zip`
- `openapi-testgen-<version>-linux-arm64.zip`
- `openapi-testgen-<version>-macos-arm64.zip`
- `openapi-testgen-<version>-windows-amd64.zip`
- `SHA256SUMS`

### 4. Publish each registry manually

Run **Manual Release Publication** once per target, always with the same `version` and release-candidate run id:

1. Select `maven-central`. The job uploads signed modules in user-managed mode.
2. Review the Central Portal deployment, press **Publish**, and wait until all modules are public on `repo1.maven.org`.
3. Select `gradle-plugin`. Its job refuses to publish until every Maven module is public.
4. Select `npm`. Its job downloads and publishes the tarballs from the validated release-candidate run.

The Gradle Plugin and npm targets are separate dispatches and may run in either order after Maven Central is public.

### 5. Create the tag and GitHub Release

Run **Manual Release Publication** with target `github-release`. This final job verifies Maven Central, the Gradle Plugin Portal, and all npm packages, downloads the release ZIPs from the validated candidate run, creates the tag and GitHub Release, then dispatches the **Docs Deploy** workflow from the release tag (releases created by the pipeline's own token cannot trigger it via the `release` event).

## Publish to Maven Central

### Using the publish script (recommended)

The simplest way to publish all artifacts:

```bash
./scripts/publish-to-maven-central.sh
```

This script:

- Verifies credentials are set
- Uploads all modules in dependency order with one Gradle invocation
- Provides instructions for the manual release step

Useful options:

```bash
# Print the selected publish tasks without uploading
./scripts/publish-to-maven-central.sh --dry-run

# Publish a single module if a previous upload needs to be resumed
./scripts/publish-to-maven-central.sh --module core

# Verify selected modules through Maven local
./scripts/publish-to-maven-central.sh --local --module core
```

### Manual publishing

Publish individual modules:

```bash
# Single module
./gradlew :model:publishAllPublicationsToMavenCentralRepository

# All library modules
for module in model example-value core generator-template pattern-value pattern-support distribution-bundle cli plugin; do
  ./gradlew ":${module}:publishAllPublicationsToMavenCentralRepository"
done
```

### Local verification

Test publishing locally before uploading:

```bash
./gradlew :core:publishToMavenLocal

# Check the local Maven repository
ls ~/.m2/repository/art/galushko/openapi/testgen/core/
```

### Post-upload steps (manual release)

The project uses manual release mode (`automaticRelease = false`). After artifacts are uploaded:

1. Go to [central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments)
2. Find your deployment (status should show **VALIDATED**)
3. Review the artifacts list
4. Click **Publish** to release to Maven Central

!!! note "Sync timing"
- Artifacts appear on [repo1.maven.org](https://repo1.maven.org/maven2/) within ~30 minutes
- Searchable on [search.maven.org](https://search.maven.org/) within ~2-4 hours

## Publish to Gradle Plugin Portal

The Gradle plugin is published separately:

```bash
./gradlew :plugin:publishPlugins
```

The plugin portal is separate from Maven Central; run both for a complete release.

## Publish to npm Registry

CLI packages are published to npm under the `@openapi-testgen` organization.

### Prerequisites

- npm account with access to `@openapi-testgen` organization
- npm authentication: `npm login --scope=@openapi-testgen`

### Packages

| Package                             | Description                    |
|-------------------------------------|--------------------------------|
| `@openapi-testgen/cli`              | Main CLI with JAR and launcher |
| `@openapi-testgen/cli-linux-x64`    | Native Linux binary            |
| `@openapi-testgen/cli-linux-arm64`  | Native Linux ARM64 binary      |
| `@openapi-testgen/cli-darwin-arm64` | Native macOS binary            |
| `@openapi-testgen/cli-win32-x64`    | Native Windows binary          |

### From release-candidate tarballs

Download and extract the `npm-tarballs-<version>` artifact, then run:

```bash
# Validate the publish order with npm
./scripts/publish-npm-tarballs.sh <version> --tarball-dir ./npm-tarballs --dry-run

# Publish after manually reviewing the dry run
./scripts/publish-npm-tarballs.sh <version> --tarball-dir ./npm-tarballs --yes
```

Native packages must be published before the main package. The script enforces that order and requires `--yes` for real publishing. See [npm Publishing Guide](npm-publishing.md) for detailed steps.

## Manual publication workflow

`.github/workflows/release-publish.yml` is started with `workflow_dispatch` and publishes one target per run.

| Input                      | Description                                                                    |
|----------------------------|--------------------------------------------------------------------------------|
| `version`                  | Must match `gradle/libs.versions.toml` and the changelog section               |
| `target`                   | `maven-central`, `gradle-plugin`, `npm`, or final `github-release`             |
| `release_candidate_run_id` | Successful Release Candidate run for the exact commit selected for publication |

Configure these secrets in the protected `release` environment:

| Secret                   | Used by                |
|--------------------------|------------------------|
| `MAVEN_CENTRAL_USERNAME` | Maven Central staging  |
| `MAVEN_CENTRAL_PASSWORD` | Maven Central staging  |
| `SIGNING_KEY`            | Maven artifact signing |
| `SIGNING_KEY_PASSWORD`   | Maven artifact signing |
| `GRADLE_PUBLISH_KEY`     | Gradle Plugin Portal   |
| `GRADLE_PUBLISH_SECRET`  | Gradle Plugin Portal   |
| `NPM_TOKEN`              | npm packages           |

## GitHub release

The final `github-release` target creates the tag, extracts the matching `CHANGELOG.md` section, and attaches the ZIPs and `SHA256SUMS` from `github-release-assets-<version>`. It fails before tagging if any required registry publication is unavailable. As its last step it dispatches the **Docs Deploy** workflow from the new tag, since a release created with the workflow's own `GITHUB_TOKEN` does not fire the `release: published` trigger.

## Post-release

After a successful release:

1. Bump the project to the next development version
2. Update the changelog `Unreleased` section if needed
3. Verify public package pages and docs links

## Hotfix releases

For urgent fixes on top of a tagged release:

```bash
git checkout -b hotfix/<next-patch-version> <previous-version-tag>
```

Apply the fix, follow the same checklist, and publish the new patch version.

## Dry run

Use local publishing and docs verification before a real release:

```bash
./scripts/release-preflight.sh <version>
./scripts/publish-to-maven-central.sh --local
./scripts/package-github-release-assets.sh <version> --allow-missing-native
```

## Troubleshooting

### Signing failures

- Ensure the signing key contains the full ASCII-armored key including headers
- Check that the key is not expired: `gpg --list-secret-keys`
- Verify the passphrase is correct

### Upload failures (401/403)

- Regenerate Portal credentials if expired
- Verify namespace `art.galushko` is claimed and verified
- Check credentials are Maven Central Portal tokens (not OSSRH or other credentials)

### Deployment not visible

- Check [central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments)
- Verify the upload completed without errors in Gradle output
- Contact [central-support@sonatype.com](mailto:central-support@sonatype.com) for assistance

### Validation failures

Common validation errors in the Portal:

| Error               | Solution                                        |
|---------------------|-------------------------------------------------|
| Missing POM element | Ensure `description` is set in build.gradle.kts |
| Invalid signature   | Check GPG key and passphrase                    |
| Missing javadoc JAR | Dokka task may have failed; check build logs    |

## Reference links

- [vanniktech maven-publish plugin](https://github.com/vanniktech/gradle-maven-publish-plugin)
- [Central Portal documentation](https://central.sonatype.org/)
- [Central Portal publishing guide](https://central.sonatype.org/publish/publish-portal-gradle/)
- [Gradle Plugin Portal publishing](https://plugins.gradle.org/docs/publish-plugin)
