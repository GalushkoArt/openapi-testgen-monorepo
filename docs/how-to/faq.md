---
description: Frequently asked questions about OpenAPI Test Generator - installation, Gradle and CLI usage, security values, filtering, positive tests, customization, budgets, and running generated tests.
---

# FAQ

Short answers to common questions, each linking to the page that covers the topic in depth.

## How do I install the CLI?

The quickest path is npm: `npm install -g @openapi-testgen/cli` (native binaries are used
automatically where available, falling back to a bundled JAR that needs Java 21+). Native binaries
and JVM distributions are also on GitHub Releases. See [Installation](../getting-started/installation.md).

## How do I generate tests from an OpenAPI spec in a Gradle build?

Apply the plugin `art.galushko.openapi-test-generator`, configure `specFile`, `outputDir`, and
`generator` in the `openApiTestGenerator { ... }` block, then run `./gradlew generateOpenApiTests`.
Template output is wired into your test sources automatically. See
[Gradle integration](../getting-started/gradle-integration.md) and the
[Gradle plugin reference](../reference/gradle-plugin.md).

## How do I generate JSON or YAML test suites instead of Java code?

Use the `test-suite-writer` generator instead of `template`. It writes `TestSuite` data files for
data-driven frameworks or custom runners. See
[Test-suite-writer generator](generators.md#test-suite-writer-generator).

## How do I provide API keys or tokens for secured endpoints?

Set `validSecurityValues`, keyed by the scheme name under `components.securitySchemes` (e.g.
`ApiKeyAuth`) — not the header name (e.g. `X-API-Key`). Without it, secured requests carry
`<valid_..._placeholder>` values. See [Security values](configuration.md#security-values).

## How do I generate tests for only specific endpoints?

Use `includeOperations` with exact paths (or `*` wildcards for path/method) — it filters before
generation, so it is also the fastest option on large specs. See
[Include operations](configuration.md#include-operations).

## How do I skip specific test cases or disable a validation rule?

Skip generated cases by operation and name with `ignoreTestCases`; disable a rule everywhere by
adding its fully qualified class name to `ignoreSchemaValidationRules` /
`ignoreAuthValidationRules`. See [Ignore rules](configuration.md#ignore-rules) and
[Disable a rule](../reference/catalogs/rules-catalog.md#disable-a-rule-by-fully-qualified-class-name).

## Does it generate positive (2xx) tests or only negative ones?

Negative (4xx) tests by default. Set `includeValidCase: true` to add one baseline positive case
named "Test Valid Case" per operation. See [Positive testing](positive-testing.md).

## Does the generator call my API during generation?

No. Generation is offline: it parses the spec (a local file or a remote URL) and writes artifacts.
Your API is only called when you *run* the generated tests.

## Can I run the generated tests against a live server?

Yes — the RestAssured template sets take the target host from the `baseUrl` template variable, so
point it at any environment. For data-driven suites, your own runner decides the target. See
[Generators](generators.md#restassured-integration).

## Can I customize the generated test code?

Yes, at two levels: pass `templateVariables` to the built-in RestAssured template sets, or supply
your own Mustache templates with `customTemplateDir` for full control over the output. See
[Custom Mustache templates](generators.md#custom-mustache-templates).

## How do I keep manual edits when the suites are regenerated?

With `test-suite-writer`, use `writeMode: MERGE` and list the fields you edited in
`protectedTestCaseFields`. With the `template` generator, use `writeMode: SKIP_IF_EXISTS` to leave
existing files untouched. See [Merge semantics](generators.md#merge-semantics).

## What does "Budget exceeded" mean?

A complexity limit (schema combinations or test cases per operation) was hit — a guard against
combinatorial explosion on deeply composed schemas. Raise the limit the message names, simplify
the schema, or exclude the operation. See
[Troubleshooting — Budget exceeded](troubleshooting.md#budget-exceeded-for-method-path).

## What happens when the YAML config file, CLI flags, and Gradle DSL disagree?

CLI flags and Gradle DSL values override the YAML config file. Nested maps are deep-merged; lists
are replaced. See
[Configuration sources and precedence](../reference/distribution-settings.md#configuration-sources-and-precedence).

## Which OpenAPI and Swagger versions are supported?

OpenAPI 3.0.x and 3.1.x directly; Swagger 2.0 through normalization to the OpenAPI 3 model.
Swagger 1.x is rejected. Webhooks are parsed but not generated. See
[Supported specifications](../reference/supported-specifications.md).

## Can I use the generator programmatically from Kotlin or Java?

Yes — `TestGenerationRunner` from `distribution-bundle` is the high-level entry point; `core`'s
`TestGenerationEngine` gives lower-level control. See
[Distribution-bundle](../modules/distribution-bundle.md#testgenerationrunner).

## Is the output deterministic?

Yes: the same spec and settings always produce the same tests. Rules, providers, and modules are
deterministically ordered, and value generation uses fixed seeds. See
[Architecture — determinism](../concepts/architecture.md).

## Related docs

- [Getting started](../getting-started/index.md)
- [Configuration](configuration.md)
- [Troubleshooting](troubleshooting.md)
