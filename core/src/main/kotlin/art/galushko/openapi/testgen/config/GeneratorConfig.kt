package art.galushko.openapi.testgen.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.nio.file.Files
import java.nio.file.Path

/**
 * YAML configuration model for the OpenAPI Test Generator.
 *
 * Mirrors CLI/Gradle task options and is merged with overrides by
 * [TestGeneratorExecutionOptionsFactory.fromConfig].
 */
public data class GeneratorConfig(
    val logLevel: String? = null,
    val specFile: String? = null,
    val outputDir: String? = null,
    val generator: String? = null,
    val generatorOptions: Map<String, Any?>? = null,
    val testGenerationSettings: Map<String, Any?>? = null,
    val alwaysWriteTests: Boolean? = null,
)

/**
 * Loads [GeneratorConfig] from a YAML file.
 *
 * Unknown fields are ignored to allow forward-compatible configs.
 */
public object GeneratorConfigLoader {
    private val mapper: ObjectMapper = ObjectMapper(YAMLFactory())
        .registerModule(KotlinModule.Builder().build())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    /**
     * Loads configuration from a YAML file.
     *
     * @throws IllegalArgumentException if the file is missing or unreadable
     */
    public fun load(path: Path): GeneratorConfig {
        require(Files.exists(path)) { "Config file does not exist: $path" }
        require(Files.isReadable(path)) { "Config file is not readable: $path" }
        Files.newBufferedReader(path).use { reader ->
            return mapper.readValue(reader, GeneratorConfig::class.java)
        }
    }
}


