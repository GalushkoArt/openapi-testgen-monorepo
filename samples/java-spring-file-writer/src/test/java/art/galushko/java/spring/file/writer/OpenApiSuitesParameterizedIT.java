package art.galushko.java.spring.file.writer;

import art.galushko.openapi.testgen.model.KeyValuePair;
import art.galushko.openapi.testgen.model.TestCase;
import art.galushko.openapi.testgen.model.TestSuite;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;

@DisplayName("OpenAPI suites: parameterized execution via Rest Assured")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OpenApiSuitesParameterizedIT {

    @LocalServerPort
    private int port;

    private static final String RESOURCE_NAME = "openapi-test-suites.json";
    private static final String baseUri = "http://localhost:%d/v1";
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new KotlinModule.Builder().build());

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCasesProvider")
    void executeGeneratedCase(String scenarioLabel, TestCase testCase) throws Exception {
        RestAssured.baseURI = String.format(baseUri, port);
        var req = RestAssured.given()
            .log()
            .all()
            .pathParams(testCase.getPathParams())
            .queryParams(testCase.getQueryParams());

        // Headers
        for (KeyValuePair<String, Object> header : testCase.getHeaders()) {
            req.header(header.getKey(), String.valueOf(header.getValue()));
        }

        // Cookies
        for (KeyValuePair<String, Object> cookie : testCase.getCookie()) {
            req.cookie(cookie.getKey(), String.valueOf(cookie.getValue()));
        }

        // Body
        if (testCase.getBody() != null) {
            var bodyJson = mapper.writeValueAsString(testCase.getBody());
            req.body(bodyJson);
        }

        req.header("Content-Type", "application/json")
            .request(testCase.getMethod(), testCase.getPath())
            .then().statusCode(testCase.getExpectedStatusCode())
            .body("", equalTo(testCase.getExpectedBody()));
    }

    static Stream<Arguments> testCasesProvider() throws Exception {
        try (InputStream is = Objects.requireNonNull(
            OpenApiSuitesParameterizedIT.class.getClassLoader().getResourceAsStream(RESOURCE_NAME),
            "Resource not found: " + RESOURCE_NAME
        )) {
            Map<String, TestSuite> suites = mapper.readValue(is, new TypeReference<>() {
            });

            return suites.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .flatMap(entry -> entry.getValue().getTestCases().stream()
                    .map(tc -> Arguments.of(entry.getKey() + " - " + tc.getName(), tc))
                );
        }
    }
}


