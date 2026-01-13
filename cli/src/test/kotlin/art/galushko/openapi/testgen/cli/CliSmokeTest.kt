package art.galushko.openapi.testgen.cli

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class CliSmokeTest {
    @Test
    fun runHelp() {
        // Just ensure main doesn't blow up when asking for help
        val exitCode = picocli.CommandLine(GenerateCommand()).execute("--help")
        assertTrue(exitCode == 0)
    }

    @Test
    fun generateFromTinySpec(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi.yaml")).toURI()
        val out = tmp.resolve("out")
        Files.createDirectories(out)

        val exitCode = picocli.CommandLine(GenerateCommand()).execute(
            "--spec-file", Path.of(spec).toString(),
            "--output-dir", out.toString(),
            "--generator", "test-suite-writer",
            "--log-level", "INFO",
            "--always-write-test",
            "--setting", "validSecurityValues.ApiKeyAuth=test-api-key-123",
            "--setting", "ignoreTestCases./orders.post[]=Incorrect Request Body: Missed Required Object Properties items",
            "--generator-option", "outputFileName=generated.json",
        )
        assertTrue(exitCode == 0)
        val outputFile = out.resolve("generated.json")
        assertTrue(Files.exists(outputFile), "Expected output file at $outputFile")
        val txt = outputFile.readText().trim()
        val expected = requireNotNull(this::class.java.classLoader.getResource("openapi-test-suites.json")).readText().trim()
        assertEquals(expected.lines(), txt.lines(), "Output file content does not match expected")
    }

    @Test
    fun generateFromConfigFile(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi.yaml")).toURI()
        val specPath = Path.of(spec).toString()
        val out = tmp.resolve("out-config")
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
                  validSecurityValues:
                    ApiKeyAuth: 'test-api-key-123'
                  ignoreTestCases:
                    '/orders':
                      post:
                        - 'Incorrect Request Body: Missed Required Object Properties items'
            """.trimIndent(),
        )

        val exitCode = picocli.CommandLine(GenerateCommand()).execute(
            "--config-file", configFile.toString(),
        )
        assertTrue(exitCode == 0)
        val outputFile = out.resolve("generated.json")
        assertTrue(Files.exists(outputFile), "Expected output file at $outputFile")
        val txt = outputFile.readText().trim()
        val expected = requireNotNull(this::class.java.classLoader.getResource("openapi-test-suites.json")).readText().trim()
        assertEquals(expected.lines(), txt.lines(), "Output file content does not match expected")
    }

    @Test
    fun generateFromConfigFileWithOverrides(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi.yaml")).toURI()
        val specPath = Path.of(spec).toString()
        val out = tmp.resolve("out-config-overrides")
        Files.createDirectories(out)

        val configFile = tmp.resolve("config-overrides.yaml")
        Files.writeString(
            configFile,
            """
                specFile: '$specPath'
                outputDir: '$out'
                generator: 'test-suite-writer'
                logLevel: 'INFO'
                alwaysWriteTests: true
                generatorOptions:
                  outputFileName: 'from-config.json'
                testGenerationSettings:
                  validSecurityValues:
                    ApiKeyAuth: 'config-api-key'
                  ignoreTestCases:
                    '/orders':
                      post:
                        - 'Incorrect Request Body: Missed Required Object Properties items'
            """.trimIndent(),
        )

        val exitCode = picocli.CommandLine(GenerateCommand()).execute(
            "--config-file", configFile.toString(),
            "--setting", "validSecurityValues.ApiKeyAuth=test-api-key-123",
            "--generator-option", "outputFileName=generated.json",
        )
        assertTrue(exitCode == 0)
        val outputFile = out.resolve("generated.json")
        assertTrue(Files.exists(outputFile), "Expected output file at $outputFile")
        val txt = outputFile.readText().trim()
        val expected = requireNotNull(this::class.java.classLoader.getResource("openapi-test-suites.json")).readText().trim()
        assertEquals(expected.lines(), txt.lines(), "Output file content does not match expected")
    }
}


