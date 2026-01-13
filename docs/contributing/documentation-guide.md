# Documentation guide

Documentation lives under `docs/` and is built with MkDocs.

## Principles

- Write for the audience (Getting started vs How-to vs Concepts vs Reference).
- Keep examples copy-pastable and aligned with actual code.
  If code changes, update docs in the same PR.
- Prefer links to reference docs instead of duplicating normative details.

## Local preview

Install Python deps:

```bash
python -m pip install -r docs/requirements.txt
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

1. **Create or edit docs** under `docs/` following the Diataxis structure:
   - `getting-started/`: Tutorials and quick start guides
   - `how-to/`: Task-oriented guides for specific goals
   - `concepts/`: Explanatory content for understanding
   - `reference/`: API and SPI documentation for lookup
   - `modules/`: Per-module technical documentation

2. **Preview locally** with `./gradlew docsServe` or `mkdocs serve`

3. **Update navigation** in `mkdocs.yml` if adding new pages

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
