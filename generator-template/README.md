# Generator-Template Module

Mustache-based code generation for test suites.

This module provides the `template` generator, which renders test suites as Java or Kotlin test
classes using Mustache templates. Built-in template sets support Rest-Assured with JUnit 5.

## Features

- Built-in template sets: `restassured-java`, `restassured-kotlin`
- Custom template support with configurable paths
- Template variables for package names, base URLs, and custom values
- Spring Boot test integration support

## Generator Options

| Option | Required | Description |
|--------|----------|-------------|
| `templateSet` | Yes* | Built-in template set (`restassured-java`, `restassured-kotlin`) |
| `customTemplateDir` | Yes* | Path to custom templates directory |
| `classTemplatePath` | No | Template file name (default: `class.mustache`) |
| `outputFileExtension` | No | Output file extension (default: `java` or `kt`) |
| `templateVariables` | No | Map of variables available in templates |

*Either `templateSet` or `customTemplateDir` is required.

## Usage

### Via Gradle Plugin

```kotlin
openApiTestGenerator {
    generator.set("template")
    generatorOptions.putAll(mapOf(
        "templateSet" to "restassured-java",
        "templateVariables" to mapOf(
            "package" to "com.example.generated",
            "baseUrl" to "http://localhost:8080",
            "springBootTest" to "true",
        ),
    ))
}
```

### Via CLI

```bash
openapi-testgen \
  --generator template \
  --generator-option templateSet=restassured-java \
  --generator-option templateVariables.package=com.example.generated \
  --generator-option templateVariables.baseUrl=http://localhost:8080
```

### Custom Templates

```kotlin
openApiTestGenerator {
    generator.set("template")
    generatorOptions.putAll(mapOf(
        "customTemplateDir" to "src/test/templates",
        "classTemplatePath" to "my-template.mustache",
        "outputFileExtension" to "java",
    ))
}
```

## Documentation

- [Template Generator Guide](https://docs.galushko.art/openapi-test-generator/how-to/generators/#template-generator)
- [Custom Templates Guide](https://docs.galushko.art/openapi-test-generator/how-to/generators/#custom-mustache-templates)
- [Generators Guide](https://docs.galushko.art/openapi-test-generator/how-to/generators/)
- [Module Catalog](https://docs.galushko.art/openapi-test-generator/modules/#generator-template)
- [API Reference (Dokka)](https://docs.galushko.art/openapi-test-generator/api/)

## Development

```bash
./gradlew :generator-template:test
./gradlew :generator-template:check
```
