# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository. The canonical repository guidelines are tool-neutral and live in `AGENTS.md`,
imported below — read them as part of this file.

@AGENTS.md

## Claude-specific notes

- For any documentation task (site pages, READMEs, DOCS_MAP), use the `project-docs` skill — it
  encodes this repo's authoring rules and verification checklist.
- Assertion examples for the precise-assertion rules (AssertJ):

```kotlin
// Rule results: compare complete structures, not fragments
assertThat(rule.apply(schema, openAPI))
    .usingRecursiveComparison()
    .isEqualTo(expected)

// Test case lists: exact contents
assertThat(provider.provideTestCases(validCase, spec, openAPI))
    .containsExactlyInAnyOrderElementsOf(expectedCases)

// GOOD: assertThat(error.message).isEqualTo("Invalid API key: must be 32 characters")
// BAD:  assertThat(error.message).contains("Invalid")
```

- Custom test conditions exist in `core` test fixtures:
  `Conditions.ruleAppliedTo(rule, expected)` and `Conditions.correctAppliedTo(expected)`.
