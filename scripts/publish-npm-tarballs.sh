#!/usr/bin/env bash
#
# Publish prepared npm tarballs in dependency order.
#
# This script must be run manually. Without --yes it only prints the publish
# plan, so CI cannot accidentally publish packages.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

VERSION=""
TARBALL_DIR="$PROJECT_ROOT/cli/build/npm-tarballs"
DRY_RUN=false
YES=false

usage() {
    cat <<'USAGE'
Usage:
  ./scripts/publish-npm-tarballs.sh [VERSION] [options]

Options:
  --tarball-dir DIR   Directory containing npm .tgz files.
  --dry-run           Pass --dry-run to npm publish.
  --yes               Actually publish. Required unless --dry-run is used.
  -h, --help          Show this help.

Packages are published in this order:
  1. @openapi-testgen/cli-linux-x64
  2. @openapi-testgen/cli-linux-arm64
  3. @openapi-testgen/cli-darwin-arm64
  4. @openapi-testgen/cli-win32-x64
  5. @openapi-testgen/cli
USAGE
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --tarball-dir)
            TARBALL_DIR="${2:-}"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --yes)
            YES=true
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

if [[ -z "$VERSION" ]]; then
    VERSION="$(catalog_version)"
fi

if [[ -z "$VERSION" ]]; then
    echo "Could not determine release version." >&2
    exit 1
fi

if [[ ! -d "$TARBALL_DIR" ]]; then
    echo "Tarball directory not found: $TARBALL_DIR" >&2
    exit 1
fi

TARBALL_DIR="$(cd "$TARBALL_DIR" && pwd)"

tarballs=(
    "$TARBALL_DIR/openapi-testgen-cli-linux-x64-$VERSION.tgz"
    "$TARBALL_DIR/openapi-testgen-cli-linux-arm64-$VERSION.tgz"
    "$TARBALL_DIR/openapi-testgen-cli-darwin-arm64-$VERSION.tgz"
    "$TARBALL_DIR/openapi-testgen-cli-win32-x64-$VERSION.tgz"
    "$TARBALL_DIR/openapi-testgen-cli-$VERSION.tgz"
)

missing=0
for tarball in "${tarballs[@]}"; do
    if [[ ! -f "$tarball" ]]; then
        echo "Missing tarball: $tarball" >&2
        missing=$((missing + 1))
    fi
done

if [[ "$missing" -gt 0 ]]; then
    exit 1
fi

echo "npm publish plan for $VERSION:"
printf '  %s\n' "${tarballs[@]}"
echo ""

if [[ "$DRY_RUN" == true ]]; then
    publish_args=(--access public --dry-run)
elif [[ "$YES" != true ]]; then
    echo "No packages published. Rerun with --yes to publish or --dry-run to validate with npm."
    exit 0
else
    publish_args=(--access public)
fi

if [[ "$DRY_RUN" != true ]]; then
    npm whoami > /dev/null
fi

for tarball in "${tarballs[@]}"; do
    npm publish "$tarball" "${publish_args[@]}"
done
