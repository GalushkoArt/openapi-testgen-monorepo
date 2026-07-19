package art.galushko.openapi.testgen.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TestCase model")
class TestCaseTest {

    @Test
    @DisplayName("should default optional request details to empty or null")
    fun testCaseDefaults() {
        val case = TestCase(name = "Valid Case", method = "GET", path = "/pets")

        assertThat(case.queryParams).isEmpty()
        assertThat(case.pathParams).isEmpty()
        assertThat(case.headers).isEmpty()
        assertThat(case.cookie).isEmpty()
        assertThat(case.securityValues).isEqualTo(SecurityValues())
        assertThat(case.body).isNull()
        assertThat(case.requestBodyMediaType).isNull()
        assertThat(case.expectedBody).isNull()
        assertThat(case.responseBodyMediaType).isNull()
        assertThat(case.needToComplete).isFalse()
        assertThat(case.expectedStatusCode).isEqualTo(0)
        assertThat(case.rule).isNull()
    }

    @Test
    @DisplayName("value equality should cover nested security values")
    fun testCaseEquality() {
        fun case() = TestCase(
            name = "Auth Case",
            method = "POST",
            path = "/pets",
            securityValues = SecurityValues(
                headers = listOf(KeyValuePair("X-API-Key", "key")),
                other = mapOf("authorizationScopes" to listOf("read:pets")),
            ),
        )

        assertThat(case()).isEqualTo(case())
        assertThat(case().copy(expectedStatusCode = 401)).isNotEqualTo(case())
    }

    @Test
    @DisplayName("TestSuite should carry its cases")
    fun testSuiteHoldsCases() {
        val case = TestCase(name = "Valid Case", method = "GET", path = "/pets")
        val suite = TestSuite(path = "/pets", method = "GET", operationName = "listPets", testCases = listOf(case))

        assertThat(suite.testCases).containsExactly(case)
        assertThat(suite.operationName).isEqualTo("listPets")
    }

    @Test
    @DisplayName("KeyValuePair should preserve key and value")
    fun keyValuePair() {
        val pair = KeyValuePair("Accept", "application/json")

        assertThat(pair.key).isEqualTo("Accept")
        assertThat(pair.value).isEqualTo("application/json")
    }
}
