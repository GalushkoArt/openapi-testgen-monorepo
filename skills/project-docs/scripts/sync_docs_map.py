#!/usr/bin/env python3
"""
Sync DOCS_MAP.md with mkdocs.yml navigation.

Usage:
    sync_docs_map.py [--check]

Options:
    --check    Only check if DOCS_MAP.md is in sync (exit 1 if not)

This script reads mkdocs.yml navigation and updates the
`## Site Documentation (`docs/`)` section in DOCS_MAP.md to reflect the current
documentation structure. It extracts descriptions from YAML frontmatter in each
markdown file. Files without frontmatter descriptions show [Description needed].
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
from typing import Any

import yaml

PURPOSE_PLACEHOLDER = "[Description needed]"


class SafeLineLoader(yaml.SafeLoader):
    """Custom loader that ignores unknown tags (like Python object references)."""
    pass


def _ignore_unknown(loader, tag_suffix, node):
    """Return None for unknown tags."""
    return None


SafeLineLoader.add_multi_constructor("", _ignore_unknown)


def find_project_root(start: Path) -> Path:
    """Find repository root by searching for mkdocs.yml."""
    for candidate in [start, *start.parents]:
        if (candidate / "mkdocs.yml").is_file():
            return candidate
    raise RuntimeError(f"Unable to locate project root from {start}")


def load_mkdocs_config(project_root: Path) -> dict[str, Any]:
    mkdocs_path = project_root / "mkdocs.yml"
    with open(mkdocs_path) as f:
        config = yaml.load(f, Loader=SafeLineLoader)
    if not isinstance(config, dict):
        raise ValueError("mkdocs.yml did not parse to an object")
    return config


def iter_mkdocs_nav_paths(nav: list[Any]) -> list[str]:
    """Flatten mkdocs nav into an ordered list of Markdown file paths (relative to docs_dir)."""
    paths: list[str] = []

    def walk(value: Any) -> None:
        if isinstance(value, str):
            paths.append(value)
            return
        if isinstance(value, list):
            for item in value:
                walk(item)
            return
        if isinstance(value, dict):
            for _, nested in value.items():
                walk(nested)
            return
        raise ValueError(f"Unsupported mkdocs nav node: {type(value)}")

    walk(nav)
    return paths


def docs_path(docs_dir: str, rel_path: str) -> str:
    rel_path = rel_path.lstrip("/")
    if rel_path.startswith(f"{docs_dir}/"):
        return rel_path
    return f"{docs_dir}/{rel_path}"


def extract_description_from_file(file_path: Path) -> str | None:
    """Extract description from YAML frontmatter.

    Looks for YAML frontmatter delimited by --- and extracts the 'description' field.
    Returns None if no frontmatter exists or no description field is present.
    """
    if not file_path.exists():
        return None

    content = file_path.read_text()
    if not content.startswith("---"):
        return None

    # Find the end of frontmatter (second ---)
    end = content.find("---", 3)
    if end == -1:
        return None

    frontmatter_text = content[3:end]
    try:
        frontmatter = yaml.safe_load(frontmatter_text)
    except yaml.YAMLError:
        return None

    if not isinstance(frontmatter, dict):
        return None

    return frontmatter.get("description")


def extract_site_docs_section(content: str) -> tuple[str, str, str]:
    """
    Return (before, section, after) where section starts at the '## Site Documentation' heading.
    """
    match = re.search(r"^##\s+Site Documentation.*$", content, flags=re.MULTILINE)
    if not match:
        raise RuntimeError("DOCS_MAP.md is missing '## Site Documentation' section")

    start = match.start()
    rest = content[start:]

    next_section = re.search(r"^##\s+(?!Site Documentation).*$", rest, flags=re.MULTILINE)
    end = start + next_section.start() if next_section else len(content)

    before = content[:start].rstrip()
    section = content[start:end].rstrip() + "\n"
    after = content[end:].lstrip("\n")
    return before, section, after


def parse_existing_purpose_labels(site_docs_section: str) -> dict[str, str]:
    """
    Parse purpose labels from lines like:
      - `docs/foo.md` - Short purpose
    """
    mapping: dict[str, str] = {}
    pattern = re.compile(r"^\s*-\s+`(?P<path>docs/[^`]+\.md)`\s*(?:-\s*(?P<purpose>.+))?$")

    for line in site_docs_section.splitlines():
        match = pattern.match(line)
        if not match:
            continue
        path = match.group("path")
        purpose = (match.group("purpose") or "").strip()
        if purpose:
            mapping[path] = purpose

    return mapping


def normalize_heading(title: str) -> str:
    if "-" in title:
        return title
    if any(ch.isupper() for ch in title[1:]):
        return title
    return title.title()


def render_page_line(path: str, project_root: Path) -> str:
    """Render a page line with description from frontmatter.

    Extracts description from YAML frontmatter of the markdown file.
    Falls back to PURPOSE_PLACEHOLDER if no description exists.
    """
    file_path = project_root / path
    description = extract_description_from_file(file_path)
    if not description:
        description = PURPOSE_PLACEHOLDER
    return f"- `{path}` - {description}"


def render_site_docs_section(nav: list[Any], docs_dir: str, project_root: Path) -> str:
    """Render the Site Documentation section from mkdocs nav.

    Extracts descriptions from YAML frontmatter of each markdown file.
    """
    section_header = f"## Site Documentation (`{docs_dir}/`)"
    lines: list[str] = [section_header, ""]

    def ensure_blank_line() -> None:
        if lines and lines[-1] != "":
            lines.append("")

    def render_children(items: list[Any], heading_level: int) -> None:
        for item in items:
            if isinstance(item, dict):
                for title, value in item.items():
                    if isinstance(value, str):
                        lines.append(render_page_line(docs_path(docs_dir, value), project_root))
                    elif isinstance(value, list):
                        ensure_blank_line()
                        lines.append(f"{'#' * heading_level} {normalize_heading(title)}")
                        lines.append("")
                        render_children(value, heading_level + 1)
                        ensure_blank_line()
                    else:
                        raise ValueError(f"Unsupported mkdocs nav node: {type(value)}")
            elif isinstance(item, str):
                lines.append(render_page_line(docs_path(docs_dir, item), project_root))
            else:
                raise ValueError(f"Unsupported mkdocs nav node: {type(item)}")

    for item in nav:
        if isinstance(item, dict):
            for title, value in item.items():
                lines.append(f"### {normalize_heading(title)}")
                lines.append("")
                if isinstance(value, str):
                    lines.append(render_page_line(docs_path(docs_dir, value), project_root))
                elif isinstance(value, list):
                    render_children(value, 4)
                else:
                    raise ValueError(f"Unsupported mkdocs nav node: {type(value)}")
                lines.append("")
        elif isinstance(item, str):
            derived = Path(item).stem.replace("-", " ").title()
            lines.append(f"### {derived}")
            lines.append("")
            lines.append(render_page_line(docs_path(docs_dir, item), project_root))
            lines.append("")
        else:
            raise ValueError(f"Unsupported mkdocs nav node: {type(item)}")

    return "\n".join(lines).rstrip() + "\n"


def extract_docs_paths_from_site_docs_section(site_docs_section: str) -> list[str]:
    paths: list[str] = []
    pattern = re.compile(r"^\s*-\s+`(?P<path>docs/[^`]+\.md)`\s*(?:-\s*.+)?$")
    for line in site_docs_section.splitlines():
        match = pattern.match(line)
        if match:
            paths.append(match.group("path"))
    return paths


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="Only check sync status; do not modify DOCS_MAP.md")
    args = parser.parse_args()

    project_root = find_project_root(Path(__file__).resolve())
    config = load_mkdocs_config(project_root)
    docs_dir = config.get("docs_dir", "docs")
    nav = config.get("nav", [])
    if not isinstance(nav, list):
        raise ValueError("mkdocs.yml 'nav' must be a list")

    docs_map_path = project_root / "DOCS_MAP.md"
    if not docs_map_path.exists():
        raise RuntimeError("DOCS_MAP.md does not exist")

    docs_map_content = docs_map_path.read_text()
    before, current_site_docs_section, after = extract_site_docs_section(docs_map_content)

    expected_rel_paths = iter_mkdocs_nav_paths(nav)
    expected_docs_paths = [docs_path(docs_dir, p) for p in expected_rel_paths]
    if len(expected_docs_paths) != len(set(expected_docs_paths)):
        raise ValueError("mkdocs.yml nav contains duplicate page paths")

    current_docs_paths = extract_docs_paths_from_site_docs_section(current_site_docs_section)

    in_sync = current_docs_paths == expected_docs_paths
    if args.check:
        if in_sync:
            print("DOCS_MAP.md is in sync with mkdocs.yml")
            return

        current_set = set(current_docs_paths)
        expected_set = set(expected_docs_paths)
        missing = sorted(expected_set - current_set)
        extra = sorted(current_set - expected_set)

        print("DOCS_MAP.md is out of sync with mkdocs.yml")
        if missing:
            print("  Missing pages:")
            for p in missing:
                print(f"    - {p}")
        if extra:
            print("  Extra pages:")
            for p in extra:
                print(f"    - {p}")
        if not missing and not extra:
            for i, (expected, actual) in enumerate(zip(expected_docs_paths, current_docs_paths), start=1):
                if expected != actual:
                    print(f"  Order differs at position {i}: expected {expected}, found {actual}")
                    break
        print("Run without --check to rewrite the Site Documentation section.")
        sys.exit(1)

    new_site_docs_section = render_site_docs_section(nav, docs_dir, project_root)

    new_content = before
    if new_content:
        new_content += "\n\n"
    new_content += new_site_docs_section
    if after:
        new_content += "\n" + after.rstrip() + "\n"

    docs_map_path.write_text(new_content)
    print(f"Updated {docs_map_path} (Site Documentation section)")


if __name__ == "__main__":
    main()
