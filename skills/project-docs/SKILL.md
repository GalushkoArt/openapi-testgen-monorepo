---
name: project-docs
description: Create and maintain project documentation from actual source code. Use this skill whenever the user mentions writing, creating, or updating docs — including site docs (`docs/`), repo docs (`README.md`, `CLAUDE.md`), module pages, how-to guides, or navigation sync. Also use when user references DOCS_MAP.md, mkdocs.yml, or asks to document a feature, module, or API.
---

# Project Documentation

Write accurate documentation derived from source code. Every claim in a doc page — class names, method signatures, default values, CLI flags, config keys — must be verified against the actual code before writing.

## Step 1: Read the Code

Before writing or editing any documentation, read the relevant source code. This is the most important step because READMEs and existing docs can be stale or wrong.

**What to read (pick what applies):**
- Source files: `<module>/src/main/kotlin/**/*.kt` for classes, APIs, method signatures
- Public API surface: `<module>/api/<module>.api` for exported symbols
- Build config: `<module>/build.gradle.kts` for dependency coordinates and module name
- Tests: `<module>/src/test/kotlin/**/*.kt` for real usage patterns and expected behavior
- CLI flags: `cli/src/main/kotlin/**/Main.kt` for picocli-annotated options

**Verify, don't trust:**
- READMEs may have wrong class names, stale defaults, or missing parameters
- Existing docs may reference renamed or removed APIs
- Import paths matter: check whether classes are top-level or nested (read the actual file, check the `package` declaration and class declaration scope)

Build a mental model of what the code actually does before writing a single line of documentation.

## Step 2: Check What Already Exists

Read `DOCS_MAP.md` and scan `mkdocs.yml` nav to understand what's already documented. Then read any existing pages that cover related topics.

**Before creating a new page**, verify:
- No existing page already covers this topic (check `DOCS_MAP.md`)
- No existing page has a section that should be expanded instead of creating a new page

**Before updating an existing page**, verify:
- The page actually needs changes — compare the current docs content against the source code you read in Step 1
- If the docs already match the code, don't change anything. Making unnecessary edits risks introducing regressions

**Deduplication rule:** Each piece of information should live in exactly one place. When another page already covers something (e.g., distribution-settings.md covers config options, rules-catalog.md covers built-in rules), link to it instead of repeating it. Use the pattern `[Topic name](../relative/path.md)` or `[Topic name](../relative/path.md#anchor)`.

## Step 3: Write the Documentation

### Page placement

Pick the category based on what the reader needs to do:

| Category | Path | Reader goal |
|----------|------|-------------|
| Tutorials | `getting-started/` | Get first success |
| Task guides | `how-to/` | Solve a specific problem |
| Explanation | `concepts/` | Understand why/how |
| Lookup | `reference/` | Find a setting, flag, or SPI |
| Module docs | `modules/` | Understand a module's API and role |
| Contributor | `contributing/` | Contribute to the project |

For repo-level docs (`README.md`, `CLAUDE.md`, `AGENTS.md`, `<module>/README.md`), edit the file directly without mkdocs.yml changes.

### Required frontmatter

Every site page under `docs/` (except `docs/includes/**`) needs:

```yaml
---
description: What the reader will learn or do on this page (1–4 sentences).
---

# Page Title
```

### Structure rules

- Keep headings flat: h1 for the page title, h2 for major sections, h3 only when genuinely needed. Avoid h4+.
- Pages can be long — a single comprehensive page is better than many thin pages that force readers to jump around.
- Start with "when to use this" or "what this covers" so readers can bail early if it's not what they need.
- End with a "Related docs" section linking to connected pages.

### Content rules

- Every code example must be derived from actual source code — correct class names, correct import paths, correct method signatures, correct default values.
- Show Kotlin code blocks with language tag: ` ```kotlin `
- Use admonitions sparingly: `!!! note` for important context, `!!! warning` for gotchas, `!!! tip` for shortcuts.
- Active voice: "Configure the generator" not "The generator can be configured".
- When documenting options/settings, use a table with columns: Name, Type/Default, Description.

### Outline by doc type

| Doc type | Sections |
|----------|----------|
| **Getting started** | Prerequisites → Steps → Verify output → Next steps |
| **How-to** | Goal → Prerequisites → Steps → Examples → Related docs |
| **Concepts** | What/why → How it works → Key components → Trade-offs → Related docs |
| **Reference** | What this covers → Options table → Defaults → Examples → Related docs |
| **Modules** | Purpose → When to use → Depends on / Used by → Installation → Key types and API → Usage examples → Related docs |

## Step 4: Update Navigation

If you created a new page:
1. Add it to `mkdocs.yml` under `nav:` in the correct section
2. Run `python3 skills/project-docs/scripts/sync_docs_map.py` to regenerate `DOCS_MAP.md`

If you only edited an existing page, run the check to make sure nothing drifted:
```bash
python3 skills/project-docs/scripts/sync_docs_map.py --check
```

## Verification Checklist

Before finishing:
- [ ] Every class name, method signature, and default value in the docs matches the source code
- [ ] Import paths reference the correct package (top-level vs. nested classes verified)
- [ ] Code examples would compile against the actual API
- [ ] No content is duplicated from other docs pages — cross-links used instead
- [ ] Frontmatter `description` is present on all site pages
- [ ] New pages are in `mkdocs.yml` nav and `DOCS_MAP.md` is in sync
- [ ] Terminology matches `docs/concepts/glossary.md`
