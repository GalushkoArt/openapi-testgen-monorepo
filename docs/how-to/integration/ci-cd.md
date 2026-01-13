# CI/CD integration

This page is intentionally **tool-agnostic**: it shows what commands to run in CI to generate tests, but it does not add CI configuration to this repository.

## Option A: Gradle plugin (recommended for Gradle builds)

1. Ensure the plugin is applied and configured (see [Gradle integration](../../getting-started/gradle-integration.md)).
2. Run generation as part of your pipeline:

```bash
./gradlew generateOpenApiTests
```

If you want generation to run only when explicitly invoked, set `manualOnly=true` in the DSL.
If you want it wired automatically into compilation/resource processing, keep `manualOnly=false` (default).

## Option B: CLI (works in any build)

1. Build the CLI distribution:

```bash
./gradlew :cli:installDist
```

2. Run the generator:

```bash
./cli/build/install/openapi-testgen/bin/openapi-testgen \
  --spec-file ./path/to/openapi.yaml \
  --output-dir ./build/generated-tests \
  --generator test-suite-writer \
  --generator-option outputFileName=openapi-test-suites.json
```

See [CLI reference](../../reference/cli.md) for all flags.

## Fail-fast vs best-effort

- Use `errorMode=FAIL_FAST` for strict CI (stop on first error).
- Use `errorMode=COLLECT_ALL` when you prefer collecting multiple errors per run (bounded by `maxErrors`).

## Tips

- Cache Gradle (`~/.gradle`) between CI runs when possible.
- Keep generation deterministic by pinning inputs (spec, config) and avoiding environment-dependent values.
