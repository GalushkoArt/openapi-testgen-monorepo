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
@DisplayName("Tests for createUser")
public class CreateUserTest {
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
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"email\":\"test0@example.com\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
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
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"email\":\"test0@example.com\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
        response.then().statusCode(401);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}";
        assertExpectedBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Invalid Header Idempotency-Key parameter: Out Of Maximum Length String")
    public void invalidHeaderIdempotencyKeyParameterOutOfMaximumLengthString() {
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
        assertExpectedBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Required Request Body is missing")
    public void requiredRequestBodyIsMissing() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Missed Required Object Properties email")
    public void incorrectRequestBodyMissedRequiredObjectPropertiesEmail() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Missed Required Object Properties name")
    public void incorrectRequestBodyMissedRequiredObjectPropertiesName() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"email\":\"test0@example.com\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property id Wrong UUID Format")
    public void incorrectRequestBodyObjectPropertyIdWrongUUIDFormat() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"email\":\"test0@example.com\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"id\":\"8e258b27-c787-49ef-9539-11461b251ffg\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property name Out Of Maximum Length String")
    public void incorrectRequestBodyObjectPropertyNameOutOfMaximumLengthString() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"email\":\"test0@example.com\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property name Out Of Minimum Length String")
    public void incorrectRequestBodyObjectPropertyNameOutOfMinimumLengthString() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"email\":\"test0@example.com\",\"name\":\"\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property email Invalid Pattern")
    public void incorrectRequestBodyObjectPropertyEmailInvalidPattern() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"email\":\"[ {\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedBody(expectedBodyJson, responseBody);
    }

    @Test
    @DisplayName("Incorrect Request Body: Object Property email Wrong Email Format")
    public void incorrectRequestBodyObjectPropertyEmailWrongEmailFormat() {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.header("Content-Type", "application/json");
        String requestBody = "{\"email\":\"invalid.email@example\",\"name\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}";
        requestSpec.body(requestBody);

        Response response = requestSpec.post("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        assertExpectedBody(expectedBodyJson, responseBody);
    }

}
