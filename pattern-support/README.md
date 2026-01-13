# Pattern-Support Module

Core integration for regex pattern-based value generation.

This module bridges `pattern-value` with `core`, providing automatic pattern-based example value
generation and an `InvalidPattern` validation rule for test case generation.

## Features

- Registers `PatternValueProvider` for schema example generation
- Adds `InvalidPatternSchemaValidationRule` for pattern violation tests
- Configurable via `testGenerationSettings.patternGeneration`
- Implements `TestGenerationModule` for automatic wiring

## Included in Distribution

The `distribution-bundle` module includes `pattern-support` by default. No additional
configuration is needed when using the CLI or Gradle plugin.

## Manual Integration

For custom wiring without `distribution-bundle`:

```kotlin
dependencies {
    implementation("art.galushko.openapi:testgen-pattern-support:0.8.0")
}
```

```kotlin
val engine = TestGenerationEngine(
    modules = listOf(PatternSupportModule()),
    settingsExtractors = listOf(PatternModuleSettingsExtractor()),
    testGenerationSettings = settings
)
```

## Configuration

Configure pattern generation via `testGenerationSettings`:

```yaml
testGenerationSettings:
  patternGeneration:
    defaultMinLength: 10
    defaultMaxLength: 50
    spaceChars: " "
    anyPrintableChars: "abcABC123"
```

Or in Gradle:

```kotlin
testGenerationSettings {
    patternGeneration.putAll(mapOf(
        "defaultMinLength" to 10,
        "defaultMaxLength" to 50,
    ))
}
```

## Documentation

- [Module Overview](https://docs.galushko.art/openapi-test-generator/modules/pattern-support/)
- [Pattern Value Module](https://docs.galushko.art/openapi-test-generator/modules/pattern-value/)
- [YAML Configuration](https://docs.galushko.art/openapi-test-generator/how-to/configuration/yaml-config/)
- [API Reference (Dokka)](https://docs.galushko.art/openapi-test-generator/api/)

## Development

```bash
./gradlew :pattern-support:test
./gradlew :pattern-support:check
```
