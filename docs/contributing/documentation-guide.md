---
description: Guide for writing and maintaining project documentation using MkDocs. Covers frontmatter requirements, Diataxis structure, local preview, style guidelines, and contribution workflow.
---

# Documentation guide

Documentation lives under `docs/` and is built with MkDocs.

## Frontmatter requirements

All documentation pages that are part of the MkDocs site (listed in `mkdocs.yml` under `nav:`) must include YAML frontmatter with a `description` field.
The description is used to populate `DOCS_MAP.md` and helps readers understand the page's purpose.

Markdown snippets under `mkdocs/includes/` are auto-included via `pymdownx.snippets` and should not have frontmatter (for example, `mkdocs/includes/abbreviations.md`).

```yaml
---
description: Concise description of page content and purpose. Can be up to 4 sentences providing context for what the reader will learn.
---

# Page Title

Content starts here...
```

The `sync_docs_map.py` script extracts descriptions from frontmatter to populate `DOCS_MAP.md`. Pages without descriptions show `[Description needed]`.

```bash
python3 skills/project-docs/scripts/sync_docs_map.py --check
python3 skills/project-docs/scripts/sync_docs_map.py
```

## Principles

- Write for the audience (Getting started vs How-to vs Concepts vs Reference).
- Keep examples copy-pastable and aligned with actual code.
  If code changes, update docs in the same PR.
- Prefer links to reference docs instead of duplicating normative details.
- Prefer expanding an existing page over creating another thin index or near-duplicate guide.

## Local preview

Install Python deps:

```bash
python -m pip install -r requirements.txt
```

Run locally:

```bash
./gradlew docsServe
```

Or run MkDocs directly (without regenerating Dokka output):

```bash
mkdocs serve
```

## Contribution workflow

1. **Create or edit docs** under `docs/` following the existing structure:
   - `getting-started/`: Tutorials and quick start guides
   - `how-to/`: Task-oriented guides for specific goals
   - `concepts/`: Explanatory content for understanding
   - `reference/`: API and SPI documentation for lookup
   - `modules/`: Per-module technical documentation

2. **Preview locally** with `./gradlew docsServe` or `mkdocs serve`

3. **Update navigation** in `mkdocs.yml` if adding new substantive pages

4. **Cross-reference** related docs instead of duplicating content

5. **Verify code examples** compile and run correctly

## Style guidelines

- **Use active voice**: "Configure the client" not "The client can be configured"
- **Include runnable code examples** for every feature when possible
- **Use admonitions** for callouts:
  ```markdown
  !!! note
      Important information here.

  !!! warning
      Potential issues to be aware of.

  !!! tip
      Helpful hints for users.
  ```
- **Link to Dokka API docs** for class references using relative paths
- **Follow existing voice** in neighboring documents for consistency
- **Keep headings hierarchical**: `#` for title, `##` for sections, `###` for subsections

## File naming

Use lowercase with hyphens:

```
getting-started.md   # correct
GettingStarted.md    # incorrect
getting_started.md   # incorrect
```

## Adding new pages

1. Create the markdown file in the appropriate directory
2. Add the page to `mkdocs.yml` under `nav:`
3. Add cross-references from related pages
4. Verify navigation with `mkdocs serve`
