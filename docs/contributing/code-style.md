# Code style

This repo is Kotlin-first and relies on automated checks to enforce consistency.

## Kotlin conventions

- PascalCase for classes/interfaces; camelCase for functions/variables; UPPER_SNAKE_CASE for constants.
- Avoid `!!`; use safe calls, `requireNotNull`, or null-object defaults.
- Prefer immutable data; use `data class` for value types.
- Use guard clauses and fail fast with `require(...)`/`check(...)`.

## Formatting and lint

- Formatting is enforced by Detekt (with ktlint rules).
- Keep edits minimal and avoid reformatting unrelated code.

## Logging

- Use SLF4J parameterized logging (no string concatenation).

