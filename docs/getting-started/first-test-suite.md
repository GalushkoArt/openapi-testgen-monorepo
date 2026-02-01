---
description: Step-by-step tutorial for generating your first test suite using the CLI. Demonstrates both template-based test generation (RestAssured Java) and JSON/YAML test suite output.
---

# Generate your first test suite (CLI)

This tutorial generates tests from an OpenAPI spec using the CLI.

## 1) Get the CLI

Install via npm (recommended):

```bash
npm install -g @openapi-testgen/cli
```

Or run without installing:

```bash
npx @openapi-testgen/cli --help
```

For native binaries and building from source, see [Installation](installation.md).

Verify installation:

```bash
openapi-testgen --help
```

## 2) Generate template-based tests

```bash
openapi-testgen \
  --spec-file path/to/openapi.yaml \
  --output-dir ./build/generated-tests \
  --generator template \
  --generator-option templateSet=restassured-java \
  --generator-option templateVariables.package=com.example.generated \
  --generator-option templateVariables.baseUrl=http://localhost:8080
```

This generates Java test classes using RestAssured that you can run with JUnit.

## 3) Generate JSON/YAML test suites

For data-driven testing or custom frameworks:

```bash
openapi-testgen \
  --spec-file path/to/openapi.yaml \
  --output-dir ./build/out \
  --generator test-suite-writer \
  --generator-option outputFileName=openapi-test-suites.json
```

This generates a JSON file containing test case definitions that can be consumed by any test runner.

## Next steps

- Learn how to interpret outputs: [Understanding output](understanding-output.md)
- Configure generation via YAML: [YAML config](../how-to/configuration/yaml-config.md)
- Full CLI reference: [CLI reference](../reference/cli.md)
