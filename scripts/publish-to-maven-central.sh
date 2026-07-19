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
#   ./scripts/publish-to-maven-central.sh [--module MODULE] [--dry-run]
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

DRY_RUN=0
LOCAL_ONLY=0
SELECTED_MODULES=()

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

usage() {
    cat <<'USAGE'
Usage:
  ./scripts/publish-to-maven-central.sh [options]

Options:
  --module MODULE   Publish only one module. Can be provided multiple times.
  --local           Publish selected modules to Maven local instead of Central.
  --dry-run         Print the Gradle publish plan; no upload happens.
  -h, --help        Show this help.

Remote uploads still use manual Central Portal release mode. After upload, review
and publish the deployment at https://central.sonatype.com/publishing/deployments.
USAGE
}

contains_module() {
    local candidate="$1"
    local module
    for module in "${MODULES[@]}"; do
        [[ "$module" == "$candidate" ]] && return 0
    done
    return 1
}

parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --module)
                if [[ -z "${2:-}" ]]; then
                    echo_error "--module requires a module name"
                    exit 1
                fi
                if ! contains_module "$2"; then
                    echo_error "Unknown module: $2"
                    echo "Allowed modules: ${MODULES[*]}"
                    exit 1
                fi
                SELECTED_MODULES+=("$2")
                shift 2
                ;;
            --local)
                LOCAL_ONLY=1
                shift
                ;;
            --dry-run)
                DRY_RUN=1
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                echo_error "Unknown argument: $1"
                usage
                exit 1
                ;;
        esac
    done
}

# Check required credentials
check_credentials() {
    if [[ $LOCAL_ONLY -eq 1 || $DRY_RUN -eq 1 ]]; then
        echo_info "Skipping remote credential check"
        return
    fi

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

# Publish all modules
publish_all() {
    cd "$PROJECT_ROOT"

    local modules=("${MODULES[@]}")
    if [[ ${#SELECTED_MODULES[@]} -gt 0 ]]; then
        modules=("${SELECTED_MODULES[@]}")
    fi

    local gradle_tasks=()
    if [[ $LOCAL_ONLY -eq 0 && ${#SELECTED_MODULES[@]} -eq 0 ]]; then
        # Full publish goes through the root aggregate so the module list cannot drift.
        gradle_tasks+=("publishAllToMavenCentral")
    else
        local module
        for module in "${modules[@]}"; do
            if [[ $LOCAL_ONLY -eq 1 ]]; then
                gradle_tasks+=(":${module}:publishToMavenLocal")
            else
                gradle_tasks+=(":${module}:publishAllPublicationsToMavenCentralRepository")
            fi
        done
    fi

    local gradle_args=("--no-daemon")

    if [[ $LOCAL_ONLY -eq 1 ]]; then
        echo_info "Publishing modules to Maven local: ${modules[*]}"
    else
        echo_info "Uploading modules to Maven Central Portal: ${modules[*]}"
    fi
    echo ""

    if [[ $DRY_RUN -eq 1 ]]; then
        echo_info "Dry run. No Gradle publish tasks were invoked."
        printf './gradlew'
        printf ' %q' "${gradle_args[@]}" "${gradle_tasks[@]}"
        printf '\n'
        return
    fi

    ./gradlew "${gradle_args[@]}" "${gradle_tasks[@]}"

    echo_info "Gradle publish tasks completed successfully."
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

    parse_args "$@"
    check_credentials
    publish_all
}

main "$@"
