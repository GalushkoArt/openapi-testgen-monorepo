# RestAssured integration

The built-in Mustache templates include RestAssured-based template sets:

- `restassured-java`
- `restassured-kotlin`

See [Template generator](../generators/template-generator.md) for all options.

## Gradle example

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

## CLI example

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./build/generated-tests \
  --generator template \
  --generator-option templateSet=restassured-java \
  --generator-option templateVariables.package=com.example.generated \
  --generator-option templateVariables.baseUrl=http://localhost:8080
```

## Configure base URL

Built-in templates read `baseUrl` from `templateVariables`. This is required for RestAssured to know where to send requests.

## Samples

- [Java Spring RestAssured sample](../../samples/java-spring-rest-assured.md)
- [Kotlin Spring RestAssured sample](../../samples/kotlin-spring-rest-assured.md)
