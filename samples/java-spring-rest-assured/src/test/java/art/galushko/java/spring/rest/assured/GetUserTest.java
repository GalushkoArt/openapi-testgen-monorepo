package art.galushko.java.spring.rest.assured;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@DisplayName("Tests for getUser")
public class GetUserTest {
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "http://localhost:8080/v1";
    }

    @Test
    @DisplayName("No security values provided")
    public void noSecurityValuesProvided() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.pathParam("userId", "wha_262laxjwhyaz8");

        Response response = requestSpec.get("/users/{userId}");
        response.then().statusCode(401);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Invalid X-API-Key API key security")
    public void invalidXAPIKeyAPIKeySecurity() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "new_invalid_api_key");
        requestSpec.pathParam("userId", "wha_262laxjwhyaz8");

        Response response = requestSpec.get("/users/{userId}");
        response.then().statusCode(401);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Invalid Path userId parameter: Invalid Pattern")
    public void invalidPathUserIdParameterInvalidPattern() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.pathParam("userId", "AE.");

        Response response = requestSpec.get("/users/{userId}");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

}
