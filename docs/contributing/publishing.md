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
3. **Generate user token**: Visit [central.sonatype.com/usertoken](https://central.sonatype.com/usertoken) and click "Generate User Token"
4. **Save credentials**: Copy the username and password from the modal (cannot be retrieved later)

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

Provide credentials via `~/.gradle/gradle.properties` or environment variables:

```properties
# Maven Central Portal (generate at https://central.sonatype.com/usertoken)
centralPortalUsername=...
centralPortalPassword=...

# GPG signing
signingKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
signingPassword=...

# Gradle Plugin Portal (from https://plugins.gradle.org/ profile → API Keys)
gradle.publish.key=...
gradle.publish.secret=...
```

Or use environment variables (preferred for CI):

```bash
# Maven Central Portal
export CENTRAL_PORTAL_USERNAME=...
export CENTRAL_PORTAL_PASSWORD=...

# GPG signing
export SIGNING_KEY="$(cat private-key.asc)"
export SIGNING_PASSWORD=...

# Gradle Plugin Portal
export GRADLE_PUBLISH_KEY=...
export GRADLE_PUBLISH_SECRET=...
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

Publish each library module (excludes samples):

```bash
for module in model example-value core generator-template pattern-value pattern-support distribution-bundle cli plugin; do
  ./gradlew -p "$module" publishAllPublicationsToMavenCentralRepository
done
```

For local verification before publishing:

```bash
./gradlew -p core publishToMavenLocal
```

### Post-upload steps

After artifacts are uploaded to the staging API:

1. Log in to [central.sonatype.com](https://central.sonatype.com/)
2. Go to "Deployments" to view staged repositories
3. Verify the staged content
4. Click "Publish" to release to Maven Central

!!! tip "Automatic publishing"
    The staging API supports automatic publishing. Add `publishing_type=automatic` to skip manual verification (use with caution for releases).

## Publish to Gradle Plugin Portal

```bash
./gradlew -p plugin publishPlugins
```

The plugin portal is separate from Maven Central; run both for a complete release.

## CI/CD publishing

For GitHub Actions or other CI systems, configure secrets:

```yaml
env:
  CENTRAL_PORTAL_USERNAME: ${{ secrets.CENTRAL_PORTAL_USERNAME }}
  CENTRAL_PORTAL_PASSWORD: ${{ secrets.CENTRAL_PORTAL_PASSWORD }}
  SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
  SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
  GRADLE_PUBLISH_KEY: ${{ secrets.GRADLE_PUBLISH_KEY }}
  GRADLE_PUBLISH_SECRET: ${{ secrets.GRADLE_PUBLISH_SECRET }}
```

## Troubleshooting

### Signing failures

- Ensure `SIGNING_KEY` contains the full ASCII-armored key including headers
- Check that the key is not expired: `gpg --list-secret-keys`
- Verify password is correct

### Upload failures (401/403)

- Regenerate Portal User Token if expired
- Verify namespace is claimed and verified
- Check that credentials are Portal User Token (not other credentials)

### Staging repository not visible

- Artifacts must be uploaded from the same IP address
- Check [central.sonatype.com/deployments](https://central.sonatype.com/deployments) for pending uploads
- Contact [central-support@sonatype.com](mailto:central-support@sonatype.com) for assistance

## Reference links

- [Central Portal documentation](https://central.sonatype.org/)
- [Central Portal Gradle publishing](https://central.sonatype.org/publish/publish-portal-gradle/)
- [Gradle Plugin Portal publishing](https://plugins.gradle.org/docs/publish-plugin)
- [Gradle signing plugin](https://docs.gradle.org/current/userguide/signing_plugin.html)
