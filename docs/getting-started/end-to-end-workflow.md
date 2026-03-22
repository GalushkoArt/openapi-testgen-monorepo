---
description: Complete tutorial from writing a small OpenAPI spec to generating JSON test suites, generating executable RestAssured tests, and running them locally and in CI.
---

# End-to-end workflow (OpenAPI spec → tests → CI)

This tutorial walks through the full workflow: create a small OpenAPI spec, generate test suites you can inspect with `jq`, generate runnable RestAssured tests,
and wire everything into a CI test job.

## What you'll build

- `src/test/resources/openapi.yaml`: a complete 3-operation OpenAPI spec (users list, user get, orders create)
- `generated/test-suites.json`: JSON test suites you can inspect and filter
- `build/generated/openapi-tests/`: generated JUnit tests (RestAssured Java) produced by the Gradle plugin
- A CI job recipe that runs generation + tests on every commit

## Prerequisites

- Java 21 (for Gradle builds)
- npm with `npx` (for CLI runs without installing)
- `jq` (to verify generated suites)
- Gradle 8+ (optional if you already have a Gradle wrapper)

## 1) Create an OpenAPI spec

Create `src/test/resources/openapi.yaml`:

```yaml
openapi: 3.0.3
info:
    title: Demo API
    version: 1.0.0
servers:
    -   url: http://localhost:8080

security:
    -   ApiKeyAuth: [ ]

paths:
    /users:
        get:
            operationId: listUsers
            summary: List users
            parameters:
                -   name: page
                    in: query
                    required: true
                    schema:
                        type: integer
                        minimum: 1
                -   name: limit
                    in: query
                    required: false
                    schema:
                        type: integer
                        minimum: 1
                        maximum: 100
            responses:
                "200":
                    description: OK
                    content:
                        application/json:
                            schema:
                                type: array
                                items:
                                    $ref: '#/components/schemas/User'
                "400":
                    $ref: '#/components/responses/BadRequest'
                "401":
                    $ref: '#/components/responses/Unauthorized'

    /users/{userId}:
        get:
            operationId: getUser
            summary: Get user by id
            parameters:
                -   name: userId
                    in: path
                    required: true
                    schema:
                        type: string
                        format: uuid
            responses:
                "200":
                    description: OK
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/User'
                "400":
                    $ref: '#/components/responses/BadRequest'
                "401":
                    $ref: '#/components/responses/Unauthorized'
                "404":
                    description: Not found
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/Error'
                            example:
                                code: not_found
                                message: User not found

    /orders:
        post:
            operationId: createOrder
            summary: Create order
            parameters:
                -   name: X-Request-ID
                    in: header
                    required: true
                    schema:
                        type: string
                        format: uuid
            requestBody:
                required: true
                content:
                    application/json:
                        schema:
                            $ref: '#/components/schemas/NewOrder'
            responses:
                "201":
                    description: Created
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/Order'
                "400":
                    $ref: '#/components/responses/BadRequest'
                "401":
                    $ref: '#/components/responses/Unauthorized'

components:
    securitySchemes:
        ApiKeyAuth:
            type: apiKey
            in: header
            name: X-API-Key

    responses:
        BadRequest:
            description: Bad request
            content:
                application/json:
                    schema:
                        $ref: '#/components/schemas/Error'
                    example:
                        code: bad_request
                        message: Invalid input
        Unauthorized:
            description: Unauthorized
            content:
                application/json:
                    schema:
                        $ref: '#/components/schemas/Error'
                    example:
                        code: unauthorized
                        message: API key required

    schemas:
        User:
            type: object
            required: [ id, name ]
            additionalProperties: false
            properties:
                id: { type: string, format: uuid }
                name: { type: string, minLength: 1, maxLength: 100 }

        OrderItem:
            type: object
            required: [ sku, quantity, price ]
            additionalProperties: false
            properties:
                sku: { type: string, minLength: 1, maxLength: 64 }
                quantity: { type: integer, minimum: 1 }
                price: { type: number, minimum: 0 }

        NewOrder:
            type: object
            required: [ userId, items ]
            additionalProperties: false
            properties:
                userId: { type: string, format: uuid }
                items:
                    type: array
                    minItems: 1
                    items: { $ref: '#/components/schemas/OrderItem' }

        Order:
            type: object
            required: [ id, userId, items ]
            additionalProperties: false
            properties:
                id: { type: string, format: uuid }
                userId: { type: string, format: uuid }
                items:
                    type: array
                    items: { $ref: '#/components/schemas/OrderItem' }

        Error:
            type: object
            required: [ code, message ]
            additionalProperties: false
            properties:
                code: { type: string }
                message: { type: string }
```

## 2) Generate JSON test suites (CLI)

Generate JSON suites using the `test-suite-writer` generator:

```bash
mkdir -p generated

npx @openapi-testgen/cli \
  --spec-file ./src/test/resources/openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json \
  --setting 'validSecurityValues.ApiKeyAuth=test-api-key-123'
```

!!! note "Security values use security scheme names"
`validSecurityValues` keys must match names under `components.securitySchemes` (for example, `ApiKeyAuth`), not header names (for example, `X-API-Key`).

## 3) Inspect generated suites (jq)

Verify the output file exists:

```bash
test -f generated/test-suites.json && echo "OK: generated/test-suites.json"
```

Inspect operation keys and total test count:

```bash
# Operation keys (SINGLE_FILE output)
jq -r 'keys[]' generated/test-suites.json

# Total test cases across all operations
jq '[.[] | .testCases[]] | length' generated/test-suites.json
```

Verify that each major category is present (non-zero counts):

```bash
# Path parameters
jq '[.[] | .testCases[] | select(.name | startswith("Invalid Path"))] | length' generated/test-suites.json

# Query parameters (missing + invalid)
jq '[.[] | .testCases[] | select(.name | contains("Missed Required Query") or (.name | startswith("Invalid Query")))] | length' generated/test-suites.json

# Header parameters (missing + invalid, non-security)
jq '[.[] | .testCases[] | select(.name | contains("Missed Required Header") or (.name | startswith("Invalid Header")))] | length' generated/test-suites.json

# Request body (missing + invalid)
jq '[.[] | .testCases[] | select(.name == "Required Request Body is missing" or (.name | startswith("Incorrect Request Body")))] | length' generated/test-suites.json

# Auth/security
jq '[.[] | .testCases[] | select(.rule | contains(".rules.auth."))] | length' generated/test-suites.json
```

!!! note "Test counts can vary"
The exact number of generated tests depends on your OpenAPI constraints and enabled modules (for example, `pattern-support` adds pattern-based cases). Prefer
verifying *presence* of categories rather than hard-coding counts.

## 4) Generate executable tests (template generator)

Generate runnable JUnit tests using the built-in RestAssured Java templates:

```bash
mkdir -p generated-tests

npx @openapi-testgen/cli \
  --spec-file ./src/test/resources/openapi.yaml \
  --output-dir ./generated-tests \
  --generator template \
  --generator-option templateSet=restassured-java \
  --generator-option templateVariables.package=com.example.generated.tests \
  --generator-option templateVariables.baseUrl=http://localhost:8080 \
  --setting 'validSecurityValues.ApiKeyAuth=test-api-key-123'
```

You should see one `*Test.java` file per `operationId` under `generated-tests/`.

## 5) Set up a Gradle build (recommended)

Use the Gradle plugin to generate executable tests as part of `./gradlew test`.

Create or update `build.gradle.kts`:

See [Version placeholders](installation.md#version-placeholders) for how to choose `<version>`.

```kotlin
plugins {
    id("java")
    id("art.galushko.openapi-test-generator") version "<version>"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    testImplementation("io.rest-assured:rest-assured:5.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
}

tasks.test {
    useJUnitPlatform()
}

openApiTestGenerator {
    specFile.set("src/test/resources/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests"))
    generator.set("template")
    generatorOptions.putAll(
        mapOf(
            "templateSet" to "restassured-java",
            "templateVariables" to mapOf(
                "package" to "com.example.generated.tests",
                "baseUrl" to (System.getenv("API_BASE_URL") ?: "http://localhost:8080"),
            ),
        )
    )
    testGenerationSettings {
        validSecurityValues.put("ApiKeyAuth", System.getenv("API_TEST_KEY") ?: "test-api-key-123")
    }
}
```

## 6) Run tests locally

Generate tests and run them:

```bash
./gradlew generateOpenApiTests
./gradlew test
```

!!! warning "Generated tests call a real API"
The generated RestAssured tests make HTTP requests to `baseUrl`. Run your API locally (or point `API_BASE_URL` at a test environment) before running
`./gradlew test`.

## 7) Run in CI

In CI, use the same commands you run locally:

```bash
./gradlew test
```

Set CI secrets/environment variables for:

- `API_BASE_URL` (where the API under test is reachable)
- `API_TEST_KEY` (a valid API key for `ApiKeyAuth`)

For CI patterns (split generation vs tests, caching, and artifacts), see [CI/CD integration](../how-to/ci-cd.md).

## 8) Target specific operations (optional)

To generate tests for a specific path/method only:

```bash
npx @openapi-testgen/cli \
  --spec-file ./src/test/resources/openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json \
  --setting 'validSecurityValues.ApiKeyAuth=test-api-key-123' \
  --setting 'includeOperations./users/{userId}[]=GET'
```

See [Include operations](../how-to/configuration.md#include-operations) for more filtering options (including Gradle DSL and wildcards).

## Summary

| Artifact        | Example path                      | Purpose                             |
|-----------------|-----------------------------------|-------------------------------------|
| OpenAPI spec    | `src/test/resources/openapi.yaml` | Input for generation                |
| JSON suites     | `generated/test-suites.json`      | Inspectable test case definitions   |
| Generated tests | `build/generated/openapi-tests/`  | Runnable JUnit tests (RestAssured)  |
| Gradle wiring   | `build.gradle.kts`                | Reproducible generation + execution |

## Next steps

- Customize generated code: [Custom templates](../how-to/generators.md#custom-mustache-templates)
- Add your own rules: [Custom rules](../how-to/extending.md#custom-validation-rules)
- Filter out unwanted cases: [Ignore rules](../how-to/configuration.md#ignore-rules)
