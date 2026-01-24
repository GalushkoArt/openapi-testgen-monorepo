package art.galushko.openapi.testgen.filtering

import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Epic("Filtering")
@Feature("Include Operations Handler")
@DisplayName("IncludeOperationsHandler")
class IncludeOperationsHandlerTest {

    @Nested
    @Story("Empty Configuration")
    @DisplayName("Empty Configuration (Default Behavior)")
    inner class EmptyConfiguration {

        private val handler = IncludeOperationsHandler(emptyMap())

        @Test
        @DisplayName("isEnabled should return false when config is empty")
        @Description("Empty configuration means include all - filtering is disabled")
        fun isEnabledReturnsFalseWhenEmpty() {
            assertThat(handler.isEnabled()).isFalse()
        }

        @Test
        @DisplayName("shouldIncludePath should return true for any path when config is empty")
        @Description("When no include config, all paths should be included")
        fun shouldIncludePathReturnsTrueForAnyPath() {
            assertThat(handler.shouldIncludePath("/users")).isTrue()
            assertThat(handler.shouldIncludePath("/orders")).isTrue()
            assertThat(handler.shouldIncludePath("/any/path")).isTrue()
        }

        @Test
        @DisplayName("shouldIncludeOperation should return true for any operation when config is empty")
        @Description("When no include config, all operations should be included")
        fun shouldIncludeOperationReturnsTrueForAnyOperation() {
            assertThat(handler.shouldIncludeOperation("/users", "GET")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "POST")).isTrue()
            assertThat(handler.shouldIncludeOperation("/orders", "DELETE")).isTrue()
        }
    }

    @Nested
    @Story("Exact Path Matching")
    @DisplayName("Exact Path Matching")
    inner class ExactPathMatching {

        private val handler = IncludeOperationsHandler(
            mapOf(
                "/users/{userId}" to listOf("GET"),
                "/orders" to listOf("GET", "POST")
            )
        )

        @Test
        @DisplayName("isEnabled should return true when config is not empty")
        fun isEnabledReturnsTrueWhenNotEmpty() {
            assertThat(handler.isEnabled()).isTrue()
        }

        @Test
        @DisplayName("shouldIncludePath should return true for configured paths")
        fun shouldIncludePathReturnsTrueForConfiguredPaths() {
            assertThat(handler.shouldIncludePath("/users/{userId}")).isTrue()
            assertThat(handler.shouldIncludePath("/orders")).isTrue()
        }

        @Test
        @DisplayName("shouldIncludePath should return false for non-configured paths")
        fun shouldIncludePathReturnsFalseForNonConfiguredPaths() {
            assertThat(handler.shouldIncludePath("/products")).isFalse()
            assertThat(handler.shouldIncludePath("/users")).isFalse()
        }

        @Test
        @DisplayName("shouldIncludeOperation should return true for exact path and method match")
        fun shouldIncludeOperationReturnsTrueForExactMatch() {
            assertThat(handler.shouldIncludeOperation("/users/{userId}", "GET")).isTrue()
            assertThat(handler.shouldIncludeOperation("/orders", "GET")).isTrue()
            assertThat(handler.shouldIncludeOperation("/orders", "POST")).isTrue()
        }

        @Test
        @DisplayName("shouldIncludeOperation should return false for non-matching method")
        fun shouldIncludeOperationReturnsFalseForNonMatchingMethod() {
            assertThat(handler.shouldIncludeOperation("/users/{userId}", "POST")).isFalse()
            assertThat(handler.shouldIncludeOperation("/users/{userId}", "DELETE")).isFalse()
            assertThat(handler.shouldIncludeOperation("/orders", "DELETE")).isFalse()
        }

        @Test
        @DisplayName("shouldIncludeOperation should return false for non-configured path")
        fun shouldIncludeOperationReturnsFalseForNonConfiguredPath() {
            assertThat(handler.shouldIncludeOperation("/products", "GET")).isFalse()
            assertThat(handler.shouldIncludeOperation("/products", "POST")).isFalse()
        }
    }

    @Nested
    @Story("Case Insensitive Method Matching")
    @DisplayName("Case Insensitive Method Matching")
    inner class CaseInsensitiveMethodMatching {

        private val handler = IncludeOperationsHandler(
            mapOf("/users" to listOf("GET", "post"))
        )

        @Test
        @DisplayName("should match method regardless of case in config")
        fun shouldMatchMethodRegardlessOfCaseInConfig() {
            assertThat(handler.shouldIncludeOperation("/users", "GET")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "get")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "Get")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "POST")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "post")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "Post")).isTrue()
        }

        @Test
        @DisplayName("should not match non-configured method regardless of case")
        fun shouldNotMatchNonConfiguredMethod() {
            assertThat(handler.shouldIncludeOperation("/users", "DELETE")).isFalse()
            assertThat(handler.shouldIncludeOperation("/users", "delete")).isFalse()
        }
    }

    @Nested
    @Story("Wildcard Method Matching")
    @DisplayName("Wildcard Method (*) Matching")
    inner class WildcardMethodMatching {

        private val handler = IncludeOperationsHandler(
            mapOf("/users" to listOf("*"))
        )

        @Test
        @DisplayName("shouldIncludePath should return true for path with wildcard method")
        fun shouldIncludePathReturnsTrueForWildcardMethod() {
            assertThat(handler.shouldIncludePath("/users")).isTrue()
        }

        @Test
        @DisplayName("shouldIncludeOperation should return true for any method when wildcard is used")
        fun shouldIncludeOperationReturnsTrueForAnyMethod() {
            assertThat(handler.shouldIncludeOperation("/users", "GET")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "POST")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "PUT")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "DELETE")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "PATCH")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "HEAD")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "OPTIONS")).isTrue()
        }

        @Test
        @DisplayName("shouldIncludeOperation should return false for non-configured path")
        fun shouldIncludeOperationReturnsFalseForNonConfiguredPath() {
            assertThat(handler.shouldIncludeOperation("/orders", "GET")).isFalse()
        }
    }

    @Nested
    @Story("Wildcard Path Matching")
    @DisplayName("Wildcard Path (*) Matching")
    inner class WildcardPathMatching {

        private val handler = IncludeOperationsHandler(
            mapOf("*" to listOf("GET"))
        )

        @Test
        @DisplayName("shouldIncludePath should return true for any path when wildcard path is used")
        fun shouldIncludePathReturnsTrueForAnyPath() {
            assertThat(handler.shouldIncludePath("/users")).isTrue()
            assertThat(handler.shouldIncludePath("/orders")).isTrue()
            assertThat(handler.shouldIncludePath("/any/path")).isTrue()
        }

        @Test
        @DisplayName("shouldIncludeOperation should return true for matching method on any path")
        fun shouldIncludeOperationReturnsTrueForMatchingMethodOnAnyPath() {
            assertThat(handler.shouldIncludeOperation("/users", "GET")).isTrue()
            assertThat(handler.shouldIncludeOperation("/orders", "GET")).isTrue()
            assertThat(handler.shouldIncludeOperation("/any/path", "GET")).isTrue()
        }

        @Test
        @DisplayName("shouldIncludeOperation should return false for non-matching method")
        fun shouldIncludeOperationReturnsFalseForNonMatchingMethod() {
            assertThat(handler.shouldIncludeOperation("/users", "POST")).isFalse()
            assertThat(handler.shouldIncludeOperation("/orders", "DELETE")).isFalse()
        }
    }

    @Nested
    @Story("Wildcard Path and Method")
    @DisplayName("Wildcard Path and Wildcard Method (*:*)")
    inner class WildcardPathAndMethod {

        private val handler = IncludeOperationsHandler(
            mapOf("*" to listOf("*"))
        )

        @Test
        @DisplayName("should include all paths")
        fun shouldIncludeAllPaths() {
            assertThat(handler.shouldIncludePath("/users")).isTrue()
            assertThat(handler.shouldIncludePath("/orders")).isTrue()
            assertThat(handler.shouldIncludePath("/any/path")).isTrue()
        }

        @Test
        @DisplayName("should include all operations")
        fun shouldIncludeAllOperations() {
            assertThat(handler.shouldIncludeOperation("/users", "GET")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "POST")).isTrue()
            assertThat(handler.shouldIncludeOperation("/orders", "DELETE")).isTrue()
            assertThat(handler.shouldIncludeOperation("/any/path", "PATCH")).isTrue()
        }

        @Test
        @DisplayName("isEnabled should still return true")
        @Description("Even though *:* matches everything, the config is not empty so filtering is enabled")
        fun isEnabledStillReturnsTrue() {
            assertThat(handler.isEnabled()).isTrue()
        }
    }

    @Nested
    @Story("Exact Path Takes Precedence")
    @DisplayName("Exact Path Takes Precedence Over Wildcard")
    inner class ExactPathPrecedence {

        private val handler = IncludeOperationsHandler(
            mapOf(
                "/users" to listOf("GET"),
                "*" to listOf("POST")
            )
        )

        @Test
        @DisplayName("exact path should use its own methods, not wildcard methods")
        @Description("When both exact path and wildcard are configured, exact path methods should be checked first")
        fun exactPathUsesOwnMethods() {
            // /users has GET configured explicitly
            assertThat(handler.shouldIncludeOperation("/users", "GET")).isTrue()
            // /users does NOT have POST configured (wildcard doesn't add to it)
            assertThat(handler.shouldIncludeOperation("/users", "POST")).isFalse()
        }

        @Test
        @DisplayName("non-configured path should use wildcard methods")
        fun nonConfiguredPathUsesWildcard() {
            // /orders is not configured, so wildcard applies
            assertThat(handler.shouldIncludeOperation("/orders", "POST")).isTrue()
            assertThat(handler.shouldIncludeOperation("/orders", "GET")).isFalse()
        }
    }

    @Nested
    @Story("Complex Configuration")
    @DisplayName("Complex Configuration Scenarios")
    inner class ComplexConfiguration {

        private val handler = IncludeOperationsHandler(
            mapOf(
                "/users/{userId}" to listOf("GET", "DELETE"),
                "/orders" to listOf("*"),
                "*" to listOf("OPTIONS")
            )
        )

        @Test
        @DisplayName("should handle multiple paths with different method configurations")
        fun shouldHandleMultiplePaths() {
            // /users/{userId} has GET and DELETE
            assertThat(handler.shouldIncludeOperation("/users/{userId}", "GET")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users/{userId}", "DELETE")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users/{userId}", "POST")).isFalse()
            assertThat(handler.shouldIncludeOperation("/users/{userId}", "OPTIONS")).isFalse()

            // /orders has all methods (wildcard)
            assertThat(handler.shouldIncludeOperation("/orders", "GET")).isTrue()
            assertThat(handler.shouldIncludeOperation("/orders", "POST")).isTrue()
            assertThat(handler.shouldIncludeOperation("/orders", "DELETE")).isTrue()
            assertThat(handler.shouldIncludeOperation("/orders", "OPTIONS")).isTrue()

            // Other paths only have OPTIONS (wildcard path)
            assertThat(handler.shouldIncludeOperation("/products", "OPTIONS")).isTrue()
            assertThat(handler.shouldIncludeOperation("/products", "GET")).isFalse()
        }

        @Test
        @DisplayName("shouldIncludePath should return true for all paths in this config")
        fun shouldIncludePathForAllConfiguredPaths() {
            assertThat(handler.shouldIncludePath("/users/{userId}")).isTrue()
            assertThat(handler.shouldIncludePath("/orders")).isTrue()
            assertThat(handler.shouldIncludePath("/products")).isTrue() // via wildcard
            assertThat(handler.shouldIncludePath("/any")).isTrue() // via wildcard
        }
    }

    @Nested
    @Story("Edge Cases")
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        @DisplayName("should handle empty method list gracefully")
        @Description("Empty method list for a path means no methods are included for that path")
        fun shouldHandleEmptyMethodList() {
            val handler = IncludeOperationsHandler(mapOf("/users" to emptyList()))

            assertThat(handler.isEnabled()).isTrue()
            assertThat(handler.shouldIncludePath("/users")).isTrue()
            // But no methods should be included
            assertThat(handler.shouldIncludeOperation("/users", "GET")).isFalse()
            assertThat(handler.shouldIncludeOperation("/users", "POST")).isFalse()
        }

        @Test
        @DisplayName("should handle paths with special characters")
        fun shouldHandlePathsWithSpecialCharacters() {
            val handler = IncludeOperationsHandler(
                mapOf("/users/{userId}/orders/{orderId}" to listOf("GET"))
            )

            assertThat(handler.shouldIncludeOperation("/users/{userId}/orders/{orderId}", "GET")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users/{userId}/orders/{orderId}", "POST")).isFalse()
        }

        @Test
        @DisplayName("should handle multiple wildcard methods in list")
        @Description("Multiple wildcards in list should behave the same as single wildcard")
        fun shouldHandleMultipleWildcardsInList() {
            val handler = IncludeOperationsHandler(mapOf("/users" to listOf("*", "*")))

            assertThat(handler.shouldIncludeOperation("/users", "GET")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "POST")).isTrue()
        }

        @Test
        @DisplayName("should handle mixed explicit methods and wildcard")
        @Description("When wildcard is in the list with explicit methods, wildcard takes precedence")
        fun shouldHandleMixedMethodsAndWildcard() {
            val handler = IncludeOperationsHandler(mapOf("/users" to listOf("GET", "*")))

            assertThat(handler.shouldIncludeOperation("/users", "GET")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "POST")).isTrue()
            assertThat(handler.shouldIncludeOperation("/users", "DELETE")).isTrue()
        }
    }
}
