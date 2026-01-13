# Generators SPI

Generators emit artifacts based on generated test suites.

## ArtifactGenerator

`ArtifactGenerator` writes artifacts for a `TestSuite` (or a list of suites).

Guidelines:

- Keep output deterministic (stable ordering, no timestamps).
- Validate inputs early and fail with actionable error messages.
- Localize I/O; avoid global state.

## ArtifactGeneratorFactory

Factories create generator instances and validate generator options.

Guidelines:

- Use a stable, unique generator id.
- Validate options up-front.
- Create a fresh generator instance per execution run.

