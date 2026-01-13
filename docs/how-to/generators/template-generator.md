# Template generator (`template`)

The `template` generator renders tests from Mustache templates.

## When to use it

Use the template generator when you want source-code tests (Java/Kotlin) that can be compiled and executed as part of your test suite.

## Generator id

- `generator = "template"`

## Generator options

Options are provided via `generatorOptions`.

### `templateSet`

Select a built-in template set:

- `restassured-java` (default)
- `restassured-kotlin`

### `templateVariables`

`templateVariables` is a map of values available to templates as `customVariables.*`.

Built-in RestAssured templates use:

- `package`: optional package declaration
- `baseUrl`: assigned to `RestAssured.baseURI`
- `springBootTest`: when truthy, emits Spring Boot test annotations/imports

Example (CLI):

```bash
openapi-testgen \
  --spec-file ./openapi.yaml \
  --output-dir ./build/generated-tests \
  --generator template \
  --generator-option templateSet=restassured-java \
  --generator-option templateVariables.package=com.example.generated \
  --generator-option templateVariables.baseUrl=http://localhost:8080
```

Example (Gradle):

```kotlin
openApiTestGenerator {
    generator.set("template")
    generatorOptions.putAll(
        mapOf(
            "templateSet" to "restassured-java",
            "templateVariables" to mapOf(
                "package" to "com.example.generated",
                "baseUrl" to "http://localhost:8080",
            ),
        )
    )
}
```

### Output control

- `outputFileExtension`: file extension (inferred from `templateSet` if not set)
- `outputFileNamePattern`: defaults to `{{className}}.{{outputFileExtension}}`
- `writeMode`: `OVERWRITE` or `SKIP_IF_EXISTS`
- `fileHeaderComment`: optional header emitted by templates

## Related documentation

- Modules: [`generator-template`](../../modules/generator-template.md)

