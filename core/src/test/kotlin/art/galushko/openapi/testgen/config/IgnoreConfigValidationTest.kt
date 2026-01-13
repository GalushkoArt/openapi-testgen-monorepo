package art.galushko.openapi.testgen.config

import art.galushko.openapi.testgen.config.IgnoreConfigValidator.validateIgnoreConfig
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Epic("Configuration")
@Feature("Ignore Configuration Validation")
@DisplayName("validateIgnoreConfig")
class IgnoreConfigValidationTest {

    @Nested
    @Story("Valid Configurations")
    @DisplayName("Valid Configurations")
    inner class ValidConfigurations {

        @Test
        @DisplayName("should accept null input")
        @Description("Null input is valid - no configuration to validate")
        fun shouldAcceptNullInput() {
            Assertions.assertThatNoException().isThrownBy {
                validateIgnoreConfig(null)
            }
        }

        @Test
        @DisplayName("should accept empty map")
        @Description("Empty map is valid - no ignore rules specified")
        fun shouldAcceptEmptyMap() {
            val config = emptyMap<String, Any>()

            Assertions.assertThatNoException().isThrownBy {
                validateIgnoreConfig(config)
            }
        }

        @Test
        @DisplayName("should accept path with star wildcard")
        @Description("Path key mapped to star string means ignore all operations for that path")
        fun shouldAcceptPathWithStarWildcard() {
            val config = mapOf("/api/users" to "*")

            Assertions.assertThatNoException().isThrownBy {
                validateIgnoreConfig(config)
            }
        }

        @Test
        @DisplayName("should accept path with method star")
        @Description("Path key mapped to map with method key mapped to star means ignore all test cases for that method")
        fun shouldAcceptPathWithMethodStar() {
            val config = mapOf(
                "/api/users" to mapOf("DELETE" to "*")
            )

            Assertions.assertThatNoException().isThrownBy {
                validateIgnoreConfig(config)
            }
        }

        @Test
        @DisplayName("should accept path with test case list")
        @Description("Path key mapped to map with method key mapped to list of strings means ignore specific test cases by name")
        fun shouldAcceptPathWithTestCaseList() {
            val config = mapOf(
                "/api/users" to mapOf(
                    "GET" to listOf("Invalid param", "Missing auth")
                )
            )

            Assertions.assertThatNoException().isThrownBy {
                validateIgnoreConfig(config)
            }
        }

        @Test
        @DisplayName("should accept wildcard path with method map")
        @Description("Wildcard path '*' applies ignore rules to all paths in the API")
        fun shouldAcceptWildcardPathWithMethodMap() {
            val config = mapOf(
                "*" to mapOf("HEAD" to "*")
            )

            Assertions.assertThatNoException().isThrownBy {
                validateIgnoreConfig(config)
            }
        }

        @Test
        @DisplayName("should accept complex nested structure")
        @Description("Complex configuration with multiple paths, methods, and ignore rules")
        fun shouldAcceptComplexNestedStructure() {
            val config = mapOf(
                "/api/users" to mapOf(
                    "GET" to listOf("test1", "test2"),
                    "POST" to "*"
                ),
                "/api/products" to "*",
                "*" to mapOf("OPTIONS" to "*")
            )

            Assertions.assertThatNoException().isThrownBy {
                validateIgnoreConfig(config)
            }
        }

        @Test
        @DisplayName("should accept empty test case list")
        @Description("Empty list is valid - no test cases to ignore for this method")
        fun shouldAcceptEmptyTestCaseList() {
            val config = mapOf(
                "/api/users" to mapOf("GET" to emptyList<String>())
            )

            Assertions.assertThatNoException().isThrownBy {
                validateIgnoreConfig(config)
            }
        }

        @Test
        @DisplayName("should accept multiple methods for same path")
        @Description("Multiple HTTP methods can be configured with different ignore rules for the same path")
        fun shouldAcceptMultipleMethodsForSamePath() {
            val config = mapOf(
                "/api/users" to mapOf(
                    "GET" to listOf("test1"),
                    "POST" to "*",
                    "DELETE" to listOf("test2", "test3")
                )
            )

            Assertions.assertThatNoException().isThrownBy {
                validateIgnoreConfig(config)
            }
        }
    }

    @Nested
    @Story("Invalid Configurations")
    @DisplayName("Invalid Configurations")
    inner class InvalidConfigurations {

        @Test
        @DisplayName("should reject non-string path key")
        @Description("Path keys must be strings - integer keys are invalid")
        fun shouldRejectNonStringPathKey() {
            val config = mapOf(123 to "*")

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo(
                "Path key must be String, was kotlin.Int"
            )
        }

        @Test
        @DisplayName("should reject null path key")
        @Description("Path keys cannot be null")
        fun shouldRejectNullPathKey() {
            val config = mapOf<Any?, Any?>(null to "*")

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo(
                "Path key must be String, was null"
            )
        }

        @Test
        @DisplayName("should reject invalid path value type")
        @Description("Path value must be star string or map - integer is invalid")
        fun shouldRejectInvalidPathValueType() {
            val config = mapOf("/test" to 123)

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo(
                "Value for path '/test' must be \"*\" or Map<String, ...>, was kotlin.Int"
            )
        }

        @Test
        @DisplayName("should reject null path value")
        @Description("Path value cannot be null")
        fun shouldRejectNullPathValue() {
            val config = mapOf<String, Any?>("/test" to null)

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo(
                "Value for path '/test' must be \"*\" or Map<String, ...>, was null"
            )
        }

        @Test
        @DisplayName("should reject non-string method key")
        @Description("Method keys must be strings - integer keys are invalid")
        fun shouldRejectNonStringMethodKey() {
            val config = mapOf("/test" to mapOf(123 to "*"))

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo(
                "Method key for path '/test' must be String, was kotlin.Int"
            )
        }

        @Test
        @DisplayName("should reject null method key")
        @Description("Method keys cannot be null")
        fun shouldRejectNullMethodKey() {
            val config = mapOf("/test" to mapOf<Any?, Any?>(null to "*"))

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo(
                "Method key for path '/test' must be String, was null"
            )
        }

        @Test
        @DisplayName("should reject invalid test case value type")
        @Description("Test case value must be star string or list - integer is invalid")
        fun shouldRejectInvalidTestCaseValueType() {
            val config = mapOf("/test" to mapOf("GET" to 123))

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo(
                "Test case value for path '/test' method 'GET' must be \"*\" or List<String>, was kotlin.Int"
            )
        }

        @Test
        @DisplayName("should reject null test case value")
        @Description("Test case value cannot be null")
        fun shouldRejectNullTestCaseValue() {
            val config = mapOf("/test" to mapOf<String, Any?>("GET" to null))

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo(
                "Test case value for path '/test' method 'GET' must be \"*\" or List<String>, was null"
            )
        }

        @Test
        @DisplayName("should reject list with non-string elements at beginning")
        @Description("Test case list must contain only strings - integer at index 0 is invalid")
        fun shouldRejectListWithNonStringElementsAtBeginning() {
            val config = mapOf("/test" to mapOf("GET" to listOf(123, "valid")))

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo(
                "Test case name at index 0 for path '/test' method 'GET' must be String, was kotlin.Int"
            )
        }

        @Test
        @DisplayName("should reject list with non-string elements in middle")
        @Description("All elements in test case list must be strings - integer at index 1 is invalid")
        fun shouldRejectListWithNonStringElementsInMiddle() {
            val config = mapOf("/test" to mapOf("GET" to listOf("valid", 123, "another")))

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo(
                "Test case name at index 1 for path '/test' method 'GET' must be String, was kotlin.Int"
            )
        }

        @Test
        @DisplayName("should reject list with null elements")
        @Description("Test case list cannot contain null elements")
        fun shouldRejectListWithNullElements() {
            val config = mapOf("/test" to mapOf("GET" to listOf("valid", null, "another")))

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo(
                "Test case name at index 1 for path '/test' method 'GET' must be String, was null"
            )
        }

        @Test
        @DisplayName("should reject map value as test case list")
        @Description("Test case value must be star or list - nested map is invalid")
        fun shouldRejectMapValueAsTestCaseList() {
            val config = mapOf(
                "/test" to mapOf(
                    "GET" to mapOf("invalid" to "structure")
                )
            )

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo(
                "Test case value for path '/test' method 'GET' must be \"*\" or List<String>, " +
                    "was java.util.Collections.SingletonMap"
            )
        }
    }

    @Nested
    @Story("Regression Tests")
    @DisplayName("Regression Tests")
    inner class RegressionTests {

        @Test
        @DisplayName("should accept star value in method map (regression for OR bug)")
        @Description(
            """
            This test proves the critical OR→AND bug was fixed.
            Previous buggy code: it != "*" || it !is List<*>
            For value "*": it != "*" is false, but it !is List<*> is true → OR = true (incorrectly flagged as bad)
            Fixed code: it != "*" && it !is List<*>
            For value "*": it != "*" is false → AND short-circuits to false (correctly accepted)
            """
        )
        fun shouldAcceptStarValueInMethodMap() {
            val config = mapOf("/test" to mapOf("GET" to "*"))

            Assertions.assertThatNoException().isThrownBy {
                validateIgnoreConfig(config)
            }
        }

        @Test
        @DisplayName("should accept list value in method map (regression for OR bug)")
        @Description(
            """
            This test also validates the OR→AND bug fix.
            Previous buggy code: it != "*" || it !is List<*>
            For value List: it != "*" is true → OR = true (incorrectly flagged as bad)
            Fixed code: it != "*" && it !is List<*>
            For value List: it !is List<*> is false → AND = false (correctly accepted)
            """
        )
        fun shouldAcceptListValueInMethodMap() {
            val config = mapOf("/test" to mapOf("GET" to listOf("test case 1", "test case 2")))

            Assertions.assertThatNoException().isThrownBy {
                validateIgnoreConfig(config)
            }
        }

        @Test
        @DisplayName("should work with configuration from TestGeneratorTest")
        @Description("Validates that the actual ignore configuration used in TestGeneratorTest is accepted")
        fun shouldWorkWithConfigurationFromTestGeneratorTest() {
            val config = mapOf(
                "/fake" to "*",
                "*" to mapOf(
                    "head" to "*",
                    "*" to "*"
                ),
                "/projects/{projectId}" to mapOf("delete" to "*"),
                "/users" to mapOf("get" to listOf("Invalid Query role parameter: Array Item Invalid Enum Value"))
            )

            Assertions.assertThatNoException().isThrownBy {
                validateIgnoreConfig(config)
            }
        }
    }

    @Nested
    @Story("Error Message Quality")
    @DisplayName("Error Message Quality")
    inner class ErrorMessageQuality {

        @Test
        @DisplayName("error messages should include path context")
        @Description("Error messages must specify which path has the invalid configuration")
        fun errorMessagesShouldIncludePathContext() {
            val config = mapOf("/api/users" to 123)

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo("Value for path '/api/users' must be \"*\" or Map<String, ...>, was kotlin.Int")
        }

        @Test
        @DisplayName("error messages should include method context")
        @Description("Error messages must specify which HTTP method has the invalid configuration")
        fun errorMessagesShouldIncludeMethodContext() {
            val config = mapOf("/api/users" to mapOf("GET" to 123))

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo("Test case value for path '/api/users' method 'GET' must be \"*\" or List<String>, was kotlin.Int")
        }

        @Test
        @DisplayName("error messages should include index for list elements")
        @Description("Error messages must specify the exact index of the invalid list element")
        fun errorMessagesShouldIncludeIndexForListElements() {
            val config = mapOf("/api/users" to mapOf("POST" to listOf("valid", 123)))

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo("Test case name at index 1 for path '/api/users' method 'POST' must be String, was kotlin.Int")
        }

        @Test
        @DisplayName("error messages should include actual type")
        @Description("Error messages must specify the actual type of the invalid value for debugging")
        fun errorMessagesShouldIncludeActualType() {
            val config = mapOf("/test" to 123)

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo("Value for path '/test' must be \"*\" or Map<String, ...>, was kotlin.Int")
        }

        @Test
        @DisplayName("error messages should be actionable")
        @Description("Error messages should tell users what is required and what was provided")
        fun errorMessagesShouldBeActionable() {
            val config = mapOf("/test" to mapOf("GET" to 123))

            val exception = assertThrows<IllegalArgumentException> {
                validateIgnoreConfig(config)
            }

            assertThat(exception.message).isEqualTo("Test case value for path '/test' method 'GET' must be \"*\" or List<String>, was kotlin.Int")
        }
    }
}
