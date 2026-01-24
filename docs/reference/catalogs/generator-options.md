# Generator options

Generator options are passed via `generatorOptions` (YAML/Gradle) or `--generator-option` (CLI).

## `test-suite-writer` options

These options are parsed by `transformAndValidateWriterOptions`.

- `outputMode`: `SINGLE_FILE` (default) or `MULTIPLE_FILES`
- `outputFileName`: required when `outputMode=SINGLE_FILE`
- `format`: `JSON` (default) or `YAML`
- `indent`: indentation string for JSON output (default: 4 spaces)
- `writeMode`: `MERGE` (default) or `OVERWRITE`
- `preventOverwriteSuites`: boolean (default: false)
- `preventOverwriteCases`: boolean (default: true; existing cases are preserved, new cases are added)
- `protectedTestCaseFields`: list or comma-separated string (default: empty; used when overwriting cases)
- `fileNamePrefix`: prefix used in `MULTIPLE_FILES` mode (default: empty)

## `template` options

These options are parsed by `transformAndValidateTemplateOptions`.

- `templateSet`: template set id (default: `restassured-java`)
- `classTemplatePath`: class template path (default: `templates/{{templateSet}}/class.mustache`)
- `customTemplateDir`: filesystem directory containing templates (optional)
- `templateVariables`: map of custom variables available as `customVariables.*` in templates
- `outputFileExtension`: inferred from `templateSet` when unset (`java`/`kt`)
- `outputFileNamePattern`: default `{{className}}.{{outputFileExtension}}`
- `writeMode`: `OVERWRITE` (default) or `SKIP_IF_EXISTS`
- `fileHeaderComment`: optional header string used by templates

