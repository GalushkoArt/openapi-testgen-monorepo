package art.galushko.openapi.testgen.cli

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

@Suppress("LongMethod")
class CliSmokeTest {
    @Test
    fun runHelp() {
        // Just ensure main doesn't blow up when asking for help
        val exitCode = picocli.CommandLine(GenerateCommand()).execute("--help")
        assertTrue(exitCode == 0)
    }

    @ParameterizedTest
    @CsvSource(value = [
        "openapi-31.yaml,openapi-31-test-suites.json",
        "openapi-30.yaml,openapi-30-test-suites.json",
    ])
    fun generateFromTinySpec(spec: String, expected: String, @TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource(spec)).toURI()
        val out = tmp.resolve("out")
        Files.createDirectories(out)

        val exitCode = picocli.CommandLine(GenerateCommand()).execute(
            "--spec-file", Path.of(spec).toString(),
            "--output-dir", out.toString(),
            "--generator", "test-suite-writer",
            "--log-level", "INFO",
            "--always-write-test",
            "--setting", "validSecurityValues.ApiKeyAuth=test-api-key-123",
            "--setting", "ignoreTestCases./orders.post[]=Incorrect Request Body: Missed Required Object Properties paymentMethod",
            "--generator-option", "outputFileName=generated.json",
        )
        assertTrue(exitCode == 0)
        val outputFile = out.resolve("generated.json")
        assertTrue(Files.exists(outputFile), "Expected output file at $outputFile")
        val txt = outputFile.readText().trim()
        val expected = requireNotNull(this::class.java.classLoader.getResource(expected)).readText().trim()
        assertEquals(expected.lines(), txt.lines(), "Output file content does not match expected")
    }

    @Test
    fun generateFromConfigFile(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi-31.yaml")).toURI()
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
                        - 'Incorrect Request Body: Missed Required Object Properties paymentMethod'
            """.trimIndent(),
        )

        val exitCode = picocli.CommandLine(GenerateCommand()).execute(
            "--config-file", configFile.toString(),
        )
        assertTrue(exitCode == 0)
        val outputFile = out.resolve("generated.json")
        assertTrue(Files.exists(outputFile), "Expected output file at $outputFile")
        val txt = outputFile.readText().trim()
        val expected = requireNotNull(this::class.java.classLoader.getResource("openapi-31-test-suites.json")).readText().trim()
        assertEquals(expected.lines(), txt.lines(), "Output file content does not match expected")
    }

    @Test
    fun generateFromConfigFileWithOverrides(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi-31.yaml")).toURI()
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
                        - 'Incorrect Request Body: Missed Required Object Properties paymentMethod'
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
        val expected = requireNotNull(this::class.java.classLoader.getResource("openapi-31-test-suites.json")).readText().trim()
        assertEquals(expected.lines(), txt.lines(), "Output file content does not match expected")
    }

    @Test
    fun generateWithParserOptions(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi-31.yaml")).toURI()
        val out = tmp.resolve("out-parser")
        Files.createDirectories(out)

        val exitCode = picocli.CommandLine(GenerateCommand()).execute(
            "--spec-file", Path.of(spec).toString(),
            "--output-dir", out.toString(),
            "--generator", "test-suite-writer",
            "--log-level", "DEBUG",
            "--always-write-test",
            "--setting", "validSecurityValues.ApiKeyAuth=test-api-key-123",
            "--setting", "ignoreTestCases./orders.post[]=Incorrect Request Body: Missed Required Object Properties paymentMethod",
            "--generator-option", "outputFileName=generated.json",
            "--parser-setting", "yamlCodePointLimit=10000000",
            "--parser-setting", "yamlMaxAliasesForCollections=100",
            "--parser-setting", "yamlAllowRecursiveKeys=true",
            "--parser-setting", "yamlNestingDepthLimit=100",
        )
        assertTrue(exitCode == 0)
        val outputFile = out.resolve("generated.json")
        assertTrue(Files.exists(outputFile), "Expected output file at $outputFile")
    }

    @Test
    fun generateWithParserOptionsFromConfigFile(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi-31.yaml")).toURI()
        val specPath = Path.of(spec).toString()
        val out = tmp.resolve("out-parser-config")
        Files.createDirectories(out)

        val configFile = tmp.resolve("config-parser.yaml")
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
                        - 'Incorrect Request Body: Missed Required Object Properties paymentMethod'
                parserSettings:
                  yamlCodePointLimit: 10000000
                  yamlMaxAliasesForCollections: 100
                  yamlAllowRecursiveKeys: true
                  yamlNestingDepthLimit: 100
            """.trimIndent(),
        )

        val exitCode = picocli.CommandLine(GenerateCommand()).execute(
            "--config-file", configFile.toString(),
        )
        assertTrue(exitCode == 0)
        val outputFile = out.resolve("generated.json")
        assertTrue(Files.exists(outputFile), "Expected output file at $outputFile")
    }

    @Test
    fun generateWithParserSettingsOverridingConfigFile(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi-31.yaml")).toURI()
        val specPath = Path.of(spec).toString()
        val out = tmp.resolve("out-parser-overrides")
        Files.createDirectories(out)

        val configFile = tmp.resolve("config-parser-overrides.yaml")
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
                        - 'Incorrect Request Body: Missed Required Object Properties paymentMethod'
                parserSettings:
                  yamlCodePointLimit: 5000000
                  yamlMaxAliasesForCollections: 50
                  yamlAllowRecursiveKeys: false
                  yamlNestingDepthLimit: 50
            """.trimIndent(),
        )

        // CLI parser settings should override config file values
        val exitCode = picocli.CommandLine(GenerateCommand()).execute(
            "--config-file", configFile.toString(),
            "--parser-setting", "yamlCodePointLimit=10000000",
            "--parser-setting", "yamlAllowRecursiveKeys=true",
            "--parser-setting", "yamlNestingDepthLimit=100",
        )
        assertTrue(exitCode == 0)
        val outputFile = out.resolve("generated.json")
        assertTrue(Files.exists(outputFile), "Expected output file at $outputFile")
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "false,1,false",
            "true,0,true",
        ]
    )
    fun alwaysWriteTestShouldControlExitCodeAndArtifacts(
        alwaysWriteTests: Boolean,
        expectedExitCode: Int,
        expectOutputFile: Boolean,
        @TempDir tmp: Path,
    ) {
        val specFile = tmp.resolve("partial-success.yaml")
        Files.writeString(specFile, partialSuccessSpec())

        val out = tmp.resolve("out-partial")
        Files.createDirectories(out)

        val args = mutableListOf(
            "--spec-file", specFile.toString(),
            "--output-dir", out.toString(),
            "--generator", "test-suite-writer",
            "--generator-option", "outputFileName=generated.json",
            "--setting", "maxSchemaCombinations=3",
        )
        if (alwaysWriteTests) {
            args += "--always-write-test"
        }

        val exitCode = picocli.CommandLine(GenerateCommand()).execute(*args.toTypedArray())
        assertEquals(expectedExitCode, exitCode)

        val outputFile = out.resolve("generated.json")
        assertEquals(expectOutputFile, Files.exists(outputFile))
        if (expectOutputFile) {
            assertTrue(outputFile.readText().contains("\"partialSuccessOperation\""))
        }
    }

    @Test
    fun generateShouldFailFastForInvalidNumericParserSetting(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi-31.yaml")).toURI()
        val command = GenerateCommand().apply {
            specFile = Path.of(spec).toString()
            outputDir = tmp.resolve("out-invalid-parser-int").toString()
            generator = "test-suite-writer"
            parserSettingsRaw = arrayOf("yamlCodePointLimit=0")
        }

        val error = assertThrows(IllegalArgumentException::class.java) {
            command.call()
        }
        assertEquals("yamlCodePointLimit must be positive or null, was 0", error.message)
    }

    @Test
    fun generateShouldFailFastForInvalidBooleanParserSetting(@TempDir tmp: Path) {
        val spec = requireNotNull(this::class.java.classLoader.getResource("openapi-31.yaml")).toURI()
        val command = GenerateCommand().apply {
            specFile = Path.of(spec).toString()
            outputDir = tmp.resolve("out-invalid-parser-boolean").toString()
            generator = "test-suite-writer"
            parserSettingsRaw = arrayOf("yamlAllowRecursiveKeys=not-a-boolean")
        }

        val error = assertThrows(RuntimeException::class.java) {
            command.call()
        }
        assertTrue(
            error.message?.contains("Configuration error for 'yamlAllowRecursiveKeys'") == true,
            "Unexpected exception message: ${error.message}",
        )
    }

    private fun partialSuccessSpec(): String =
        """
            openapi: 3.1.0

            info:
              title: Partial Success Test API
              version: 1.0.0
              description: |
                API designed to trigger PartialSuccess outcome during test generation.
                Simple parameters will succeed, but complex request body will exceed schema combination budget.

            servers:
              - url: https://api.example.com/v1
                description: Test server

            paths:
              /simple-operation:
                get:
                  summary: Simple operation that succeeds completely
                  operationId: simpleOperation
                  parameters:
                    - name: status
                      in: query
                      required: true
                      schema:
                        type: string
                        enum: [active, inactive]
                  responses:
                    '200':
                      description: Success
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              result:
                                type: string
                    '400':
                      description: Bad request

              /partial-success:
                post:
                  summary: Operation causing partial success
                  operationId: partialSuccessOperation
                  parameters:
                    - name: simpleParam
                      in: query
                      required: true
                      schema:
                        type: integer
                        minimum: 1
                        maximum: 100
                  requestBody:
                    required: true
                    content:
                      application/json:
                        schema:
                          ${'$'}ref: '#/components/schemas/ComplexBudgetExceedingSchema'
                  responses:
                    '201':
                      description: Created
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              id:
                                type: string
                    '400':
                      description: Bad request

            components:
              schemas:
                ComplexBudgetExceedingSchema:
                  type: object
                  required: [data]
                  properties:
                    data:
                      oneOf:
                        - ${'$'}ref: '#/components/schemas/VariantA'
                        - ${'$'}ref: '#/components/schemas/VariantB'
                        - ${'$'}ref: '#/components/schemas/VariantC'

                VariantA:
                  type: object
                  required: [typeA, valueA]
                  properties:
                    typeA:
                      type: string
                      const: "typeA"
                    valueA:
                      anyOf:
                        - type: string
                          minLength: 1
                        - type: integer
                          minimum: 0

                VariantB:
                  type: object
                  required: [typeB, nestedB]
                  properties:
                    typeB:
                      type: string
                      const: "typeB"
                    nestedB:
                      oneOf:
                        - type: object
                          required: [x]
                          properties:
                            x:
                              type: number
                        - type: object
                          required: [y]
                          properties:
                            y:
                              type: boolean

                VariantC:
                  type: object
                  required: [typeC, configC]
                  properties:
                    typeC:
                      type: string
                      const: "typeC"
                    configC:
                      anyOf:
                        - type: string
                          enum: [option1, option2]
                        - type: integer
                          enum: [1, 2, 3]
        """.trimIndent()
}
