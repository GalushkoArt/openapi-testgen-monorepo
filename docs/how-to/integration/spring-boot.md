---
description: Enable Spring Boot test annotations in generated tests by setting the springBootTest template variable. Works with the built-in RestAssured templates.
---

# Spring Boot integration

If you want generated tests to run as Spring Boot tests, use the `template` generator and enable the template variable `springBootTest`.

Built-in RestAssured templates use `customVariables.springBootTest` to include Spring Boot annotations/imports.

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
                "springBootTest" to true,
            ),
        )
    )
}
```

## Notes

- The built-in templates use `SpringBootTest.WebEnvironment.DEFINED_PORT`.
  Configure your app under test accordingly.

## Samples

- [Java Spring RestAssured sample](../../samples/java-spring-rest-assured.md)
- [Kotlin Spring RestAssured sample](../../samples/kotlin-spring-rest-assured.md)
