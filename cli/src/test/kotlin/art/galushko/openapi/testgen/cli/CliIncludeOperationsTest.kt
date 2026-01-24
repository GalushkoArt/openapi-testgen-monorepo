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

class CliIncludeOperationsTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `should filter operations using includeOperations via setting`(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi.yaml")).toURI()
        val out = tmp.resolve("out-include-setting")
        Files.createDirectories(out)

        val exitCode = picocli.CommandLine(GenerateCommand()).execute(
            "--spec-file", Path.of(spec).toString(),
            "--output-dir", out.toString(),
            "--generator", "test-suite-writer",
            "--generator-option", "outputFileName=generated.json",
            "--setting", "includeOperations./users/{userId}[]=GET",
            "--log-level", "INFO",
        )
        assertEquals(0, exitCode)

        val suites = readSuites(out.resolve("generated.json"))
        assertEquals(setOf("getUser"), suites.keys)
        val suite = suites.getValue("getUser")
        assertEquals("/users/{userId}", suite.path)
        assertEquals("GET", suite.method)
        assertTrue(suite.testCases.isNotEmpty(), "Expected test cases for getUser")
    }

    @Test
    fun `should filter operations using includeOperations via config file`(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi.yaml")).toURI()
        val specPath = Path.of(spec).toString()
        val out = tmp.resolve("out-include-config")
        Files.createDirectories(out)

        val configFile = tmp.resolve("config.yaml")
        Files.writeString(
            configFile,
            """
                specFile: '$specPath'
                outputDir: '$out'
                generator: 'test-suite-writer'
                logLevel: 'INFO'
                alwaysWriteTests: true
                generatorOptions:
                  outputFileName: 'generated.json'
                testGenerationSettings:
                  includeOperations:
                    "/orders":
                      - "POST"
            """.trimIndent(),
        )

        val exitCode = picocli.CommandLine(GenerateCommand()).execute(
            "--config-file", configFile.toString(),
        )
        assertEquals(0, exitCode)

        val suites = readSuites(out.resolve("generated.json"))
        assertEquals(setOf("createOrder"), suites.keys)
        val suite = suites.getValue("createOrder")
        assertEquals("/orders", suite.path)
        assertEquals("POST", suite.method)
        assertTrue(suite.testCases.isNotEmpty(), "Expected test cases for createOrder")
    }

    @Test
    fun `should support wildcard path with specific method`(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi.yaml")).toURI()
        val out = tmp.resolve("out-include-wildcard-path")
        Files.createDirectories(out)

        val exitCode = picocli.CommandLine(GenerateCommand()).execute(
            "--spec-file", Path.of(spec).toString(),
            "--output-dir", out.toString(),
            "--generator", "test-suite-writer",
            "--generator-option", "outputFileName=generated.json",
            "--setting", "includeOperations.*[]=GET",
            "--log-level", "INFO",
        )
        assertEquals(0, exitCode)

        val suites = readSuites(out.resolve("generated.json"))
        assertEquals(setOf("listUsers", "getUser", "listOrders"), suites.keys)
        assertTrue(suites.values.all { it.method == "GET" }, "Expected only GET operations for wildcard path")
    }

    @Test
    fun `should support wildcard method for specific path`(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi.yaml")).toURI()
        val out = tmp.resolve("out-include-wildcard-method")
        Files.createDirectories(out)

        val exitCode = picocli.CommandLine(GenerateCommand()).execute(
            "--spec-file", Path.of(spec).toString(),
            "--output-dir", out.toString(),
            "--generator", "test-suite-writer",
            "--generator-option", "outputFileName=generated.json",
            "--setting", "includeOperations./orders[]=*",
            "--log-level", "INFO",
        )
        assertEquals(0, exitCode)

        val suites = readSuites(out.resolve("generated.json"))
        assertEquals(setOf("listOrders", "createOrder"), suites.keys)
        assertTrue(suites.values.all { it.path == "/orders" }, "Expected only /orders operations")
    }

    private fun readSuites(outputFile: Path): Map<String, TestSuite> {
        assertTrue(Files.exists(outputFile), "Expected output file at $outputFile")
        return objectMapper.readValue(outputFile.toFile())
    }
}
