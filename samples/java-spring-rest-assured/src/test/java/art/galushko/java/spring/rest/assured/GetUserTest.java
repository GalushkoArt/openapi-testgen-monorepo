package art.galushko.java.spring.rest.assured;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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

    private void assertExpectedBody(String expectedBodyJson, String responseBody) {
        JsonNode expectedNode;
        try {
            expectedNode = objectMapper.readTree(expectedBodyJson);
        } catch (JsonProcessingException e) {
            Assertions.fail("Expected body is not valid JSON: " + e.getMessage());
            return;
        }

        JsonNode actualNode;
        try {
            actualNode = objectMapper.readTree(responseBody);
        } catch (JsonProcessingException ignored) {
            actualNode = null;
        }

        if (actualNode != null) {
            Assertions.assertEquals(expectedNode, actualNode);
        } else if (expectedNode.isValueNode()) {
            Assertions.assertEquals(expectedNode.asText(), responseBody);
        } else {
            Assertions.fail("Response body is not valid JSON");
        }
    }

    @Test
    @DisplayName("No security values provided")
    public void noSecurityValuesProvided() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.pathParam("userId", "wha_262laxjwhyaz8");

        Response response = requestSpec.get("/users/{userId}");
        response.then().statusCode(401);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}";
        assertExpectedBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Invalid X-API-Key API key security")
    public void invalidXAPIKeyAPIKeySecurity() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "new_invalid_api_key");
        requestSpec.pathParam("userId", "wha_262laxjwhyaz8");

        Response response = requestSpec.get("/users/{userId}");
        response.then().statusCode(401);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}";
        assertExpectedBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Invalid Path userId parameter: Invalid Pattern")
    public void invalidPathUserIdParameterInvalidPattern() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.pathParam("userId", "AE.");

        Response response = requestSpec.get("/users/{userId}");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedBody(expectedBodyJson, responseBody);
    }

}
