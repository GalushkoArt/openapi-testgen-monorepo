package art.galushko.openapi.testgen.cli

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("KeyValueParser")
@Suppress("UNCHECKED_CAST")
class ParseKeyValueTest {

    private fun parseKeyValueArrayWithDotNotation(entries: Array<String>): Map<String, Any> {
        return KeyValueParser.parse(entries)
    }

    @Nested
    @DisplayName("Simple Values")
    inner class SimpleValues {

        @Test
        fun `should parse single simple value`() {
            val result = parseKeyValueArrayWithDotNotation(arrayOf("maxErrors=10"))

            assertEquals(1, result.size)
            assertEquals("10", result["maxErrors"])
        }

        @Test
        fun `should parse multiple simple values`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf(
                    "maxErrors=10",
                    "maxSchemaDepth=5",
                    "errorMode=FAIL_FAST"
                )
            )

            assertEquals(3, result.size)
            assertEquals("10", result["maxErrors"])
            assertEquals("5", result["maxSchemaDepth"])
            assertEquals("FAIL_FAST", result["errorMode"])
        }

        @Test
        fun `should handle empty array`() {
            val result = parseKeyValueArrayWithDotNotation(emptyArray())

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("Nested Maps")
    inner class NestedMaps {

        @Test
        fun `should parse single level nested map`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf("validSecurityValues.ApiKey=test-key-123")
            )

            assertEquals(1, result.size)
            assertTrue(result.containsKey("validSecurityValues"))

            val nested = result["validSecurityValues"] as Map<String, Any>
            assertEquals("test-key-123", nested["ApiKey"])
        }

        @Test
        fun `should parse multiple entries in nested map`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf(
                    "validSecurityValues.ApiKey=test-key-123",
                    "validSecurityValues.BearerToken=bearer-token-456"
                )
            )

            assertEquals(1, result.size)
            assertTrue(result.containsKey("validSecurityValues"))

            val nested = result["validSecurityValues"] as Map<String, Any>
            assertEquals(2, nested.size)
            assertEquals("test-key-123", nested["ApiKey"])
            assertEquals("bearer-token-456", nested["BearerToken"])
        }

        @Test
        fun `should parse deeply nested map`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf("level1.level2.level3.value=deep")
            )

            assertEquals(1, result.size)
            assertTrue(result.containsKey("level1"))

            var current = result["level1"] as Map<String, Any>
            assertTrue(current.containsKey("level2"))

            current = current["level2"] as Map<String, Any>
            assertTrue(current.containsKey("level3"))

            current = current["level3"] as Map<String, Any>
            assertEquals("deep", current["value"])
        }
    }

    @Nested
    @DisplayName("Wildcard Values")
    inner class WildcardValues {

        @Test
        fun `should preserve wildcard value for ignoreTestCases path`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf("ignoreTestCases./api/users=*")
            )

            assertEquals(1, result.size)
            assertTrue(result.containsKey("ignoreTestCases"))

            val ignoreTestCases = result["ignoreTestCases"] as Map<String, Any>
            assertEquals("*", ignoreTestCases["/api/users"])
        }

        @Test
        fun `should preserve wildcard value for ignoreTestCases method`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf("ignoreTestCases./api/users.DELETE=*")
            )

            assertEquals(1, result.size)
            assertTrue(result.containsKey("ignoreTestCases"))

            val ignoreTestCases = result["ignoreTestCases"] as Map<String, Any>
            assertTrue(ignoreTestCases.containsKey("/api/users"))

            val methods = ignoreTestCases["/api/users"] as Map<String, Any>
            assertEquals("*", methods["DELETE"])
        }

        @Test
        fun `should handle wildcard for all paths`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf("ignoreTestCases.*.HEAD=*")
            )

            assertEquals(1, result.size)
            assertTrue(result.containsKey("ignoreTestCases"))

            val ignoreTestCases = result["ignoreTestCases"] as Map<String, Any>
            assertTrue(ignoreTestCases.containsKey("*"))

            val methods = ignoreTestCases["*"] as Map<String, Any>
            assertEquals("*", methods["HEAD"])
        }
    }

    @Nested
    @DisplayName("Array Notation")
    inner class ArrayNotation {

        @Test
        fun `should parse single array value`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf("ignoreTestCases./api/users.GET[]=MissingRequiredParam")
            )

            assertEquals(1, result.size)
            assertTrue(result.containsKey("ignoreTestCases"))

            val ignoreTestCases = result["ignoreTestCases"] as Map<String, Any>
            assertTrue(ignoreTestCases.containsKey("/api/users"))

            val methods = ignoreTestCases["/api/users"] as Map<String, Any>
            assertTrue(methods.containsKey("GET"))

            val testCases = methods["GET"] as List<String>
            assertEquals(1, testCases.size)
            assertEquals(listOf("MissingRequiredParam"), testCases)
        }

        @Test
        fun `should merge multiple array values with same key`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf(
                    "ignoreTestCases./api/users.GET[]=MissingRequiredParam",
                    "ignoreTestCases./api/users.GET[]=InvalidFormat",
                    "ignoreTestCases./api/users.GET[]=UnauthorizedAccess"
                )
            )

            assertEquals(1, result.size)
            assertTrue(result.containsKey("ignoreTestCases"))

            val ignoreTestCases = result["ignoreTestCases"] as Map<String, Any>
            assertTrue(ignoreTestCases.containsKey("/api/users"))

            val methods = ignoreTestCases["/api/users"] as Map<String, Any>
            assertTrue(methods.containsKey("GET"))

            val testCases = methods["GET"] as List<String>
            assertEquals(3, testCases.size)
            assertEquals(
                listOf("MissingRequiredParam", "InvalidFormat", "UnauthorizedAccess"),
                testCases
            )
        }

        @Test
        fun `should handle array notation for multiple paths and methods`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf(
                    "ignoreTestCases./api/users.GET[]=test1",
                    "ignoreTestCases./api/users.GET[]=test2",
                    "ignoreTestCases./api/users.POST[]=test3",
                    "ignoreTestCases./api/products.DELETE[]=test4"
                )
            )

            assertEquals(1, result.size)
            assertTrue(result.containsKey("ignoreTestCases"))

            val ignoreTestCases = result["ignoreTestCases"] as Map<String, Any>
            assertEquals(2, ignoreTestCases.size)

            // Verify /api/users
            val usersMethods = ignoreTestCases["/api/users"] as Map<String, Any>
            assertEquals(2, usersMethods.size)

            val usersGet = usersMethods["GET"] as List<String>
            assertEquals(listOf("test1", "test2"), usersGet)

            val usersPost = usersMethods["POST"] as List<String>
            assertEquals(listOf("test3"), usersPost)

            // Verify /api/products
            val productsMethods = ignoreTestCases["/api/products"] as Map<String, Any>
            assertEquals(1, productsMethods.size)

            val productsDelete = productsMethods["DELETE"] as List<String>
            assertEquals(listOf("test4"), productsDelete)
        }

        @Test
        fun `should handle simple array notation`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf(
                    "simpleList[]=value1",
                    "simpleList[]=value2"
                )
            )

            assertEquals(1, result.size)
            assertTrue(result.containsKey("simpleList"))

            val list = result["simpleList"] as List<String>
            assertEquals(listOf("value1", "value2"), list)
        }
    }

    @Nested
    @DisplayName("Complex Mixed Scenarios")
    inner class ComplexMixedScenarios {

        @Test
        fun `should handle all ignoreTestCases patterns from validator`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf(
                    // Pattern 1: ignore entire path
                    "ignoreTestCases./api/admin=*",
                    // Pattern 2: ignore specific method
                    "ignoreTestCases./api/users.DELETE=*",
                    // Pattern 3: ignore specific test cases
                    "ignoreTestCases./api/products.GET[]=InvalidParameter",
                    "ignoreTestCases./api/products.GET[]=MissingAuth",
                    // Wildcard path
                    "ignoreTestCases.*.OPTIONS=*"
                )
            )

            assertEquals(1, result.size)
            assertTrue(result.containsKey("ignoreTestCases"))

            val ignoreTestCases = result["ignoreTestCases"] as Map<String, Any>
            assertEquals(4, ignoreTestCases.size)

            // Pattern 1: entire path
            assertEquals("*", ignoreTestCases["/api/admin"])

            // Pattern 2: specific method
            val usersMethods = ignoreTestCases["/api/users"] as Map<String, Any>
            assertEquals("*", usersMethods["DELETE"])

            // Pattern 3: specific test cases
            val productsMethods = ignoreTestCases["/api/products"] as Map<String, Any>
            val productsGetTests = productsMethods["GET"] as List<String>
            assertEquals(listOf("InvalidParameter", "MissingAuth"), productsGetTests)

            // Wildcard path
            val wildcardMethods = ignoreTestCases["*"] as Map<String, Any>
            assertEquals("*", wildcardMethods["OPTIONS"])
        }
    }

    @Nested
    @DisplayName("Edge Cases and Special Characters")
    inner class EdgeCasesAndSpecialCharacters {

        @Test
        fun `should handle paths with slashes`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf("ignoreTestCases./api/v1/users/{id}.GET=*")
            )

            assertEquals(1, result.size)
            val ignoreTestCases = result["ignoreTestCases"] as Map<String, Any>
            assertTrue(ignoreTestCases.containsKey("/api/v1/users/{id}"))
        }

        @Test
        fun `should handle values with special characters`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf("validSecurityValues.ApiKey=test-key-123!@#")
            )

            val nested = result["validSecurityValues"] as Map<String, Any>
            assertEquals("test-key-123!@#", nested["ApiKey"])
        }

        @Test
        fun `should handle value with equals sign`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf("base64Value=dGVzdD0xMjM=")
            )

            assertEquals("dGVzdD0xMjM=", result["base64Value"])
        }
    }

    @Nested
    @DisplayName("Validation and Error Cases")
    inner class ValidationAndErrorCases {

        @Test
        fun `should throw for missing equals sign`() {
            val exception = assertThrows(IllegalArgumentException::class.java) {
                parseKeyValueArrayWithDotNotation(arrayOf("invalidEntry"))
            }
            assertTrue(exception.message!!.contains("Invalid key=value: 'invalidEntry'"))
        }

        @Test
        fun `should throw for missing key`() {
            val exception = assertThrows(IllegalArgumentException::class.java) {
                parseKeyValueArrayWithDotNotation(arrayOf("=value"))
            }
            assertTrue(exception.message!!.contains("Invalid key=value: '=value'"))
        }

        @Test
        fun `should throw for conflicting scalar and nested values on same path`() {
            val exception = assertThrows(IllegalArgumentException::class.java) {
                parseKeyValueArrayWithDotNotation(
                    arrayOf(
                        "ignoreTestCases./users=*",
                        "ignoreTestCases./users.GET=test",
                    ),
                )
            }
            assertTrue(
                exception.message!!.contains("Conflicting values for 'ignoreTestCases./users.GET'"),
            )
        }

        @Test
        fun `should throw for conflicting nested and scalar values on same path`() {
            val exception = assertThrows(IllegalArgumentException::class.java) {
                parseKeyValueArrayWithDotNotation(
                    arrayOf(
                        "ignoreTestCases./users.GET=test",
                        "ignoreTestCases./users=*",
                    ),
                )
            }
            assertTrue(
                exception.message!!.contains("Conflicting values for 'ignoreTestCases./users'") ||
                    exception.message!!.contains("cannot assign scalar value to key '/users'"),
            )
        }
    }

    @Nested
    @DisplayName("Real-world Usage Scenarios")
    inner class RealWorldUsageScenarios {

        @Test
        fun `should parse typical CLI settings override`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf(
                    "maxErrors=100",
                    "maxSchemaDepth=10",
                    "errorMode=COLLECT_ALL",
                    "validSecurityValues.ApiKeyAuth=test-key-123",
                    "validSecurityValues.BearerAuth=Bearer token123",
                    "ignoreTestCases./admin=*",
                    "ignoreTestCases./users.DELETE=*",
                    "ignoreTestCases./products.GET[]=MinLengthTest",
                    "ignoreTestCases./products.GET[]=MaxLengthTest"
                )
            )

            assertEquals(5, result.size)
            assertEquals("100", result["maxErrors"])
            assertEquals("10", result["maxSchemaDepth"])
            assertEquals("COLLECT_ALL", result["errorMode"])

            val securityValues = result["validSecurityValues"] as Map<String, Any>
            assertEquals(2, securityValues.size)

            val ignoreTestCases = result["ignoreTestCases"] as Map<String, Any>
            assertEquals(3, ignoreTestCases.size)
        }

        @Test
        fun `should handle override basic test data`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf(
                    "overrideBasicTestData.validEmail=test@example.com",
                    "overrideBasicTestData.validPhone=+1234567890",
                    "overrideBasicTestData.validUrl=https://example.com"
                )
            )

            assertEquals(1, result.size)
            val overrides = result["overrideBasicTestData"] as Map<String, Any>
            assertEquals(3, overrides.size)
            assertEquals("test@example.com", overrides["validEmail"])
            assertEquals("+1234567890", overrides["validPhone"])
            assertEquals("https://example.com", overrides["validUrl"])
        }

        @Test
        fun `should parse exampleValues provider order with array notation`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf(
                    "exampleValues.providers[]=enum",
                    "exampleValues.providers[]=const",
                    "exampleValues.providers[]=pattern",
                    "exampleValues.providers[]=plain-string",
                )
            )

            assertEquals(1, result.size)
            assertTrue(result.containsKey("exampleValues"))

            val exampleValues = result["exampleValues"] as Map<String, Any>
            val providers = exampleValues["providers"] as List<String>
            assertEquals(listOf("enum", "const", "pattern", "plain-string"), providers)
        }

        @Test
        fun `should parse exampleValues nested overrides`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf(
                    "exampleValues.uuid.template=d5a5495b-cbdc-4237-a66e-%s",
                    "exampleValues.email.template=user%s@mycompany.com",
                    "exampleValues.date.startDate=2025-01-01",
                    "exampleValues.dateTime.startDate=2025-01-01",
                    "exampleValues.dateTime.timeSuffixTemplate=%sT00:00:00Z",
                    "exampleValues.plainString.validChars=abc123",
                    "exampleValues.maxExampleDepth=30",
                )
            )

            assertEquals(1, result.size)
            val exampleValues = result["exampleValues"] as Map<String, Any>
            assertEquals("30", exampleValues["maxExampleDepth"])

            val uuid = exampleValues["uuid"] as Map<String, Any>
            assertEquals("d5a5495b-cbdc-4237-a66e-%s", uuid["template"])

            val email = exampleValues["email"] as Map<String, Any>
            assertEquals("user%s@mycompany.com", email["template"])

            val date = exampleValues["date"] as Map<String, Any>
            assertEquals("2025-01-01", date["startDate"])

            val dateTime = exampleValues["dateTime"] as Map<String, Any>
            assertEquals("2025-01-01", dateTime["startDate"])
            assertEquals("%sT00:00:00Z", dateTime["timeSuffixTemplate"])

            val plainString = exampleValues["plainString"] as Map<String, Any>
            assertEquals("abc123", plainString["validChars"])
        }

        @Test
        fun `should parse patternGeneration options`() {
            val result = parseKeyValueArrayWithDotNotation(
                arrayOf(
                    "patternGeneration.defaultMinLength=10",
                    "patternGeneration.spaceChars= \t",
                    "patternGeneration.anyPrintableChars=abc",
                )
            )

            assertEquals(1, result.size)
            val patternGeneration = result["patternGeneration"] as Map<String, Any>
            assertEquals("10", patternGeneration["defaultMinLength"])
            assertEquals(" \t", patternGeneration["spaceChars"])
            assertEquals("abc", patternGeneration["anyPrintableChars"])
        }
    }
}
