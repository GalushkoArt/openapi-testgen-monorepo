package art.galushko.openapi.testgen.distribution

import art.galushko.openapi.testgen.config.GeneratorConfig
import art.galushko.openapi.testgen.config.TestGeneratorOverrides
import art.galushko.openapi.testgen.generator.GeneratorIds
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.writeText

@DisplayName("TestGenerationExecution")
class TestGenerationExecutionTest {

    private val log = LoggerFactory.getLogger(TestGenerationExecutionTest::class.java)

    @Test
    @DisplayName("loadConfig should return null when no config file is given")
    fun loadConfigShouldReturnNullWithoutFile() {
        assertThat(TestGenerationExecution.loadConfig(null)).isNull()
    }

    @Test
    @DisplayName("loadConfig should load a YAML config file")
    fun loadConfigShouldLoadYaml(@TempDir tempDir: Path) {
        val configFile = tempDir.resolve("config.yaml")
        configFile.writeText("logLevel: debug\ngenerator: test-suite-writer\n")

        val config = TestGenerationExecution.loadConfig(configFile)

        assertThat(config)
            .usingRecursiveComparison()
            .isEqualTo(GeneratorConfig(logLevel = "debug", generator = "test-suite-writer"))
    }

    @Test
    @DisplayName("loadConfig should fail for a missing config file")
    fun loadConfigShouldFailForMissingFile(@TempDir tempDir: Path) {
        val missing = tempDir.resolve("missing.yaml")

        assertThatThrownBy { TestGenerationExecution.loadConfig(missing) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Config file does not exist: $missing")
    }

    @Test
    @DisplayName("run should apply the resolved log level and execute generation")
    fun runShouldApplyLogLevelAndExecute(@TempDir tempDir: Path) {
        val spec = tempDir.resolve("spec.yaml")
        spec.writeText(MINIMAL_SPEC)
        val outputDir = tempDir.resolve("out")
        val appliedLevels = mutableListOf<String>()

        val result = TestGenerationExecution.run(
            runner = TestGenerationRunner.withDefaults(Slf4jReporter(log)),
            config = GeneratorConfig(logLevel = "debug"),
            overrides = TestGeneratorOverrides(
                specFile = spec.toString(),
                outputDir = outputDir,
                generatorId = GeneratorIds.TEST_SUITE_WRITER,
                generatorOptions = mapOf("outputFileName" to "generated.json"),
            ),
            applyLogLevel = appliedLevels::add,
        )

        assertThat(appliedLevels).containsExactly("DEBUG")
        assertThat(result).isInstanceOf(TestGenerationResult.Success::class.java)
        assertThat(outputDir.resolve("generated.json")).exists()
    }

    @Test
    @DisplayName("run should not invoke the log-level callback when no level is configured")
    fun runShouldSkipLogLevelWhenUnset(@TempDir tempDir: Path) {
        val spec = tempDir.resolve("spec.yaml")
        spec.writeText(MINIMAL_SPEC)
        val appliedLevels = mutableListOf<String>()

        val result = TestGenerationExecution.run(
            runner = TestGenerationRunner.withDefaults(Slf4jReporter(log)),
            config = null,
            overrides = TestGeneratorOverrides(
                specFile = spec.toString(),
                outputDir = tempDir.resolve("out"),
                generatorId = GeneratorIds.TEST_SUITE_WRITER,
                generatorOptions = mapOf("outputFileName" to "generated.json"),
            ),
            applyLogLevel = appliedLevels::add,
        )

        assertThat(appliedLevels).isEmpty()
        assertThat(result).isInstanceOf(TestGenerationResult.Success::class.java)
    }

    @Test
    @DisplayName("run should fail fast on an invalid log level")
    fun runShouldFailFastOnInvalidLogLevel() {
        assertThatThrownBy {
            TestGenerationExecution.run(
                runner = TestGenerationRunner.withDefaults(Slf4jReporter(log)),
                config = null,
                overrides = TestGeneratorOverrides(logLevel = "bogus"),
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Invalid log level 'BOGUS'. Expected one of ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF.")
    }

    private companion object {
        // A required, constrained parameter so at least one negative test case is generated
        // and the writer produces output.
        val MINIMAL_SPEC = """
            openapi: 3.0.3
            info:
              title: Ping
              version: "1.0"
            paths:
              /ping:
                get:
                  operationId: ping
                  parameters:
                    - name: limit
                      in: query
                      required: true
                      schema:
                        type: integer
                        format: int32
                        minimum: 1
                        maximum: 10
                  responses:
                    '200':
                      description: ok
                    '400':
                      description: bad request
        """.trimIndent()
    }
}
