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
@DisplayName("Tests for createOrder")
public class CreateOrderTest {
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
        requestSpec.header("Content-Type", "application/json");
        requestSpec.header("Accept", "application/json");
        String requestBody = "{\"userId\":\"u_123\",\"items\":[{\"sku\":\"SKU-1\",\"quantity\":1,\"price\":99.95},{\"sku\":\"SKU-2\",\"quantity\":1,\"price\":50.0}]}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
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
        requestSpec.header("Content-Type", "application/json");
        requestSpec.header("Accept", "application/json");
        String requestBody = "{\"userId\":\"u_123\",\"items\":[{\"sku\":\"SKU-1\",\"quantity\":1,\"price\":99.95},{\"sku\":\"SKU-2\",\"quantity\":1,\"price\":50.0}]}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(401);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Invalid Header Idempotency-Key parameter: Out Of Maximum Length String")
    public void invalidHeaderIdempotencyKeyParameterOutOfMaximumLengthString() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Idempotency-Key", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        requestSpec.header("Content-Type", "application/json");
        requestSpec.header("Accept", "application/json");
        String requestBody = "{\"userId\":\"u_123\",\"items\":[{\"sku\":\"SKU-1\",\"quantity\":1,\"price\":99.95},{\"sku\":\"SKU-2\",\"quantity\":1,\"price\":50.0}]}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Required Request Body is missing")
    public void requiredRequestBodyIsMissing() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        requestSpec.header("Accept", "application/json");

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Missed Required Object Properties items")
    public void incorrectRequestBodyMissedRequiredObjectPropertiesItems() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        requestSpec.header("Accept", "application/json");
        String requestBody = "{\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Missed Required Object Properties userId")
    public void incorrectRequestBodyMissedRequiredObjectPropertiesUserId() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        requestSpec.header("Accept", "application/json");
        String requestBody = "{\"items\":[{\"price\":0,\"quantity\":1,\"sku\":\"a\"}]}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Below Min Items Array")
    public void incorrectRequestBodyObjectPropertyItemsBelowMinItemsArray() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        requestSpec.header("Accept", "application/json");
        String requestBody = "{\"items\":[],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Missed Required Object Properties price")
    public void incorrectRequestBodyObjectPropertyItemsArrayItemMissedRequiredObjectPropertiesPrice() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        requestSpec.header("Accept", "application/json");
        String requestBody = "{\"items\":[{\"quantity\":1,\"sku\":\"a\"}],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Missed Required Object Properties quantity")
    public void incorrectRequestBodyObjectPropertyItemsArrayItemMissedRequiredObjectPropertiesQuantity() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        requestSpec.header("Accept", "application/json");
        String requestBody = "{\"items\":[{\"price\":0,\"sku\":\"a\"}],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Missed Required Object Properties sku")
    public void incorrectRequestBodyObjectPropertyItemsArrayItemMissedRequiredObjectPropertiesSku() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        requestSpec.header("Accept", "application/json");
        String requestBody = "{\"items\":[{\"price\":0,\"quantity\":1}],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Object Property quantity Integer Breaking")
    public void incorrectRequestBodyObjectPropertyItemsArrayItemObjectPropertyQuantityIntegerBreaking() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        requestSpec.header("Accept", "application/json");
        String requestBody = "{\"items\":[{\"price\":0,\"quantity\":1.5,\"sku\":\"a\"}],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Object Property quantity Out Of Minimum Boundary Number")
    public void incorrectRequestBodyObjectPropertyItemsArrayItemObjectPropertyQuantityOutOfMinimumBoundaryNumber() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        requestSpec.header("Accept", "application/json");
        String requestBody = "{\"items\":[{\"price\":0,\"quantity\":0,\"sku\":\"a\"}],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Object Property price Out Of Minimum Boundary Number")
    public void incorrectRequestBodyObjectPropertyItemsArrayItemObjectPropertyPriceOutOfMinimumBoundaryNumber() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        requestSpec.header("Accept", "application/json");
        String requestBody = "{\"items\":[{\"price\":-1,\"quantity\":1,\"sku\":\"a\"}],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedJsonBody(expectedBodyJson, responseBody);
    }

}
