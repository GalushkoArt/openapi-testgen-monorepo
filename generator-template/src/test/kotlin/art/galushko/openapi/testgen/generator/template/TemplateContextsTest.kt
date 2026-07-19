package art.galushko.openapi.testgen.generator.template

import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Epic("Generator")
@Feature("Template Context Data Classes")
@DisplayName("Template Context Data Classes")
class TemplateContextsTest {

    @Nested
    @Story("GenericClassTemplateContext")
    @DisplayName("GenericClassTemplateContext")
    inner class ClassTemplateContextTests {

        @Test
        @DisplayName("escapeString() should escape string values like escapeStringLiteral")
        @Description("Mustache lambda exposed on the class context escapes Java/JSON string literal chars")
        fun shouldEscapeStringValuesOnClassContext() {
            val context = GenericClassTemplateContext(
                className = "FooTest",
                operationName = "foo",
                operationPath = "/foo",
                methods = emptyList(),
                customVariables = emptyMap(),
                fileHeaderComment = null,
            )

            val result = context.escapeString().apply("line1\nline2")

            assertThat(result).isEqualTo("line1\\nline2")
        }

        @Test
        @DisplayName("escapeString() should pass non-string values through unchanged")
        @Description("Non-String inputs are returned as-is by the Mustache lambda")
        fun shouldPassNonStringValuesThroughOnClassContext() {
            val context = GenericClassTemplateContext(
                className = "FooTest",
                operationName = "foo",
                operationPath = "/foo",
                methods = emptyList(),
                customVariables = emptyMap(),
                fileHeaderComment = null,
            )

            val result = context.escapeString().apply(42)

            assertThat(result).isEqualTo(42)
        }
    }

    @Nested
    @Story("GenericMethodTemplateContext")
    @DisplayName("GenericMethodTemplateContext")
    inner class MethodTemplateContextTests {

        @Test
        @DisplayName("escapeString() should escape string values like escapeStringLiteral")
        @Description("Mustache lambda exposed on the method context escapes Java/JSON string literal chars")
        fun shouldEscapeStringValuesOnMethodContext() {
            val context = newMethodContext()

            val result = context.escapeString().apply("a\"b")

            assertThat(result).isEqualTo("a\\\"b")
        }

        @Test
        @DisplayName("escapeString() should pass non-string values through unchanged")
        @Description("Non-String inputs are returned as-is by the Mustache lambda")
        fun shouldPassNonStringValuesThroughOnMethodContext() {
            val context = newMethodContext()

            val result = context.escapeString().apply(true)

            assertThat(result).isEqualTo(true)
        }

        @Test
        @DisplayName("shouldHaveBody should be true for POST, PUT, PATCH")
        @Description("Verifies the default derived shouldHaveBody flag for body-carrying methods")
        fun shouldComputeShouldHaveBodyForBodyCarryingMethods() {
            assertThat(newMethodContext(httpMethod = "post").shouldHaveBody).isTrue
            assertThat(newMethodContext(httpMethod = "PUT").shouldHaveBody).isTrue
            assertThat(newMethodContext(httpMethod = "patch").shouldHaveBody).isTrue
        }

        @Test
        @DisplayName("shouldHaveBody should be false for GET and DELETE")
        @Description("Verifies the default derived shouldHaveBody flag for non-body methods")
        fun shouldComputeShouldHaveBodyForNonBodyMethods() {
            assertThat(newMethodContext(httpMethod = "get").shouldHaveBody).isFalse
            assertThat(newMethodContext(httpMethod = "DELETE").shouldHaveBody).isFalse
        }

        private fun newMethodContext(httpMethod: String = "GET"): GenericMethodTemplateContext =
            GenericMethodTemplateContext(
                methodName = "fooTest",
                testCaseName = "Foo Test",
                description = "desc",
                httpMethod = httpMethod,
                path = "/foo",
                expectedStatusCode = 200,
                headers = emptyList(),
                pathParams = emptyList(),
                queryParams = emptyList(),
                cookies = emptyList(),
                requestBody = null,
                requestBodyMediaType = null,
                expectedResponseBody = null,
                responseBodyMediaType = null,
                assertJsonResponseBody = false,
                requestBodyTodoComment = null,
                responseAssertionTodoComment = null,
                needToComplete = false,
                notes = emptyList(),
                customVariables = emptyMap(),
            )
    }

    @Nested
    @Story("GenericParamContext / GenericBodyContext escaped fields")
    @DisplayName("Derived escaped fields")
    inner class DerivedEscapedFieldTests {

        @Test
        @DisplayName("GenericParamContext.escapedValue should reflect escapeStringLiteral(value)")
        fun shouldEscapeParamValue() {
            val context = GenericParamContext(key = "k", value = "a\tb")

            assertThat(context.escapedValue).isEqualTo("a\\tb")
        }

        @Test
        @DisplayName("GenericBodyContext.escapedRawBody should reflect escapeStringLiteral(rawBody)")
        fun shouldEscapeRawBody() {
            val context = GenericBodyContext(rawBody = "line1\nline2", body = Unit)

            assertThat(context.escapedRawBody).isEqualTo("line1\\nline2")
        }
    }
}
