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
@DisplayName("Tests for createUser")
class CreateUserTest {
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
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"email\":\"test0@example.com\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/users")
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
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"email\":\"test0@example.com\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/users")
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
        val requestBody = "{\"email\":\"test0@example.com\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/users")
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

        val response = requestSpec.post("/users")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Missed Required Object Properties email")
    fun incorrectRequestBodyMissedRequiredObjectPropertiesEmail() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/users")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Missed Required Object Properties name")
    fun incorrectRequestBodyMissedRequiredObjectPropertiesName() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"email\":\"test0@example.com\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/users")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property id Wrong UUID Format")
    fun incorrectRequestBodyObjectPropertyIdWrongUUIDFormat() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"email\":\"test0@example.com\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"id\":\"8e258b27-c787-49ef-9539-11461b251ffg\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/users")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property name Out Of Maximum Length String")
    fun incorrectRequestBodyObjectPropertyNameOutOfMaximumLengthString() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"email\":\"test0@example.com\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/users")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property name Out Of Minimum Length String")
    fun incorrectRequestBodyObjectPropertyNameOutOfMinimumLengthString() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"email\":\"test0@example.com\",\"name\":\"\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/users")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property email Invalid Pattern")
    fun incorrectRequestBodyObjectPropertyEmailInvalidPattern() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"email\":\"[ {\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/users")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property email Wrong Email Format")
    fun incorrectRequestBodyObjectPropertyEmailWrongEmailFormat() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.header("Content-Type", "application/json")
        val requestBody = "{\"email\":\"invalid.email@example\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}"
        requestSpec.body(requestBody)

        val response = requestSpec.post("/users")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        assertExpectedBody(expectedBodyJson, responseBody)
    }

}
