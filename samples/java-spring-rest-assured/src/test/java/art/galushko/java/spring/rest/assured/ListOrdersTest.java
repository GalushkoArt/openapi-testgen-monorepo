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
@DisplayName("Tests for listOrders")
public class ListOrdersTest {
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "http://localhost:8080/v1";
    }

    private void assertExpectedJsonBody(String expectedBodyJson, String responseBody) {
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
        requestSpec.header("Accept", "application/json");

        Response response = requestSpec.get("/orders");
        response.then().statusCode(401);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Invalid X-API-Key API key security")
    public void invalidXAPIKeyAPIKeySecurity() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "new_invalid_api_key");
        requestSpec.header("Accept", "application/json");

        Response response = requestSpec.get("/orders");
        response.then().statusCode(401);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Invalid Query page parameter: Integer Breaking")
    public void invalidQueryPageParameterIntegerBreaking() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Accept", "application/json");
        requestSpec.queryParam("page", "1.5");

        Response response = requestSpec.get("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Invalid Query page parameter: Out Of Minimum Boundary Number")
    public void invalidQueryPageParameterOutOfMinimumBoundaryNumber() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Accept", "application/json");
        requestSpec.queryParam("page", "0");

        Response response = requestSpec.get("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Invalid Query pageSize parameter: Integer Breaking")
    public void invalidQueryPageSizeParameterIntegerBreaking() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Accept", "application/json");
        requestSpec.queryParam("pageSize", "1.5");

        Response response = requestSpec.get("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Invalid Query pageSize parameter: Out Of Maximum Boundary Number")
    public void invalidQueryPageSizeParameterOutOfMaximumBoundaryNumber() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Accept", "application/json");
        requestSpec.queryParam("pageSize", "101");

        Response response = requestSpec.get("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Invalid Query pageSize parameter: Out Of Minimum Boundary Number")
    public void invalidQueryPageSizeParameterOutOfMinimumBoundaryNumber() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Accept", "application/json");
        requestSpec.queryParam("pageSize", "0");

        Response response = requestSpec.get("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

}
