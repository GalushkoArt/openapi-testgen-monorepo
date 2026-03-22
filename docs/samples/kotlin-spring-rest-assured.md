---
description: Demonstrates the template generator with Kotlin and custom Mustache templates for full control over generated test code. Includes three generation tasks with different configurations.
---

# Kotlin Spring RestAssured sample

This sample demonstrates the `template` generator with Kotlin, including custom Mustache templates for complete control over generated test code.

[View on GitHub](https://github.com/GalushkoArt/openapi-testgen-monorepo/tree/main/samples/kotlin-spring-rest-assured){ .md-button }

## Overview

| Property | Value |
|----------|-------|
| Module | `samples:kotlin-spring-rest-assured` |
| Generator | `template` |
| Template set | `restassured-kotlin` + custom templates |
| Output location | `build/generated/openapi-tests/` and `src/test/kotlin/` |

## What it demonstrates

- Using the `template` generator with built-in `restassured-kotlin` templates
- **Custom Mustache templates** for full control over generated code
- **Three generation tasks** with different configurations
- RestAssured Kotlin extensions
- Integration with Spring Boot test context

## Plugin configuration

### Default generation (to build directory)

```kotlin
openApiTestGenerator {
    specFile.set(file("${rootDir}/samples/openapi.yaml").toURI().toString())
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests/art/galushko/kotlin/spring/rest/assured"))
    generator.set("template")

    generatorOptions.putAll(
        mapOf(
            "templateSet" to "restassured-kotlin",
            "templateVariables" to mapOf(
                "package" to "art.galushko.kotlin.spring.rest.assured.generatedtests",
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
        errorMode.set(ErrorMode.FAIL_FAST)
    }
}
```

### Generation to source directory (YAML config)

Note: this sample uses `open-api-test-generation-config.yaml`, but the filename is arbitrary.

```kotlin
tasks.register<OpenApiTestGeneratorTask>("generateOpenApiTestsToSrc") {
    configFile.set("open-api-test-generation-config.yaml")
}

tasks.named("compileTestKotlin") { dependsOn("generateOpenApiTestsToSrc") }
```

### Custom templates generation

```kotlin
tasks.register<OpenApiTestGeneratorTask>("generateOpenApiTestsCustomTemplate") {
    specFile.set(file("${rootDir}/samples/openapi.yaml").toURI().toString())
    outputDir.set(layout.projectDirectory.dir("src/test/kotlin/art/galushko/kotlin/spring/rest/assured/custom"))
    generator.set("template")

    generatorOptions.putAll(
        mapOf(
            "templateSet" to "restassured-kotlin",
            "customTemplateDir" to "samples/kotlin-spring-rest-assured/templates",
            "classTemplatePath" to "class.mustache",
            "outputFileExtension" to "kt",
            "templateVariables" to mapOf(
                "package" to "art.galushko.kotlin.spring.rest.assured.custom",
                "baseUrl" to "http://localhost:8080/v1",
                "springBootTest" to "true",
            ),
        )
    )

    testGenerationSettings {
        validSecurityValues.putAll(mapOf("ApiKeyAuth" to "test-api-key-123"))
    }
}

tasks.named("compileTestKotlin") { dependsOn("generateOpenApiTestsCustomTemplate") }
```

## Custom templates

This sample includes custom Mustache templates demonstrating full control over generated code.

The templates are in [`templates/`](https://github.com/GalushkoArt/openapi-testgen-monorepo/tree/main/samples/kotlin-spring-rest-assured/templates):

```
templates/
├── class.mustache     # Main class template
└── method.mustache    # Test method partial
```

For detailed guidance on creating custom templates, including:

- Available template variables
- Mustache syntax examples
- Configuration options

See the [Custom templates how-to guide](../how-to/generators.md#custom-mustache-templates).

## Running the sample

```bash
# Generate tests to build directory (built-in templates)
./gradlew :samples:kotlin-spring-rest-assured:generateOpenApiTests

# Generate tests to src/test/kotlin (YAML config)
./gradlew :samples:kotlin-spring-rest-assured:generateOpenApiTestsToSrc

# Generate tests with custom templates
./gradlew :samples:kotlin-spring-rest-assured:generateOpenApiTestsCustomTemplate

# Compile and run all tests
./gradlew :samples:kotlin-spring-rest-assured:test
```

## Dependencies

```kotlin
dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.rest-assured:rest-assured:5.5.0")
    testImplementation("io.rest-assured:kotlin-extensions:5.5.0")
}
```

The `kotlin-extensions` dependency provides the idiomatic Kotlin DSL (`Given`, `When`, `Then` blocks).

## Template customization options

| Option | Description |
|--------|-------------|
| `templateSet` | Base template set (`restassured-kotlin`) |
| `customTemplateDir` | Directory containing custom templates |
| `classTemplatePath` | Path to main class template (relative to `customTemplateDir`) |
| `outputFileExtension` | File extension for output (`.kt`) |

## When to use custom templates

Custom templates are useful when you need:

- Different test framework (TestNG, Kotest, etc.)
- Custom assertions or matchers
- Additional test setup/teardown
- Different code style or conventions
- Integration with custom test utilities

## Related docs

- [Custom templates](../how-to/generators.md#custom-mustache-templates)
- [Template generator](../how-to/generators.md#template-generator)
- [RestAssured integration](../how-to/generators.md#restassured-integration)
- [Gradle plugin reference](../reference/gradle-plugin.md)
