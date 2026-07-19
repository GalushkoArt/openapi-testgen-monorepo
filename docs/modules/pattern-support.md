---
description: The pattern-support module bridges pattern-value into the core engine, contributing the `pattern` schema value provider, the InvalidPattern negative rule, and the patternGeneration settings extractor. Covers wiring, configuration ownership, and manual embedding.
---

# Module: `pattern-support`

`pattern-support` connects the standalone [pattern-value](pattern-value.md) module to the core
generation engine. It is what turns "this string schema has a `pattern`" into both a valid example
value (a string matching the regex) and a negative test case (a string that does not match).

Most users never touch this module: `distribution-bundle` registers it by default, so the CLI and
Gradle plugin have pattern-aware behavior out of the box. Wire it manually only when embedding
`core` directly.

## What it contributes

| Contribution | Type | Effect |
|--------------|------|--------|
| Schema value provider id `pattern` | `PatternValueProvider` from pattern-value | Generates valid example values for string schemas with a `pattern` |
| Negative rule | `InvalidPatternSchemaValidationRule` (rule name "Invalid Pattern") | Generates a non-matching string per patterned schema — see the [rules catalog](../reference/catalogs/rules-catalog.md#pattern-module-rules) |
| Settings extractor | `PatternModuleSettingsExtractor` (settings key `patternGeneration`) | Parses the `testGenerationSettings.patternGeneration` block into `PatternGenerationOptions` |

## Depends on / used by

- **Depends on**: `core` (module and rule SPIs) and `pattern-value` (the actual regex generation)
- **Used by**: `distribution-bundle` — `DistributionDefaults.modules()` registers
  `PatternSupportModule`, `DistributionDefaults.extractors()` registers the settings extractor, and
  the default provider order inserts `pattern` before `plain-string`

## Installation

Only needed for direct embedding — CLI and Gradle plugin users get it transitively:

```kotlin
dependencies {
    implementation("art.galushko.openapi.testgen:pattern-support:<version>")
}
```

## Key types

- `PatternSupportModule` — a `TestGenerationModule` with id `pattern-support`. Takes
  `PatternGenerationOptions` as an optional constructor argument; one `PatternValueGenerator` is
  shared by the value provider and the rule, so both honor the same options.
- `PatternModuleSettingsExtractor` — a Kotlin `object` implementing `ModuleSettingsExtractor`.
  Its `settingsKey` is `patternGeneration`; a non-map value under that key fails fast with a
  `ConfigurationException`.
- `InvalidPatternSchemaValidationRule` — a `SimpleSchemaValidationRule` that applies to string
  schemas with a non-null `pattern`. When the underlying library cannot produce a non-matching
  string (see [pattern-value limitations](pattern-value.md#library-limitations)), the rule logs and
  skips that schema instead of failing.

## Configure pattern generation

Configuration is owned by [Distribution settings — patternGeneration](../reference/distribution-settings.md#testgenerationsettingspatterngeneration);
the options themselves (`defaultMinLength`, `spaceChars`, `anyPrintableChars`) are documented in
[pattern-value](pattern-value.md#patterngenerationoptions). Example:

```yaml
testGenerationSettings:
    patternGeneration:
        defaultMinLength: 10
```

## Wire the module when embedding core

The settings extractor runs during option resolution and stores the parsed options under the
module settings key; the module is then constructed with those options:

```kotlin
import art.galushko.openapi.testgen.config.TestGenerationEngine
import art.galushko.openapi.testgen.config.TestGeneratorExecutionOptionsFactory
import art.galushko.openapi.testgen.config.TestGeneratorOverrides
import art.galushko.openapi.testgen.pattern.support.PatternModuleSettingsExtractor
import art.galushko.openapi.testgen.pattern.support.PatternSupportModule
import art.galushko.openapi.testgen.pattern.value.PatternGenerationOptions
import java.nio.file.Path

val options = TestGeneratorExecutionOptionsFactory.fromConfig(
    config = null,
    overrides = TestGeneratorOverrides(
        specFile = "openapi.yaml",
        outputDir = Path.of("build/generated"),
        generatorId = "test-suite-writer",
        testGenerationSettings = mapOf(
            "patternGeneration" to mapOf("defaultMinLength" to 10),
        ),
    ),
    moduleExtractors = listOf(PatternModuleSettingsExtractor),
)

val patternOptions = options.moduleSettings
    .get<PatternGenerationOptions>(PatternModuleSettingsExtractor.SETTINGS_KEY)
    ?: PatternGenerationOptions()

val report = TestGenerationEngine.generateReport(
    options,
    modules = listOf(PatternSupportModule(patternOptions)),
)
```

This mirrors exactly what `TestGenerationRunner.withDefaults()` does in `distribution-bundle`.

## Testing

```bash
./gradlew :pattern-support:test
./gradlew :pattern-support:check
```

## API reference

- Dokka API reference: [`docs/api/pattern-support/index.html`](../api/pattern-support/index.html)

## Related docs

- Modules: [pattern-value](pattern-value.md) — the underlying generator and its options
- Modules: [Distribution-bundle](distribution-bundle.md) — default wiring
- Reference: [Distribution settings — patternGeneration](../reference/distribution-settings.md#testgenerationsettingspatterngeneration)
- Reference: [Rules catalog — pattern module rules](../reference/catalogs/rules-catalog.md#pattern-module-rules)
- Reference: [SPI — SchemaValueProvider](../reference/spi.md#schemavalueprovider)
