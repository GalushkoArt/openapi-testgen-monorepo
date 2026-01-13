# Distribution settings (CLI + Gradle plugin)

Both the CLI and Gradle plugin ship the same "distribution" wiring from `distribution-bundle`.
They use `TestGenerationRunner.withDefaults`, which applies shared defaults and then merges
user configuration and overrides.

## Default wiring

### Modules

- `TemplateGeneratorModule` (template generator)
- `PatternSupportModule` (pattern-based values and rules)

### Module settings extraction

- `PatternModuleSettingsExtractor` reads `testGenerationSettings.patternGeneration`
  into `PatternGenerationOptions`.

### Default settings

`DistributionDefaults.settings()` inserts the `pattern` provider before `plain-string` in
the example-value provider order, so pattern-based example generation is enabled by default.

## Configuration sources and precedence

1. YAML config file (`configFile`) is loaded with `GeneratorConfigLoader`.
2. CLI flags or Gradle properties are applied as overrides (`TestGeneratorOverrides`).
3. Overrides win over config values; missing values fall back to defaults.

Module settings extraction happens before core settings parsing, so `patternGeneration`
is available when modules are created.

## Top-level settings

| Setting            | CLI                   | Gradle             | Type    | Required | Description                                               |
|--------------------|-----------------------|--------------------|---------|----------|-----------------------------------------------------------|
| Config file        | `--config-file`       | `configFile`       | String  | No       | YAML base config path. Can contain other require settings |
| Spec file          | `--spec-file`         | `specFile`         | String  | Yes      | OpenAPI specification file path                           |
| Output dir         | `--output-dir`        | `outputDir`        | String  | Yes      | Output directory for generated artifacts                  |
| Generator id       | `--generator`         | `generator`        | String  | Yes      | Generator: `template` or `test-suite-writer`              |
| Generator options  | `--generator-option`  | `generatorOptions` | Map     | No       | Generator-specific options                                |
| Always write tests | `--always-write-test` | `alwaysWriteTests` | Boolean | No       | Write artifacts even on errors (default: `false`)         |
| Log level          | `--log-level`         | `logLevel`         | String  | No       | `ALL`, `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `OFF`   |

## testGenerationSettings

Core settings for test case generation behavior.

### Budget controls

| Setting                    | Type    | Default       | Description                                                     |
|----------------------------|---------|---------------|-----------------------------------------------------------------|
| `maxSchemaDepth`           | Integer | 50            | Maximum depth of schema validation traversal                    |
| `maxMergedSchemaDepth`     | Integer | 50            | Maximum depth when merging composed schemas (allOf/anyOf/oneOf) |
| `maxSchemaCombinations`    | Integer | 100           | Maximum schema combinations per parameter/property              |
| `maxTestCasesPerOperation` | Integer | 1000          | Maximum test cases generated per operation                      |
| `maxErrors`                | Integer | 100           | Maximum errors to collect before stopping                       |
| `errorMode`                | String  | `COLLECT_ALL` | `FAIL_FAST` (stop on first error) or `COLLECT_ALL`              |

### Output options

| Setting            | Type    | Default | Description                                                  |
|--------------------|---------|---------|--------------------------------------------------------------|
| `includeValidCase` | Boolean | `false` | Include the baseline valid case (2xx status) in test suites  |

When enabled, each test suite includes a "Test Valid Case" entry representing a valid request
with all required parameters populated. The expected status code is the first 2xx response
defined in the OpenAPI spec (e.g., 200, 201, 204).

Example:

```yaml
testGenerationSettings:
    includeValidCase: true
```

### Security

| Setting               | Type                      | Default | Description                                 |
|-----------------------|---------------------------|---------|---------------------------------------------|
| `validSecurityValues` | Map&lt;String, String&gt; | `{}`    | Security scheme name to valid value mapping |

Example:

```yaml
testGenerationSettings:
    validSecurityValues:
        ApiKeyAuth: "my-api-key"
        BearerAuth: "Bearer eyJ..."
```

### Ignore configuration

| Setting                       | Type              | Default | Description                                   |
|-------------------------------|-------------------|---------|-----------------------------------------------|
| `ignoreTestCases`             | Map               | `{}`    | Path/method/test case patterns to skip        |
| `ignoreSchemaValidationRules` | Set&lt;String&gt; | `[]`    | Fully qualified rule class names to skip      |
| `ignoreAuthValidationRules`   | Set&lt;String&gt; | `[]`    | Fully qualified auth rule class names to skip |

Example:

```yaml
testGenerationSettings:
    ignoreTestCases:
        "/internal/*":
            "*": [ "*" ]  # Skip all tests for /internal/ paths
        "/pets/{petId}":
            "GET": [ "Invalid*" ]  # Skip "Invalid*" tests for GET
    ignoreSchemaValidationRules:
        - "art.galushko.openapi.testgen.rules.schema.OutOfMinimumLengthStringSchemaValidationRule"
```

### Basic test data overrides

| Setting                 | Type                      | Default | Description                                          |
|-------------------------|---------------------------|---------|------------------------------------------------------|
| `overrideBasicTestData` | Map&lt;String, String&gt; | `{}`    | Override values for basic test data provider methods |

## testGenerationSettings.exampleValues

Configuration for schema-derived example value generation.

| Setting           | Type               | Default   | Description                                    |
|-------------------|--------------------|-----------|------------------------------------------------|
| `providers`       | List&lt;String&gt; | See below | Ordered provider list; first match wins        |
| `maxExampleDepth` | Integer            | 50        | Maximum recursion depth for example generation |

Default provider order:

```yaml
providers:
    - enum
    - const
    - uuid
    - email
    - date
    - date-time
    - pattern      # Added by distribution (before plain-string)
    - plain-string
    - number
    - boolean
```

### Provider-specific settings

#### uuid

| Setting    | Type   | Default     | Description                                        |
|------------|--------|-------------|----------------------------------------------------|
| `template` | String | `"d5a5495b-cbdc-4237-a66e-%s"` | RFC 9562 UUID template with `%s` placeholder for variation index |

#### email

| Setting    | Type   | Default                 | Description                    |
|------------|--------|-------------------------|--------------------------------|
| `template` | String | `"test%s@example.com"` | Template with `%s` placeholder |

#### date

| Setting     | Type   | Default        | Description                                |
|-------------|--------|----------------|--------------------------------------------|
| `startDate` | String | `"2025-05-05"` | Start date in ISO-8601 format (YYYY-MM-DD) |

#### dateTime

| Setting              | Type   | Default          | Description                                      |
|----------------------|--------|------------------|--------------------------------------------------|
| `startDate`          | String | `"2025-05-05"`   | Start date in ISO-8601 format                    |
| `timeSuffixTemplate` | String | `"%sT17:32:28Z"` | RFC 3339 date-time template with `%s` for date   |

#### plainString

| Setting      | Type   | Default           | Description                             |
|--------------|--------|-------------------|-----------------------------------------|
| `validChars` | String | `"abcdefghij..."` | Characters allowed in generated strings |

Example:

```yaml
testGenerationSettings:
    exampleValues:
        providers:
            - enum
            - const
            - pattern
            - plain-string
        maxExampleDepth: 15
        uuid:
            template: "test-uuid-%s"
        email:
            template: "test%s@myapp.com"
        date:
            startDate: "2024-01-01"
```

## testGenerationSettings.patternGeneration

Configuration for regex pattern-based value generation (provided by `pattern-support` module).

| Setting             | Type    | Default             | Description                                          |
|---------------------|---------|---------------------|------------------------------------------------------|
| `defaultMinLength`  | Integer | 3                   | Default minimum length when not specified by schema  |
| `spaceChars`        | String  | `" \t\f\n\r\u00a0"` | Characters matching `\s` in patterns                 |
| `anyPrintableChars` | String  | `null`              | Characters matching `.`; `null` uses library default |

Example:

```yaml
testGenerationSettings:
    patternGeneration:
        defaultMinLength: 5
        spaceChars: " \t"
```

## Generator options

### template generator

| Option                      | Type   | Description                                                     |
|-----------------------------|--------|-----------------------------------------------------------------|
| `templateSet`               | String | Built-in template set: `restassured-java`, `restassured-kotlin` |
| `customTemplateDir`         | String | Path to custom Mustache templates                               |
| `templateVariables`         | Map    | Variables passed to templates                                   |
| `templateVariables.package` | String | Package name for generated classes                              |
| `templateVariables.baseUrl` | String | Base URL for API requests                                       |

Example:

```yaml
generator: template
generatorOptions:
    templateSet: restassured-java
    templateVariables:
        package: com.example.generated
        baseUrl: http://localhost:8080
```

### test-suite-writer generator

| Option                   | Type   | Default       | Description                                        |
|--------------------------|--------|---------------|----------------------------------------------------|
| `outputFileName`         | String | (required)    | Output file name (required for `SINGLE_FILE` mode) |
| `format`                 | String | `json`        | Output format: `json` or `yaml`                    |
| `writeMode`              | String | `MERGE`       | `MERGE` (default) or `OVERWRITE`                   |
| `outputMode`             | String | `SINGLE_FILE` | `SINGLE_FILE` or `MULTIPLE_FILES`                  |
| `preventOverwriteSuites` | Boolean | `true`       | Block suite-level replacement during merge         |
| `preventOverwriteCases`  | Boolean | `true`       | Preserve existing test case fields during merge    |
| `protectedTestCaseFields`| List   | `[]`          | Fields to preserve when merging cases              |
| `indent`                 | String | 4 spaces      | Indentation string for JSON output                 |
| `fileNamePrefix`         | String | (empty)       | Prefix for per-suite files in `MULTIPLE_FILES` mode |

Example:

```yaml
generator: test-suite-writer
generatorOptions:
    format: json
    writeMode: MERGE
    outputFileName: test-suites.json
```

## Gradle-only settings

| Setting      | Type    | Default | Description                                               |
|--------------|---------|---------|-----------------------------------------------------------|
| `manualOnly` | Boolean | `false` | Disable automatic source set wiring and task dependencies |

## Complete YAML example

```yaml
specFile: "src/test/resources/openapi.yaml"
outputDir: "./build/generated/openapi-tests"
generator: "template"
generatorOptions:
    templateSet: restassured-java
    templateVariables:
        package: com.example.generated
        baseUrl: http://localhost:8080

testGenerationSettings:
    # Budget controls
    maxSchemaDepth: 50
    maxMergedSchemaDepth: 50
    maxSchemaCombinations: 100
    maxTestCasesPerOperation: 1000
    maxErrors: 100
    errorMode: COLLECT_ALL

    # Security
    validSecurityValues:
        ApiKeyAuth: "test-api-key"
        BearerAuth: "Bearer test-token"

    # Ignore configuration
    ignoreTestCases:
        "/health":
            "*": [ "*" ]
    ignoreSchemaValidationRules: [ ]
    ignoreAuthValidationRules: [ ]

    # Example value settings
    exampleValues:
        providers:
            - enum
            - const
            - uuid
            - email
            - date
            - date-time
            - pattern
            - plain-string
            - number
            - boolean
        maxExampleDepth: 50

    # Pattern generation (from pattern-support module)
    patternGeneration:
        defaultMinLength: 3

alwaysWriteTests: false
logLevel: INFO
```

## Related docs

- [Distribution-bundle module](../modules/distribution-bundle.md)
- [CLI reference](cli.md)
- [Gradle plugin reference](gradle-plugin.md)
- [Budget controls](../concepts/budget-controls.md)
- [Ignore rules how-to](../how-to/configuration/ignore-rules.md)
- [Security values how-to](../how-to/configuration/security-values.md)
