package art.galushko.openapi.testgen.cli

import art.galushko.openapi.testgen.config.TestGeneratorOverrides
import art.galushko.openapi.testgen.distribution.Slf4jReporter
import art.galushko.openapi.testgen.distribution.TestGenerationExecution
import art.galushko.openapi.testgen.distribution.TestGenerationResult
import art.galushko.openapi.testgen.distribution.TestGenerationRunner
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.nio.file.Path
import java.util.concurrent.Callable

@Suppress("SpreadOperator")
public fun main(args: Array<String>) {
    val cmd = CommandLine(GenerateCommand())
    if (args.isEmpty()) {
        cmd.usage(System.out)
        return
    }
    val exitCode = cmd.execute(*args)
    kotlin.system.exitProcess(exitCode)
}

@Command(
    name = "openapi-testgen",
    description = ["Generate tests from an OpenAPI or Swagger specification"],
    mixinStandardHelpOptions = true,
    versionProvider = CliVersionProvider::class,
)
@Suppress("UNCHECKED_CAST")
public class GenerateCommand : Callable<Int> {
    private val log = LoggerFactory.getLogger(GenerateCommand::class.java)

    @Option(
        names = ["--log-level"],
        description = ["Log level for OpenAPI test generator logs: ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF (default: INFO)"],
    )
    private var logLevel: String? = null

    @Option(names = ["--config-file"], description = ["Path to YAML config file with generator options"])
    public var configFile: String? = null

    @Option(names = ["--spec-file"], description = ["Path to OpenAPI 3.x or Swagger 2.0 spec file (YAML/JSON)"])
    public var specFile: String? = null

    @Option(names = ["--output-dir"], description = ["Output directory for generated files"])
    public var outputDir: String? = null

    @Option(names = ["--generator"], description = ["Generator id"])
    public var generator: String? = null

    @Option(names = ["--generator-option"], description = ["Generator option key=value"], split = ",", arity = "0..*")
    public var generatorOptionsRaw: Array<String> = emptyArray()

    @Option(
        names = ["--setting"],
        description = [
            "Test generation setting key.nested.path=value (supports dot notation for nested maps and [] for lists, e.g. exampleValues.providers[]=enum)"
        ],
        split = ";",
        arity = "0..*"
    )
    public var testSettingsRaw: Array<String> = emptyArray()

    @Option(
        names = ["--parser-setting"],
        description = [
            "Parser setting key.nested.path=value (e.g., yamlCodePointLimit=10000000)"
        ],
        split = ",",
        arity = "0..*"
    )
    public var parserSettingsRaw: Array<String> = emptyArray()

    @Option(names = ["--always-write-test"], description = ["Flag to force writing tests even if there are errors on generation (default: false)"])
    public var alwaysWriteTests: Boolean? = null

    override fun call(): Int {
        val config = TestGenerationExecution.loadConfig(configFile?.let { Path.of(it) })
        val overrides = buildOverrides()

        val runner = TestGenerationRunner.withDefaults(
            reporter = Slf4jReporter(log),
        )

        val result = TestGenerationExecution.run(runner, config, overrides, ::applyLogbackRootLevel)
        return when (result) {
            is TestGenerationResult.Success -> 0
            is TestGenerationResult.Failure -> {
                System.err.println(result.message)
                1
            }
        }
    }

    private fun applyLogbackRootLevel(level: String) {
        (LoggerFactory.getLogger(ROOT_LOGGER_NAME) as? Logger)?.level = Level.valueOf(level)
    }

    private fun buildOverrides(): TestGeneratorOverrides {
        val generatorOptions = KeyValueParser.parse(generatorOptionsRaw)
        val testSettings = KeyValueParser.parse(testSettingsRaw)
        val parserOptions = KeyValueParser.parse(parserSettingsRaw)

        return TestGeneratorOverrides(
            logLevel = logLevel,
            specFile = specFile,
            outputDir = outputDir?.let { Path.of(it) },
            generatorId = generator,
            generatorOptions = generatorOptions,
            testGenerationSettings = testSettings,
            alwaysWriteTests = alwaysWriteTests,
            parserSettings = parserOptions,
        )
    }
}

public class CliVersionProvider : CommandLine.IVersionProvider {
    override fun getVersion(): Array<String> {
        val version = GenerateCommand::class.java.`package`?.implementationVersion
        return arrayOf(version ?: "dev")
    }
}
