package art.galushko.openapi.testgen.cli

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import picocli.CommandLine

@DisplayName("CLI Setting Parsing")
class SettingParsingTest {

    @Nested
    @DisplayName("Picocli Argument Parsing")
    inner class PicocliIntegration {

        @Test
        @DisplayName("should split semicolon separated settings and parse to flat map")
        fun shouldSplitSemicolonSeparatedSettingsAndParse() {
            val command = GenerateCommand()
            CommandLine(command).parseArgs("--setting", "maxErrors=10;maxSchemaDepth=5")

            assertEquals(
                listOf("maxErrors=10", "maxSchemaDepth=5"),
                command.testSettingsRaw.toList(),
            )

            val parsed = KeyValueParser.parse(command.testSettingsRaw)
            assertEquals(
                mapOf(
                    "maxErrors" to "10",
                    "maxSchemaDepth" to "5",
                ),
                parsed,
            )
        }

        @Test
        @DisplayName("should combine repeated flags into nested structure with arrays")
        fun shouldCombineRepeatedFlagsIntoNestedStructure() {
            val command = GenerateCommand()
            CommandLine(command).parseArgs(
                "--setting", "exampleValues.providers[]=enum;exampleValues.providers[]=const",
                "--setting", "validSecurityValues.ApiKeyAuth=test-key",
            )

            val parsed = KeyValueParser.parse(command.testSettingsRaw)

            val expected = mapOf(
                "exampleValues" to mapOf(
                    "providers" to mutableListOf("enum", "const"),
                ),
                "validSecurityValues" to mapOf(
                    "ApiKeyAuth" to "test-key",
                ),
            )
            assertEquals(expected, parsed)
        }
    }

    @Nested
    @DisplayName("KeyValueParser")
    inner class KeyValueParserTests {

        @Test
        @DisplayName("should parse deeply nested paths into nested map structure")
        fun shouldParseDeeplyNestedPaths() {
            val parsed = KeyValueParser.parse(arrayOf("a.b.c.d=deep-value"))

            val expected = mapOf(
                "a" to mapOf(
                    "b" to mapOf(
                        "c" to mapOf(
                            "d" to "deep-value",
                        ),
                    ),
                ),
            )
            assertEquals(expected, parsed)
        }

        @Test
        @DisplayName("should handle empty input and values with equals signs")
        fun shouldHandleEdgeCases() {
            assertEquals(emptyMap<String, Any>(), KeyValueParser.parse(emptyArray()))

            assertEquals(
                mapOf("key" to "value=with=equals"),
                KeyValueParser.parse(arrayOf("key=value=with=equals")),
            )
        }

        @Test
        @DisplayName("should reject invalid formats")
        fun shouldRejectInvalidFormats() {
            val invalidInputs = listOf("no-equals-sign", "=value-only", "key-only=")

            invalidInputs.forEach { input ->
                val exception = assertThrows(IllegalArgumentException::class.java) {
                    KeyValueParser.parse(arrayOf(input))
                }
                assertTrue(
                    exception.message?.contains("Invalid key=value") == true,
                    "Expected 'Invalid key=value' in message for input '$input', got: ${exception.message}",
                )
            }
        }

        @Test
        @DisplayName("should reject conflicting scalar and map types")
        fun shouldRejectConflictingTypes() {
            val exception = assertThrows(IllegalArgumentException::class.java) {
                KeyValueParser.parse(arrayOf("key=scalar", "key.nested=map"))
            }
            assertTrue(
                exception.message?.contains("Conflicting values") == true,
                "Expected 'Conflicting values' in message, got: ${exception.message}",
            )
        }
    }
}
