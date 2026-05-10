---
description: Gradle plugin reference for OpenAPI Test Generator. Documents extension properties, testGenerationSettings DSL, task registration, and automatic source set wiring.
---

# Gradle plugin reference

Plugin id: `art.galushko.openapi-test-generator`

Implementation class: `art.galushko.openapi.testgen.plugin.OpenApiTestGeneratorPlugin`

## Adding to an existing project

### Step 1: Apply the plugin

```kotlin
plugins {
    id("art.galushko.openapi-test-generator") version "<version>"
}
```
See [Version placeholders](../getting-started/installation.md#version-placeholders) for how to choose `<version>`.

### Step 2: Configure the generator

Minimum configuration:

```kotlin
openApiTestGenerator {
    specFile.set("src/main/resources/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests"))
    generator.set("template") // or "test-suite-writer"
}
```

### Step 3: Add test dependencies

For the template generator with RestAssured:

```kotlin
dependencies {
    testImplementation("io.rest-assured:rest-assured:5.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}
```

### Step 4: Run in CI

```bash
./gradlew test
```

Or explicitly:

```bash
./gradlew generateOpenApiTests test
```

See [CI/CD integration](../how-to/ci-cd.md) for job wiring patterns.

## Extension: `openApiTestGenerator` {#extension-properties}

Apply and configure:

```kotlin
plugins {
    id("art.galushko.openapi-test-generator") version "<version>"
}

openApiTestGenerator {
    specFile.set("src/test/resources/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests"))
    generator.set("template")
}
```

Example: configure example value options:

```kotlin
openApiTestGenerator {
    testGenerationSettings {
        exampleValues.putAll(
            mapOf(
                "includeOptionalExampleProperties" to true,
                "includeWriteOnly" to false,
                "useSchemaExampleFallback" to true,
                "fullExample" to true,
            )
        )
    }
    parserSettings {
        yamlCodePointLimit.set(10_000_000)
    }
}
```

### Fields

- `configFile: Property<String>` (optional): path to YAML config.
- `specFile: Property<String>`: path to the OpenAPI spec (relative to project dir or absolute).
- `outputDir: DirectoryProperty`: output directory for generated artifacts.
- `generator: Property<String>`: generator id (`template` or `test-suite-writer`).
- `generatorOptions: MapProperty<String, Any?>`: generator-specific options.
- `testGenerationSettings: TestGenerationSettingsExtension`: typed settings DSL (nested block).
- `parserSettings: ParserSettingsExtension`: parser settings DSL (SnakeYAML).
- `manualOnly: Property<Boolean>` (default `false`): disable automatic wiring when `true`.
- `alwaysWriteTests: Property<Boolean>` (optional): force writing artifacts even when generation fails.
- `logLevel: Property<String>` (optional): log level for generator logs (SLF4J backend).

### Nested extension: `parserSettings { ... }`

- `yamlCodePointLimit: Property<Int>`
- `yamlMaxAliasesForCollections: Property<Int>`
- `yamlAllowRecursiveKeys: Property<Boolean>`
- `yamlNestingDepthLimit: Property<Int>`

## Nested extension: `testGenerationSettings { ... }` {#test-generation-settings}

```kotlin
openApiTestGenerator {
    testGenerationSettings {
        maxSchemaDepth.set(50)
        maxSchemaCombinations.set(100)
        maxTestCasesPerOperation.set(1000)
        maxErrors.set(100)
        errorMode.set(art.galushko.openapi.testgen.model.error.ErrorMode.COLLECT_ALL)
        includeOperations.put("/users/{userId}", listOf("GET"))
        validSecurityValues.put("ApiKeyAuth", "test-key")
    }
}
```

### Fields

- `includeOperations: MapProperty<String, Any>`
- `ignoreTestCases: MapProperty<String, Any>`
- `ignoreSchemaValidationRules: SetProperty<String>`
- `ignoreAuthValidationRules: SetProperty<String>`
- `maxSchemaDepth: Property<Int>`
- `overrideBasicTestData: MapProperty<String, String>`
- `maxSchemaCombinations: Property<Int>`
- `maxMergedSchemaDepth: Property<Int>`
- `maxTestCasesPerOperation: Property<Int>`
- `validSecurityValues: MapProperty<String, String>`
- `errorMode: Property<ErrorMode>`
- `includeValidCase: Property<Boolean>`
- `maxErrors: Property<Int>`
- `exampleValues: MapProperty<String, Any>` (raw map passed to core)
- `patternGeneration: MapProperty<String, Any>` (raw map parsed by pattern support)

`includeOperations` expects a map of path strings to a list of HTTP methods (e.g., `listOf("GET")`).

## Task: `generateOpenApiTests`

The plugin registers a single task:

- Name: `generateOpenApiTests`
- Group: `verification`

## Wiring behavior

Wiring depends on `generator` and `manualOnly`:

- `template`:
  - Adds `outputDir` to the test source set
  - Wires compilation (`compileTestJava` / `compileTestKotlin`) to depend on generation when `manualOnly=false`
- `test-suite-writer`:
  - Wires generated files into `processTestResources` when `manualOnly=false`

## Operation scope

- Generation currently targets operations under `paths`.
- OpenAPI `webhooks` are parsed but are not yet generated into test suites.
- When a spec contains only `webhooks` and no `paths`, `generateOpenApiTests` completes successfully with zero generated suites.

## See also

- [Include operations](../how-to/configuration.md#include-operations) - Target specific operations (recommended)
- [Ignore rules](../how-to/configuration.md#ignore-rules) - Filter by exclusion
- [CI/CD integration](../how-to/ci-cd.md) - CI job wiring patterns
- [Test-suite-writer](../how-to/generators.md#test-suite-writer-generator) - JSON/YAML output format

## Distribution defaults

See [Distribution settings](distribution-settings.md) for the shared defaults and precedence
used by the CLI and Gradle plugin.
