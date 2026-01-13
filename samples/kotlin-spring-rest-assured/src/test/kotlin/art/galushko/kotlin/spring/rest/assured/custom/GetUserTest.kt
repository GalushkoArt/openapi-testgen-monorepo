package art.galushko.kotlin.spring.rest.assured.custom

import io.restassured.RestAssured
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeAll
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@DisplayName("Tests for getUser")
class GetUserTest {
    @Autowired
    lateinit var objectMapper: ObjectMapper

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            RestAssured.baseURI = "http://localhost:8080/v1"
        }
    }

    @Test
    @DisplayName("No security values provided")
    fun noSecurityValuesProvided() {
        val requestSpec = RestAssured.given()
        requestSpec.pathParam("userId", "wha_262laxjwhyaz8")

        val response = requestSpec.get("/users/{userId}")
        response.then().statusCode(401)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}"
        val expected = objectMapper.readValue(expectedBodyJson, java.util.Map::class.java)
        val actual = objectMapper.readValue(responseBody, java.util.Map::class.java)
        Assertions.assertEquals(expected, actual)
    }

    @Test
    @DisplayName("Invalid X-API-Key API key security")
    fun invalidXAPIKeyAPIKeySecurity() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "some_really_invalid_api_key")
        requestSpec.pathParam("userId", "wha_262laxjwhyaz8")

        val response = requestSpec.get("/users/{userId}")
        response.then().statusCode(401)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}"
        val expected = objectMapper.readValue(expectedBodyJson, java.util.Map::class.java)
        val actual = objectMapper.readValue(responseBody, java.util.Map::class.java)
        Assertions.assertEquals(expected, actual)
    }

    @Test
    @DisplayName("Invalid Path userId parameter: Invalid Pattern")
    fun invalidPathUserIdParameterInvalidPattern() {
        val requestSpec = RestAssured.given()
        requestSpec.header("X-API-Key", "test-api-key-123")
        requestSpec.pathParam("userId", "AE.")

        val response = requestSpec.get("/users/{userId}")
        response.then().statusCode(400)
        val responseBody = response.body.asString()
        val expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}"
        val expected = objectMapper.readValue(expectedBodyJson, java.util.Map::class.java)
        val actual = objectMapper.readValue(responseBody, java.util.Map::class.java)
        Assertions.assertEquals(expected, actual)
    }

}
