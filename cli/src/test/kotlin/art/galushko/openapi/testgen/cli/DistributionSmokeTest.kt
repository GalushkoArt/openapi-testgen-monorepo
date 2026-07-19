package art.galushko.openapi.testgen.cli

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * Smoke tests that verify fat JAR and native binary outputs match expected fixtures and each other.
 *
 * These tests require the following environment variables:
 * - `TEST_FATJAR_PATH`: Path to the fat JAR file
 * - `TEST_NATIVE_PATH`: Path to the native binary
 *
 * Run with:
 * - `./gradlew :cli:testFatJar` for fat JAR tests
 * - `./gradlew :cli:testNative` for native binary tests
 * - `./gradlew :cli:testDistributions` for combined validation
 */
internal class DistributionSmokeTest {

    @Tag("fat-jar")
    @ParameterizedTest
    @CsvSource(value = [
        "openapi-31.yaml,openapi-31-test-suites.json",
        "openapi-30.yaml,openapi-30-test-suites.json",
        "swagger-20.yaml,swagger-20-test-suites.json",
    ])
    fun `fat JAR produces expected output`(spec: String, expected: String, @TempDir tmp: Path) {
        val fatJarPath = System.getenv("TEST_FATJAR_PATH")
        val specPath = resolveTestResource(spec)
        val outputDir = tmp.resolve("fatjar-out")
        Files.createDirectories(outputDir)

        val process = ProcessBuilder(
            "java", "-jar", fatJarPath,
            "--spec-file", specPath,
            "--output-dir", outputDir.toString(),
            "--generator", "test-suite-writer",
            "--setting", "validSecurityValues.ApiKeyAuth=test-api-key-123",
            "--setting", "ignoreTestCases./orders.post[]=Incorrect Request Body: Missed Required Object Properties paymentMethod",
            "--generator-option", "outputFileName=generated.json",
        )
            .redirectErrorStream(true)
            .start()

        val completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertTrue(completed, "Fat JAR execution timed out")
        assertEquals(0, process.exitValue(), "Fat JAR exited with non-zero code: ${process.inputStream.bufferedReader().readText()}")

        val outputFile = outputDir.resolve("generated.json")
        assertTrue(Files.exists(outputFile), "Output file not created at $outputFile")

        val expected = loadExpectedOutput(expected)
        val actual = Files.readString(outputFile).trim()
        assertEquals(expected.lines(), actual.lines(), "Output mismatch for fat JAR")
    }

    @ParameterizedTest
    @CsvSource(value = [
        "openapi-31.yaml,openapi-31-test-suites.json",
        "openapi-30.yaml,openapi-30-test-suites.json",
        "swagger-20.yaml,swagger-20-test-suites.json",
    ])
    @Tag("native-binary")
    fun `native binary produces expected output`(spec: String, expected: String, @TempDir tmp: Path) {
        val nativePath = System.getenv("TEST_NATIVE_PATH")
        val specPath = resolveTestResource(spec)
        val outputDir = tmp.resolve("native-out")
        Files.createDirectories(outputDir)

        val process = ProcessBuilder(
            nativePath,
            "--spec-file", specPath,
            "--output-dir", outputDir.toString(),
            "--generator", "test-suite-writer",
            "--setting", "validSecurityValues.ApiKeyAuth=test-api-key-123",
            "--setting", "ignoreTestCases./orders.post[]=Incorrect Request Body: Missed Required Object Properties paymentMethod",
            "--generator-option", "outputFileName=generated.json",
        )
            .redirectErrorStream(true)
            .start()

        val completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertTrue(completed, "Native binary execution timed out")
        assertEquals(0, process.exitValue(), "Native binary exited with non-zero code: ${process.inputStream.bufferedReader().readText()}")

        val outputFile = outputDir.resolve("generated.json")
        assertTrue(Files.exists(outputFile), "Output file not created at $outputFile")

        val expected = loadExpectedOutput(expected)
        val actual = Files.readString(outputFile).trim()
        assertEquals(expected.lines(), actual.lines(), "Output mismatch for native binary")
    }

    @ParameterizedTest
    @CsvSource(value = [
        "openapi-31.yaml",
        "swagger-20.yaml",
    ])
    @Tag("fat-jar")
    @Tag("native-binary")
    fun `native and fat JAR produce identical output`(spec: String, @TempDir tmp: Path) {
        val fatJarPath = System.getenv("TEST_FATJAR_PATH")
        val nativePath = System.getenv("TEST_NATIVE_PATH")
        val specPath = resolveTestResource(spec)

        val fatJarOut = tmp.resolve("fatjar")
        val nativeOut = tmp.resolve("native")
        Files.createDirectories(fatJarOut)
        Files.createDirectories(nativeOut)

        // Run fat JAR
        runProcess(
            listOf(
                "java", "-jar", fatJarPath,
                "--spec-file", specPath,
                "--output-dir", fatJarOut.toString(),
                "--generator", "test-suite-writer",
                "--setting", "validSecurityValues.ApiKeyAuth=test-key",
                "--generator-option", "outputFileName=result.json",
            ),
            "fat JAR",
        )

        // Run native binary
        runProcess(
            listOf(
                nativePath,
                "--spec-file", specPath,
                "--output-dir", nativeOut.toString(),
                "--generator", "test-suite-writer",
                "--setting", "validSecurityValues.ApiKeyAuth=test-key",
                "--generator-option", "outputFileName=result.json",
            ),
            "native binary",
        )

        val fatJarContent = Files.readString(fatJarOut.resolve("result.json"))
        val nativeContent = Files.readString(nativeOut.resolve("result.json"))

        assertEquals(
            fatJarContent,
            nativeContent,
            "Fat JAR and native binary produced different output",
        )
    }

    @Test
    @Tag("fat-jar")
    fun `fat JAR help command works`() {
        val fatJarPath = System.getenv("TEST_FATJAR_PATH")

        val process = ProcessBuilder("java", "-jar", fatJarPath, "--help")
            .redirectErrorStream(true)
            .start()

        val completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertTrue(completed, "Fat JAR help command timed out")

        val output = process.inputStream.bufferedReader().readText()
        assertEquals(0, process.exitValue(), "Fat JAR help exited with non-zero code: $output")
        assertTrue(output.contains("Usage:"), "Help output should contain 'Usage:'")
    }

    @Test
    @Tag("native-binary")
    fun `native binary help command works`() {
        val nativePath = System.getenv("TEST_NATIVE_PATH")

        val process = ProcessBuilder(nativePath, "--help")
            .redirectErrorStream(true)
            .start()

        val completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertTrue(completed, "Native binary help command timed out")

        val output = process.inputStream.bufferedReader().readText()
        assertEquals(0, process.exitValue(), "Native binary help exited with non-zero code: $output")
        assertTrue(output.contains("Usage:"), "Help output should contain 'Usage:'")
    }

    private fun runProcess(command: List<String>, description: String) {
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertTrue(completed, "$description execution timed out")

        val output = process.inputStream.bufferedReader().readText()
        assertEquals(0, process.exitValue(), "$description failed with exit code ${process.exitValue()}: $output")
    }

    private fun resolveTestResource(name: String): String {
        val url = requireNotNull(this::class.java.classLoader.getResource(name)) {
            "Test resource not found: $name"
        }
        // Use Paths.get(URI) for proper cross-platform path handling (Windows compatibility)
        return Paths.get(url.toURI()).toString()
    }

    private fun loadExpectedOutput(expected: String): String {
        return requireNotNull(this::class.java.classLoader.getResourceAsStream(expected)) {
            "Expected output resource not found"
        }.bufferedReader().readText().trim()
    }

    private companion object {
        const val TIMEOUT_SECONDS = 60L
    }
}
