package art.galushko.kotlin.spring.rest.assured

import io.restassured.RestAssured
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeEach
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@DisplayName("Tests for createOrder")
class CreateOrderTest {
    @Autowired
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        RestAssured.baseURI = "http://localhost:8080/v1"
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
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"items\":[{\"price\":0,\"quantity\":1,\"sku\":\"a\"}],\"userId\":\"a\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/orders")
        response.then().statusCode(401)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Invalid X-API-Key API key security")
    fun invalidXAPIKeyAPIKeySecurity() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "new_invalid_api_key")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"items\":[{\"price\":0,\"quantity\":1,\"sku\":\"a\"}],\"userId\":\"a\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/orders")
        response.then().statusCode(401)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Invalid Header Idempotency-Key parameter: Out Of Maximum Length String")
    fun invalidHeaderIdempotencyKeyParameterOutOfMaximumLengthString() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Idempotency-Key", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"items\":[{\"price\":0,\"quantity\":1,\"sku\":\"a\"}],\"userId\":\"a\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Required Request Body is missing")
    fun requiredRequestBodyIsMissing() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")

        val response = requestSpec.post("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Missed Required Object Properties items")
    fun incorrectRequestBodyMissedRequiredObjectPropertiesItems() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"userId\":\"a\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Missed Required Object Properties userId")
    fun incorrectRequestBodyMissedRequiredObjectPropertiesUserId() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"items\":[{\"price\":0,\"quantity\":1,\"sku\":\"a\"}]}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Below Min Items Array")
    fun incorrectRequestBodyObjectPropertyItemsBelowMinItemsArray() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"items\":[],\"userId\":\"a\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Missed Required Object Properties price")
    fun incorrectRequestBodyObjectPropertyItemsArrayItemMissedRequiredObjectPropertiesPrice() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"items\":[{\"quantity\":1,\"sku\":\"a\"}],\"userId\":\"a\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Missed Required Object Properties quantity")
    fun incorrectRequestBodyObjectPropertyItemsArrayItemMissedRequiredObjectPropertiesQuantity() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"items\":[{\"price\":0,\"sku\":\"a\"}],\"userId\":\"a\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Missed Required Object Properties sku")
    fun incorrectRequestBodyObjectPropertyItemsArrayItemMissedRequiredObjectPropertiesSku() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"items\":[{\"price\":0,\"quantity\":1}],\"userId\":\"a\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Object Property quantity Integer Breaking")
    fun incorrectRequestBodyObjectPropertyItemsArrayItemObjectPropertyQuantityIntegerBreaking() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"items\":[{\"price\":0,\"quantity\":1.5,\"sku\":\"a\"}],\"userId\":\"a\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Object Property quantity Out Of Minimum Boundary Number")
    fun incorrectRequestBodyObjectPropertyItemsArrayItemObjectPropertyQuantityOutOfMinimumBoundaryNumber() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"items\":[{\"price\":0,\"quantity\":0,\"sku\":\"a\"}],\"userId\":\"a\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Object Property price Out Of Minimum Boundary Number")
    fun incorrectRequestBodyObjectPropertyItemsArrayItemObjectPropertyPriceOutOfMinimumBoundaryNumber() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"items\":[{\"price\":-1,\"quantity\":1,\"sku\":\"a\"}],\"userId\":\"a\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/orders")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

}
