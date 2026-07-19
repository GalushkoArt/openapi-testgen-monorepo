#!/usr/bin/env bash
#
# Validate local release metadata and optionally run the release verification build.
#
# This script does not tag, publish, or create releases.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

VERSION=""
SKIP_GRADLE=false
ALLOW_DIRTY=false
INCLUDE_NATIVE=false

usage() {
    cat <<'USAGE'
Usage:
  ./scripts/release-preflight.sh [VERSION] [options]

Options:
  --skip-gradle       Only validate metadata; do not run Gradle checks.
  --allow-dirty       Do not fail when the git working tree has changes.
  --include-native    Also run :cli:testNative (requires GraalVM/native-image).
  -h, --help          Show this help.
USAGE
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-gradle)
            SKIP_GRADLE=true
            shift
            ;;
        --allow-dirty)
            ALLOW_DIRTY=true
            shift
            ;;
        --include-native)
            INCLUDE_NATIVE=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        -*)
            echo "Unknown option: $1" >&2
            usage >&2
            exit 1
            ;;
        *)
            if [[ -n "$VERSION" ]]; then
                echo "Unexpected argument: $1" >&2
                usage >&2
                exit 1
            fi
            VERSION="$1"
            shift
            ;;
    esac
done

catalog_version() {
    sed -nE 's/^openapi-testgen[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$PROJECT_ROOT/gradle/libs.versions.toml"
}

CATALOG_VERSION="$(catalog_version)"
if [[ -z "$VERSION" ]]; then
    VERSION="$CATALOG_VERSION"
fi

if [[ -z "$VERSION" ]]; then
    echo "Could not determine release version." >&2
    exit 1
fi

if [[ "$VERSION" != "$CATALOG_VERSION" ]]; then
    echo "Version mismatch: requested $VERSION, but gradle/libs.versions.toml has $CATALOG_VERSION." >&2
    exit 1
fi

if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]]; then
    echo "Version does not look like a SemVer release: $VERSION" >&2
    exit 1
fi

if ! grep -Eq "^##[[:space:]]+$VERSION([[:space:]]|\$)" "$PROJECT_ROOT/CHANGELOG.md"; then
    echo "CHANGELOG.md does not contain a '## $VERSION' section." >&2
    exit 1
fi

if [[ "$ALLOW_DIRTY" != true && -n "$(git -C "$PROJECT_ROOT" status --short)" ]]; then
    echo "Working tree is not clean. Commit or stash changes, or rerun with --allow-dirty." >&2
    git -C "$PROJECT_ROOT" status --short >&2
    exit 1
fi

echo "Release metadata looks valid for $VERSION."

if [[ "$SKIP_GRADLE" == true ]]; then
    exit 0
fi

# Root `build` aggregates every included build's `check` (see root build.gradle.kts)
# plus the samples' `build` via task-name matching.
GRADLE_TASKS=(
    "build"
    ":cli:testFatJar"
    "docsBuild"
)

if [[ "$INCLUDE_NATIVE" == true ]]; then
    GRADLE_TASKS+=(":cli:testNative")
fi

cd "$PROJECT_ROOT"
./gradlew "${GRADLE_TASKS[@]}"

# Consumer compatibility: publishes the stack to Maven Local and consumes the published plugin
# from real consumer projects (Gradle version matrix + consumer-forced Jackson versions).
"$SCRIPT_DIR/compat-check.sh"
