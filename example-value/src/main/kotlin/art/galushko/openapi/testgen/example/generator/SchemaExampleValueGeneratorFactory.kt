package art.galushko.openapi.testgen.example.generator

import art.galushko.openapi.testgen.example.config.ExampleValueSettings
import art.galushko.openapi.testgen.example.openapi.SchemaMerger
import art.galushko.openapi.testgen.example.providers.BooleanValueProvider
import art.galushko.openapi.testgen.example.providers.ConstValueProvider
import art.galushko.openapi.testgen.example.providers.DateTimeValueProvider
import art.galushko.openapi.testgen.example.providers.DateValueProvider
import art.galushko.openapi.testgen.example.providers.EmailValueProvider
import art.galushko.openapi.testgen.example.providers.EnumValueProvider
import art.galushko.openapi.testgen.example.providers.NumberValueProvider
import art.galushko.openapi.testgen.example.providers.PlainStringValueProvider
import art.galushko.openapi.testgen.example.providers.UuidValueProvider
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import org.slf4j.LoggerFactory

/**
 * Factory for creating configured [SchemaExampleValueGenerator] instances.
 *
 * Providers are resolved by id in the order specified by [ExampleValueSettings.providers].
 * Missing providers are skipped with a warning; if all configured providers are missing,
 * the default provider order is used as a fallback.
 *
 * @param schemaMerger merges composed schemas before generating example values
 * @param extraProviders additional schema value providers keyed by id
 */
public class SchemaExampleValueGeneratorFactory(
    private val schemaMerger: SchemaMerger = SchemaMerger(),
    private val extraProviders: Map<String, SchemaValueProvider> = emptyMap(),
) {
    private val log = LoggerFactory.getLogger(SchemaExampleValueGeneratorFactory::class.java)

    /**
     * Creates a [SchemaExampleValueGenerator] configured according to [settings].
     */
    public fun create(settings: ExampleValueSettings = ExampleValueSettings()): SchemaExampleValueGenerator {
        val options = SchemaExampleValueGeneratorOptions(
            maxExampleDepth = settings.maxExampleDepth,
        )
        val providers = buildProviders(settings)
        return SchemaExampleValueGenerator(
            valueProviders = providers,
            schemaMerger = schemaMerger,
            options = options,
        )
    }

    private fun buildProviders(settings: ExampleValueSettings): List<SchemaValueProvider> {
        val providerMap = createProviderMap(settings)

        val resolved = mutableListOf<SchemaValueProvider>()
        val missing = mutableListOf<String>()

        for (id in settings.providers) {
            val provider = providerMap[id]
            if (provider == null) {
                missing.add(id)
                continue
            }
            resolved.add(provider)
        }

        if (missing.isNotEmpty()) {
            log.warn(
                "Schema example value providers are not registered and will be skipped: {}. Registered: {}",
                missing.sorted(),
                providerMap.keys.sorted(),
            )
        }

        if (resolved.isNotEmpty()) {
            return resolved
        }

        log.warn(
            "All configured schema example value providers are missing. Falling back to core defaults: {}",
            ExampleValueSettings.DEFAULT_PROVIDER_ORDER,
        )
        val fallback = ExampleValueSettings.DEFAULT_PROVIDER_ORDER.mapNotNull { providerMap[it] }
        check(fallback.isNotEmpty()) {
            "No schema example value providers available. Registered: ${providerMap.keys.sorted()}"
        }
        return fallback
    }

    private fun createProviderMap(settings: ExampleValueSettings): Map<String, SchemaValueProvider> {
        val builtIns: LinkedHashMap<String, SchemaValueProvider> = linkedMapOf(
            "enum" to EnumValueProvider(),
            "const" to ConstValueProvider(),
            "uuid" to UuidValueProvider(settings.uuid.template),
            "email" to EmailValueProvider(settings.email.template),
            "date" to DateValueProvider(settings.date.startDate),
            "date-time" to DateTimeValueProvider(
                timeSuffixTemplate = settings.dateTime.timeSuffixTemplate,
                startDateString = settings.dateTime.startDate,
            ),
            "plain-string" to PlainStringValueProvider(settings.plainString.validChars),
            "number" to NumberValueProvider(),
            "boolean" to BooleanValueProvider(),
        )

        for ((id, provider) in extraProviders) {
            require(id.isNotBlank()) {
                "SchemaValueProvider id must not be blank (provider=${provider::class.java.name})"
            }
            val existing = builtIns[id]
            require(existing == null) {
                "Schema value provider '$id' already registered by ${existing!!::class.java.name} " +
                    "and cannot be overridden by ${provider::class.java.name}"
            }
            builtIns[id] = provider
        }

        return builtIns.toMap()
    }
}
