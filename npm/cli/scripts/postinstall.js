#!/usr/bin/env node

/**
 * Postinstall script for @openapi-testgen/cli
 *
 * This script provides informational messages about the installation.
 * It NEVER fails the installation - all errors are caught and logged as warnings.
 */

const { execSync } = require("child_process");

const NATIVE_PACKAGES = {
  "linux-x64": "@openapi-testgen/cli-linux-x64",
  "linux-arm64": "@openapi-testgen/cli-linux-arm64",
  "darwin-arm64": "@openapi-testgen/cli-darwin-arm64",
  "win32-x64": "@openapi-testgen/cli-win32-x64",
};

const platform = `${process.platform}-${process.arch}`;
const nativePackage = NATIVE_PACKAGES[platform];

/**
 * Check if a native package is installed.
 */
function isNativePackageInstalled(pkgName) {
  try {
    require.resolve(`${pkgName}/package.json`);
    return true;
  } catch {
    return false;
  }
}

/**
 * Get the installed Java version.
 */
function getJavaVersion() {
  try {
    const version = execSync("java -version 2>&1", { encoding: "utf8" });
    const match = version.match(/version "(\d+)/);
    return match ? parseInt(match[1], 10) : 0;
  } catch {
    return 0;
  }
}

/**
 * Check if the system uses glibc (vs musl or other libc).
 * Returns true if glibc is detected, false otherwise.
 */
function hasGlibc() {
  if (process.platform !== "linux") return false;
  try {
    // ldd --version outputs "ldd (GNU libc)" or similar for glibc systems
    const output = execSync("ldd --version 2>&1", { encoding: "utf8" });
    return /glibc|gnu libc/i.test(output);
  } catch {
    return false;
  }
}

// Main postinstall logic
try {
  console.log("");
  console.log("@openapi-testgen/cli installed successfully!");
  console.log("");

  // Check for native binary
  if (nativePackage) {
    if (isNativePackageInstalled(nativePackage)) {
      // Check glibc presence for Linux ARM64 (native binary requires glibc, not musl)
      if (platform === "linux-arm64" && !hasGlibc()) {
        console.log(`  Native binary installed from ${nativePackage}`);
        console.warn("  Warning: glibc not detected (musl or other libc).");
        console.warn("  The native binary may not work. Will fall back to JAR at runtime.");
        const javaVersion = getJavaVersion();
        if (javaVersion >= 21) {
          console.log(`  Java ${javaVersion} detected - JAR fallback available.`);
        } else {
          console.warn("  Please install Java 21+ for the JAR fallback to work.");
        }
      } else {
        console.log(`  Using native binary from ${nativePackage}`);
        console.log("  No Java required.");
      }
    } else {
      // Native package not installed - check Java fallback
      const javaVersion = getJavaVersion();

      if (javaVersion >= 21) {
        console.log(`  Using JAR-based CLI with Java ${javaVersion}`);
        console.log("");
        console.log(
          "  Tip: Install the native package for faster startup:"
        );
        console.log(`    npm install -g ${nativePackage}`);
      } else if (javaVersion > 0) {
        console.warn(
          `  Warning: Java ${javaVersion} detected, but Java 21+ is required.`
        );
        console.warn("  The CLI will not work until you:");
        console.warn("    - Upgrade to Java 21 or later, OR");
        console.warn(`    - Install the native package: npm install -g ${nativePackage}`);
      } else {
        console.warn("  Warning: Java not found in PATH.");
        console.warn("  The CLI requires Java 21+ to run.");
        console.warn("");
        console.warn("  Options:");
        console.warn("    - Install Java 21 or later, OR");
        console.warn(`    - Install the native package: npm install -g ${nativePackage}`);
      }
    }
  } else {
    // Unsupported platform - must use Java
    const javaVersion = getJavaVersion();

    console.log(`  Platform: ${platform} (no native binary available)`);

    if (javaVersion >= 21) {
      console.log(`  Using JAR-based CLI with Java ${javaVersion}`);
    } else if (javaVersion > 0) {
      console.warn(
        `  Warning: Java ${javaVersion} detected, but Java 21+ is required.`
      );
      console.warn("  Please upgrade to Java 21 or later.");
    } else {
      console.warn("  Warning: Java not found in PATH.");
      console.warn("  The CLI requires Java 21+ to run. Please install Java.");
    }
  }

  console.log("");
  console.log("  Run 'openapi-testgen --help' to get started.");
  console.log("");
} catch (error) {
  // Never fail the installation
  // Just silently continue - the user will see errors when they try to run the CLI
}
