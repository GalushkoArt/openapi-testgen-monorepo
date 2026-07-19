---
description: Demonstrates the template generator with RestAssured to produce executable Java test classes. Shows automatic build integration, template variables, and multiple generation tasks.
---

# Java Spring RestAssured sample

This sample demonstrates the `template` generator with RestAssured, producing executable Java test classes that can be compiled and run directly.

[View on GitHub](https://github.com/GalushkoArt/openapi-testgen-monorepo/tree/main/samples/java-spring-rest-assured){ .md-button }

## Overview

| Property | Value |
|----------|-------|
| Module | `samples:java-spring-rest-assured` |
| Generator | `template` |
| Template set | `restassured-java` |
| Output location | `build/generated/openapi-tests/` (auto-added to test sources) |

## What it demonstrates

- Using the `template` generator with built-in `restassured-java` templates
- Template variables for package name, base URL, and Spring Boot integration
- Automatic wiring of generated tests into the compilation pipeline
- Additional generation task using YAML configuration file
- Running generated tests against a Spring Boot application

## Plugin configuration

```kotlin
openApiTestGenerator {
    specFile.set(file("${rootDir}/samples/openapi.yaml").toURI().toString())
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests/art/galushko/java/spring/rest/assured"))
    generator.set("template")

    generatorOptions.putAll(
        mapOf(
            "templateSet" to "restassured-java",
            "templateVariables" to mapOf(
                "package" to "art.galushko.java.spring.rest.assured.generatedtests",
                "baseUrl" to "http://localhost:8080/v1",
                "springBootTest" to "true",
            ),
        )
    )

    testGenerationSettings {
        ignoreTestCases.putAll(mapOf(
            "/orders" to mapOf("GET" to listOf("Invalid Query page parameter: Integer Breaking"))
        ))
        validSecurityValues.putAll(mapOf("ApiKeyAuth" to "test-api-key-123"))
        overrideBasicTestData.putAll(mapOf("invalidApiKey" to "unrealistic_key"))
        maxSchemaDepth.set(15)
        maxSchemaCombinations.set(100)
        maxTestCasesPerOperation.set(1000)
        errorMode.set(ErrorMode.FAIL_FAST)
    }

    manualOnly.set(false)
}
```

### Template variables

| Variable | Value | Description |
|----------|-------|-------------|
| `package` | `art.galushko.java.spring.rest.assured.generatedtests` | Java package for generated classes |
| `baseUrl` | `http://localhost:8080/v1` | Base URL for API requests |
| `springBootTest` | `true` | Adds `@SpringBootTest` annotation |

## Generated test structure

The generator creates one test class per OpenAPI operation:

```java
package art.galushko.java.spring.rest.assured.generatedtests;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ListUsersTest {

    private static final String BASE_URL = "http://localhost:8080/v1";

    @Test
    void validRequest() {
        RestAssured.given()
            .baseUri(BASE_URL)
            .header("X-API-Key", "test-api-key-123")
        .when()
            .get("/users")
        .then()
            .statusCode(200);
    }

    @Test
    void missingSecurityAllSecurityMissed() {
        RestAssured.given()
            .baseUri(BASE_URL)
        .when()
            .get("/users")
        .then()
            .statusCode(401);
    }

    // ... more test methods for validation rules
}
```

## Additional generation with YAML config

The sample registers a second task that uses a YAML configuration file:

Note: this sample uses `open-api-test-generation-config.yaml`, but the filename is arbitrary.

```kotlin
tasks.register<OpenApiTestGeneratorTask>("generateOpenApiTestsToSrc") {
    configFile.set(layout.projectDirectory.file("open-api-test-generation-config.yaml"))
}

tasks.named("compileTestJava") { dependsOn("generateOpenApiTestsToSrc") }
```

This generates tests to `src/test/java/` with a different package, demonstrating how to use multiple generation configurations.

See [`open-api-test-generation-config.yaml`](https://github.com/GalushkoArt/openapi-testgen-monorepo/blob/main/samples/java-spring-rest-assured/open-api-test-generation-config.yaml) for the config file.

## Running the sample

```bash
# Generate tests only
./gradlew :samples:java-spring-rest-assured:generateOpenApiTests

# Generate tests from YAML config
./gradlew :samples:java-spring-rest-assured:generateOpenApiTestsToSrc

# Compile and run all tests
./gradlew :samples:java-spring-rest-assured:test
```

## Build integration

The plugin automatically:

1. Registers the output directory as a test source set
2. Wires `generateOpenApiTests` as a dependency of `compileTestJava`
3. Ensures tests are regenerated when the OpenAPI spec changes

```kotlin
// This happens automatically when manualOnly.set(false):
sourceSets.test.java.srcDir(outputDir)
tasks.compileTestJava.dependsOn(generateOpenApiTests)
```

## Dependencies

```kotlin
dependencies {
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.rest-assured:rest-assured:5.5.0")
}
```

## Test execution flow

1. **Build starts** `./gradlew test`
2. **Generation runs** before `compileTestJava`
3. **Tests compile** with generated sources in classpath
4. **Spring Boot starts** with `@SpringBootTest`
5. **Tests execute** against the running application
6. **Results reported** via JUnit Platform

## Related docs

- [Template generator](../how-to/generators.md#template-generator)
- [RestAssured integration](../how-to/generators.md#restassured-integration)
- [Spring Boot integration](../how-to/generators.md#spring-boot-integration)
- [Gradle plugin reference](../reference/gradle-plugin.md)
