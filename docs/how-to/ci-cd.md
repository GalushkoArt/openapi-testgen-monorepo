---
description: GitHub Actions and GitLab CI workflow examples, job patterns for splitting generation and tests, caching, artifact retention, and environment-specific overrides.
---

# Integrating test generation into CI test jobs

This guide is CI-system agnostic. It focuses on how to add OpenAPI Test Generator to your build and test jobs that run on every commit, and how to fail the pipeline before deployment if generation or tests fail.

## TL;DR

### Gradle projects (recommended)

If you're using the Gradle plugin with the `template` generator, generation is wired into `test` by default — run `./gradlew test` in your CI job. See [Gradle integration](../getting-started/gradle-integration.md) for plugin setup.

### CLI (generate suites in CI)

Run via `npx` (no global install required):

```bash
npx @openapi-testgen/cli \
  --spec-file ./openapi.yaml \
  --output-dir ./generated \
  --generator test-suite-writer \
  --generator-option format=json \
  --generator-option outputFileName=test-suites.json
```

For other installation options, see [Installation](../getting-started/installation.md).

## Verify Locally Before CI

Before pushing to CI, run the same commands locally to catch issues early.

### Gradle projects

```bash
# Step 1: Generate tests
./gradlew generateOpenApiTests

# Step 2: Verify generated files exist
ls build/generated/openapi-tests/
# Expected: Java/Kotlin test files or JSON suites depending on generator

# Step 3: Run tests (compile + execute)
./gradlew test

# Step 4: Check exit code (0 = success, non-zero = failure)
echo "Exit code: $?"
```

If any step fails, fix the issue before pushing. The same commands run in CI.

### CLI projects

```bash
# Step 1: Generate test suites (see "Generate your first test suite" for full command)
npx @openapi-testgen/cli --config-file openapi-testgen.yaml

# Step 2: Verify output exists
test -f ./generated/test-suites.json && echo "Generated successfully" || echo "Generation failed"

# Step 3: Inspect generated test cases (optional)
jq 'to_entries | map({operation: .key, tests: (.value.testCases | length)})' ./generated/test-suites.json

# Step 4: Run your test framework against generated suites
./gradlew test  # or: pytest, jest, etc.
```

## Failure Handling

!!! warning "Gate deployments on test failures"
    Both `./gradlew test` and `openapi-testgen` return non-zero exit codes on failure.
    CI runners (GitHub Actions, Jenkins, GitLab CI) automatically fail the job when commands
    return non-zero, blocking deployment.

**Artifacts to preserve on failure** (for debugging):

| Artifact | Purpose |
|----------|---------|
| `build/generated/openapi-tests/` | Generated test code or JSON suites |
| `build/reports/tests/` | HTML test reports |
| `build/test-results/test/*.xml` | JUnit XML for CI parsing |

See [Exit codes](#exit-codes) and [Caching and artifacts](#caching-and-artifacts) below for details.

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

Recommended (no global install):

```bash
npx @openapi-testgen/cli --help
```

Or install globally:

```bash
npm install -g @openapi-testgen/cli
openapi-testgen --version
```

See [Installation](../getting-started/installation.md) for native binaries, npm wrapper behavior, platform notes, and troubleshooting.

## Gradle plugin integration

Prerequisite: your project already applies and configures the plugin.

See:

- Tutorial: [Gradle integration](../getting-started/gradle-integration.md)
- Reference: [Gradle plugin reference](../reference/gradle-plugin.md)

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
    generatorOptions.putAll(
        mapOf(
            "format" to "json",
            "outputFileName" to "test-suites.json"
        )
    )
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

- [Generators](generators.md) - Template generator, custom templates, test-suite-writer
- [Gradle plugin reference](../reference/gradle-plugin.md)
- [CLI reference](../reference/cli.md)
- [YAML configuration](configuration.md#yaml-configuration)
- [Error handling](../concepts/error-handling.md)
