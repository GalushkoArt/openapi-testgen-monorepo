# Gradle plugin reference

Plugin id: `art.galushko.openapi-test-generator`

Implementation class: `art.galushko.openapi.testgen.plugin.OpenApiTestGeneratorPlugin`

## Extension: `openApiTestGenerator`

Apply and configure:

```kotlin
plugins {
    id("art.galushko.openapi-test-generator") version "0.8.0"
}

openApiTestGenerator {
    specFile.set("src/test/resources/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests"))
    generator.set("template")
}
```

### Fields

- `configFile: Property<String>` (optional): path to YAML config.
- `specFile: Property<String>`: path to the OpenAPI spec (relative to project dir or absolute).
- `outputDir: DirectoryProperty`: output directory for generated artifacts.
- `generator: Property<String>`: generator id (`template` or `test-suite-writer`).
- `generatorOptions: MapProperty<String, Any?>`: generator-specific options.
- `testGenerationSettings: TestGenerationSettingsExtension`: typed settings DSL (nested block).
- `manualOnly: Property<Boolean>` (default `false`): disable automatic wiring when `true`.
- `alwaysWriteTests: Property<Boolean>` (optional): force writing artifacts even when generation fails.
- `logLevel: Property<String>` (optional): log level for generator logs (SLF4J backend).

## Nested extension: `testGenerationSettings { ... }`

```kotlin
openApiTestGenerator {
    testGenerationSettings {
        maxSchemaDepth.set(50)
        maxSchemaCombinations.set(100)
        maxTestCasesPerOperation.set(1000)
        maxErrors.set(100)
        errorMode.set(art.galushko.openapi.testgen.model.error.ErrorMode.COLLECT_ALL)
        validSecurityValues.put("ApiKeyAuth", "test-key")
    }
}
```

### Fields

- `validSecurityValues: MapProperty<String, String>`
- `errorMode: Property<ErrorMode>`
- `includeValidCase: Property<Boolean>`
- `maxErrors: Property<Int>`
- `maxSchemaDepth: Property<Int>`
- `maxSchemaCombinations: Property<Int>`
- `maxMergedSchemaDepth: Property<Int>`
- `maxTestCasesPerOperation: Property<Int>`
- `ignoreTestCases: MapProperty<String, Any>`
- `ignoreSchemaValidationRules: SetProperty<String>`
- `ignoreAuthValidationRules: SetProperty<String>`
- `overrideBasicTestData: MapProperty<String, String>`
- `exampleValues: MapProperty<String, Any>` (raw map passed to core)
- `patternGeneration: MapProperty<String, Any>` (raw map parsed by pattern support)

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

## Distribution defaults

See [Distribution settings](distribution-settings.md) for the shared defaults and precedence
used by the CLI and Gradle plugin.
