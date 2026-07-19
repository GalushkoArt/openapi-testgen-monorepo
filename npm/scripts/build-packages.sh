#!/usr/bin/env bash
#
# Build npm packages for @openapi-testgen/cli
#
# Usage:
#   ./npm/scripts/build-packages.sh [VERSION]
#
# Environment variables:
#   NATIVE_DIR - Path to directory containing native binaries (optional)
#                Expected structure: native-linux-x64/, native-linux-arm64/, native-darwin-arm64/, native-win32-x64/
#   FAT_JAR - Path to a prebuilt fat JAR (optional)
#   FAT_JAR_DIR - Directory to search for a prebuilt fat JAR (optional)
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
NPM_DIR="$ROOT_DIR/npm"
BUILD_DIR="$ROOT_DIR/cli/build/npm"
NATIVE_DIR="${NATIVE_DIR:-}"
FAT_JAR="${FAT_JAR:-}"
FAT_JAR_DIR="${FAT_JAR_DIR:-}"

# Get version from argument or extract from libs.versions.toml
if [[ $# -ge 1 ]]; then
  VERSION="$1"
else
  VERSION=$(grep -E '^openapi-testgen\s*=' "$ROOT_DIR/gradle/libs.versions.toml" | sed 's/.*"\(.*\)".*/\1/')
  if [[ -z "$VERSION" ]]; then
    echo "Error: Could not determine version from gradle/libs.versions.toml"
    exit 1
  fi
fi

echo "================================================"
echo "Building npm packages for version $VERSION"
echo "================================================"
echo ""

# Check for jq (required for portable JSON manipulation)
if ! command -v jq &> /dev/null; then
  echo "Error: jq is required but not installed."
  echo "Install it with:"
  echo "  - macOS: brew install jq"
  echo "  - Ubuntu/Debian: apt-get install jq"
  echo "  - Windows: choco install jq"
  exit 1
fi

# Resolve fat JAR (prefer prebuilt artifacts)
resolve_fat_jar() {
  if [[ -n "$FAT_JAR" ]]; then
    if [[ ! -f "$FAT_JAR" ]]; then
      echo "Error: FAT_JAR set but file not found: $FAT_JAR"
      exit 1
    fi
    return 0
  fi

  # Only accept the jar matching VERSION so a stale build can never be relabeled as this release.
  if [[ -n "$FAT_JAR_DIR" ]]; then
    local candidate
    candidate=$(find "$FAT_JAR_DIR" -name "openapi-testgen-${VERSION}-all.jar" | head -1 || true)
    if [[ -n "$candidate" && -f "$candidate" ]]; then
      FAT_JAR="$candidate"
      return 0
    fi
    echo "Error: openapi-testgen-${VERSION}-all.jar not found in $FAT_JAR_DIR"
    return 1
  fi

  local from_build
  from_build=$(find "$ROOT_DIR/cli/build/libs" -name "openapi-testgen-${VERSION}-all.jar" | head -1 || true)
  if [[ -n "$from_build" && -f "$from_build" ]]; then
    FAT_JAR="$from_build"
    return 0
  fi

  return 1
}

if resolve_fat_jar; then
  echo "Using prebuilt fat JAR: $FAT_JAR"
  echo ""
else
  echo "Error: openapi-testgen-${VERSION}-all.jar not found in cli/build/libs/"
  exit 1
fi

# Prepare main CLI package (JAR-based)
echo "Preparing @openapi-testgen/cli package..."
rm -rf "$BUILD_DIR/cli"
mkdir -p "$BUILD_DIR/cli/lib" "$BUILD_DIR/cli/bin" "$BUILD_DIR/cli/scripts"

# Copy package files
cp "$NPM_DIR/cli/package.json" "$BUILD_DIR/cli/"
cp "$NPM_DIR/cli/README.md" "$BUILD_DIR/cli/"
cp "$NPM_DIR/cli/bin/openapi-testgen" "$BUILD_DIR/cli/bin/"
cp "$NPM_DIR/cli/scripts/postinstall.js" "$BUILD_DIR/cli/scripts/"
cp "$ROOT_DIR/LICENSE" "$BUILD_DIR/cli/"

# Copy fat JAR
cp "$FAT_JAR" "$BUILD_DIR/cli/lib/openapi-testgen.jar"

# Make launcher executable
chmod +x "$BUILD_DIR/cli/bin/openapi-testgen"

# Update version in package.json using jq
jq --arg v "$VERSION" '
  .version = $v |
  .optionalDependencies["@openapi-testgen/cli-linux-x64"] = $v |
  .optionalDependencies["@openapi-testgen/cli-linux-arm64"] = $v |
  .optionalDependencies["@openapi-testgen/cli-darwin-arm64"] = $v |
  .optionalDependencies["@openapi-testgen/cli-win32-x64"] = $v
' "$BUILD_DIR/cli/package.json" > "$BUILD_DIR/cli/package.json.tmp"
mv "$BUILD_DIR/cli/package.json.tmp" "$BUILD_DIR/cli/package.json"

echo "  Created: $BUILD_DIR/cli/"
echo ""

# Prepare native packages if binaries are provided
if [[ -n "$NATIVE_DIR" && -d "$NATIVE_DIR" ]]; then
  echo "Preparing native packages from $NATIVE_DIR..."
  echo ""

  for platform in linux-x64 linux-arm64 darwin-arm64 win32-x64; do
    PLATFORM_DIR="$NATIVE_DIR/native-$platform"

    if [[ ! -d "$PLATFORM_DIR" ]]; then
      echo "  Warning: $PLATFORM_DIR not found, skipping cli-$platform"
      continue
    fi

    echo "  Preparing @openapi-testgen/cli-$platform..."

    rm -rf "$BUILD_DIR/cli-$platform"
    mkdir -p "$BUILD_DIR/cli-$platform/bin"

    # Copy package.json
    cp "$NPM_DIR/cli-$platform/package.json" "$BUILD_DIR/cli-$platform/"
    cp "$ROOT_DIR/LICENSE" "$BUILD_DIR/cli-$platform/"

    # Copy native binary
    if [[ "$platform" == "win32-x64" ]]; then
      BINARY_NAME="openapi-testgen.exe"
    else
      BINARY_NAME="openapi-testgen"
    fi

    if [[ -f "$PLATFORM_DIR/$BINARY_NAME" ]]; then
      cp "$PLATFORM_DIR/$BINARY_NAME" "$BUILD_DIR/cli-$platform/bin/"
      if [[ "$platform" != "win32-x64" ]]; then
        chmod +x "$BUILD_DIR/cli-$platform/bin/$BINARY_NAME"
      fi
    else
      echo "    Warning: $PLATFORM_DIR/$BINARY_NAME not found"
      continue
    fi

    # Update version in package.json using jq
    jq --arg v "$VERSION" '.version = $v' \
      "$BUILD_DIR/cli-$platform/package.json" > "$BUILD_DIR/cli-$platform/package.json.tmp"
    mv "$BUILD_DIR/cli-$platform/package.json.tmp" "$BUILD_DIR/cli-$platform/package.json"

    echo "    Created: $BUILD_DIR/cli-$platform/"
  done
else
  echo "Skipping native packages (set NATIVE_DIR to include them)."
  echo "Example: NATIVE_DIR=/path/to/binaries ./npm/scripts/build-packages.sh"
fi

echo ""
echo "================================================"
echo "Packages prepared at: $BUILD_DIR"
echo "================================================"
echo ""
echo "To verify:"
echo "  cd $BUILD_DIR/cli && npm pack --dry-run"
echo ""
echo "To publish:"
echo "  cd $BUILD_DIR/cli && npm publish --access public"
echo ""
