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
@DisplayName("Tests for listUsers")
public class ListUsersTest {
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

        Response response = requestSpec.get("/users");
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

        Response response = requestSpec.get("/users");
        response.then().statusCode(401);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"unauthorized\",\"message\":\"API key required\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Invalid Query page parameter: Integer Breaking")
    public void invalidQueryPageParameterIntegerBreaking() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.queryParam("page", "1.5");

        Response response = requestSpec.get("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Invalid Query page parameter: Out Of Minimum Boundary Number")
    public void invalidQueryPageParameterOutOfMinimumBoundaryNumber() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.queryParam("page", "0");

        Response response = requestSpec.get("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Invalid Query pageSize parameter: Integer Breaking")
    public void invalidQueryPageSizeParameterIntegerBreaking() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.queryParam("pageSize", "1.5");

        Response response = requestSpec.get("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Invalid Query pageSize parameter: Out Of Maximum Boundary Number")
    public void invalidQueryPageSizeParameterOutOfMaximumBoundaryNumber() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.queryParam("pageSize", "101");

        Response response = requestSpec.get("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Invalid Query pageSize parameter: Out Of Minimum Boundary Number")
    public void invalidQueryPageSizeParameterOutOfMinimumBoundaryNumber() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.queryParam("pageSize", "0");

        Response response = requestSpec.get("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Invalid Query q parameter: Out Of Maximum Length String")
    public void invalidQueryQParameterOutOfMaximumLengthString() throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestSpecification requestSpec = RestAssured.given();
        requestSpec.header("X-API-Key", "test-api-key-123");
        requestSpec.queryParam("q", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        Response response = requestSpec.get("/users");
        response.then().statusCode(400);

        String responseBody = response.getBody().asString();
        String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
        java.util.Map<?, ?> expected = objectMapper.readValue(expectedBodyJson, java.util.Map.class);
        java.util.Map<?, ?> actual = objectMapper.readValue(responseBody, java.util.Map.class);
        Assertions.assertEquals(expected, actual);
    }

}
