---
description: Configure the template generator to render Mustache-based Java or Kotlin tests from OpenAPI specifications. Covers generator options, template variables, and output control settings.
---

# Template generator (`template`)

The `template` generator renders tests from Mustache templates.

## When to use it

Use the template generator when you want source-code tests (Java/Kotlin) that can be compiled and executed as part of your test suite.

## Generator id

- `generator = "template"`

## Generator options

Options are provided via `generatorOptions`.

For the complete list of options and defaults, see [Generator options](../../reference/catalogs/generator-options.md#template-options).

### `templateSet`

Select a built-in template set:

- `restassured-java` (default)
- `restassured-kotlin`

### `templateVariables`

`templateVariables` is a map of values available to templates as `customVariables.*`.

Built-in RestAssured templates use:

- `package`: optional package declaration
- `baseUrl`: assigned to `RestAssured.baseURI`
- `springBootTest`: when truthy, emits Spring Boot test annotations/imports

Example (CLI):

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./build/generated-tests \
  --generator template \
  --generator-option templateSet=restassured-java \
  --generator-option templateVariables.package=com.example.generated \
  --generator-option templateVariables.baseUrl=http://localhost:8080
```

Example (Gradle):

```kotlin
openApiTestGenerator {
    generator.set("template")
    generatorOptions.putAll(
        mapOf(
            "templateSet" to "restassured-java",
            "templateVariables" to mapOf(
                "package" to "com.example.generated",
                "baseUrl" to "http://localhost:8080",
            ),
        )
    )
}
```

### Output control

Output naming and write behavior are controlled by template generator options (for example, `outputFileNamePattern`, `writeMode`, `fileHeaderComment`).

See [Generator options](../../reference/catalogs/generator-options.md#template-options) for the authoritative list, defaults, and behavior.

Note: `className` is derived from `operationName` (operationId). When operationId is missing, the generator uses HTTP method + path and normalizes the result into a valid identifier by treating non-alphanumeric characters as word separators.

## Example: Request Body Validation Test

Excerpt from `samples/java-spring-rest-assured/src/test/java/art/galushko/java/spring/rest/assured/CreateOrderTest.java`:

```java
@Test
@DisplayName("Incorrect Request Body: Missed Required Object Properties items")
public void incorrectRequestBodyMissedRequiredObjectPropertiesItems()  {
    RequestSpecification requestSpec = RestAssured.given();
    requestSpec.header("X-API-Key", "test-api-key-123");
    requestSpec.header("Content-Type", "application/json");
    String requestBody = "{\"userId\":\"a\"}";
    requestSpec.body(requestBody);

    Response response = requestSpec.post("/orders");
    response.then().statusCode(400);

    String responseBody = response.getBody().asString();
    String expectedBodyJson = "{\"code\":\"bad_request\",\"message\":\"Invalid input\"}";
    assertExpectedBody(expectedBodyJson, responseBody);
}
```

!!! note "Response body assertions"
    The built-in RestAssured templates assert `expectedBody` as JSON. For non-JSON responses, provide an explicit example value (for example, a `text/plain` string) in the OpenAPI spec. Schema-derived fallbacks are applied only for JSON-like media types.

## Related documentation

- Modules: [generator-template](../../modules/generator-template.md)
- [Custom templates](./custom-templates.md)

