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
@DisplayName("Tests for createUser")
public class CreateUserTest {
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
        String requestBody = "{\"email\":\"test0@example.com\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
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
        String requestBody = "{\"email\":\"test0@example.com\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
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
        String requestBody = "{\"email\":\"test0@example.com\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
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

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Missed Required Object Properties email")
    public void incorrectRequestBodyMissedRequiredObjectPropertiesEmail() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Missed Required Object Properties name")
    public void incorrectRequestBodyMissedRequiredObjectPropertiesName() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"email\":\"test0@example.com\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property id Wrong UUID Format")
    public void incorrectRequestBodyObjectPropertyIdWrongUUIDFormat() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"email\":\"test0@example.com\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"id\":\"8e258b27-c787-49ef-9539-11461b251ffg\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property name Out Of Maximum Length String")
    public void incorrectRequestBodyObjectPropertyNameOutOfMaximumLengthString() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"email\":\"test0@example.com\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property name Out Of Minimum Length String")
    public void incorrectRequestBodyObjectPropertyNameOutOfMinimumLengthString() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"email\":\"test0@example.com\",\"name\":\"\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property email Invalid Pattern")
    public void incorrectRequestBodyObjectPropertyEmailInvalidPattern() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"email\":\"[ {\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property email Wrong Email Format")
    public void incorrectRequestBodyObjectPropertyEmailWrongEmailFormat() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"email\":\"invalid.email@example\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

}
