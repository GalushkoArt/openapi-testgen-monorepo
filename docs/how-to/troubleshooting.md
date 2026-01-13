# Troubleshooting

## No tests generated

- Check your OpenAPI spec contains operations with parameters and/or request bodies.
- Check ignore filters are not excluding everything: `testGenerationSettings.ignoreTestCases`, `ignoreSchemaValidationRules`, `ignoreAuthValidationRules`.

## Budget exceeded

Symptoms often include `BudgetExceededException` or partial generation with many errors.

Try increasing:

- `maxSchemaDepth` / `maxMergedSchemaDepth`
- `maxSchemaCombinations`
- `maxTestCasesPerOperation`

Or skip problematic operations via ignore config.

## Output not written when errors occur

By default, artifacts are written only on success.

- CLI: use `--always-write-test`
- YAML/Gradle: set `alwaysWriteTests: true` / `alwaysWriteTests.set(true)`

