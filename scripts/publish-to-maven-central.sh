#!/usr/bin/env bash
#
# Publishes all OpenAPI Test Generator artifacts to Maven Central Portal.
#
# Prerequisites:
#   - ORG_GRADLE_PROJECT_mavenCentralUsername (Portal username)
#   - ORG_GRADLE_PROJECT_mavenCentralPassword (Portal password/token)
#   - ORG_GRADLE_PROJECT_signingInMemoryKey (GPG private key, ASCII-armored)
#   - ORG_GRADLE_PROJECT_signingInMemoryKeyPassword (GPG key passphrase)
#
# Usage:
#   ./scripts/publish-to-maven-central.sh
#
# After successful upload, go to https://central.sonatype.com/publishing/deployments
# to review and release the deployment.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
echo_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
echo_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Check required credentials
check_credentials() {
    local missing=0

    if [[ -z "${ORG_GRADLE_PROJECT_mavenCentralUsername:-}" ]]; then
        echo_error "Missing ORG_GRADLE_PROJECT_mavenCentralUsername"
        missing=1
    fi

    if [[ -z "${ORG_GRADLE_PROJECT_mavenCentralPassword:-}" ]]; then
        echo_error "Missing ORG_GRADLE_PROJECT_mavenCentralPassword"
        missing=1
    fi

    if [[ -z "${ORG_GRADLE_PROJECT_signingInMemoryKey:-}" ]]; then
        echo_error "Missing ORG_GRADLE_PROJECT_signingInMemoryKey"
        missing=1
    fi

    if [[ -z "${ORG_GRADLE_PROJECT_signingInMemoryKeyPassword:-}" ]]; then
        echo_warn "Missing ORG_GRADLE_PROJECT_signingInMemoryKeyPassword (may be required if key is encrypted)"
    fi

    if [[ $missing -eq 1 ]]; then
        echo ""
        echo "Set credentials as environment variables or in ~/.gradle/gradle.properties:"
        echo "  mavenCentralUsername=your-portal-username"
        echo "  mavenCentralPassword=your-portal-password"
        echo "  signingInMemoryKey=-----BEGIN PGP PRIVATE KEY BLOCK-----..."
        echo "  signingInMemoryKeyPassword=your-gpg-passphrase"
        exit 1
    fi

    echo_info "Credentials verified"
}

# Modules to publish (in dependency order)
MODULES=(
    "model"
    "example-value"
    "core"
    "generator-template"
    "pattern-value"
    "pattern-support"
    "distribution-bundle"
    "plugin"
    "cli"
)

# Publish all modules
publish_all() {
    cd "$PROJECT_ROOT"

    echo_info "Publishing all modules to Maven Central Portal..."
    echo ""

    for module in "${MODULES[@]}"; do
        echo_info "Publishing :${module}..."
        ./gradlew ":${module}:publishAllPublicationsToMavenCentralRepository" --no-daemon
        echo ""
    done

    echo_info "All modules published successfully!"
    echo ""
    echo "=============================================="
    echo "  NEXT STEPS"
    echo "=============================================="
    echo ""
    echo "1. Go to: https://central.sonatype.com/publishing/deployments"
    echo "2. Find your deployment (status should be 'VALIDATED')"
    echo "3. Review the artifacts"
    echo "4. Click 'Publish' to release to Maven Central"
    echo ""
    echo "After publishing:"
    echo "  - Artifacts appear on repo1.maven.org within ~30 minutes"
    echo "  - Searchable on search.maven.org within ~2-4 hours"
    echo ""
}

# Main
main() {
    echo ""
    echo "=========================================="
    echo "  OpenAPI Test Generator - Maven Publish"
    echo "=========================================="
    echo ""

    check_credentials
    publish_all
}

main "$@"
