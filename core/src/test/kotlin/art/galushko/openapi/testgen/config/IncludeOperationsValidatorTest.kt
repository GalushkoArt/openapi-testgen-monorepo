package art.galushko.openapi.testgen.config

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
@Feature("Include Operations Validation")
@DisplayName("IncludeOperationsValidator")
class IncludeOperationsValidatorTest {

    @Nested
    @Story("Valid Configurations")
    @DisplayName("Valid Configurations")
    inner class ValidConfigurations {

        @Test
        @DisplayName("should accept null input")
        @Description("Null input is valid - no configuration to validate")
        fun shouldAcceptNullInput() {
            Assertions.assertThatNoException().isThrownBy {
                IncludeOperationsValidator.validate(null)
            }
        }

        @Test
        @DisplayName("should accept empty map")
        @Description("Empty map is valid - include all operations (default)")
        fun shouldAcceptEmptyMap() {
            Assertions.assertThatNoException().isThrownBy {
                IncludeOperationsValidator.validate(emptyMap())
            }
        }

        @Test
        @DisplayName("should accept single path with single method")
        fun shouldAcceptSinglePathWithSingleMethod() {
            val config = mapOf("/users/{userId}" to listOf("GET"))

            Assertions.assertThatNoException().isThrownBy {
                IncludeOperationsValidator.validate(config)
            }
        }

        @Test
        @DisplayName("should accept single path with multiple methods")
        fun shouldAcceptSinglePathWithMultipleMethods() {
            val config = mapOf("/users/{userId}" to listOf("GET", "DELETE", "PATCH"))

            Assertions.assertThatNoException().isThrownBy {
                IncludeOperationsValidator.validate(config)
            }
        }

        @Test
        @DisplayName("should accept multiple paths")
        fun shouldAcceptMultiplePaths() {
            val config = mapOf(
                "/users/{userId}" to listOf("GET"),
                "/orders" to listOf("GET", "POST"),
                "/products" to listOf("DELETE")
            )

            Assertions.assertThatNoException().isThrownBy {
                IncludeOperationsValidator.validate(config)
            }
        }

        @Test
        @DisplayName("should accept wildcard method")
        fun shouldAcceptWildcardMethod() {
            val config = mapOf("/users" to listOf("*"))

            Assertions.assertThatNoException().isThrownBy {
                IncludeOperationsValidator.validate(config)
            }
        }

        @Test
        @DisplayName("should accept wildcard path")
        fun shouldAcceptWildcardPath() {
            val config = mapOf("*" to listOf("GET"))

            Assertions.assertThatNoException().isThrownBy {
                IncludeOperationsValidator.validate(config)
            }
        }

        @Test
        @DisplayName("should accept wildcard path and method")
        fun shouldAcceptWildcardPathAndMethod() {
            val config = mapOf("*" to listOf("*"))

            Assertions.assertThatNoException().isThrownBy {
                IncludeOperationsValidator.validate(config)
            }
        }

        @Test
        @DisplayName("should accept all valid HTTP methods")
        fun shouldAcceptAllValidHttpMethods() {
            val config = mapOf(
                "/test" to listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE")
            )

            Assertions.assertThatNoException().isThrownBy {
                IncludeOperationsValidator.validate(config)
            }
        }

        @Test
        @DisplayName("should accept lowercase HTTP methods")
        @Description("HTTP methods are case-insensitive")
        fun shouldAcceptLowercaseHttpMethods() {
            val config = mapOf("/test" to listOf("get", "post", "put", "delete"))

            Assertions.assertThatNoException().isThrownBy {
                IncludeOperationsValidator.validate(config)
            }
        }

        @Test
        @DisplayName("should accept mixed case HTTP methods")
        fun shouldAcceptMixedCaseHttpMethods() {
            val config = mapOf("/test" to listOf("Get", "pOsT", "PUT"))

            Assertions.assertThatNoException().isThrownBy {
                IncludeOperationsValidator.validate(config)
            }
        }

        @Test
        @DisplayName("should accept complex paths with parameters")
        fun shouldAcceptComplexPaths() {
            val config = mapOf(
                "/users/{userId}/orders/{orderId}/items" to listOf("GET"),
                "/api/v1/products" to listOf("POST")
            )

            Assertions.assertThatNoException().isThrownBy {
                IncludeOperationsValidator.validate(config)
            }
        }
    }

    @Nested
    @Story("Invalid Configurations")
    @DisplayName("Invalid Configurations")
    inner class InvalidConfigurations {

        @Test
        @DisplayName("should reject blank path")
        fun shouldRejectBlankPath() {
            val config = mapOf("" to listOf("GET"))

            val exception = assertThrows<IllegalArgumentException> {
                IncludeOperationsValidator.validate(config)
            }

            assertThat(exception.message).isEqualTo("includeOperations path cannot be blank")
        }

        @Test
        @DisplayName("should reject whitespace-only path")
        fun shouldRejectWhitespaceOnlyPath() {
            val config = mapOf("   " to listOf("GET"))

            val exception = assertThrows<IllegalArgumentException> {
                IncludeOperationsValidator.validate(config)
            }

            assertThat(exception.message).isEqualTo("includeOperations path cannot be blank")
        }

        @Test
        @DisplayName("should reject empty methods list")
        fun shouldRejectEmptyMethodsList() {
            val config = mapOf("/users" to emptyList<String>())

            val exception = assertThrows<IllegalArgumentException> {
                IncludeOperationsValidator.validate(config)
            }

            assertThat(exception.message).isEqualTo("includeOperations methods list cannot be empty for path '/users'")
        }

        @Test
        @DisplayName("should reject invalid HTTP method")
        fun shouldRejectInvalidHttpMethod() {
            val config = mapOf("/users" to listOf("INVALID"))

            val exception = assertThrows<IllegalArgumentException> {
                IncludeOperationsValidator.validate(config)
            }

            assertThat(exception.message).isEqualTo(
                "Invalid HTTP method 'INVALID' for path '/users' in includeOperations. " +
                    "Valid methods: *, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT, TRACE"
            )
        }

        @Test
        @DisplayName("should reject invalid method among valid methods")
        fun shouldRejectInvalidMethodAmongValidMethods() {
            val config = mapOf("/users" to listOf("GET", "WRONG", "POST"))

            val exception = assertThrows<IllegalArgumentException> {
                IncludeOperationsValidator.validate(config)
            }

            assertThat(exception.message).isEqualTo(
                "Invalid HTTP method 'WRONG' for path '/users' in includeOperations. " +
                    "Valid methods: *, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT, TRACE"
            )
        }

        @Test
        @DisplayName("should reject invalid method with lowercase")
        fun shouldRejectInvalidMethodWithLowercase() {
            val config = mapOf("/users" to listOf("invalid"))

            val exception = assertThrows<IllegalArgumentException> {
                IncludeOperationsValidator.validate(config)
            }

            assertThat(exception.message).isEqualTo(
                "Invalid HTTP method 'invalid' for path '/users' in includeOperations. " +
                    "Valid methods: *, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT, TRACE"
            )
        }
    }

    @Nested
    @Story("Error Message Quality")
    @DisplayName("Error Message Quality")
    inner class ErrorMessageQuality {

        @Test
        @DisplayName("error messages should include path context")
        fun errorMessagesShouldIncludePathContext() {
            val config = mapOf("/api/users" to emptyList<String>())

            val exception = assertThrows<IllegalArgumentException> {
                IncludeOperationsValidator.validate(config)
            }

            assertThat(exception.message).isEqualTo("includeOperations methods list cannot be empty for path '/api/users'")
        }

        @Test
        @DisplayName("error messages should include method and path context")
        fun errorMessagesShouldIncludeMethodAndPathContext() {
            val config = mapOf("/api/orders" to listOf("FETCH"))

            val exception = assertThrows<IllegalArgumentException> {
                IncludeOperationsValidator.validate(config)
            }

            assertThat(exception.message).isEqualTo(
                "Invalid HTTP method 'FETCH' for path '/api/orders' in includeOperations. " +
                    "Valid methods: *, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT, TRACE"
            )
        }

        @Test
        @DisplayName("error messages should list valid methods")
        fun errorMessagesShouldListValidMethods() {
            val config = mapOf("/test" to listOf("CONNECT"))

            val exception = assertThrows<IllegalArgumentException> {
                IncludeOperationsValidator.validate(config)
            }

            assertThat(exception.message).contains("Valid methods:")
            assertThat(exception.message).contains("GET")
            assertThat(exception.message).contains("POST")
            assertThat(exception.message).contains("*")
        }
    }
}
