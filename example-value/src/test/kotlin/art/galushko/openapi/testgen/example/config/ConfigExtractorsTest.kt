package art.galushko.openapi.testgen.example.config

import art.galushko.openapi.testgen.model.error.ErrorMode
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
@Feature("Type-Safe Extractors")
@DisplayName("ConfigExtractors")
class ConfigExtractorsTest {

    @Nested
    @Story("String-Any Map Extraction")
    @DisplayName("extractStringAnyMap")
    inner class StringAnyMapExtraction {

        @Test
        @DisplayName("should extract valid Map<String, Any>")
        @Description("Properly extracts and removes a Map<String, Any> from the source map")
        fun shouldExtractValidMap() {
            val map = mutableMapOf<String, Any?>(
                "test" to mapOf("key1" to "value1", "key2" to 123)
            )

            val result = ConfigExtractors.extractStringAnyMap("test", map)

            assertThat(result).containsEntry("key1", "value1")
            assertThat(result).containsEntry("key2", 123)
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should return null for absent key")
        fun shouldReturnNullForAbsentKey() {
            val map = mutableMapOf<String, Any?>()

            val result = ConfigExtractors.extractStringAnyMap("missing", map)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("should return null for null value")
        fun shouldReturnNullForNullValue() {
            val map = mutableMapOf<String, Any?>("test" to null)

            val result = ConfigExtractors.extractStringAnyMap("test", map)

            assertThat(result).isNull()
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should throw for non-map type")
        fun shouldThrowForNonMap() {
            val map = mutableMapOf<String, Any?>("test" to "not a map")

            assertThatThrownBy {
                ConfigExtractors.extractStringAnyMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test': expected Map<String, Any>, got kotlin.String")
        }

        @Test
        @DisplayName("should throw for non-string key")
        fun shouldThrowForNonStringKey() {
            val map = mutableMapOf<String, Any?>("test" to mapOf(123 to "value"))

            assertThatThrownBy {
                ConfigExtractors.extractStringAnyMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test.keys': expected String, got kotlin.Int")
        }

        @Test
        @DisplayName("should throw for null key in map")
        @Suppress("UNCHECKED_CAST")
        fun shouldThrowForNullKeyInMap() {
            val innerMap = HashMap<Any?, Any?>()
            innerMap[null] = "value"
            val map = mutableMapOf<String, Any?>("test" to innerMap)

            assertThatThrownBy {
                ConfigExtractors.extractStringAnyMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test.keys': expected String, got null")
        }

        @Test
        @DisplayName("should throw for null value in map")
        fun shouldThrowForNullValueInMap() {
            val mapWithNullValue = HashMap<String, Any?>()
            mapWithNullValue["key"] = null
            val map = mutableMapOf<String, Any?>("test" to mapWithNullValue)

            assertThatThrownBy {
                ConfigExtractors.extractStringAnyMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test[key]': expected non-null value, got null")
        }
    }

    @Nested
    @Story("String-String Map Extraction")
    @DisplayName("extractStringStringMap")
    inner class StringStringMapExtraction {

        @Test
        @DisplayName("should extract valid Map<String, String>")
        fun shouldExtractValidMap() {
            val map = mutableMapOf<String, Any?>(
                "test" to mapOf("key1" to "value1", "key2" to "value2")
            )

            val result = ConfigExtractors.extractStringStringMap("test", map)

            assertThat(result).containsEntry("key1", "value1")
            assertThat(result).containsEntry("key2", "value2")
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should throw for non-string value")
        fun shouldThrowForNonStringValue() {
            val map = mutableMapOf<String, Any?>("test" to mapOf("key" to 123))

            assertThatThrownBy {
                ConfigExtractors.extractStringStringMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test[key]': expected String, got kotlin.Int")
        }

        @Test
        @DisplayName("should throw for null value")
        fun shouldThrowForNullValueInMap() {
            val mapWithNullValue = HashMap<String, String?>()
            mapWithNullValue["key"] = null
            val map = mutableMapOf<String, Any?>("test" to mapWithNullValue)

            assertThatThrownBy {
                ConfigExtractors.extractStringStringMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test[key]': expected String, got null")
        }

        @Test
        @DisplayName("should return null for absent key")
        fun shouldReturnNullForAbsentKey() {
            val map = mutableMapOf<String, Any?>()

            val result = ConfigExtractors.extractStringStringMap("missing", map)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("should return null for null value")
        fun shouldReturnNullForNullValue() {
            val map = mutableMapOf<String, Any?>("test" to null)

            val result = ConfigExtractors.extractStringStringMap("test", map)

            assertThat(result).isNull()
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should throw for non-map type")
        fun shouldThrowForNonMap() {
            val map = mutableMapOf<String, Any?>("test" to "not a map")

            assertThatThrownBy {
                ConfigExtractors.extractStringStringMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test': expected Map<String, String>, got kotlin.String")
        }

        @Test
        @DisplayName("should throw for non-string key")
        fun shouldThrowForNonStringKey() {
            val map = mutableMapOf<String, Any?>("test" to mapOf(123 to "value"))

            assertThatThrownBy {
                ConfigExtractors.extractStringStringMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test.keys': expected String, got kotlin.Int")
        }

        @Test
        @DisplayName("should throw for null key in map")
        @Suppress("UNCHECKED_CAST")
        fun shouldThrowForNullKeyInMap() {
            val innerMap = HashMap<Any?, Any?>()
            innerMap[null] = "value"
            val map = mutableMapOf<String, Any?>("test" to innerMap)

            assertThatThrownBy {
                ConfigExtractors.extractStringStringMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test.keys': expected String, got null")
        }
    }

    @Nested
    @Story("String Set Extraction")
    @DisplayName("extractStringSet")
    inner class StringSetExtraction {

        @Test
        @DisplayName("should extract valid Set<String>")
        fun shouldExtractValidSet() {
            val map = mutableMapOf<String, Any?>(
                "test" to listOf("a", "b", "c")
            )

            val result = ConfigExtractors.extractStringSet("test", map)

            assertThat(result).containsExactlyInAnyOrder("a", "b", "c")
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should handle Set input")
        fun shouldHandleSetInput() {
            val map = mutableMapOf<String, Any?>(
                "test" to setOf("x", "y")
            )

            val result = ConfigExtractors.extractStringSet("test", map)

            assertThat(result).containsExactlyInAnyOrder("x", "y")
        }

        @Test
        @DisplayName("should throw for non-collection type")
        fun shouldThrowForNonCollection() {
            val map = mutableMapOf<String, Any?>("test" to "not-a-list")

            assertThatThrownBy {
                ConfigExtractors.extractStringSet("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test': expected Collection<String>, got kotlin.String")
        }

        @Test
        @DisplayName("should throw for non-string element with index")
        fun shouldThrowForNonStringElement() {
            val map = mutableMapOf<String, Any?>("test" to listOf("a", 123, "b"))

            assertThatThrownBy {
                ConfigExtractors.extractStringSet("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test[1]': expected String, got kotlin.Int")
        }

        @Test
        @DisplayName("should throw for null element")
        fun shouldThrowForNullElement() {
            val map = mutableMapOf<String, Any?>("test" to listOf("a", null))

            assertThatThrownBy {
                ConfigExtractors.extractStringSet("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test[1]': expected String, got null")
        }

        @Test
        @DisplayName("should return null for absent key")
        fun shouldReturnNullForAbsentKey() {
            val map = mutableMapOf<String, Any?>()

            val result = ConfigExtractors.extractStringSet("missing", map)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("should return null for null value")
        fun shouldReturnNullForNullValue() {
            val map = mutableMapOf<String, Any?>("test" to null)

            val result = ConfigExtractors.extractStringSet("test", map)

            assertThat(result).isNull()
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should handle empty collection")
        fun shouldHandleEmptyCollection() {
            val map = mutableMapOf<String, Any?>("test" to emptyList<String>())

            val result = ConfigExtractors.extractStringSet("test", map)

            assertThat(result).isEmpty()
            assertThat(map).isEmpty()
        }
    }

    @Nested
    @Story("Boolean Extraction")
    @DisplayName("extractBoolean")
    inner class BooleanExtraction {

        @Test
        @DisplayName("should extract Boolean directly")
        fun shouldExtractBoolean() {
            val map = mutableMapOf<String, Any?>("field" to true)

            val result = ConfigExtractors.extractBoolean("field", map)

            assertThat(result).isTrue
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should parse boolean string")
        fun shouldParseBooleanString() {
            val map = mutableMapOf<String, Any?>("field" to "false")

            val result = ConfigExtractors.extractBoolean("field", map)

            assertThat(result).isFalse
        }

        @Test
        @DisplayName("should throw for non-boolean string")
        fun shouldThrowForNonBooleanString() {
            val map = mutableMapOf<String, Any?>("field" to "not-boolean")

            assertThatThrownBy {
                ConfigExtractors.extractBoolean("field", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'field': expected Boolean or boolean String, got non-boolean string: 'not-boolean'")
        }

        @Test
        @DisplayName("should throw for invalid type")
        fun shouldThrowForInvalidType() {
            val map = mutableMapOf<String, Any?>("field" to 1)

            assertThatThrownBy {
                ConfigExtractors.extractBoolean("field", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'field': expected Boolean or boolean String, got kotlin.Int")
        }

        @Test
        @DisplayName("should return null for absent key")
        fun shouldReturnNullForAbsentKey() {
            val map = mutableMapOf<String, Any?>()

            val result = ConfigExtractors.extractBoolean("missing", map)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("should return null for null value")
        fun shouldReturnNullForNullValue() {
            val map = mutableMapOf<String, Any?>("field" to null)

            val result = ConfigExtractors.extractBoolean("field", map)

            assertThat(result).isNull()
            assertThat(map).isEmpty()
        }
    }

    @Nested
    @Story("Integer Extraction")
    @DisplayName("extractInteger")
    inner class IntegerExtraction {

        @Test
        @DisplayName("should extract Int directly")
        fun shouldExtractInt() {
            val map = mutableMapOf<String, Any?>("field" to 42)

            val result = ConfigExtractors.extractInteger("field", map)

            assertThat(result).isEqualTo(42)
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should convert Long to Int")
        fun shouldConvertLong() {
            val map = mutableMapOf<String, Any?>("field" to 42L)

            val result = ConfigExtractors.extractInteger("field", map)

            assertThat(result).isEqualTo(42)
        }

        @Test
        @DisplayName("should convert Double to Int")
        fun shouldConvertDouble() {
            val map = mutableMapOf<String, Any?>("field" to 42.9)

            val result = ConfigExtractors.extractInteger("field", map)

            assertThat(result).isEqualTo(42)
        }

        @Test
        @DisplayName("should parse numeric string")
        fun shouldParseNumericString() {
            val map = mutableMapOf<String, Any?>("field" to "123")

            val result = ConfigExtractors.extractInteger("field", map)

            assertThat(result).isEqualTo(123)
        }

        @Test
        @DisplayName("should throw for non-numeric string")
        fun shouldThrowForNonNumericString() {
            val map = mutableMapOf<String, Any?>("field" to "not-a-number")

            assertThatThrownBy {
                ConfigExtractors.extractInteger("field", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'field': expected Integer or numeric string, got non-numeric string: 'not-a-number'")
        }

        @Test
        @DisplayName("should throw for Long out of Int range")
        fun shouldThrowForOutOfRangeLong() {
            val map = mutableMapOf<String, Any?>("field" to Long.MAX_VALUE)

            assertThatThrownBy {
                ConfigExtractors.extractInteger("field", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'field': expected Int (32-bit integer), got Long out of Int range: ${Long.MAX_VALUE}")
        }

        @Test
        @DisplayName("should throw for invalid type")
        fun shouldThrowForInvalidType() {
            val map = mutableMapOf<String, Any?>("field" to true)

            assertThatThrownBy {
                ConfigExtractors.extractInteger("field", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'field': expected Number or numeric String, got kotlin.Boolean")
        }

        @Test
        @DisplayName("should return null for absent key")
        fun shouldReturnNullForAbsentKey() {
            val map = mutableMapOf<String, Any?>()

            val result = ConfigExtractors.extractInteger("missing", map)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("should return null for null value")
        fun shouldReturnNullForNullValue() {
            val map = mutableMapOf<String, Any?>("field" to null)

            val result = ConfigExtractors.extractInteger("field", map)

            assertThat(result).isNull()
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should throw for negative Long out of Int range")
        fun shouldThrowForNegativeOutOfRangeLong() {
            val map = mutableMapOf<String, Any?>("field" to Long.MIN_VALUE)

            assertThatThrownBy {
                ConfigExtractors.extractInteger("field", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'field': expected Int (32-bit integer), got Long out of Int range: ${Long.MIN_VALUE}")
        }

        @Test
        @DisplayName("should convert Long at Int boundary")
        fun shouldConvertLongAtIntBoundary() {
            val map = mutableMapOf<String, Any?>("field" to Int.MAX_VALUE.toLong())

            val result = ConfigExtractors.extractInteger("field", map)

            assertThat(result).isEqualTo(Int.MAX_VALUE)
        }
    }

    @Nested
    @Story("Enum Extraction")
    @DisplayName("extractEnum")
    inner class EnumExtraction {

        @Test
        @DisplayName("should extract enum value directly")
        fun shouldExtractEnumDirectly() {
            val map = mutableMapOf<String, Any?>("mode" to ErrorMode.FAIL_FAST)

            val result = ConfigExtractors.extractEnum<ErrorMode>("mode", map)

            assertThat(result).isEqualTo(ErrorMode.FAIL_FAST)
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should parse uppercase string")
        fun shouldParseUppercaseString() {
            val map = mutableMapOf<String, Any?>("mode" to "COLLECT_ALL")

            val result = ConfigExtractors.extractEnum<ErrorMode>("mode", map)

            assertThat(result).isEqualTo(ErrorMode.COLLECT_ALL)
        }

        @Test
        @DisplayName("should parse lowercase string")
        fun shouldParseLowercaseString() {
            val map = mutableMapOf<String, Any?>("mode" to "fail_fast")

            val result = ConfigExtractors.extractEnum<ErrorMode>("mode", map)

            assertThat(result).isEqualTo(ErrorMode.FAIL_FAST)
        }

        @Test
        @DisplayName("should parse hyphenated string")
        fun shouldParseHyphenatedString() {
            val map = mutableMapOf<String, Any?>("mode" to "collect-all")

            val result = ConfigExtractors.extractEnum<ErrorMode>("mode", map)

            assertThat(result).isEqualTo(ErrorMode.COLLECT_ALL)
        }

        @Test
        @DisplayName("should throw for invalid enum string")
        fun shouldThrowForInvalidEnumString() {
            val map = mutableMapOf<String, Any?>("mode" to "INVALID_MODE")

            assertThatThrownBy {
                ConfigExtractors.extractEnum<ErrorMode>("mode", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'mode': expected one of [FAIL_FAST, COLLECT_ALL], got 'INVALID_MODE'")
        }

        @Test
        @DisplayName("should throw for non-string type")
        fun shouldThrowForNonStringType() {
            val map = mutableMapOf<String, Any?>("mode" to 123)

            assertThatThrownBy {
                ConfigExtractors.extractEnum<ErrorMode>("mode", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'mode': expected ErrorMode or String, got kotlin.Int")
        }

        @Test
        @DisplayName("should return null for absent key")
        fun shouldReturnNullForAbsentKey() {
            val map = mutableMapOf<String, Any?>()

            val result = ConfigExtractors.extractEnum<ErrorMode>("missing", map)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("should return null for null value")
        fun shouldReturnNullForNullValue() {
            val map = mutableMapOf<String, Any?>("mode" to null)

            val result = ConfigExtractors.extractEnum<ErrorMode>("mode", map)

            assertThat(result).isNull()
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should parse mixed case hyphenated string")
        fun shouldParseMixedCaseHyphenatedString() {
            val map = mutableMapOf<String, Any?>("mode" to "Collect-All")

            val result = ConfigExtractors.extractEnum<ErrorMode>("mode", map)

            assertThat(result).isEqualTo(ErrorMode.COLLECT_ALL)
        }

        @Test
        @DisplayName("should parse mixed case underscore string")
        fun shouldParseMixedCaseUnderscoreString() {
            val map = mutableMapOf<String, Any?>("mode" to "Fail_Fast")

            val result = ConfigExtractors.extractEnum<ErrorMode>("mode", map)

            assertThat(result).isEqualTo(ErrorMode.FAIL_FAST)
        }
    }

    @Nested
    @Story("String List Extraction")
    @DisplayName("extractStringList")
    inner class StringListExtraction {

        @Test
        @DisplayName("should extract valid List<String>")
        fun shouldExtractValidList() {
            val map = mutableMapOf<String, Any?>(
                "test" to listOf("a", "b", "c")
            )

            val result = ConfigExtractors.extractStringList("test", map)

            assertThat(result).containsExactly("a", "b", "c")
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should preserve list order")
        fun shouldPreserveListOrder() {
            val map = mutableMapOf<String, Any?>(
                "test" to listOf("z", "a", "m")
            )

            val result = ConfigExtractors.extractStringList("test", map)

            assertThat(result).containsExactly("z", "a", "m")
        }

        @Test
        @DisplayName("should throw for non-collection type")
        fun shouldThrowForNonCollection() {
            val map = mutableMapOf<String, Any?>("test" to "not-a-list")

            assertThatThrownBy {
                ConfigExtractors.extractStringList("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test': expected Collection<String>, got kotlin.String")
        }

        @Test
        @DisplayName("should throw for non-string element with index")
        fun shouldThrowForNonStringElement() {
            val map = mutableMapOf<String, Any?>("test" to listOf("a", 123, "b"))

            assertThatThrownBy {
                ConfigExtractors.extractStringList("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test[1]': expected String, got kotlin.Int")
        }

        @Test
        @DisplayName("should throw for null element")
        fun shouldThrowForNullElement() {
            val map = mutableMapOf<String, Any?>("test" to listOf("a", null))

            assertThatThrownBy {
                ConfigExtractors.extractStringList("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test[1]': expected String, got null")
        }

        @Test
        @DisplayName("should return null for absent key")
        fun shouldReturnNullForAbsentKey() {
            val map = mutableMapOf<String, Any?>()

            val result = ConfigExtractors.extractStringList("missing", map)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("should return null for null value")
        fun shouldReturnNullForNullValue() {
            val map = mutableMapOf<String, Any?>("test" to null)

            val result = ConfigExtractors.extractStringList("test", map)

            assertThat(result).isNull()
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should handle empty collection")
        fun shouldHandleEmptyCollection() {
            val map = mutableMapOf<String, Any?>("test" to emptyList<String>())

            val result = ConfigExtractors.extractStringList("test", map)

            assertThat(result).isEmpty()
            assertThat(map).isEmpty()
        }
    }

    @Nested
    @Story("String-Any Nullable Map Extraction")
    @DisplayName("extractStringAnyNullableMap")
    inner class StringAnyNullableMapExtraction {

        @Test
        @DisplayName("should extract valid Map<String, Any?>")
        fun shouldExtractValidMap() {
            val map = mutableMapOf<String, Any?>(
                "test" to mapOf("key1" to "value1", "key2" to 123)
            )

            val result = ConfigExtractors.extractStringAnyNullableMap("test", map)

            assertThat(result).containsEntry("key1", "value1")
            assertThat(result).containsEntry("key2", 123)
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should allow null values in map")
        fun shouldAllowNullValuesInMap() {
            val mapWithNullValue = HashMap<String, Any?>()
            mapWithNullValue["key1"] = "value"
            mapWithNullValue["key2"] = null
            val map = mutableMapOf<String, Any?>("test" to mapWithNullValue)

            val result = ConfigExtractors.extractStringAnyNullableMap("test", map)

            assertThat(result).containsEntry("key1", "value")
            assertThat(result).containsEntry("key2", null)
        }

        @Test
        @DisplayName("should return null for absent key")
        fun shouldReturnNullForAbsentKey() {
            val map = mutableMapOf<String, Any?>()

            val result = ConfigExtractors.extractStringAnyNullableMap("missing", map)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("should return null for null value")
        fun shouldReturnNullForNullValue() {
            val map = mutableMapOf<String, Any?>("test" to null)

            val result = ConfigExtractors.extractStringAnyNullableMap("test", map)

            assertThat(result).isNull()
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should throw for non-map type")
        fun shouldThrowForNonMap() {
            val map = mutableMapOf<String, Any?>("test" to "not a map")

            assertThatThrownBy {
                ConfigExtractors.extractStringAnyNullableMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test': expected Map<String, Any?>, got kotlin.String")
        }

        @Test
        @DisplayName("should throw for non-string key")
        fun shouldThrowForNonStringKey() {
            val map = mutableMapOf<String, Any?>("test" to mapOf(123 to "value"))

            assertThatThrownBy {
                ConfigExtractors.extractStringAnyNullableMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test.keys': expected String, got kotlin.Int")
        }

        @Test
        @DisplayName("should throw for null key in map")
        @Suppress("UNCHECKED_CAST")
        fun shouldThrowForNullKeyInMap() {
            val innerMap = HashMap<Any?, Any?>()
            innerMap[null] = "value"
            val map = mutableMapOf<String, Any?>("test" to innerMap)

            assertThatThrownBy {
                ConfigExtractors.extractStringAnyNullableMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test.keys': expected String, got null")
        }
    }

    @Nested
    @Story("String-List Map Extraction")
    @DisplayName("extractStringListMap")
    inner class StringListMapExtraction {

        @Test
        @DisplayName("should extract valid Map<String, List<String>>")
        @Description("Properly extracts and removes a Map<String, List<String>> from the source map")
        fun shouldExtractValidMap() {
            val map = mutableMapOf<String, Any?>(
                "test" to mapOf("/users" to listOf("GET", "POST"), "/orders" to listOf("DELETE"))
            )

            val result = ConfigExtractors.extractStringListMap("test", map)

            assertThat(result).containsEntry("/users", listOf("GET", "POST"))
            assertThat(result).containsEntry("/orders", listOf("DELETE"))
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should convert single string to single-element list")
        @Description("String shorthand is converted to single-element list")
        fun shouldConvertStringToSingleElementList() {
            val map = mutableMapOf<String, Any?>(
                "test" to mapOf("/users" to "GET", "/orders" to listOf("POST", "PUT"))
            )

            val result = ConfigExtractors.extractStringListMap("test", map)

            assertThat(result).containsEntry("/users", listOf("GET"))
            assertThat(result).containsEntry("/orders", listOf("POST", "PUT"))
        }

        @Test
        @DisplayName("should return null for absent key")
        fun shouldReturnNullForAbsentKey() {
            val map = mutableMapOf<String, Any?>()

            val result = ConfigExtractors.extractStringListMap("missing", map)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("should return null for null value")
        fun shouldReturnNullForNullValue() {
            val map = mutableMapOf<String, Any?>("test" to null)

            val result = ConfigExtractors.extractStringListMap("test", map)

            assertThat(result).isNull()
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should throw for non-map type")
        fun shouldThrowForNonMap() {
            val map = mutableMapOf<String, Any?>("test" to "not a map")

            assertThatThrownBy {
                ConfigExtractors.extractStringListMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test': expected Map<String, List<String>>, got kotlin.String")
        }

        @Test
        @DisplayName("should throw for non-string key")
        fun shouldThrowForNonStringKey() {
            val map = mutableMapOf<String, Any?>("test" to mapOf(123 to listOf("GET")))

            assertThatThrownBy {
                ConfigExtractors.extractStringListMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test.keys': expected String, got kotlin.Int")
        }

        @Test
        @DisplayName("should throw for null key in map")
        @Suppress("UNCHECKED_CAST")
        fun shouldThrowForNullKeyInMap() {
            val innerMap = HashMap<Any?, Any?>()
            innerMap[null] = listOf("GET")
            val map = mutableMapOf<String, Any?>("test" to innerMap)

            assertThatThrownBy {
                ConfigExtractors.extractStringListMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test.keys': expected String, got null")
        }

        @Test
        @DisplayName("should throw for invalid value type (not String or Collection)")
        fun shouldThrowForInvalidValueType() {
            val map = mutableMapOf<String, Any?>("test" to mapOf("/users" to 123))

            assertThatThrownBy {
                ConfigExtractors.extractStringListMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test[/users]': expected List<String> or String, got kotlin.Int")
        }

        @Test
        @DisplayName("should throw for null value in map")
        fun shouldThrowForNullValueInMap() {
            val mapWithNullValue = HashMap<String, Any?>()
            mapWithNullValue["/users"] = null
            val map = mutableMapOf<String, Any?>("test" to mapWithNullValue)

            assertThatThrownBy {
                ConfigExtractors.extractStringListMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test[/users]': expected List<String> or String, got null")
        }

        @Test
        @DisplayName("should throw for non-string element in list")
        fun shouldThrowForNonStringElementInList() {
            val map = mutableMapOf<String, Any?>("test" to mapOf("/users" to listOf("GET", 123)))

            assertThatThrownBy {
                ConfigExtractors.extractStringListMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test[/users][1]': expected String, got kotlin.Int")
        }

        @Test
        @DisplayName("should throw for null element in list")
        fun shouldThrowForNullElementInList() {
            val map = mutableMapOf<String, Any?>("test" to mapOf("/users" to listOf("GET", null)))

            assertThatThrownBy {
                ConfigExtractors.extractStringListMap("test", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'test[/users][1]': expected String, got null")
        }

        @Test
        @DisplayName("should handle empty list value")
        fun shouldHandleEmptyListValue() {
            val map = mutableMapOf<String, Any?>("test" to mapOf("/users" to emptyList<String>()))

            val result = ConfigExtractors.extractStringListMap("test", map)

            assertThat(result).containsEntry("/users", emptyList())
        }

        @Test
        @DisplayName("should handle empty map")
        fun shouldHandleEmptyMap() {
            val map = mutableMapOf<String, Any?>("test" to emptyMap<String, List<String>>())

            val result = ConfigExtractors.extractStringListMap("test", map)

            assertThat(result).isEmpty()
            assertThat(map).isEmpty()
        }
    }

    @Nested
    @Story("String Extraction")
    @DisplayName("extractString")
    inner class StringExtraction {

        @Test
        @DisplayName("should extract valid String")
        fun shouldExtractValidString() {
            val map = mutableMapOf<String, Any?>("field" to "hello")

            val result = ConfigExtractors.extractString("field", map)

            assertThat(result).isEqualTo("hello")
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should return null for absent key")
        fun shouldReturnNullForAbsentKey() {
            val map = mutableMapOf<String, Any?>()

            val result = ConfigExtractors.extractString("missing", map)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("should return null for null value")
        fun shouldReturnNullForNullValue() {
            val map = mutableMapOf<String, Any?>("field" to null)

            val result = ConfigExtractors.extractString("field", map)

            assertThat(result).isNull()
            assertThat(map).isEmpty()
        }

        @Test
        @DisplayName("should throw for non-string type")
        fun shouldThrowForNonString() {
            val map = mutableMapOf<String, Any?>("field" to 123)

            assertThatThrownBy {
                ConfigExtractors.extractString("field", map)
            }
                .isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration error for 'field': expected String, got kotlin.Int")
        }

        @Test
        @DisplayName("should handle empty string")
        fun shouldHandleEmptyString() {
            val map = mutableMapOf<String, Any?>("field" to "")

            val result = ConfigExtractors.extractString("field", map)

            assertThat(result).isEqualTo("")
        }
    }
}
