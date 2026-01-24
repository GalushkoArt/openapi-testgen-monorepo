package art.galushko.openapi.testgen.example.generator

import art.galushko.openapi.testgen.example.config.DateProviderSettings
import art.galushko.openapi.testgen.example.config.DateTimeProviderSettings
import art.galushko.openapi.testgen.example.config.EmailProviderSettings
import art.galushko.openapi.testgen.example.config.ExampleValueSettings
import art.galushko.openapi.testgen.example.config.PlainStringProviderSettings
import art.galushko.openapi.testgen.example.config.UuidProviderSettings
import art.galushko.openapi.testgen.example.spi.SchemaValueProvider
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.stream.Stream

@Epic("Test Data Generation")
@Feature("Schema Example Value Generator Factory")
@DisplayName("SchemaExampleValueGeneratorFactory")
class SchemaExampleValueGeneratorFactoryTest {

    private val factory = SchemaExampleValueGeneratorFactory()

    @Nested
    @DisplayName("Default configuration")
    inner class DefaultConfiguration {

        @Test
        @DisplayName("should create generator with default settings")
        fun shouldCreateGeneratorWithDefaultSettings() {
            val generator = factory.create()

            // Verify default provider order works correctly
            val enumSchema = StringSchema().apply {
                enum = listOf("a", "b")
            }
            assertThat(generator.getExampleValue("field", enumSchema, OpenAPI()))
                .isEqualTo("a")
        }

        @Test
        @DisplayName("should use default provider order (enum > const > uuid > email > date > date-time > plain-string > number > boolean)")
        fun shouldUseDefaultProviderOrder() {
            val generator = factory.create()

            // enum comes first in default order
            val enumUuidSchema = StringSchema().apply {
                format = "uuid"
                enum = listOf("enum-value")
            }
            assertThat(generator.getExampleValue("field", enumUuidSchema, OpenAPI()))
                .isEqualTo("enum-value")
        }
    }

    @Nested
    @DisplayName("Generator options")
    inner class GeneratorOptions {

        @Test
        @DisplayName("should include optional example properties when enabled")
        fun shouldIncludeOptionalExamplePropertiesWhenEnabled() {
            val generator = factory.create(
                ExampleValueSettings(includeOptionalExampleProperties = true)
            )

            val schema = ObjectSchema().apply {
                addProperty("requiredProp", StringSchema().example("required"))
                addProperty("optionalProp", StringSchema().example("optional"))
                required = listOf("requiredProp")
            }

            val result = generator.getExampleObject("obj", schema, OpenAPI())

            assertThat(result)
                .containsEntry("requiredProp", "required")
                .containsEntry("optionalProp", "optional")
        }

        @Test
        @DisplayName("should exclude writeOnly properties when disabled")
        fun shouldExcludeWriteOnlyPropertiesWhenDisabled() {
            val generator = factory.create(
                ExampleValueSettings(
                    includeOptionalExampleProperties = true,
                    includeWriteOnly = false,
                )
            )

            val schema = ObjectSchema().apply {
                addProperty("visible", StringSchema().example("ok"))
                addProperty("secret", StringSchema().example("hidden").writeOnly(true))
                required = listOf("visible")
            }

            val result = generator.getExampleObject("obj", schema, OpenAPI())

            assertThat(result)
                .containsEntry("visible", "ok")
                .doesNotContainKey("secret")
        }

        @Test
        @DisplayName("should use schema examples/defaults as fallback when enabled")
        fun shouldUseSchemaExampleFallbackWhenEnabled() {
            val generator = factory.create(
                ExampleValueSettings(useSchemaExampleFallback = true)
            )

            val schema = StringSchema()._default("fallback")

            val result = generator.getExampleValue("field", schema, OpenAPI())

            assertThat(result).isEqualTo("fallback")
        }
    }

    @Nested
    @DisplayName("Provider ordering")
    inner class ProviderOrdering {

        private val factoryWithPattern = SchemaExampleValueGeneratorFactory(
            extraProviders = mapOf("pattern" to FixedPatternProvider)
        )

        fun providerOrderTestCases(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "pattern before uuid - pattern wins",
                listOf("pattern", "uuid"),
                StringSchema().format("uuid").pattern("^abc$"),
                "abc"
            ),
            Arguments.of(
                "uuid before pattern - uuid wins",
                listOf("uuid", "pattern"),
                StringSchema().format("uuid").pattern("^abc$"),
                "d5a5495b-cbdc-4237-a66e-000000000000"
            ),
            Arguments.of(
                "enum before uuid - enum wins for uuid format with enum",
                listOf("enum", "uuid"),
                StringSchema().apply {
                    format = "uuid"
                    enum = listOf("custom-enum")
                },
                "custom-enum"
            ),
            Arguments.of(
                "uuid before enum - uuid wins for uuid format with enum",
                listOf("uuid", "enum"),
                StringSchema().apply {
                    format = "uuid"
                    enum = listOf("custom-enum")
                },
                "d5a5495b-cbdc-4237-a66e-000000000000"
            ),
            Arguments.of(
                "const before enum - const wins",
                listOf("const", "enum"),
                StringSchema().apply {
                    enum = listOf("enum-value")
                    setConst("const-value")
                },
                "const-value"
            ),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("providerOrderTestCases")
        @DisplayName("should respect configured provider order")
        fun shouldRespectConfiguredProviderOrder(
            scenario: String,
            providers: List<String>,
            schema: Schema<*>,
            expected: Any,
        ) {
            val generator = factoryWithPattern.create(
                ExampleValueSettings(providers = providers)
            )

            val value = generator.getExampleValue("field", schema, OpenAPI())
            assertThat(value).isEqualTo(expected)
        }

        @Test
        @DisplayName("should allow disabling a provider by omitting it from providers list")
        fun shouldAllowDisablingProviderByOmittingItFromProvidersList() {
            val generator = factoryWithPattern.create(
                ExampleValueSettings(
                    providers = listOf("plain-string"),
                )
            )

            // pattern provider is not in list, so it won't match even though schema has pattern
            val schema = StringSchema().pattern("^abc$")
            val value = generator.getExampleValue("name", schema, OpenAPI())
            assertThat(value).isEqualTo("a")
        }
    }

    @Nested
    @DisplayName("Built-in providers")
    inner class BuiltInProviders {
        @Suppress("LongMethod")
        fun builtInProviderTestCases(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "enum provider",
                listOf("enum"),
                StringSchema().apply { enum = listOf("first", "second") },
                { v: Any -> v == "first" }
            ),
            Arguments.of(
                "const provider",
                listOf("const"),
                StringSchema().apply { setConst("constant-value") },
                { v: Any -> v == "constant-value" }
            ),
            Arguments.of(
                "uuid provider",
                listOf("uuid"),
                StringSchema().format("uuid"),
                { v: Any -> v.toString().matches(Regex("[a-f0-9-]{36}")) }
            ),
            Arguments.of(
                "email provider",
                listOf("email"),
                StringSchema().format("email"),
                { v: Any -> v.toString().contains("@") && v.toString().endsWith(".com") }
            ),
            Arguments.of(
                "date provider",
                listOf("date"),
                StringSchema().format("date"),
                { v: Any -> v.toString().matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            ),
            Arguments.of(
                "date-time provider",
                listOf("date-time"),
                StringSchema().format("date-time"),
                { v: Any -> v.toString().matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")) }
            ),
            Arguments.of(
                "plain-string provider",
                listOf("plain-string"),
                StringSchema(),
                { v: Any -> v == "a" }
            ),
            Arguments.of(
                "number provider for integer schema",
                listOf("number"),
                IntegerSchema(),
                { v: Any -> v == BigDecimal.ONE }
            ),
            Arguments.of(
                "number provider for number schema",
                listOf("number"),
                NumberSchema(),
                { v: Any -> v == BigDecimal.ONE }
            ),
            Arguments.of(
                "number provider with minimum",
                listOf("number"),
                NumberSchema().minimum(BigDecimal.TEN),
                { v: Any -> v == BigDecimal.TEN }
            ),
            Arguments.of(
                "boolean provider",
                listOf("boolean"),
                BooleanSchema(),
                { v: Any -> v == true }
            ),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("builtInProviderTestCases")
        @DisplayName("should generate correct values for each built-in provider")
        fun shouldGenerateCorrectValuesForBuiltInProviders(
            scenario: String,
            providers: List<String>,
            schema: Schema<*>,
            expectedMatcher: (Any) -> Boolean,
        ) {
            val generator = factory.create(
                ExampleValueSettings(providers = providers)
            )

            val value = generator.getExampleValue("field", schema, OpenAPI())
            assertThat(value).matches { expectedMatcher(it) }
        }

        @Test
        @DisplayName("should generate const value with correct variation index")
        fun shouldGenerateConstValueWithVariationIndex() {
            val generator = factory.create(
                ExampleValueSettings(providers = listOf("const"))
            )
            val schema = StringSchema().apply {
                setConst("fixed")
            }

            // const should always return the same value regardless of variation
            val value0 = generator.getExampleValue("field", schema, OpenAPI(), variationIndex = 0)
            val value1 = generator.getExampleValue("field", schema, OpenAPI(), variationIndex = 1)

            assertThat(value0).isEqualTo("fixed")
            assertThat(value1).isEqualTo("fixed")
        }

        @Test
        @DisplayName("should generate different enum values for different variation indices")
        fun shouldGenerateDifferentEnumValuesForVariations() {
            val generator = factory.create(
                ExampleValueSettings(providers = listOf("enum"))
            )
            val schema = StringSchema().apply {
                enum = listOf("a", "b", "c")
            }

            val value0 = generator.getExampleValue("field", schema, OpenAPI(), variationIndex = 0)
            val value1 = generator.getExampleValue("field", schema, OpenAPI(), variationIndex = 1)
            val value2 = generator.getExampleValue("field", schema, OpenAPI(), variationIndex = 2)

            assertThat(value0).isEqualTo("a")
            assertThat(value1).isEqualTo("b")
            assertThat(value2).isEqualTo("c")
        }
    }

    @Nested
    @DisplayName("Provider settings customization")
    inner class ProviderSettingsCustomization {

        @Test
        @DisplayName("should apply custom uuid template")
        fun shouldApplyCustomUuidTemplate() {
            val generator = factory.create(
                ExampleValueSettings(
                    providers = listOf("uuid"),
                    uuid = UuidProviderSettings(template = "custom-%s-uuid")
                )
            )

            val value = generator.getExampleValue("id", StringSchema().format("uuid"), OpenAPI())
            assertThat(value.toString()).startsWith("custom-")
            assertThat(value.toString()).endsWith("-uuid")
        }

        @Test
        @DisplayName("should apply custom email template")
        fun shouldApplyCustomEmailTemplate() {
            val generator = factory.create(
                ExampleValueSettings(
                    providers = listOf("email"),
                    email = EmailProviderSettings(template = "user%s@mycompany.com")
                )
            )

            val value = generator.getExampleValue("email", StringSchema().format("email"), OpenAPI())
            assertThat(value).isEqualTo("user0@mycompany.com")
        }

        @Test
        @DisplayName("should apply custom date startDate")
        fun shouldApplyCustomDateStartDate() {
            val generator = factory.create(
                ExampleValueSettings(
                    providers = listOf("date"),
                    date = DateProviderSettings(startDate = "2025-06-15")
                )
            )

            val value = generator.getExampleValue("date", StringSchema().format("date"), OpenAPI())
            assertThat(value).isEqualTo("2025-06-15")
        }

        @Test
        @DisplayName("should apply custom dateTime settings")
        fun shouldApplyCustomDateTimeSettings() {
            val generator = factory.create(
                ExampleValueSettings(
                    providers = listOf("date-time"),
                    dateTime = DateTimeProviderSettings(
                        startDate = "2025-01-01",
                        timeSuffixTemplate = "%sT00:00:00Z"
                    )
                )
            )

            val value = generator.getExampleValue("datetime", StringSchema().format("date-time"), OpenAPI())
            assertThat(value).isEqualTo("2025-01-01T00:00:00Z")
        }

        @Test
        @DisplayName("should apply custom plainString validChars")
        fun shouldApplyCustomPlainStringValidChars() {
            val generator = factory.create(
                ExampleValueSettings(
                    providers = listOf("plain-string"),
                    plainString = PlainStringProviderSettings(validChars = "xyz")
                )
            )

            val value = generator.getExampleValue("field", StringSchema(), OpenAPI())
            assertThat(value).isEqualTo("x")
        }

        @Test
        @DisplayName("should apply all custom provider settings together")
        fun shouldApplyAllCustomProviderSettingsTogether() {
            val generator = factory.create(
                ExampleValueSettings(
                    providers = listOf("email", "date", "date-time", "plain-string"),
                    maxExampleDepth = 30,
                    email = EmailProviderSettings(template = "user%s@mycompany.com"),
                    date = DateProviderSettings(startDate = "2025-01-01"),
                    dateTime = DateTimeProviderSettings(
                        startDate = "2025-01-01",
                        timeSuffixTemplate = "%sT00:00:00Z",
                    ),
                    plainString = PlainStringProviderSettings(validChars = "01"),
                )
            )

            val openAPI = OpenAPI()

            assertThat(generator.getExampleValue("email", StringSchema().format("email"), openAPI))
                .isEqualTo("user0@mycompany.com")
            assertThat(generator.getExampleValue("date", StringSchema().format("date"), openAPI))
                .isEqualTo("2025-01-01")
            assertThat(generator.getExampleValue("datetime", StringSchema().format("date-time"), openAPI))
                .isEqualTo("2025-01-01T00:00:00Z")
            assertThat(generator.getExampleValue("plain", StringSchema(), openAPI))
                .isEqualTo("0")
        }
    }

    @Nested
    @DisplayName("maxExampleDepth behavior")
    inner class MaxExampleDepthBehavior {

        @Test
        @DisplayName("should respect maxExampleDepth and stop recursion at limit")
        fun shouldRespectMaxExampleDepthLimit() {
            val generator = factory.create(
                ExampleValueSettings(
                    providers = ExampleValueSettings.DEFAULT_PROVIDER_ORDER,
                    maxExampleDepth = 2
                )
            )

            // Create a schema with 4 levels of nesting
            val level4 = ObjectSchema().apply {
                addProperty("level4Prop", StringSchema().example("deep-value"))
                required = listOf("level4Prop")
            }
            val level3 = ObjectSchema().apply {
                addProperty("level3Prop", level4)
                required = listOf("level3Prop")
            }
            val level2 = ObjectSchema().apply {
                addProperty("level2Prop", level3)
                required = listOf("level2Prop")
            }
            val level1 = ObjectSchema().apply {
                addProperty("level1Prop", level2)
                required = listOf("level1Prop")
            }

            val result = generator.getExampleObject("root", level1, OpenAPI())

            // With maxExampleDepth=2, we should stop at depth 2
            // level1Prop (depth 1) -> level2Prop (depth 2) -> stops
            assertThat(result).containsKey("level1Prop")
            @Suppress("UNCHECKED_CAST")
            val nested1 = result["level1Prop"] as Map<String, Any>
            assertThat(nested1).containsKey("level2Prop")
            @Suppress("UNCHECKED_CAST")
            val nested2 = nested1["level2Prop"] as Map<String, Any>
            // At depth 2, the nested object should be empty or truncated
            assertThat(nested2).isEmpty()
        }

        @Test
        @DisplayName("should handle circular references with depth limit")
        fun shouldHandleCircularReferencesWithDepthLimit() {
            val generator = factory.create(
                ExampleValueSettings(
                    providers = ExampleValueSettings.DEFAULT_PROVIDER_ORDER,
                    maxExampleDepth = 3
                )
            )

            val openAPI = OpenAPI().apply {
                components = Components().apply {
                    addSchemas("Node", ObjectSchema().apply {
                        addProperty("value", StringSchema().example("node"))
                        addProperty("child", Schema<Any>().apply { `$ref` = "#/components/schemas/Node" })
                        required = listOf("value")
                    })
                }
            }

            val schema = Schema<Any>().apply { `$ref` = "#/components/schemas/Node" }

            // Should not throw, should handle gracefully
            val result = generator.getExampleObject("node", schema, openAPI)
            assertThat(result).containsKey("value")
            assertThat(result["value"]).isEqualTo("node")
        }
    }

    @Nested
    @DisplayName("Missing provider handling")
    inner class MissingProviderHandling {

        @Test
        @DisplayName("should skip unknown providers and use remaining registered ones")
        fun shouldSkipUnknownProvidersAndUseRemainingRegisteredOnes() {
            val generator = factory.create(
                ExampleValueSettings(
                    providers = listOf("unknown-provider", "enum", "plain-string"),
                )
            )

            // Should still work with the registered providers
            val schema = StringSchema().apply {
                enum = listOf("a", "b")
            }
            val value = generator.getExampleValue("field", schema, OpenAPI())
            assertThat(value).isEqualTo("a")
        }

        @Test
        @DisplayName("should fall back to defaults when all configured providers are missing")
        fun shouldFallBackToDefaultsWhenAllProvidersMissing() {
            val generator = factory.create(
                ExampleValueSettings(
                    providers = listOf("nonexistent-a", "nonexistent-b"),
                )
            )

            // Should fall back to default providers
            val schema = StringSchema()
            val value = generator.getExampleValue("field", schema, OpenAPI())
            assertThat(value).isEqualTo("a")
        }

        @Test
        @DisplayName("should use first valid provider when some are missing")
        fun shouldUseFirstValidProviderWhenSomeAreMissing() {
            val generator = factory.create(
                ExampleValueSettings(
                    providers = listOf("missing1", "missing2", "uuid", "plain-string"),
                )
            )

            val schema = StringSchema().format("uuid")
            val value = generator.getExampleValue("field", schema, OpenAPI())
            assertThat(value.toString()).matches("[a-f0-9-]{36}")
        }
    }

    @Nested
    @DisplayName("Extra providers validation")
    inner class ExtraProvidersValidation {
        fun invalidExtraProviderTestCases(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "blank provider ID",
                "",
                "SchemaValueProvider id must not be blank"
            ),
            Arguments.of(
                "whitespace-only provider ID",
                "   ",
                "SchemaValueProvider id must not be blank"
            ),
            Arguments.of(
                "duplicate with built-in uuid",
                "uuid",
                "Schema value provider 'uuid' already registered"
            ),
            Arguments.of(
                "duplicate with built-in enum",
                "enum",
                "Schema value provider 'enum' already registered"
            ),
            Arguments.of(
                "duplicate with built-in const",
                "const",
                "Schema value provider 'const' already registered"
            ),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidExtraProviderTestCases")
        @DisplayName("should reject invalid extra provider configurations")
        fun shouldRejectInvalidExtraProviderConfigurations(
            scenario: String,
            providerId: String,
            expectedMessagePart: String,
        ) {
            assertThatThrownBy {
                SchemaExampleValueGeneratorFactory(
                    extraProviders = mapOf(providerId to FixedPatternProvider)
                ).create()
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining(expectedMessagePart)
        }

        @Test
        @DisplayName("should accept valid custom provider ID")
        fun shouldAcceptValidCustomProviderId() {
            val customFactory = SchemaExampleValueGeneratorFactory(
                extraProviders = mapOf("custom-provider" to FixedPatternProvider)
            )

            // Should not throw
            val generator = customFactory.create(
                ExampleValueSettings(providers = listOf("custom-provider"))
            )

            assertThat(generator).isNotNull
        }

        @Test
        @DisplayName("should allow custom provider to integrate with built-ins")
        fun shouldAllowCustomProviderToIntegrateWithBuiltIns() {
            val customFactory = SchemaExampleValueGeneratorFactory(
                extraProviders = mapOf("pattern" to FixedPatternProvider)
            )

            val generator = customFactory.create(
                ExampleValueSettings(providers = listOf("pattern", "plain-string"))
            )

            // Custom provider matches
            val patternSchema = StringSchema().pattern("^abc$")
            assertThat(generator.getExampleValue("field", patternSchema, OpenAPI()))
                .isEqualTo("abc")

            // Falls through to plain-string when pattern doesn't match
            val plainSchema = StringSchema()
            assertThat(generator.getExampleValue("field", plainSchema, OpenAPI()))
                .isEqualTo("a")
        }
    }

    @Nested
    @DisplayName("Generator integration")
    inner class GeneratorIntegration {

        @Test
        @DisplayName("should create generator that handles complex nested schemas")
        fun shouldCreateGeneratorThatHandlesComplexNestedSchemas() {
            val generator = factory.create()

            val schema = ObjectSchema().apply {
                addProperty("name", StringSchema().example("John"))
                addProperty("age", IntegerSchema().example(30))
                addProperty("active", BooleanSchema().example(true))
                addProperty("tags", io.swagger.v3.oas.models.media.ArraySchema().apply {
                    items = StringSchema().example("tag")
                    minItems = 2
                })
                required = listOf("name", "age", "active", "tags")
            }

            val result = generator.getExampleObject("user", schema, OpenAPI())

            assertThat(result).containsEntry("name", "John")
            assertThat(result).containsEntry("age", 30)
            assertThat(result).containsEntry("active", true)
            @Suppress("UNCHECKED_CAST")
            assertThat(result["tags"] as List<Any>).hasSize(2)
        }

        @Test
        @DisplayName("should create generator with deterministic output")
        fun shouldCreateGeneratorWithDeterministicOutput() {
            val generator1 = factory.create()
            val generator2 = factory.create()

            val schema = ObjectSchema().apply {
                addProperty("id", StringSchema().format("uuid"))
                addProperty("email", StringSchema().format("email"))
                addProperty("count", IntegerSchema())
                required = listOf("id", "email", "count")
            }

            val result1 = generator1.getExampleObject("entity", schema, OpenAPI())
            val result2 = generator2.getExampleObject("entity", schema, OpenAPI())

            assertThat(result1).isEqualTo(result2)
        }
    }
}

private object FixedPatternProvider : SchemaValueProvider {
    override fun provide(schema: Schema<*>, variationIndex: Int): Any? {
        return if (schema.pattern == "^abc$") "abc" else null
    }
}
