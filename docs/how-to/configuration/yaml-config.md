# Configure generation via YAML

You can provide a YAML config file that mirrors execution options (used by both CLI and Gradle plugin).

## Example config

```yaml
specFile: "openapi.yaml"
outputDir: "./build/generated"
generator: "test-suite-writer"
generatorOptions:
  outputFileName: "openapi-test-suites.json"
  writeMode: "MERGE"
testGenerationSettings:
  maxSchemaDepth: 50
  maxMergedSchemaDepth: 50
  maxSchemaCombinations: 100
  maxTestCasesPerOperation: 1000
  maxErrors: 100
  errorMode: "COLLECT_ALL"
  validSecurityValues:
    ApiKeyAuth: "test-key"
alwaysWriteTests: false
logLevel: "INFO"
```

## Using the config

### CLI

```bash
openapi-testgen \
  --config-file ./openapi-testgen.yaml \
  --spec-file ./src/test/resources/openapi.yaml
```

### Gradle plugin

```kotlin
openApiTestGenerator {
    configFile.set("openapi-testgen.yaml")
}
```

## Overrides and merge behavior

- Overrides win over config values.
- Nested maps are deep-merged; lists/arrays are replaced.

See [Distribution settings](../../reference/distribution-settings.md) for defaults and precedence.

## Settings reference

### Top-level options

| Setting | Type | Required | Description |
|---------|------|----------|-------------|
| `specFile` | String | Yes | Path to OpenAPI specification file |
| `outputDir` | String | Yes | Directory for generated artifacts |
| `generator` | String | Yes | Generator id: `template` or `test-suite-writer` |
| `generatorOptions` | Map | No | Generator-specific options (see below) |
| `testGenerationSettings` | Object | No | Core generation settings |
| `alwaysWriteTests` | Boolean | No | Write artifacts even on errors (default: `false`) |
| `logLevel` | String | No | Log verbosity: `ALL`, `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `OFF` |

### testGenerationSettings

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `maxSchemaDepth` | Integer | 50 | Schema traversal depth limit |
| `maxMergedSchemaDepth` | Integer | 50 | Composed schema merge depth limit |
| `maxSchemaCombinations` | Integer | 100 | Max allOf/anyOf/oneOf combinations |
| `maxTestCasesPerOperation` | Integer | 1000 | Max test cases per operation |
| `maxErrors` | Integer | 100 | Max errors before stopping |
| `errorMode` | String | `COLLECT_ALL` | `FAIL_FAST` or `COLLECT_ALL` |
| `includeValidCase` | Boolean | `false` | Include baseline valid case (2xx status) in test suites |
| `validSecurityValues` | Map | `{}` | Security scheme name to value mapping |
| `overrideBasicTestData` | Map | `{}` | Override values for basic test data provider |
| `ignoreTestCases` | Map | `{}` | Path/method/name patterns to skip |
| `ignoreSchemaValidationRules` | List | `[]` | Rule class names to skip |
| `ignoreAuthValidationRules` | List | `[]` | Auth rule class names to skip |
| `exampleValues` | Object | (defaults) | Example value generation settings |
| `patternGeneration` | Object | (defaults) | Regex pattern generation settings |

### testGenerationSettings.exampleValues

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `providers` | List | See defaults | Ordered provider list (first match wins) |
| `maxExampleDepth` | Integer | 50 | Maximum recursion depth for example generation |

Provider-specific settings (e.g., `uuid.template`, `email.template`, `date.startDate`) are documented in [Distribution settings](../../reference/distribution-settings.md#testgenerationsettingsexamplevalues).

### testGenerationSettings.patternGeneration

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `defaultMinLength` | Integer | 3 | Default minimum length for generated strings |
| `spaceChars` | String | `" \t\f\n\r\u00a0"` | Characters matching `\s` in patterns |

### generatorOptions (template generator)

| Option | Description |
|--------|-------------|
| `templateSet` | Built-in template set: `restassured-java`, `restassured-kotlin` |
| `customTemplateDir` | Path to custom Mustache templates |
| `templateVariables.package` | Package name for generated classes |
| `templateVariables.baseUrl` | Base URL for API requests |

### generatorOptions (test-suite-writer)

| Option | Description |
|--------|-------------|
| `outputFileName` | Output file name (required for default `SINGLE_FILE` mode) |
| `format` | Output format: `json` or `yaml` (default: `json`) |
| `writeMode` | `MERGE` (default) or `OVERWRITE` |

## Related docs

- [Budget controls](../../concepts/budget-controls.md) - Tuning budget limits
- [Ignore rules](ignore-rules.md) - Filtering test cases and rules
- [Security values](security-values.md) - Configuring authentication
