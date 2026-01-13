package art.galushko.openapi.testgen.cli

import art.galushko.openapi.testgen.model.TestSuite
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class CliIncludeValidCaseTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `should include valid case with 2xx status when enabled`(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi.yaml")).toURI()
        val out = tmp.resolve("out-include")
        Files.createDirectories(out)

        val exitCode = picocli.CommandLine(GenerateCommand()).execute(
            "--spec-file", Path.of(spec).toString(),
            "--output-dir", out.toString(),
            "--generator", "test-suite-writer",
            "--generator-option", "outputFileName=generated.json",
            "--setting", "includeValidCase=true",
            "--log-level", "INFO",
        )
        assertEquals(0, exitCode)

        val suites = readSuites(out.resolve("generated.json"))
        val validCases = suites.values.flatMap { it.testCases }.filter { it.name == "Test Valid Case" }
        assertTrue(validCases.isNotEmpty(), "Expected at least one valid case in output")
        validCases.forEach { validCase ->
            assertTrue(
                validCase.expectedStatusCode in 200..299,
                "Expected 2xx status for valid case, got ${validCase.expectedStatusCode}",
            )
        }

        suites.values.forEach { suite ->
            assertTrue(suite.testCases.isNotEmpty(), "Expected non-empty test cases for ${suite.operationName}")
            assertTrue(
                suite.testCases.any { it.name == "Test Valid Case" },
                "Expected valid case for ${suite.operationName}",
            )
        }
    }

    @Test
    fun `should not include valid case when setting is false`(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi.yaml")).toURI()
        val out = tmp.resolve("out-exclude")
        Files.createDirectories(out)

        val exitCode = picocli.CommandLine(GenerateCommand()).execute(
            "--spec-file", Path.of(spec).toString(),
            "--output-dir", out.toString(),
            "--generator", "test-suite-writer",
            "--generator-option", "outputFileName=generated.json",
            "--setting", "includeValidCase=false",
            "--log-level", "INFO",
        )
        assertEquals(0, exitCode)

        val suites = readSuites(out.resolve("generated.json"))
        val validCases = suites.values.flatMap { it.testCases }.filter { it.name == "Test Valid Case" }
        assertTrue(validCases.isEmpty(), "Did not expect valid cases when includeValidCase is false")
    }

    private fun readSuites(outputFile: Path): Map<String, TestSuite> {
        assertTrue(Files.exists(outputFile), "Expected output file at $outputFile")
        return objectMapper.readValue(outputFile.toFile())
    }
}
