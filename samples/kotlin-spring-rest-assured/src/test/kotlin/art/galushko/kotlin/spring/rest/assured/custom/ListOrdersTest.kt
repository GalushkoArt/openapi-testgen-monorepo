package art.galushko.kotlin.spring.rest.assured.custom

import io.restassured.RestAssured
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeAll
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@DisplayName("Tests for listOrders")
class ListOrdersTest {
    @Autowired
    lateinit var objectMapper: ObjectMapper

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            RestAssured.baseURI = "http://localhost:8080/v1"
        }
    }

    private fun assertExpectedBody(expectedBodyJson: String, responseBody: String) {
        val expectedNode = objectMapper.readTree(expectedBodyJson)
        val actualNode = try {
            objectMapper.readTree(responseBody)
        } catch (e: JsonProcessingException) {
            null
        }

        when {
            actualNode != null -> Assertions.assertEquals(expectedNode, actualNode)
            expectedNode.isValueNode -> Assertions.assertEquals(expectedNode.asText(), responseBody)
            else -> Assertions.fail("Response body is not valid JSON")
        }
    }

    @Test
    @DisplayName("No security values provided")
    fun noSecurityValuesProvided() {
        val requestSpec = RestAssured.given()

        val response = requestSpec.get("/orders")
        response.then().statusCode(401)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Invalid X-API-Key API key security")
    fun invalidXAPIKeyAPIKeySecurity() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "some_really_invalid_api_key")

        val response = requestSpec.get("/orders")
        response.then().statusCode(401)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Invalid Query page parameter: Integer Breaking")
    fun invalidQueryPageParameterIntegerBreaking() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.queryParam("page", "1.5")

        val response = requestSpec.get("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Invalid Query page parameter: Out Of Minimum Boundary Number")
    fun invalidQueryPageParameterOutOfMinimumBoundaryNumber() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.queryParam("page", "0")

        val response = requestSpec.get("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Invalid Query pageSize parameter: Integer Breaking")
    fun invalidQueryPageSizeParameterIntegerBreaking() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.queryParam("pageSize", "1.5")

        val response = requestSpec.get("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Invalid Query pageSize parameter: Out Of Maximum Boundary Number")
    fun invalidQueryPageSizeParameterOutOfMaximumBoundaryNumber() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.queryParam("pageSize", "101")

        val response = requestSpec.get("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Invalid Query pageSize parameter: Out Of Minimum Boundary Number")
    fun invalidQueryPageSizeParameterOutOfMinimumBoundaryNumber() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.queryParam("pageSize", "0")

        val response = requestSpec.get("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Invalid Query status parameter: Invalid Enum Value")
    fun invalidQueryStatusParameterInvalidEnumValue() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.queryParam("status", "invalid_enum1")

        val response = requestSpec.get("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

}
