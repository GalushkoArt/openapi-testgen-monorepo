# Contributing to OpenAPI Test Generator

Thanks for your interest in contributing! This page covers the essentials; the full guides live
on the documentation site:

- [Development setup](https://docs.galushko.art/openapi-test-generator/contributing/development-setup/)
- [Documentation guide](https://docs.galushko.art/openapi-test-generator/contributing/documentation-guide/)
- [Publishing & releases](https://docs.galushko.art/openapi-test-generator/contributing/publishing/)

## Prerequisites

- Java 21 (the build uses a Java 21 toolchain)
- No local Kotlin/Gradle installation needed — use the wrapper (`./gradlew`)

## Building and testing

```bash
# Run all checks: lint (detekt), tests, binary-compatibility, dependency health, coverage
# across every module plus the samples
./gradlew check

# Work on a single module
./gradlew :core:test
./gradlew :core:test --tests "ValidCaseBuilderTest"
```

The repo is a Gradle composite build; each module under the root (`model`, `core`, `plugin`,
`cli`, …) is an included build. `./gradlew check` at the root aggregates all of them.

## Before opening a pull request

1. `./gradlew check` passes locally.
2. Public API changes are intentional: run `./gradlew :<module>:apiDump` and commit the
   updated `<module>/api/*.api` file.
3. Add an entry under `## Unreleased` in [CHANGELOG.md](CHANGELOG.md)
   (Keep a Changelog format; mark breaking changes with **BREAKING**).
4. New behavior comes with tests. Follow the existing style: JUnit 5 + AssertJ, exact
   assertions, deterministic and isolated (no network I/O).
5. Docs affected? Update the relevant page under `docs/` (built with MkDocs; `./gradlew docsBuild`
   must pass in strict mode).

## Code style

- Kotlin style is enforced by detekt (`build-logic/config/detekt.yml`, zero-issue policy).
- Avoid `!!`; validate inputs early with `require`/`check`; never swallow exceptions.
- Keep output deterministic: sort collections by stable keys.
- No reflection for discovery — rules/generators are wired explicitly (GraalVM-friendly).

## Reporting bugs and requesting features

Use the [issue templates](https://github.com/GalushkoArt/openapi-testgen-monorepo/issues/new/choose).
For security issues, see [SECURITY.md](SECURITY.md) — please do not open public issues.
