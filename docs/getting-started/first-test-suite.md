# Generate your first test suite (CLI)

This tutorial generates tests from an OpenAPI spec using the CLI.

## 1) Get the CLI

=== "npm (recommended)"

    ```bash
    npm install -g @openapi-testgen/cli
    ```

=== "Download binary"

    ```bash
    # Download native binary (no Java required)
    curl -LO https://github.com/GalushkoArt/openapi-testgen-monorepo/releases/download/0.9.1/openapi-testgen-0.9.1-linux-amd64.zip
    unzip openapi-testgen-0.9.1-linux-amd64.zip
    cd openapi-testgen-0.9.1-linux-amd64
    chmod +x openapi-testgen
    ```

=== "Build from source"

    ```bash
    ./gradlew :cli:installDist
    alias openapi-testgen='./cli/build/install/openapi-testgen/bin/openapi-testgen'
    ```

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
