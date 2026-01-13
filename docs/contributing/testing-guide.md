# Testing guide

Tests should be deterministic, fast, and isolated.

## Frameworks

- JUnit 5
- AssertJ
- Allure (annotations + steps)

## General rules

- Avoid network access.
  Use fixtures under `core/src/test/resources` when you need OpenAPI specs or snapshot JSON.
- Prefer focused unit tests over large integration tests.
- Use stable ordering in assertions (`containsExactly`, sorted lists) to avoid flaky diffs.

## Useful commands

```bash
./gradlew check
./gradlew :core:test
./gradlew :plugin:test
./gradlew :cli:test
```

