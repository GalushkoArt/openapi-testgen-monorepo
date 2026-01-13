package art.galushko.openapi.testgen.config

import art.galushko.openapi.testgen.example.config.ConfigurationException
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Epic("Configuration")
@Feature("Config Merge")
@DisplayName("ConfigMerger")
class ConfigMergerTest {

    @Nested
    @Story("Deep Merge")
    @DisplayName("Deep Merge Behavior")
    inner class DeepMergeBehavior {

        @Test
        @DisplayName("should deep merge nested maps")
        @Description("Nested maps are merged recursively while non-map values are overridden")
        fun shouldDeepMergeNestedMaps() {
            val base = mapOf(
                "exampleValues" to mapOf(
                    "email" to mapOf(
                        "template" to "base%s@test.org",
                        "domain" to "base.org",
                    ),
                    "providers" to listOf("enum", "const"),
                ),
                "maxSchemaDepth" to 10,
            )
            val overrides = mapOf(
                "exampleValues" to mapOf(
                    "email" to mapOf(
                        "domain" to "override.org",
                        "username" to "user",
                    ),
                    "providers" to listOf<String>(),
                    "maxExampleDepth" to 5,
                ),
            )

            val result = ConfigMerger.merge(base, overrides)

            val expected = mapOf(
                "exampleValues" to mapOf(
                    "email" to mapOf(
                        "template" to "base%s@test.org",
                        "domain" to "override.org",
                        "username" to "user",
                    ),
                    "providers" to listOf<String>(),
                    "maxExampleDepth" to 5,
                ),
                "maxSchemaDepth" to 10,
            )
            assertThat(result).isEqualTo(expected)
        }

        @Test
        @DisplayName("should replace arrays instead of merging them")
        fun shouldReplaceArrays() {
            val base = mapOf("providers" to listOf("enum", "const"))
            val overrides = mapOf("providers" to listOf("pattern"))

            val result = ConfigMerger.merge(base, overrides)

            assertThat(result).isEqualTo(mapOf("providers" to listOf("pattern")))
        }

        @Test
        @DisplayName("should preserve base when override is empty")
        fun shouldPreserveBaseWhenOverrideIsEmpty() {
            val base = mapOf(
                "key1" to "value1",
                "nested" to mapOf("innerKey" to "innerValue"),
            )

            val result = ConfigMerger.merge(base, emptyMap())

            assertThat(result).isEqualTo(base)
        }

        @Test
        @DisplayName("should use override when base is empty")
        fun shouldUseOverrideWhenBaseIsEmpty() {
            val overrides = mapOf(
                "key1" to "value1",
                "nested" to mapOf("innerKey" to "innerValue"),
            )

            val result = ConfigMerger.merge(emptyMap(), overrides)

            assertThat(result).isEqualTo(overrides)
        }

        @Test
        @DisplayName("should let override entries win over base entries")
        fun shouldLetOverrideEntriesWin() {
            val base = mapOf("keep" to "fromBase", "replace" to "fromBase")
            val overrides = mapOf("replace" to "fromOverride", "add" to "new")

            val result = ConfigMerger.merge(base, overrides)

            assertThat(result).isEqualTo(
                mapOf(
                    "keep" to "fromBase",
                    "replace" to "fromOverride",
                    "add" to "new",
                )
            )
        }

        @Test
        @DisplayName("should preserve base when override value is null")
        fun shouldPreserveBaseWhenOverrideValueIsNull() {
            val base = mapOf("key" to "value", "nulled" to "preserved")
            val overrides = mapOf<String, Any?>("nulled" to null)

            val result = ConfigMerger.merge(base, overrides)

            assertThat(result).isEqualTo(mapOf("key" to "value", "nulled" to "preserved"))
        }

        @Test
        @DisplayName("should handle null in nested override - preserves base nested value")
        fun shouldPreserveBaseNestedValueWhenOverrideIsNull() {
            val base = mapOf("nested" to mapOf("key" to 42))
            val overrides = mapOf("nested" to mapOf<String, Any?>("key" to null))

            val result = ConfigMerger.merge(base, overrides)

            assertThat(result).isEqualTo(mapOf("nested" to mapOf("key" to 42)))
        }
    }

    @Nested
    @Story("Complex Merge")
    @DisplayName("Complex Merging Scenarios")
    inner class ComplexMergingScenarios {

        @Test
        @DisplayName("should merge 4-level deep nested structures")
        fun shouldMergeDeeplyNestedStructures() {
            val base = mapOf(
                "level1" to mapOf(
                    "level2" to mapOf(
                        "level3" to mapOf(
                            "level4" to mapOf(
                                "baseOnly" to "fromBase",
                                "shared" to "fromBase",
                            ),
                            "baseKey" to "baseValue",
                        ),
                        "level3BaseMap" to mapOf("baseKey" to "baseValue"),
                        "level3MissmatchList" to listOf("string"),
                        "level3MissmatchMap" to mapOf("nested" to mapOf("key" to "value")),
                        "level3MissmatchString" to "string",
                    ),
                ),
            )
            val overrides = mapOf(
                "level1" to mapOf(
                    "level2" to mapOf(
                        "level3" to mapOf(
                            "level4" to mapOf(
                                "shared" to "fromOverride",
                                "overrideOnly" to "new",
                            ),
                        ),
                        "newKey" to "added",
                        "level3MissmatchList" to "string",
                        "level3MissmatchMap" to listOf("list"),
                        "level3MissmatchString" to mapOf("nested" to mapOf("key" to "value")),
                    ),
                    "level2Map" to mapOf("newKey" to "added"),
                ),
            )

            val result = ConfigMerger.merge(base, overrides)

            val expected = mapOf(
                "level1" to mapOf(
                    "level2" to mapOf(
                        "level3" to mapOf(
                            "level4" to mapOf(
                                "baseOnly" to "fromBase",
                                "shared" to "fromOverride",
                                "overrideOnly" to "new",
                            ),
                            "baseKey" to "baseValue",
                        ),
                        "newKey" to "added",
                        "level3BaseMap" to mapOf("baseKey" to "baseValue"),
                        "level3MissmatchList" to "string",
                        "level3MissmatchMap" to listOf("list"),
                        "level3MissmatchString" to mapOf("nested" to mapOf("key" to "value")),
                    ),
                    "level2Map" to mapOf("newKey" to "added"),
                ),
            )
            assertThat(result).isEqualTo(expected)
        }

        @Test
        @DisplayName("should merge multiple sibling maps independently")
        fun shouldMergeSiblingMapsIndependently() {
            val base = mapOf(
                "database" to mapOf("host" to "localhost", "port" to 5432),
                "cache" to mapOf("host" to "redis", "ttl" to 3600),
                "logging" to mapOf("level" to "INFO"),
            )
            val overrides = mapOf(
                "database" to mapOf("port" to 5433, "ssl" to true),
                "cache" to mapOf("ttl" to 7200),
            )

            val result = ConfigMerger.merge(base, overrides)

            val expected = mapOf(
                "database" to mapOf("host" to "localhost", "port" to 5433, "ssl" to true),
                "cache" to mapOf("host" to "redis", "ttl" to 7200),
                "logging" to mapOf("level" to "INFO"),
            )
            assertThat(result).isEqualTo(expected)
        }

        @Test
        @DisplayName("should handle mixed scalar and map values at same level")
        fun shouldHandleMixedScalarAndMapValues() {
            val base = mapOf(
                "timeout" to 30,
                "retries" to 3,
                "headers" to mapOf("Content-Type" to "application/json"),
                "enabled" to true,
            )
            val overrides = mapOf(
                "timeout" to 60,
                "headers" to mapOf("Authorization" to "Bearer token"),
            )

            val result = ConfigMerger.merge(base, overrides)

            val expected = mapOf(
                "timeout" to 60,
                "retries" to 3,
                "headers" to mapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to "Bearer token",
                ),
                "enabled" to true,
            )
            assertThat(result).isEqualTo(expected)
        }

        @Test
        @DisplayName("should handle realistic testGenerationSettings merge")
        fun shouldHandleRealisticConfigMerge() {
            val base = mapOf(
                "maxSchemaDepth" to 10,
                "maxTestCasesPerOperation" to 500,
                "validSecurityValues" to mapOf(
                    "ApiKeyAuth" to "default-key",
                    "BearerAuth" to "default-token",
                ),
                "exampleValues" to mapOf(
                    "providers" to listOf("enum", "const", "pattern"),
                    "email" to mapOf("template" to "%s@example.com"),
                    "maxExampleDepth" to 5,
                ),
                "ignoreSchemaValidationRules" to listOf("OutOfMinimumLengthString"),
            )
            val overrides = mapOf(
                "maxSchemaDepth" to 15,
                "validSecurityValues" to mapOf(
                    "ApiKeyAuth" to "prod-key",
                    "OAuth2" to "prod-oauth-token",
                ),
                "exampleValues" to mapOf(
                    "email" to mapOf("domain" to "prod.example.com"),
                ),
            )

            val result = ConfigMerger.merge(base, overrides, "testGenerationSettings")

            val expected = mapOf(
                "maxSchemaDepth" to 15,
                "maxTestCasesPerOperation" to 500,
                "validSecurityValues" to mapOf(
                    "ApiKeyAuth" to "prod-key",
                    "BearerAuth" to "default-token",
                    "OAuth2" to "prod-oauth-token",
                ),
                "exampleValues" to mapOf(
                    "providers" to listOf("enum", "const", "pattern"),
                    "email" to mapOf(
                        "template" to "%s@example.com",
                        "domain" to "prod.example.com",
                    ),
                    "maxExampleDepth" to 5,
                ),
                "ignoreSchemaValidationRules" to listOf("OutOfMinimumLengthString"),
            )
            assertThat(result).isEqualTo(expected)
        }
    }

    @Nested
    @Story("Edge Cases")
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        @DisplayName("should fail on non-string map keys")
        fun shouldFailOnNonStringMapKeys() {
            val base = emptyMap<String, Any?>()
            val overrides = mapOf("key" to mapOf(123 to "value"))

            assertThatThrownBy { ConfigMerger.merge(base, overrides) }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessageContaining("map.keys")
                .hasMessageContaining("expected String")
        }

        @Test
        @DisplayName("should handle empty maps at all levels")
        fun shouldHandleEmptyMapsAtAllLevels() {
            val result = ConfigMerger.merge(emptyMap(), emptyMap())
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("should handle various value types")
        fun shouldHandleVariousValueTypes() {
            val base = mapOf(
                "string" to "text",
                "int" to 42,
                "double" to 3.14,
                "boolean" to true,
                "list" to listOf(1, 2, 3),
            )
            val overrides = mapOf(
                "int" to 100,
                "newKey" to "added",
            )

            val result = ConfigMerger.merge(base, overrides)

            val expected = mapOf(
                "string" to "text",
                "int" to 100,
                "double" to 3.14,
                "boolean" to true,
                "list" to listOf(1, 2, 3),
                "newKey" to "added",
            )
            assertThat(result).isEqualTo(expected)
        }
    }
}
