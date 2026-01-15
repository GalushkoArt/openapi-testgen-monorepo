# Publishing artifacts

This project publishes library modules to Maven Central and the Gradle plugin to the Gradle Plugin Portal.

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
2. Update `docs/changelog/CHANGELOG.md`
3. Run all checks:

```bash
./gradlew check
./gradlew apiCheck
```

## Publish to Maven Central

### Using the publish script (recommended)

The simplest way to publish all artifacts:

```bash
./scripts/publish-to-maven-central.sh
```

This script:

- Verifies credentials are set
- Publishes all modules in dependency order
- Provides instructions for the manual release step

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

## CI/CD publishing

For GitHub Actions, configure repository secrets:

```yaml
env:
  ORG_GRADLE_PROJECT_mavenCentralUsername: ${{ secrets.MAVEN_CENTRAL_USERNAME }}
  ORG_GRADLE_PROJECT_mavenCentralPassword: ${{ secrets.MAVEN_CENTRAL_PASSWORD }}
  ORG_GRADLE_PROJECT_signingInMemoryKey: ${{ secrets.SIGNING_KEY }}
  ORG_GRADLE_PROJECT_signingInMemoryKeyPassword: ${{ secrets.SIGNING_PASSWORD }}
  GRADLE_PUBLISH_KEY: ${{ secrets.GRADLE_PUBLISH_KEY }}
  GRADLE_PUBLISH_SECRET: ${{ secrets.GRADLE_PUBLISH_SECRET }}
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

| Error | Solution |
|-------|----------|
| Missing POM element | Ensure `description` is set in build.gradle.kts |
| Invalid signature | Check GPG key and passphrase |
| Missing javadoc JAR | Dokka task may have failed; check build logs |

## Reference links

- [vanniktech maven-publish plugin](https://github.com/vanniktech/gradle-maven-publish-plugin)
- [Central Portal documentation](https://central.sonatype.org/)
- [Central Portal publishing guide](https://central.sonatype.org/publish/publish-portal-gradle/)
- [Gradle Plugin Portal publishing](https://plugins.gradle.org/docs/publish-plugin)
