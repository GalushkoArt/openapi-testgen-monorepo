# Repository Guidelines

## Project Structure & Module Organization
This repo is a Gradle composite build for the **OpenAPI Test Generator**. Key modules:
- `model/`: shared data models (`TestCase`, `TestSuite`)
- `core/`: test generation logic (providers, rules, generators)
- `plugin/`: Gradle plugin integration
- `cli/`: command-line interface (optionally native via GraalVM)
- `generator-template/`: template helpers and resources
- `pattern-value/`: regex-based value generation (no `core` dependency)
- `pattern-support/`: pattern integration with `core` (module + rules + settings)
- `distribution-bundle/`: shared default wiring for CLI and Gradle plugin
- `samples/`, `example-value/`: runnable usage/examples

Standard layout is used across modules:
- Sources: `*/src/main/kotlin`
- Tests: `*/src/test/kotlin`
- Test fixtures: `*/src/test/resources`
- Templates: `generator-template/src/main/resources`

## Build, Test, and Development Commands
- `./gradlew check`: run lint, tests, and compatibility checks across modules
- `./gradlew :core:test` / `:plugin:test` / `:cli:test`: run module tests
- `./gradlew :core:test --tests "ValidCaseBuilderTest"`: run a single test class/method
- `./gradlew detekt`: run Kotlin linting/formatting rules
- `./gradlew :example-value:apiCheck`: verify binary compatibility for a module's public APIs
- `./gradlew :cli:installDist`: build the CLI distribution under `cli/build/install/`
- `./gradlew :cli:run --args="--help"`: run the CLI from Gradle
- `./gradlew :cli:nativeCompile`: build the GraalVM native CLI (if installed)

## Coding Style & Naming Conventions
- Indentation: 4 spaces; max line length: 160 (see `.editorconfig`)
- Kotlin naming: `PascalCase` classes, `camelCase` functions/vars, `UPPER_SNAKE_CASE` constants
- Prefer immutable data; avoid `!!`; validate with `require(...)` / `check(...)`
- Keep edits minimal and follow `.cursor/rules/01-kotlin-style.mdc` (detekt enforces formatting)

## Testing Guidelines
- Frameworks: JUnit 5, AssertJ, Allure
- Tests should be deterministic and avoid network I/O; fixtures live in `*/src/test/resources`
- Prefer `@DisplayName` and focused assertions (exact matches for strings/collections)
- Generator outputs are often compared against snapshot JSON fixtures—update fixtures when outputs change

## Commit & Pull Request Guidelines
- Commit messages are short, imperative summaries (no conventional-commit prefix required)
- PRs should include a concise description, modules touched, and tests run (e.g., `./gradlew :core:check :plugin:check`)
- If you change generators/templates, include updated fixtures or updated example output under `samples/`

## Architecture Notes
See `README.md` for usage and `CLAUDE.md` for architecture notes, acceptance criteria, and testing expectations.
