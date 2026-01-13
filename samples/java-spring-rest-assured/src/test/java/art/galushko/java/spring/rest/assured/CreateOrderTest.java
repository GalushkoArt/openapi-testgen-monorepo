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
@DisplayName("Tests for createOrder")
public class CreateOrderTest {
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
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"items\":[{\"price\":0,\"quantity\":1,\"sku\":\"a\"}],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
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
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"items\":[{\"price\":0,\"quantity\":1,\"sku\":\"a\"}],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(401);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Invalid Header Idempotency-Key parameter: Out Of Maximum Length String")
    public void invalidHeaderIdempotencyKeyParameterOutOfMaximumLengthString() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Idempotency-Key", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"items\":[{\"price\":0,\"quantity\":1,\"sku\":\"a\"}],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Required Request Body is missing")
    public void requiredRequestBodyIsMissing() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Missed Required Object Properties items")
    public void incorrectRequestBodyMissedRequiredObjectPropertiesItems() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Missed Required Object Properties userId")
    public void incorrectRequestBodyMissedRequiredObjectPropertiesUserId() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"items\":[{\"price\":0,\"quantity\":1,\"sku\":\"a\"}]}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Below Min Items Array")
    public void incorrectRequestBodyObjectPropertyItemsBelowMinItemsArray() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"items\":[],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Missed Required Object Properties price")
    public void incorrectRequestBodyObjectPropertyItemsArrayItemMissedRequiredObjectPropertiesPrice() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"items\":[{\"quantity\":1,\"sku\":\"a\"}],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Missed Required Object Properties quantity")
    public void incorrectRequestBodyObjectPropertyItemsArrayItemMissedRequiredObjectPropertiesQuantity() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"items\":[{\"price\":0,\"sku\":\"a\"}],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Missed Required Object Properties sku")
    public void incorrectRequestBodyObjectPropertyItemsArrayItemMissedRequiredObjectPropertiesSku() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"items\":[{\"price\":0,\"quantity\":1}],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Object Property quantity Integer Breaking")
    public void incorrectRequestBodyObjectPropertyItemsArrayItemObjectPropertyQuantityIntegerBreaking() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"items\":[{\"price\":0,\"quantity\":1.5,\"sku\":\"a\"}],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Object Property quantity Out Of Minimum Boundary Number")
    public void incorrectRequestBodyObjectPropertyItemsArrayItemObjectPropertyQuantityOutOfMinimumBoundaryNumber() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"items\":[{\"price\":0,\"quantity\":0,\"sku\":\"a\"}],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property items Array Item Object Property price Out Of Minimum Boundary Number")
    public void incorrectRequestBodyObjectPropertyItemsArrayItemObjectPropertyPriceOutOfMinimumBoundaryNumber() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"items\":[{\"price\":-1,\"quantity\":1,\"sku\":\"a\"}],\"userId\":\"a\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/orders");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

}
