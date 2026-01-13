# Kotlin Spring Rest-Assured Sample

Demonstrates using the Gradle plugin with the `template` generator to produce Rest-Assured Kotlin
tests, including custom template usage.

**What you'll learn:**
- Using the `template` generator with `restassured-kotlin`
- Creating and using custom Mustache templates
- Task wiring with `compileTestKotlin`

## Quick Start

```bash
# Generate Rest-Assured tests
./gradlew :samples:kotlin-spring-rest-assured:generateOpenApiTests

# Run tests (includes generation via wiring)
./gradlew :samples:kotlin-spring-rest-assured:test
```

## Documentation

- [Getting Started](https://docs.galushko.art/openapi-test-generator/getting-started/)
- [Template Generator Guide](https://docs.galushko.art/openapi-test-generator/how-to/generators/template-generator/)
- [Custom Templates Guide](https://docs.galushko.art/openapi-test-generator/how-to/generators/custom-templates/)
- [Gradle Plugin Reference](https://docs.galushko.art/openapi-test-generator/reference/gradle-plugin/)
- [Sample Walkthrough](https://docs.galushko.art/openapi-test-generator/samples/kotlin-spring-rest-assured/)

## Requirements

- JDK 21
- Gradle (use wrapper from repo root)

## Project Structure

```
samples/kotlin-spring-rest-assured/
├── build.gradle.kts                     # Plugin configuration
├── open-api-test-generation-config.yaml # YAML config example
├── templates/                           # Custom templates
│   └── class.mustache
├── src/test/kotlin/                     # Generated from YAML config + custom
└── build/generated/openapi-tests/       # Generated from default task
```

## Plugin Configuration

```kotlin
plugins {
    id("art.galushko.openapi-test-generator")
}

openApiTestGenerator {
    specFile.set(file("${rootDir}/samples/openapi.yaml").toURI().toString())
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests/art/galushko/kotlin/spring/rest/assured"))
    generator.set("template")
    generatorOptions.putAll(mapOf(
        "templateSet" to "restassured-kotlin",
        "templateVariables" to mapOf(
            "package" to "art.galushko.kotlin.spring.rest.assured.generatedtests",
            "baseUrl" to "http://localhost:8080/v1",
            "springBootTest" to "true",
        ),
    ))
    testGenerationSettings {
        validSecurityValues.putAll(mapOf("ApiKeyAuth" to "test-api-key-123"))
        errorMode.set(ErrorMode.FAIL_FAST)
    }
}
```

## Custom Template Task

This sample includes a custom template task demonstrating how to replace built-in templates:

```kotlin
tasks.register<OpenApiTestGeneratorTask>("generateOpenApiTestsCustomTemplate") {
    specFile.set(file("${rootDir}/samples/openapi.yaml").toURI().toString())
    outputDir.set(layout.projectDirectory.dir("src/test/kotlin/art/galushko/kotlin/spring/rest/assured/custom"))
    generator.set("template")
    generatorOptions.putAll(mapOf(
        "customTemplateDir" to "${projectDir}/templates",
        "classTemplatePath" to "class.mustache",
        "outputFileExtension" to "kt",
        "templateVariables" to mapOf(
            "package" to "art.galushko.kotlin.spring.rest.assured.custom",
        ),
    ))
}
```

## Key Features Demonstrated

### Template Variables

| Variable | Description |
|----------|-------------|
| `package` | Kotlin package for generated test classes |
| `baseUrl` | Base URL for API requests |
| `springBootTest` | Enable Spring Boot test annotations |

### Custom Templates

Place your Mustache templates in a directory and configure:
- `customTemplateDir`: Path to templates directory
- `classTemplatePath`: Template file name (default: `class.mustache`)
- `outputFileExtension`: Output file extension (`kt` for Kotlin)

### Task Wiring

The plugin automatically:
- Adds output directory to test source sets
- Wires `compileTestKotlin` to depend on generation tasks

Disable with `manualOnly.set(true)` for manual control.

## Available Tasks

| Task | Description |
|------|-------------|
| `generateOpenApiTests` | Generate tests to build directory |
| `generateOpenApiTestsToSrc` | Generate tests to src/test/kotlin from config |
| `generateOpenApiTestsCustomTemplate` | Generate tests using custom templates |
| `openApiGenerate` | Generate Spring server stubs (OpenAPI Generator) |

## Generated Output

Rest-Assured test classes in Kotlin with JUnit 5 annotations, ready to compile and run.
