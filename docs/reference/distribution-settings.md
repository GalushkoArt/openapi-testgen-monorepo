---
description: Comprehensive settings reference shared by CLI and Gradle plugin. Covers budget controls, security configuration, include/ignore filters, example value providers, and module settings.
---

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

| Setting            | CLI                   | Gradle             | Type    | Required | Description                                                                                                                                                         |
|--------------------|-----------------------|--------------------|---------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Config file        | `--config-file`       | `configFile`       | String  | No       | YAML base config path. Can contain other require settings                                                                                                           |
| Spec file          | `--spec-file`         | `specFile`         | String  | Yes      | OpenAPI specification file path                                                                                                                                     |
| Output dir         | `--output-dir`        | `outputDir`        | String  | Yes      | Output directory for generated artifacts                                                                                                                            |
| Generator id       | `--generator`         | `generator`        | String  | Yes      | Generator: `template` or `test-suite-writer`                                                                                                                        |
| Generator options  | `--generator-option`  | `generatorOptions` | Map     | No       | Generator-specific options (see [Generators](../how-to/generators.md))                                                                                              |
| Always write tests | `--always-write-test` | `alwaysWriteTests` | Boolean | No       | Write artifacts even when generation reports errors; if artifacts are written, execution still returns success and the report retains the errors (default: `false`) |
| Log level          | `--log-level`         | `logLevel`         | String  | No       | `ALL`, `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `OFF`                                                                                                             |

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

| Setting            | Type    | Default | Description                                                 |
|--------------------|---------|---------|-------------------------------------------------------------|
| `includeValidCase` | Boolean | `false` | Include the baseline valid case (2xx status) in test suites |

When enabled, each test suite includes a "Test Valid Case" entry representing a valid request
with all required parameters populated. The expected status code is the first 2xx response
defined in the OpenAPI spec (e.g., 200, 201, 204).

This is a baseline positive check (one valid case per operation), not a generator for multiple valid permutations.

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

### Include configuration

| Setting             | Type                                  | Default | Description                                          |
|---------------------|---------------------------------------|---------|------------------------------------------------------|
| `includeOperations` | Map&lt;String, List&lt;String&gt;&gt; | `{}`    | Path → list of HTTP methods to include (empty = all) |

Accepted shapes:

- `"/path": ["GET", "POST"]` (explicit list)
- `"/path": "GET"` (single method shorthand)

Behavior:

- Empty config → all operations are included (no filtering).
- Path keys match **exactly** (no globbing). Use `*` as a wildcard path to match all paths.
- Method matching is case-insensitive (methods are normalized to uppercase).
- Use `*` as a wildcard method to match all methods for a path.
- Exact path entries take precedence over the wildcard path:
    - When `"/users": ["GET"]` exists, wildcard path methods (like `"*": ["POST"]`) are **not** additive for `"/users"`.
- Paths that are not present in the OpenAPI spec are ignored and logged as warnings.

Example:

```yaml
testGenerationSettings:
    includeOperations:
        "/users/{userId}": [ "GET" ]
        "/orders": [ "POST" ]
        "*": [ "OPTIONS" ]
```

### Ignore configuration

| Setting                       | Type              | Default | Description                                   |
|-------------------------------|-------------------|---------|-----------------------------------------------|
| `ignoreTestCases`             | Map               | `{}`    | Path/method/test case names to skip (exact)   |
| `ignoreSchemaValidationRules` | Set&lt;String&gt; | `[]`    | Fully qualified rule class names to skip      |
| `ignoreAuthValidationRules`   | Set&lt;String&gt; | `[]`    | Fully qualified auth rule class names to skip |

`ignoreTestCases` is hierarchical:

- Path level: `"/internal": "*"` ignores all operations under that path.
- Operation level: `"/users": { "DELETE": "*" }` ignores all test cases for that method.
- Test-case level: `"/users": { "GET": ["Missed Required Query Parameter page"] }` ignores exact test case names only.

Wildcards and matching:

- Path keys match **exactly** (no globbing). Use `*` as a wildcard path to apply rules to all paths.
- Method keys are case-insensitive (methods are normalized to uppercase).
- Wildcard path rules merge with path-specific rules; wildcard method entries win on key collisions.
- The `*:*:*` pattern is forbidden, ignored, and logged as a warning.
- If all test cases for an operation are filtered out, the operation is recorded as "not tested" in the generation report summary.

Performance:

- Path-level (`"/path": "*"`) and operation-level (`"/path": { "GET": "*" }`) ignores are applied **before** generation.
- Test-case-name ignores (`"/path": { "GET": ["..."] }`) are applied **after** suite generation.
- Use `includeOperations` when you want inclusion-based targeting and predictable performance on large specs.

Example:

```yaml
testGenerationSettings:
    ignoreTestCases:
        "/internal": "*"  # Skip all tests for /internal
        "/pets/{petId}":
            "GET":
                - "Missing required path param: petId"
        "*":
            "OPTIONS": "*"
    ignoreSchemaValidationRules:
        - "art.galushko.openapi.testgen.rules.schema.OutOfMinimumLengthStringSchemaValidationRule"
```

### Basic test data overrides

| Setting                 | Type                      | Default | Description                                          |
|-------------------------|---------------------------|---------|------------------------------------------------------|
| `overrideBasicTestData` | Map&lt;String, String&gt; | `{}`    | Override values for basic test data provider methods |

## testGenerationSettings.exampleValues

Configuration for schema-derived example value generation.

| Setting                            | Type               | Default   | Description                                                             |
|------------------------------------|--------------------|-----------|-------------------------------------------------------------------------|
| `providers`                        | List&lt;String&gt; | See below | Ordered provider list; first match wins                                 |
| `maxExampleDepth`                  | Integer            | 50        | Maximum recursion depth for example generation                          |
| `includeOptionalExampleProperties` | Boolean            | false     | Include optional properties that define examples/defaults               |
| `includeWriteOnly`                 | Boolean            | true      | Include `writeOnly` properties in generated examples                    |
| `useSchemaExampleFallback`         | Boolean            | false     | Use `schema.examples`/`schema.default` when `schema.example` is missing |

Note: These flags affect request/parameter example generation. Response `expectedBody` uses response defaults
(`includeOptionalExampleProperties = true`, `includeWriteOnly = false`, `useSchemaExampleFallback = true`);
`maxExampleDepth` still applies.

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

| Setting    | Type   | Default                        | Description                                                      |
|------------|--------|--------------------------------|------------------------------------------------------------------|
| `template` | String | `"d5a5495b-cbdc-4237-a66e-%s"` | RFC 9562 UUID template with `%s` placeholder for variation index |

#### email

| Setting    | Type   | Default                | Description                    |
|------------|--------|------------------------|--------------------------------|
| `template` | String | `"test%s@example.com"` | Template with `%s` placeholder |

#### date

| Setting     | Type   | Default        | Description                                |
|-------------|--------|----------------|--------------------------------------------|
| `startDate` | String | `"2025-05-05"` | Start date in ISO-8601 format (YYYY-MM-DD) |

#### dateTime

| Setting              | Type   | Default          | Description                                    |
|----------------------|--------|------------------|------------------------------------------------|
| `startDate`          | String | `"2025-05-05"`   | Start date in ISO-8601 format                  |
| `timeSuffixTemplate` | String | `"%sT17:32:28Z"` | RFC 3339 date-time template with `%s` for date |

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
        includeOptionalExampleProperties: true
        includeWriteOnly: false
        useSchemaExampleFallback: true
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

## parserSettings

Parser-level settings shared by CLI and Gradle plugin.

### SnakeYAML options

| Setting                        | Type    | Default | Description            |
|--------------------------------|---------|---------|------------------------|
| `yamlCodePointLimit`           | Integer | `null`  | Max YAML code points   |
| `yamlMaxAliasesForCollections` | Integer | `null`  | Max YAML aliases       |
| `yamlAllowRecursiveKeys`       | Boolean | `null`  | Recursive key handling |
| `yamlNestingDepthLimit`        | Integer | `null`  | Max YAML nesting depth |

`null` means "use swagger-parser / SnakeYAML default".

Example:

```yaml
parserSettings:
    yamlCodePointLimit: 10000000
```

## Generator options

Generator options are generator-specific and live under the top-level `generatorOptions` key (YAML) / `generatorOptions` property (Gradle) /
`--generator-option` (CLI).

See:

- How-to: [Generators](../how-to/generators.md)
- How-to: [Template generator](../how-to/generators.md#template-generator)
- How-to: [Test-suite-writer](../how-to/generators.md#test-suite-writer-generator)

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
        "/health": "*"
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
- [Budget controls](../concepts/architecture.md#budget-controls)
- [Ignore rules how-to](../how-to/configuration.md#ignore-rules)
- [Include operations](../how-to/configuration.md#include-operations)
- [Security values how-to](../how-to/configuration.md#security-values)
