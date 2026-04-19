---
description: Configure built-in generators (template and test-suite-writer), create custom Mustache templates, and integrate with RestAssured and Spring Boot.
---

# Generators

The generator determines the output format of your test suites. Two built-in generators are available:

- **`template`** -- renders source-code tests (Java/Kotlin) from Mustache templates
- **`test-suite-writer`** -- writes JSON/YAML data files containing `TestSuite` structures

## Template generator

The `template` generator renders tests from Mustache templates.

### When to use it

Use the template generator when you want source-code tests (Java/Kotlin) that can be compiled and executed as part of your test suite.

### Generator id

- `generator = "template"`

### Generator options

Options are provided via `generatorOptions`.

#### `templateSet`

Select a built-in template set:

- `restassured-java` (default)
- `restassured-kotlin`

#### `templateVariables`

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

#### Output control

Output naming and write behavior are controlled by template generator options (for example, `outputFileNamePattern`, `writeMode`, `fileHeaderComment`).

Note: `className` is derived from `operationName` (operationId). When operationId is missing, the generator uses HTTP method + path and normalizes the result into a valid identifier by treating non-alphanumeric characters as word separators.

### Template generator options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `templateSet` | String | `restassured-java` | Built-in template set id (for example, `restassured-java`, `restassured-kotlin`). |
| `classTemplatePath` | String | `templates/{{templateSet}}/class.mustache` | Mustache class template path. Supports `{{templateSet}}`. |
| `customTemplateDir` | String | `null` | Filesystem directory containing user templates. |
| `templateVariables` | Map | `{}` | Variables exposed to templates as `customVariables.*`. |
| `templateVariables.classSuffix` | String | `Test` | Suffix appended to generated class names. |
| `templateVariables.methodPrefix` | String | `""` | Prefix prepended to generated method names. |
| `templateVariables.methodSuffix` | String | `""` | Suffix appended to generated method names. |
| `outputFileExtension` | String | inferred | Usually `java` or `kt`; set it explicitly for custom template sets. |
| `outputFileNamePattern` | String | `{{className}}.{{outputFileExtension}}` | Output naming pattern. Supports `{{className}}` and `{{outputFileExtension}}`. |
| `writeMode` | String | `OVERWRITE` | `OVERWRITE` or `SKIP_IF_EXISTS`. |
| `fileHeaderComment` | String | `null` | Optional header string exposed to templates. |

### Example: request body validation test

Excerpt from `samples/java-spring-rest-assured/src/test/java/art/galushko/java/spring/rest/assured/CreateOrderTest.java`:

```java
@Test
@DisplayName("Incorrect Request Body: Missed Required Object Properties items")
public void incorrectRequestBodyMissedRequiredObjectPropertiesItems()  {
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
    assertExpectedBody(expectedBodyJson, responseBody);
}
```

!!! note "Request/response media type headers"
    Built-in RestAssured templates set:
    - `Content-Type` from `TestCase.requestBodyMediaType` (fallback: `application/json` for `POST`/`PUT`/`PATCH` when unset)
    - `Accept` from `TestCase.responseBodyMediaType` when present

!!! note "Built-in RestAssured media type support"
    Built-in RestAssured templates are JSON-first:
    - JSON-like request and response bodies (`application/json`, `text/json`, `+json`) are rendered and asserted automatically
    - Non-JSON scalar/string request bodies are passed through as raw literals
    - Non-JSON structured request or response bodies emit TODO guidance plus a placeholder/request preview instead of specialized XML/form/text helpers

### RestAssured integration

The built-in Mustache templates include RestAssured-based template sets:

- `restassured-java`
- `restassured-kotlin`

Built-in templates read `baseUrl` from `templateVariables`. This is required for RestAssured to know where to send requests.

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

For CLI usage, see the [template variables](#templatevariables) section above.

**Samples:**

- [Java Spring RestAssured sample](../samples/java-spring-rest-assured.md)
- [Kotlin Spring RestAssured sample](../samples/kotlin-spring-rest-assured.md)

### Spring Boot integration

If you want generated tests to run as Spring Boot tests, enable the template variable `springBootTest`.

Built-in RestAssured templates use `customVariables.springBootTest` to include Spring Boot annotations/imports. The templates use `SpringBootTest.WebEnvironment.DEFINED_PORT` -- configure your app under test accordingly.

```kotlin
openApiTestGenerator {
    generator.set("template")
    generatorOptions.putAll(
        mapOf(
            "templateSet" to "restassured-java",
            "templateVariables" to mapOf(
                "package" to "com.example.generated",
                "baseUrl" to "http://localhost:8080",
                "springBootTest" to true,
            ),
        )
    )
}
```

**Samples:**

- [Java Spring RestAssured sample](../samples/java-spring-rest-assured.md)
- [Kotlin Spring RestAssured sample](../samples/kotlin-spring-rest-assured.md)

---

## Custom Mustache templates

The template generator can load templates from a custom directory, giving you full control over the generated test code.

### When to use custom templates

Custom templates are useful when you need:

- Different test framework (TestNG, Kotest, Spock, etc.)
- Custom assertions or matchers
- Additional test setup/teardown logic
- Different code style or conventions
- Integration with custom test utilities
- Output in languages other than Java/Kotlin

### Template directory structure

Create a directory with your custom templates:

```
templates/
├── class.mustache     # Main class template (required)
└── method.mustache    # Test method partial (optional, included via {{> method}})
```

The `class.mustache` template is the entry point. You can split reusable parts into partials and include them using `{{> partialName}}` syntax.

### Configuration

#### Gradle

```kotlin
openApiTestGenerator {
    specFile.set("openapi.yaml")
    outputDir.set(layout.projectDirectory.dir("src/test/kotlin/generated"))
    generator.set("template")
    generatorOptions.putAll(
        mapOf(
            "templateSet" to "restassured-kotlin",
            "customTemplateDir" to "templates",
            "classTemplatePath" to "class.mustache",
            "outputFileExtension" to "kt",
            "templateVariables" to mapOf(
                "package" to "com.example.generated",
                "baseUrl" to "http://localhost:8080/v1",
                "springBootTest" to "true",
            ),
        )
    )
}
```

#### CLI

```bash
openapi-testgen \
  --spec-file openapi.yaml \
  --output-dir ./generated \
  --generator template \
  --generator-option customTemplateDir=./templates \
  --generator-option classTemplatePath=class.mustache \
  --generator-option outputFileExtension=kt \
  --generator-option templateVariables.package=com.example.generated \
  --generator-option templateVariables.baseUrl=http://localhost:8080
```

#### YAML config

```yaml
generator: template
generatorOptions:
    customTemplateDir: "./templates"
    classTemplatePath: "class.mustache"
    outputFileExtension: "kt"
    templateVariables:
        package: "com.example.generated"
        baseUrl: "http://localhost:8080/v1"
        springBootTest: "true"
```

### Available template variables

#### Class-level context

| Variable            | Type   | Description                                       |
|---------------------|--------|---------------------------------------------------|
| `className`         | String | Generated class name (e.g., `ListUsersTest`)      |
| `operationName`     | String | OpenAPI operation name                            |
| `operationPath`     | String | API endpoint path                                 |
| `methods`           | Array  | List of test method contexts                      |
| `customVariables`   | Map    | Custom variables from `templateVariables` config  |
| `fileHeaderComment` | String | Optional header comment                           |
| `escapeString`      | Lambda | Helper function for escaping strings in templates |

#### Method-level context

Each item in `methods` array has:

| Variable               | Type    | Description                                      |
|------------------------|---------|--------------------------------------------------|
| `methodName`           | String  | Safe method name for code                        |
| `testCaseName`         | String  | Human-readable test case name                    |
| `description`          | String  | Test case description                            |
| `httpMethod`           | String  | HTTP method (`get`, `post`, `put`, etc.)         |
| `path`                 | String  | Request path with placeholders replaced          |
| `expectedStatusCode`   | Integer | Expected HTTP response status                    |
| `headers`              | Array   | List of `{key, value, escapedValue}` header pairs |
| `pathParams`           | Array   | List of `{key, value, escapedValue}` path parameter pairs |
| `queryParams`          | Array   | List of `{key, value, escapedValue}` query parameter pairs |
| `cookies`              | Array   | List of `{key, value, escapedValue}` cookie pairs |
| `requestBody`          | Object  | Request body context (nullable)                  |
| `requestBodyMediaType` | String  | Selected request media type (nullable)           |
| `expectedResponseBody` | Object  | Expected response body context (nullable)        |
| `responseBodyMediaType` | String  | Selected response media type (nullable)          |
| `assertJsonResponseBody` | Boolean | Built-in helper flag for JSON comparison       |
| `requestBodyTodoComment` | String | Manual-completion note for unsupported request rendering |
| `responseAssertionTodoComment` | String | Manual-completion note for unsupported response assertions |
| `notes`                | Array   | High-level TODO comments emitted by built-in templates |
| `needToComplete`       | Boolean | Whether the test requires manual completion      |
| `shouldHaveBody`       | Boolean | True for POST, PUT, PATCH methods                |
| `customVariables`      | Map     | Custom variables from `templateVariables` config |

#### Request/response body context

When `requestBody` or `expectedResponseBody` is present:

| Variable        | Type   | Description                              |
|-----------------|--------|------------------------------------------|
| `rawBody`       | String | Generated body string before template escaping |
| `escapedRawBody` | String | Body string escaped for embedding in Java/Kotlin string literals |
| `body`    | Object | Parsed body object for structured access |

### Example templates

#### class.mustache

```mustache
{{#customVariables.package}}
package {{customVariables.package}}
{{/customVariables.package}}

import io.restassured.module.kotlin.extensions.*
import org.junit.jupiter.api.Test
{{#customVariables.springBootTest}}
import org.springframework.boot.test.context.SpringBootTest
{{/customVariables.springBootTest}}

{{#customVariables.springBootTest}}
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
{{/customVariables.springBootTest}}
class {{className}} {

    companion object {
        private const val BASE_URL = "{{customVariables.baseUrl}}"
    }

{{#methods}}
{{> method}}
{{/methods}}
}
```

#### method.mustache

```mustache
    @Test
    fun `{{testCaseName}}`() {
        Given {
            baseUri(BASE_URL)
{{#headers}}
            header("{{key}}", "{{value}}")
{{/headers}}
{{#queryParams}}
            queryParam("{{key}}", "{{value}}")
{{/queryParams}}
{{#requestBodyMediaType}}
            contentType("{{requestBodyMediaType}}")
{{/requestBodyMediaType}}
{{#responseBodyMediaType}}
            accept("{{responseBodyMediaType}}")
{{/responseBodyMediaType}}
{{#requestBody}}
            body("""{{requestBody.rawBody}}""")
{{/requestBody}}
        } When {
            {{httpMethod}}("{{path}}")
        } Then {
            statusCode({{expectedStatusCode}})
        }
    }

```

### Mustache syntax reference

| Syntax                            | Description                      |
|-----------------------------------|----------------------------------|
| `{{variable}}`                    | Output escaped variable          |
| `{{{variable}}}`                  | Output unescaped variable        |
| `{{#condition}}...{{/condition}}` | Conditional block (truthy check) |
| `{{^condition}}...{{/condition}}` | Inverted block (falsy check)     |
| `{{#list}}...{{/list}}`           | Iterate over array               |
| `{{> partial}}`                   | Include partial template         |
| `{{! comment }}`                  | Comment (not rendered)           |

### Escaping strings

Use the `escapeString` lambda to safely escape strings in generated code:

```mustache
{{#escapeString}}{{value}}{{/escapeString}}
```

### Tips and best practices

1. **Start from built-in templates**: Copy a built-in template set as a starting point
2. **Use partials for reuse**: Split common patterns into separate partial files
3. **Test incrementally**: Generate a few test files and verify output before scaling up
4. **Keep templates deterministic**: Avoid logic that produces inconsistent output
5. **Handle nullable fields**: Use conditional blocks for optional data (`requestBody`, `cookies`, etc.)

---

## Test-suite-writer generator

`TestSuiteWriter` writes test suites as JSON or YAML. It is **stateful** and **not thread-safe**. Create a new instance per generation run.

For core entry points and extension context, see the [core module](../modules/core.md).
For distribution defaults and wiring, see [distribution settings](../reference/distribution-settings.md).
For configuration via YAML, see [YAML config](configuration.md#yaml-configuration).

### Output modes

- `SINGLE_FILE`: aggregate all suites into a single file keyed by operation name.
- `MULTIPLE_FILES`: write one file per suite (prefix + operation name + extension).

### Write modes

- `writeMode = OVERWRITE`: overwrite output for suites generated in the current run.
- `writeMode = MERGE` (default): load existing output and merge by suite `operationName` and test case `name`.

### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `outputMode` | String | `SINGLE_FILE` | `SINGLE_FILE` aggregates all suites into one file; `MULTIPLE_FILES` writes one file per suite. |
| `outputFileName` | String | required for `SINGLE_FILE` | Output file name used in `SINGLE_FILE` mode. |
| `format` | String | `JSON` | Output format: `JSON` or `YAML`. |
| `indent` | String | `4 spaces` | Indentation string for JSON output. |
| `writeMode` | String | `MERGE` | `MERGE` loads existing suites and merges by test case name; `OVERWRITE` starts fresh. |
| `preventOverwriteSuites` | Boolean | `false` | In merge mode, keep existing suites unchanged. |
| `preventOverwriteCases` | Boolean | `true` | In merge mode, keep existing test cases unchanged and append only new ones. |
| `protectedTestCaseFields` | List / String | `[]` | Fields to preserve when overwriting cases. Accepts a list or comma-separated string. |
| `fileNamePrefix` | String | `""` | Prefix used in `MULTIPLE_FILES` mode. |

### Merge semantics

- `OVERWRITE` does not load existing output.
- `MERGE` loads existing output first and merges incoming suites by `operationName` and test cases by `name`.
- `preventOverwriteSuites=true` keeps existing suites as-is.
- `preventOverwriteSuites=false` updates suite metadata and then merges cases.
- `preventOverwriteCases=true` preserves existing test cases and appends only missing ones.
- `preventOverwriteCases=false` overwrites matching test cases by `name`.
- `protectedTestCaseFields` applies only when `preventOverwriteCases=false`.
- Writes are atomic: output is written to a temporary file and then atomically replaced.

Valid values for `protectedTestCaseFields`:

- `name`
- `method`
- `path`
- `queryParams`
- `pathParams`
- `headers`
- `cookie`
- `securityValues`
- `body`
- `requestBodyMediaType`
- `expectedBody`
- `responseBodyMediaType`
- `needToComplete`
- `expectedStatusCode`
- `rule`

#### Batch write optimization

In `SINGLE_FILE` mode, the generator writes the aggregated file **once** after processing all suites in memory. In `MULTIPLE_FILES` mode, suite files are written in parallel for faster I/O. Both optimizations apply automatically in CLI and Gradle plugin (no configuration needed).

### Example output

```json
{
  "getUser": {
    "path": "/users/{userId}",
    "method": "GET",
    "operationName": "getUser",
    "testCases": [
      {
        "name": "Invalid Path userId parameter: Invalid Pattern",
        "method": "GET",
        "path": "/users/{userId}",
        "pathParams": { "userId": "AE." },
        "headers": [{ "key": "X-API-Key", "value": "test-api-key-123" }],
        "expectedStatusCode": 400,
        "rule": "...InvalidPatternSchemaValidationRule"
      }
    ]
  }
}
```

!!! note "Query parameter encoding"
    The `queryParams` field uses structured values. Your test runner should serialize objects
    according to the parameter's `style` and `explode` settings from the OpenAPI spec.

For detailed field definitions, types, and complete examples, see:

- [Model reference](../reference/model.md) - TestSuite, TestCase fields including `securityValues`, `body`, `requestBodyMediaType`, `expectedBody`, `responseBodyMediaType`, and OAuth2 scope metadata

### Output location

- `SINGLE_FILE` mode: `{outputDir}/{outputFileName}` (e.g., `./build/generated/test-suites.json`)
- `MULTIPLE_FILES` mode: `{outputDir}/{fileNamePrefix}{operationName}.{format}` per operation

---

## Extending generators

To add a custom generator (new generator id), see [Custom generators](extending.md#custom-generators) and [Custom modules](extending.md#custom-modules).

## Related docs

- Modules: [module catalog](../modules/index.md#generator-template)
- [Include operations](configuration.md#include-operations) - Target specific operations
- [Kotlin Spring RestAssured sample](../samples/kotlin-spring-rest-assured.md)
- [Java Spring RestAssured sample](../samples/java-spring-rest-assured.md)
