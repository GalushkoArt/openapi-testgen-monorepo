#!/usr/bin/env bash
#
# Verify npm packages are correctly structured
#
# Usage:
#   ./npm/scripts/verify-packages.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
BUILD_DIR="$ROOT_DIR/cli/build/npm"

echo "================================================"
echo "Verifying npm packages"
echo "================================================"
echo ""

ERRORS=0

# Function to check if a file exists
check_file() {
  local file="$1"
  local desc="$2"
  if [[ -f "$file" ]]; then
    echo "  ✓ $desc"
  else
    echo "  ✗ $desc (MISSING: $file)"
    ERRORS=$((ERRORS + 1))
  fi
}

# Function to check if a file is executable
check_executable() {
  local file="$1"
  local desc="$2"
  if [[ -x "$file" ]]; then
    echo "  ✓ $desc (executable)"
  else
    echo "  ✗ $desc (NOT executable: $file)"
    ERRORS=$((ERRORS + 1))
  fi
}

# Verify main CLI package
echo "Checking @openapi-testgen/cli..."
if [[ -d "$BUILD_DIR/cli" ]]; then
  check_file "$BUILD_DIR/cli/package.json" "package.json"
  check_file "$BUILD_DIR/cli/README.md" "README.md"
  check_file "$BUILD_DIR/cli/LICENSE" "LICENSE"
  check_file "$BUILD_DIR/cli/lib/openapi-testgen.jar" "lib/openapi-testgen.jar"
  check_file "$BUILD_DIR/cli/bin/openapi-testgen" "bin/openapi-testgen"
  check_executable "$BUILD_DIR/cli/bin/openapi-testgen" "bin/openapi-testgen"
  check_file "$BUILD_DIR/cli/scripts/postinstall.js" "scripts/postinstall.js"

  # Verify package.json has correct version (not 0.0.0)
  VERSION=$(jq -r '.version' "$BUILD_DIR/cli/package.json")
  if [[ "$VERSION" == "0.0.0" ]]; then
    echo "  ✗ package.json version is still 0.0.0"
    ERRORS=$((ERRORS + 1))
  else
    echo "  ✓ package.json version: $VERSION"
  fi

  # Verify npm pack works
  echo ""
  echo "  Running npm pack --dry-run..."
  if (cd "$BUILD_DIR/cli" && npm pack --dry-run 2>&1 | head -20); then
    echo "  ✓ npm pack --dry-run succeeded"
  else
    echo "  ✗ npm pack --dry-run failed"
    ERRORS=$((ERRORS + 1))
  fi
else
  echo "  ✗ Package directory not found: $BUILD_DIR/cli"
  ERRORS=$((ERRORS + 1))
fi

echo ""

# Verify native packages
for platform in linux-x64 linux-arm64 darwin-arm64 win32-x64; do
  echo "Checking @openapi-testgen/cli-$platform..."

  if [[ -d "$BUILD_DIR/cli-$platform" ]]; then
    check_file "$BUILD_DIR/cli-$platform/package.json" "package.json"
    check_file "$BUILD_DIR/cli-$platform/LICENSE" "LICENSE"

    if [[ "$platform" == "win32-x64" ]]; then
      BINARY="bin/openapi-testgen.exe"
    else
      BINARY="bin/openapi-testgen"
    fi

    check_file "$BUILD_DIR/cli-$platform/$BINARY" "$BINARY"

    if [[ "$platform" != "win32-x64" ]]; then
      check_executable "$BUILD_DIR/cli-$platform/$BINARY" "$BINARY"
    fi

    # Verify package.json has correct version
    VERSION=$(jq -r '.version' "$BUILD_DIR/cli-$platform/package.json")
    if [[ "$VERSION" == "0.0.0" ]]; then
      echo "  ✗ package.json version is still 0.0.0"
      ERRORS=$((ERRORS + 1))
    else
      echo "  ✓ package.json version: $VERSION"
    fi

    # Verify npm pack works
    echo ""
    echo "  Running npm pack --dry-run..."
    if (cd "$BUILD_DIR/cli-$platform" && npm pack --dry-run 2>&1 | head -10); then
      echo "  ✓ npm pack --dry-run succeeded"
    else
      echo "  ✗ npm pack --dry-run failed"
      ERRORS=$((ERRORS + 1))
    fi
  else
    echo "  (skipped - not built)"
  fi

  echo ""
done

echo "================================================"
if [[ $ERRORS -eq 0 ]]; then
  echo "All checks passed!"
  exit 0
else
  echo "Found $ERRORS error(s)"
  exit 1
fi
