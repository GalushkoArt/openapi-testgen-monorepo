#!/usr/bin/env bash
#
# Consumer compatibility check.
#
# Publishes the full artifact stack to Maven Local and then runs the plugin's TestKit-based
# consumer compatibility tests, which consume the published artifacts like a real consumer:
#   - a consumer project per Gradle version (default matrix: 8.5, 8.14.5, 9.6.1),
#   - a consumer whose buildscript classpath requests an older Jackson (conflict resolves up),
#   - a consumer that forces Jackson down to the version swagger-parser builds against.
#
# Usage:
#   scripts/compat-check.sh
#   COMPAT_GRADLE_VERSIONS="8.5,9.6.1" scripts/compat-check.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "==> Publishing all modules to Maven Local"
./gradlew publishAllToMavenLocal

echo "==> Running consumer compatibility tests"
./gradlew :plugin:compatibilityTest ${COMPAT_GRADLE_VERSIONS:+-PcompatGradleVersions="$COMPAT_GRADLE_VERSIONS"}

echo "==> Consumer compatibility checks passed"
