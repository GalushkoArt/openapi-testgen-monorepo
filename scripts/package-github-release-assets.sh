#!/usr/bin/env bash
#
# Build manually uploaded GitHub release ZIP assets from already built CLI artifacts.
#
# This script does not create a GitHub release and does not upload assets. It only
# prepares ZIP files and SHA256SUMS for manual inspection and upload.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

VERSION=""
OUTPUT_DIR="$PROJECT_ROOT/cli/build/release-assets"
FAT_JAR=""
FAT_JAR_DIR=""
NATIVE_DIR=""
ALLOW_MISSING_NATIVE=false

usage() {
    cat <<'USAGE'
Usage:
  ./scripts/package-github-release-assets.sh [VERSION] [options]

Options:
  --version VERSION          Release version. Defaults to gradle/libs.versions.toml.
  --output-dir DIR          Directory for ZIP assets. Default: cli/build/release-assets.
  --fat-jar FILE            Prebuilt openapi-testgen-*-all.jar.
  --fat-jar-dir DIR         Directory to search for a prebuilt fat JAR.
  --native-dir DIR          Directory containing native-* artifact directories.
  --allow-missing-native    Package available native binaries instead of failing.
  -h, --help                Show this help.

Expected native artifact directories:
  native-linux-x64/openapi-testgen
  native-linux-arm64/openapi-testgen
  native-darwin-arm64/openapi-testgen
  native-win32-x64/openapi-testgen.exe
USAGE
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --version)
            VERSION="${2:-}"
            shift 2
            ;;
        --output-dir)
            OUTPUT_DIR="${2:-}"
            shift 2
            ;;
        --fat-jar)
            FAT_JAR="${2:-}"
            shift 2
            ;;
        --fat-jar-dir)
            FAT_JAR_DIR="${2:-}"
            shift 2
            ;;
        --native-dir)
            NATIVE_DIR="${2:-}"
            shift 2
            ;;
        --allow-missing-native)
            ALLOW_MISSING_NATIVE=true
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

require_command() {
    if ! command -v "$1" > /dev/null 2>&1; then
        echo "Required command not found: $1" >&2
        exit 1
    fi
}

require_command zip

resolve_dir() {
    local dir="$1"
    mkdir -p "$dir"
    (cd "$dir" && pwd)
}

resolve_existing_dir() {
    local dir="$1"
    if [[ ! -d "$dir" ]]; then
        echo "Directory not found: $dir" >&2
        exit 1
    fi
    (cd "$dir" && pwd)
}

OUTPUT_DIR="$(resolve_dir "$OUTPUT_DIR")"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

resolve_fat_jar() {
    if [[ -n "$FAT_JAR" ]]; then
        [[ -f "$FAT_JAR" ]] || {
            echo "Fat JAR not found: $FAT_JAR" >&2
            return 1
        }
        return 0
    fi

    local candidate=""
    if [[ -n "$FAT_JAR_DIR" ]]; then
        [[ -d "$FAT_JAR_DIR" ]] || {
            echo "Fat JAR directory not found: $FAT_JAR_DIR" >&2
            return 1
        }
        # Only accept the jar matching VERSION so a stale build can never be relabeled as this release.
        candidate="$(find "$FAT_JAR_DIR" -name "openapi-testgen-${VERSION}-all.jar" -print -quit)"
    fi

    if [[ -z "$candidate" ]]; then
        candidate="$(find "$PROJECT_ROOT/cli/build/libs" -name "openapi-testgen-${VERSION}-all.jar" -print -quit 2>/dev/null || true)"
    fi

    if [[ -z "$candidate" || ! -f "$candidate" ]]; then
        echo "Fat JAR not found. Run ./gradlew :cli:shadowJar or pass --fat-jar/--fat-jar-dir." >&2
        return 1
    fi

    FAT_JAR="$candidate"
}

zip_dir() {
    local source_dir="$1"
    local zip_file="$2"
    local parent
    parent="$(dirname "$source_dir")"
    local name
    name="$(basename "$source_dir")"

    rm -f "$zip_file"
    (cd "$parent" && zip -qr "$zip_file" "$name")
}

write_jvm_launchers() {
    local dist_dir="$1"
    local jar_name="$2"

    mkdir -p "$dist_dir/bin"

    cat > "$dist_dir/bin/openapi-testgen" <<SCRIPT
#!/usr/bin/env sh
set -eu
APP_HOME=\$(CDPATH= cd -- "\$(dirname -- "\$0")/.." && pwd)
exec java -jar "\$APP_HOME/lib/$jar_name" "\$@"
SCRIPT
    chmod +x "$dist_dir/bin/openapi-testgen"

    cat > "$dist_dir/bin/openapi-testgen.bat" <<SCRIPT
@echo off
set APP_HOME=%~dp0..
java -jar "%APP_HOME%\\lib\\$jar_name" %*
SCRIPT
}

package_jvm_distribution() {
    resolve_fat_jar

    local dist_name="openapi-testgen-$VERSION"
    local dist_dir="$WORK_DIR/$dist_name"
    local jar_name="openapi-testgen-$VERSION-all.jar"

    mkdir -p "$dist_dir/lib"
    cp "$FAT_JAR" "$dist_dir/lib/$jar_name"
    cp "$PROJECT_ROOT/LICENSE" "$dist_dir/LICENSE"
    write_jvm_launchers "$dist_dir" "$jar_name"

    zip_dir "$dist_dir" "$OUTPUT_DIR/$dist_name.zip"
}

native_binary_path() {
    local artifact_dir="$1"
    local npm_platform="$2"
    local binary_name="$3"

    local candidates=(
        "$artifact_dir/native-$npm_platform/$binary_name"
        "$artifact_dir/$npm_platform/$binary_name"
        "$artifact_dir/cli-$npm_platform/bin/$binary_name"
    )

    for candidate in "${candidates[@]}"; do
        if [[ -f "$candidate" ]]; then
            echo "$candidate"
            return 0
        fi
    done

    return 1
}

package_native_distribution() {
    local npm_platform="$1"
    local asset_platform="$2"
    local binary_name="$3"

    if [[ -z "$NATIVE_DIR" ]]; then
        return 1
    fi

    local native_root
    native_root="$(resolve_existing_dir "$NATIVE_DIR")"
    local binary_path
    if ! binary_path="$(native_binary_path "$native_root" "$npm_platform" "$binary_name")"; then
        return 1
    fi

    local dist_name="openapi-testgen-$VERSION-$asset_platform"
    local dist_dir="$WORK_DIR/$dist_name"
    mkdir -p "$dist_dir"
    cp "$binary_path" "$dist_dir/$binary_name"
    cp "$PROJECT_ROOT/LICENSE" "$dist_dir/LICENSE"
    if [[ "$binary_name" != *.exe ]]; then
        chmod +x "$dist_dir/$binary_name"
    fi

    zip_dir "$dist_dir" "$OUTPUT_DIR/$dist_name.zip"
}

write_checksums() {
    local checksum_file="$OUTPUT_DIR/SHA256SUMS"
    rm -f "$checksum_file"

    if command -v sha256sum > /dev/null 2>&1; then
        (cd "$OUTPUT_DIR" && sha256sum *.zip > "$checksum_file")
    elif command -v shasum > /dev/null 2>&1; then
        (cd "$OUTPUT_DIR" && shasum -a 256 *.zip > "$checksum_file")
    else
        echo "Neither sha256sum nor shasum is available; skipping SHA256SUMS." >&2
        return 0
    fi
}

echo "Packaging GitHub release assets for $VERSION"
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

package_jvm_distribution

missing_native=0
for spec in \
    "linux-x64 linux-amd64 openapi-testgen" \
    "linux-arm64 linux-arm64 openapi-testgen" \
    "darwin-arm64 macos-arm64 openapi-testgen" \
    "win32-x64 windows-amd64 openapi-testgen.exe"; do
    read -r npm_platform asset_platform binary_name <<< "$spec"
    if ! package_native_distribution "$npm_platform" "$asset_platform" "$binary_name"; then
        echo "Missing native binary for $asset_platform" >&2
        missing_native=$((missing_native + 1))
    fi
done

if [[ "$missing_native" -gt 0 && "$ALLOW_MISSING_NATIVE" != true ]]; then
    echo "Missing $missing_native native binary asset(s). Use --allow-missing-native for local partial packaging." >&2
    exit 1
fi

write_checksums

echo ""
echo "Created release assets:"
find "$OUTPUT_DIR" -maxdepth 1 -type f -print | sort
echo ""
echo "Manual next step: inspect these files, then upload them to a GitHub release by hand."
