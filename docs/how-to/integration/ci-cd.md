# Integrating test generation into CI test jobs

This guide is CI-system agnostic. It focuses on how to add OpenAPI Test Generator to your build and test jobs that run on every commit, and how to fail the pipeline before deployment if generation or tests fail.

## Choose your integration method

### Gradle plugin (recommended for Gradle projects)

Use the Gradle plugin when:

- Your project already uses Gradle
- You want generation wired into `test` automatically
- You prefer declarative configuration in `build.gradle.kts`
- You want Gradle to manage task inputs/outputs and caching

### CLI (best for non-Gradle builds or isolated generation)

Use the CLI when:

- Your project uses Maven/Bazel/other build systems
- You want generation in a separate job or container
- You want to generate JSON/YAML suites and run them in another test framework

#### Installing the CLI in CI

=== "npm (recommended)"

    ```bash
    npm install -g @openapi-testgen/cli
    openapi-testgen --version
    ```

=== "pnpm"

    ```bash
    pnpm add -g @openapi-testgen/cli
    openapi-testgen --version
    ```

=== "Download binary"

    ```bash
    # Linux
    curl -LO https://github.com/GalushkoArt/openapi-testgen-monorepo/releases/download/0.9.1/openapi-testgen-0.9.1-linux-amd64.zip
    unzip openapi-testgen-0.9.1-linux-amd64.zip
    cd openapi-testgen-0.9.1-linux-amd64
    chmod +x openapi-testgen
    ```

See [npm Installation](../../getting-started/npm-installation.md) for detailed package manager options.

## Gradle plugin integration

### Add to an existing project

1) Apply the plugin:

```kotlin
plugins {
    id("art.galushko.openapi-test-generator") version "0.9.1"
}
```

2) Configure the generator:

```kotlin
openApiTestGenerator {
    specFile.set("src/main/resources/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests"))
    generator.set("template")
    generatorOptions.putAll(
        mapOf(
            "templateSet" to "restassured-java",
            "templateVariables" to mapOf(
                "package" to "com.example.generated.tests",
                "baseUrl" to "http://localhost:8080"
            )
        )
    )
}
```

3) Add test dependencies for generated tests:

```kotlin
dependencies {
    testImplementation("io.rest-assured:rest-assured:5.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}
```

### How the plugin wires into test jobs

The plugin registers `generateOpenApiTests` and optionally wires it into the test lifecycle:

- `template` generator: output is added to the test source set. `compileTestJava` / `compileTestKotlin` depend on generation when `manualOnly=false` (default).
- `test-suite-writer` generator: output is copied into `processTestResources` when `manualOnly=false`.

For explicit control, set `manualOnly = true` and invoke `generateOpenApiTests` yourself.

```kotlin
openApiTestGenerator {
    manualOnly.set(true)
}
```

### CI test job patterns (Gradle)

Pattern 1: generate and test in one step (default)

```bash
./gradlew test
```

Pattern 2: explicit generation before tests (same job)

```bash
./gradlew generateOpenApiTests test
```

Pattern 3: split generation and tests into separate jobs

```bash
# Job 1: generate
./gradlew generateOpenApiTests
# Archive build/generated/openapi-tests/

# Job 2: restore artifacts and run tests
# Restore archived tests to build/generated/openapi-tests/
./gradlew test
```

Use Pattern 3 if generation is slow and you want to parallelize or reuse artifacts across jobs.

If you want to avoid re-running generation in Job 2, set `manualOnly=true`. For the `template` generator, restored sources will still compile. For `test-suite-writer`, either have your tests read directly from the restored output directory or add it to the test resources:

```kotlin
val generatedSuitesDir = layout.buildDirectory.dir("generated/openapi-tests")

openApiTestGenerator {
    outputDir.set(generatedSuitesDir)
    manualOnly.set(true)
    generator.set("test-suite-writer")
}

sourceSets.named("test") {
    resources.srcDir(generatedSuitesDir)
}
```

### Error handling and budgets

Keep CI strict and predictable using settings in `testGenerationSettings`:

```kotlin
import art.galushko.openapi.testgen.model.error.ErrorMode

openApiTestGenerator {
    testGenerationSettings {
        errorMode.set(ErrorMode.FAIL_FAST) // or COLLECT_ALL
        maxErrors.set(20)
        maxTestCasesPerOperation.set(500)
    }
}
```

If generation fails, Gradle will fail the job with a non-zero exit code.

## CLI integration

### Generate and run tests in the same job

```bash
openapi-testgen \
  --spec-file src/main/resources/openapi.yaml \
  --output-dir build/generated-test-suites \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json

# Run your tests (JUnit, pytest, etc.) that consume the generated suites
./gradlew test
```

### Use a config file (recommended in CI)

`openapi-testgen.yaml`:

```yaml
specFile: src/main/resources/openapi.yaml
outputDir: build/generated-test-suites
generator: test-suite-writer
generatorOptions:
  format: json
  outputFileName: test-suites.json
testGenerationSettings:
  errorMode: FAIL_FAST
  maxErrors: 20
```

Run:

```bash
openapi-testgen --config-file openapi-testgen.yaml
```

### Split generation and test execution (two jobs)

```bash
# Job 1: generation
openapi-testgen --config-file openapi-testgen.yaml
# Archive build/generated-test-suites/

# Job 2: restore artifacts and run tests
./gradlew test
```

## Template vs test-suite-writer in CI

### Template generator (compiled tests)

```
OpenAPI spec -> template generator -> .java/.kt -> compile -> run JUnit
```

Use this when you want executable JUnit tests generated into your build. In Gradle, the plugin automatically adds the output to the test source set.

### Test-suite-writer (data-driven tests)

```
OpenAPI spec -> test-suite-writer -> .json/.yaml -> test runner consumes suites
```

Use this when you already have a data-driven test harness and want to feed it JSON/YAML suites.

## Environment-specific configuration

For base URLs or security values, use environment variables at runtime.

Gradle:

```kotlin
val apiBaseUrl = System.getenv("API_BASE_URL") ?: "http://localhost:8080"
val apiKey = System.getenv("API_TEST_KEY") ?: "test-key"

openApiTestGenerator {
    generatorOptions.put(
        "templateVariables",
        mapOf("baseUrl" to apiBaseUrl)
    )
    testGenerationSettings {
        validSecurityValues.put("ApiKeyAuth", apiKey)
    }
}
```

CLI:

```bash
API_TEST_KEY=my-secret-key openapi-testgen \
  --config-file openapi-testgen.yaml \
  --setting "validSecurityValues.ApiKeyAuth=${API_TEST_KEY}"
```

YAML config files do not expand environment variables by themselves; use CLI flags for overrides when running in CI.

## Caching and artifacts

Cache these directories for faster CI runs:

- `~/.gradle/caches`
- `~/.gradle/wrapper`
- `.gradle/`

Archive these artifacts when you split generation and tests:

| Artifact | Purpose | Suggested retention |
| --- | --- | --- |
| `build/generated/openapi-tests/` | Generated test code or suites | 1-2 weeks |
| `build/reports/tests/` | HTML test reports | 1-2 weeks |
| `build/test-results/test/*.xml` | JUnit XML for CI parsing | 1 week |

## Exit codes

- CLI returns `0` on success and `1` on generation failure or invalid configuration.
- Gradle tasks fail the build on generation errors (non-zero exit code from `./gradlew`).

## Troubleshooting

- Generation is slow: reduce `maxTestCasesPerOperation` or set stricter schema limits.
- Out of memory: increase JVM memory (for example, `GRADLE_OPTS="-Xmx4g"`).
- Invalid tests: validate your OpenAPI spec and check template settings.

## Related documentation

- [Gradle plugin reference](../../reference/gradle-plugin.md)
- [CLI reference](../../reference/cli.md)
- [YAML configuration](../configuration/yaml-config.md)
- [Error handling](../../concepts/error-handling.md)
