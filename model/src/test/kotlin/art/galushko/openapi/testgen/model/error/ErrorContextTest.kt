package art.galushko.openapi.testgen.model.error

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@DisplayName("ErrorContext")
class ErrorContextTest {

    companion object {
        private val operation = ErrorContext.Operation(path = "/pets/{id}", method = "get", operationId = "getPet")
        private val anonymousOperation = ErrorContext.Operation(path = "/pets", method = "post", operationId = null)

        @JvmStatic
        fun renderingProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "operation with operationId uppercases the method",
                operation,
                "GET /pets/{id} (getPet)",
            ),
            Arguments.of(
                "operation without operationId omits the suffix",
                anonymousOperation,
                "POST /pets",
            ),
            Arguments.of(
                "parameter context includes location and name",
                ErrorContext.Parameter(operation, parameterName = "id", location = "path", ref = null),
                "GET /pets/{id} (getPet) -> path parameter 'id'",
            ),
            Arguments.of(
                "request body context without ref",
                ErrorContext.RequestBody(anonymousOperation, ref = null),
                "POST /pets -> request body",
            ),
            Arguments.of(
                "request body context with ref",
                ErrorContext.RequestBody(anonymousOperation, ref = "#/components/requestBodies/Pet"),
                "POST /pets -> request body (#/components/requestBodies/Pet)",
            ),
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("renderingProvider")
    @DisplayName("should render contexts for reports")
    fun shouldRenderContexts(scenario: String, context: ErrorContext, expected: String) {
        assertThat(context.toString()).isEqualTo(expected)
    }
}
