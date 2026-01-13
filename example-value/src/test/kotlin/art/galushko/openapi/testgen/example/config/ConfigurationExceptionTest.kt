package art.galushko.openapi.testgen.example.config

import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@Epic("Configuration")
@Feature("Configuration Exception")
@DisplayName("ConfigurationException")
class ConfigurationExceptionTest {

    @Test
    @DisplayName("should provide clear error message with field context")
    @Description("Error message includes field name, expected type, and actual type")
    fun shouldProvideFieldContext() {
        val ex = ConfigurationException(
            field = "maxSchemaDepth",
            expected = "Integer",
            actual = "String"
        )

        assertThat(ex.message)
            .isEqualTo("Configuration error for 'maxSchemaDepth': expected Integer, got String")
    }

    @Test
    @DisplayName("should handle nested field paths")
    @Description("Error message clearly shows nested field paths like 'ignoreTestCases[/api/users]'")
    fun shouldHandleNestedFieldPaths() {
        val ex = ConfigurationException(
            field = "ignoreTestCases[/api/users]",
            expected = "Map<String, Any>",
            actual = "null"
        )

        assertThat(ex.message)
            .isEqualTo("Configuration error for 'ignoreTestCases[/api/users]': expected Map<String, Any>, got null")
    }

    @Test
    @DisplayName("should extend IllegalArgumentException for backward compatibility")
    @Description("Callers catching IllegalArgumentException will still catch ConfigurationException")
    fun shouldExtendIllegalArgumentException() {
        val ex = ConfigurationException(
            field = "test",
            expected = "String",
            actual = "Int"
        )

        assertThat(ex).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("should expose field, expected, and actual properties")
    fun shouldExposeProperties() {
        val ex = ConfigurationException(
            field = "myField",
            expected = "expectedType",
            actual = "actualType"
        )

        assertThat(ex.field).isEqualTo("myField")
        assertThat(ex.expected).isEqualTo("expectedType")
        assertThat(ex.actual).isEqualTo("actualType")
    }
}
