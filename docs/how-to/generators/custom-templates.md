# Custom Mustache templates

The template generator can load templates from a custom directory, giving you full control over the generated test code.

## When to use custom templates

Custom templates are useful when you need:

- Different test framework (TestNG, Kotest, Spock, etc.)
- Custom assertions or matchers
- Additional test setup/teardown logic
- Different code style or conventions
- Integration with custom test utilities
- Output in languages other than Java/Kotlin

## Template directory structure

Create a directory with your custom templates:

```
templates/
├── class.mustache     # Main class template (required)
└── method.mustache    # Test method partial (optional, included via {{> method}})
```

The `class.mustache` template is the entry point. You can split reusable parts into partials and include them using `{{> partialName}}` syntax.

## Configuration

### Gradle

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

### CLI

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

### YAML config

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

## Available template variables

### Class-level context

| Variable            | Type   | Description                                       |
|---------------------|--------|---------------------------------------------------|
| `className`         | String | Generated class name (e.g., `ListUsersTest`)      |
| `operationName`     | String | OpenAPI operation name                            |
| `operationPath`     | String | API endpoint path                                 |
| `methods`           | Array  | List of test method contexts                      |
| `customVariables`   | Map    | Custom variables from `templateVariables` config  |
| `fileHeaderComment` | String | Optional header comment                           |
| `escapeString`      | Lambda | Helper function for escaping strings in templates |

### Method-level context

Each item in `methods` array has:

| Variable               | Type    | Description                                      |
|------------------------|---------|--------------------------------------------------|
| `methodName`           | String  | Safe method name for code                        |
| `testCaseName`         | String  | Human-readable test case name                    |
| `description`          | String  | Test case description                            |
| `httpMethod`           | String  | HTTP method (`get`, `post`, `put`, etc.)         |
| `path`                 | String  | Request path with placeholders replaced          |
| `expectedStatusCode`   | Integer | Expected HTTP response status                    |
| `headers`              | Array   | List of `{key, value}` header pairs              |
| `pathParams`           | Array   | List of `{key, value}` path parameter pairs      |
| `queryParams`          | Array   | List of `{key, value}` query parameter pairs     |
| `cookies`              | Array   | List of `{key, value}` cookie pairs              |
| `requestBody`          | Object  | Request body context (nullable)                  |
| `expectedResponseBody` | Object  | Expected response body context (nullable)        |
| `needToComplete`       | Boolean | Whether the test requires manual completion      |
| `shouldHaveBody`       | Boolean | True for POST, PUT, PATCH methods                |
| `customVariables`      | Map     | Custom variables from `templateVariables` config |

### Request/response body context

When `requestBody` or `expectedResponseBody` is present:

| Variable  | Type   | Description                              |
|-----------|--------|------------------------------------------|
| `rawBody` | String | JSON/text body as a string               |
| `body`    | Object | Parsed body object for structured access |

### Custom variables

Variables from `templateVariables` config are available as `customVariables.*`:

```mustache
{{#customVariables.springBootTest}}
@SpringBootTest
{{/customVariables.springBootTest}}

package {{customVariables.package}}
```

## Example templates

### class.mustache

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

### method.mustache

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
{{#requestBody}}
            contentType("application/json")
            body("""{{requestBody.rawBody}}""")
{{/requestBody}}
        } When {
            {{httpMethod}}("{{path}}")
        } Then {
            statusCode({{expectedStatusCode}})
        }
    }

```

### Generated output

The templates above produce Kotlin tests like:

```kotlin
package com.example.generated

import io.restassured.module.kotlin.extensions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ListUsersTest {

    companion object {
        private const val BASE_URL = "http://localhost:8080/v1"
    }

    @Test
    fun `Valid request`() {
        Given {
            baseUri(BASE_URL)
            header("X-API-Key", "test-api-key-123")
        } When {
            get("/users")
        } Then {
            statusCode(200)
        }
    }

    @Test
    fun `Missing security: AllSecurityMissed`() {
        Given {
            baseUri(BASE_URL)
        } When {
            get("/users")
        } Then {
            statusCode(401)
        }
    }
}
```

## Template customization options

| Option                  | Type   | Default                 | Description                           |
|-------------------------|--------|-------------------------|---------------------------------------|
| `templateSet`           | String | `restassured-java`      | Base template set id                  |
| `customTemplateDir`     | String | (none)                  | Directory containing custom templates |
| `classTemplatePath`     | String | (from templateSet)      | Path to class template                |
| `outputFileExtension`   | String | (from templateSet)      | File extension (`java`, `kt`)         |
| `outputFileNamePattern` | String | `{{className}}.{{outputFileExtension}}` | Output file name pattern              |
| `writeMode`             | String | `OVERWRITE`             | `OVERWRITE` or `SKIP_IF_EXISTS`       |
| `fileHeaderComment`     | String | (none)                  | Optional header for generated files   |
| `templateVariables`     | Map    | `{}`                    | Custom variables for templates        |
| `templateVariables.classSuffix`  | String | `Test`       | Suffix appended to generated class names |
| `templateVariables.methodPrefix` | String | (none)       | Prefix for test method names          |
| `templateVariables.methodSuffix` | String | (none)       | Suffix for test method names          |

## Mustache syntax reference

Common Mustache patterns used in templates:

| Syntax                            | Description                      |
|-----------------------------------|----------------------------------|
| `{{variable}}`                    | Output escaped variable          |
| `{{{variable}}}`                  | Output unescaped variable        |
| `{{#condition}}...{{/condition}}` | Conditional block (truthy check) |
| `{{^condition}}...{{/condition}}` | Inverted block (falsy check)     |
| `{{#list}}...{{/list}}`           | Iterate over array               |
| `{{> partial}}`                   | Include partial template         |
| `{{! comment }}`                  | Comment (not rendered)           |

## Escaping strings

Use the `escapeString` lambda to safely escape strings in generated code:

```mustache
{{#escapeString}}{{value}}{{/escapeString}}
```

This handles special characters in test data that could break generated code.

## Tips and best practices

1. **Start from built-in templates**: Copy a built-in template set as a starting point
2. **Use partials for reuse**: Split common patterns into separate partial files
3. **Test incrementally**: Generate a few test files and verify output before scaling up
4. **Keep templates deterministic**: Avoid logic that produces inconsistent output
5. **Handle nullable fields**: Use conditional blocks for optional data (`requestBody`, `cookies`, etc.)

## Related docs

- [Template generator overview](template-generator.md)
- [Generator options reference](../../reference/catalogs/generator-options.md)
- [Kotlin Spring RestAssured sample](../../samples/kotlin-spring-rest-assured.md)
