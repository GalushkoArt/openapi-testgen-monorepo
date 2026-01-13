package art.galushko.openapi.testgen.generator

import art.galushko.openapi.testgen.spi.ArtifactGenerator
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Registry for artifact generator factories with explicit manual wiring.
 *
 * Uses [BuiltInGenerators] for built-in generators and supports custom factories via constructor
 * injection or [register] method. No reflection is used - all generators are explicitly listed.
 *
 * Determinism: [availableIds] and [availableGenerators] are returned in sorted order.
 *
 * @param extraFactories additional generator factories to register
 */
public class ArtifactGeneratorRegistry(
    extraFactories: List<ArtifactGeneratorFactory> = emptyList(),
) {
    private val log = LoggerFactory.getLogger(ArtifactGeneratorRegistry::class.java)
    private val factories: MutableMap<String, ArtifactGeneratorFactory> = mutableMapOf()

    init {
        registerBuiltInFactories()
        extraFactories.forEach { register(it) }
    }

    /**
     * Registers a generator factory manually.
     *
     * @param factory factory to register
     * @throws IllegalArgumentException if ID already registered
     */
    public fun register(factory: ArtifactGeneratorFactory) {
        factories.putIfAbsent(factory.id, factory)?.let { existing ->
            throw IllegalArgumentException(
                "Generator '${factory.id}' already registered by ${existing::class.java.name}"
            )
        }
        log.debug("Registered generator: {} ({})", factory.id, factory.description)
    }

    /**
     * Creates a generator by ID.
     *
     * @param generatorId registered generator ID
     * @param outputDir output directory
     * @param options generator options
     * @return configured generator
     * @throws IllegalArgumentException if generator not found
     */
    public fun create(
        generatorId: String,
        outputDir: File,
        options: Map<String, Any?>,
    ): ArtifactGenerator {
        val factory = factories[generatorId]
            ?: throw IllegalArgumentException(
                "Unknown generator: '$generatorId'. Available: ${availableIds().joinToString()}"
            )
        return factory.create(outputDir, options)
    }

    /**
     * Returns all registered generator IDs.
     */
    public fun availableIds(): Set<String> = factories.keys.toSortedSet()

    /**
     * Returns all registered factories for inspection.
     */
    public fun availableGenerators(): List<ArtifactGeneratorFactory> =
        factories.values.sortedBy { it.id }

    private fun registerBuiltInFactories() {
        BuiltInGenerators.all().forEach { factory ->
            factories[factory.id] = factory
            log.debug("Registered built-in generator: {} ({})", factory.id, factory.description)
        }
    }
}
