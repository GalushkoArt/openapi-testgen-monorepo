---
description: Reference for generator options available in the test-suite-writer and template generators. Covers output modes, file formats, merge behavior, and template customization.
---

# Generator options

Generator options are passed via `generatorOptions` (YAML/Gradle) or `--generator-option` (CLI).

## `test-suite-writer` options

These options are parsed by `transformAndValidateWriterOptions`.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `outputMode` | String | `SINGLE_FILE` | `SINGLE_FILE` aggregates all suites into one file; `MULTIPLE_FILES` writes one file per suite. |
| `outputFileName` | String | (required for `SINGLE_FILE`) | Output file name used in `SINGLE_FILE` mode. |
| `format` | String | `JSON` | Output format: `JSON` or `YAML`. |
| `indent` | String | `4 spaces` | Indentation string for JSON output. |
| `writeMode` | String | `MERGE` | `MERGE` loads existing suites and merges by test case name; `OVERWRITE` starts fresh. |
| `preventOverwriteSuites` | Boolean | `false` | When merging, keep existing suites unchanged (no case updates/additions for them). |
| `preventOverwriteCases` | Boolean | `true` | When merging, keep existing test cases unchanged; missing cases are added. |
| `protectedTestCaseFields` | List&lt;String&gt; / String | `[]` | Fields to preserve when overwriting cases (applies only when `preventOverwriteCases=false`). Accepts a list or comma-separated string. |
| `fileNamePrefix` | String | `""` | Prefix used in `MULTIPLE_FILES` mode (`{fileNamePrefix}{operationName}.{format}`). |

### Write modes and merge semantics

`writeMode` controls whether the writer starts from an empty in-memory aggregator (`OVERWRITE`) or loads existing suites before writing (`MERGE`).

- `OVERWRITE`: does not load existing output. In `SINGLE_FILE` mode, the output file is replaced with only suites generated in the current run. In
  `MULTIPLE_FILES` mode, suite files for operations generated in the current run are replaced, but files for operations not regenerated are not deleted.
- `MERGE`: loads existing output first and merges incoming suites by `operationName` and test cases by `name`. Suites that are not regenerated remain
  unchanged (preserved).

MERGE mode loads existing suites from:

- `SINGLE_FILE`: `{outputDir}/{outputFileName}`
- `MULTIPLE_FILES`: all readable files matching `{fileNamePrefix}*.{format}` in `{outputDir}`

### Merge behavior (when `writeMode = MERGE`)

- `preventOverwriteSuites=true`: existing suites are kept as-is (no suite metadata updates and no case additions/updates).
- `preventOverwriteSuites=false`: suite-level metadata (path/method/operationName) is updated from the incoming suite, then cases are merged.
- `preventOverwriteCases=true` (default): existing test cases are kept as-is; new cases are appended.
- `preventOverwriteCases=false`: incoming test cases overwrite existing test cases matched by `name`.
- `protectedTestCaseFields`: applies only when `preventOverwriteCases=false`. Fields listed here are copied from the existing test case into the incoming
  test case, so manual edits survive regeneration.

### Atomic writes

Writes are atomic: content is written to a temporary file and moved into place with an atomic replace.

Valid values for `protectedTestCaseFields` (invalid names throw an error):

- `name`
- `method`
- `path`
- `queryParams`
- `pathParams`
- `headers`
- `cookie`
- `securityValues`
- `body`
- `expectedBody`
- `needToComplete`
- `expectedStatusCode`
- `rule`

## `template` options

These options are parsed by `transformAndValidateTemplateOptions`.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `templateSet` | String | `restassured-java` | Built-in template set id (for example, `restassured-java`, `restassured-kotlin`). |
| `classTemplatePath` | String | `templates/{{templateSet}}/class.mustache` | Mustache class template path. Supports `{{templateSet}}` placeholder. |
| `customTemplateDir` | String | `null` | Filesystem directory containing templates (optional). |
| `templateVariables` | Map | `{}` | Variables passed to templates as `customVariables.*`. |
| `templateVariables.classSuffix` | String | `Test` | Suffix appended to generated class names. |
| `templateVariables.methodPrefix` | String | `""` | Prefix prepended to generated test method names. |
| `templateVariables.methodSuffix` | String | `""` | Suffix appended to generated test method names. |
| `outputFileExtension` | String | inferred | Inferred from `templateSet` when unset (`java`/`kt`). If the extension cannot be inferred, set it explicitly. |
| `outputFileNamePattern` | String | `{{className}}.{{outputFileExtension}}` | Output naming pattern. Supports `{{className}}` and `{{outputFileExtension}}` placeholders. |
| `writeMode` | String | `OVERWRITE` | `OVERWRITE` or `SKIP_IF_EXISTS`. |
| `fileHeaderComment` | String | `null` | Optional header string available to templates. |

