# Security Policy

## Supported versions

Only the latest released version receives security fixes.

| Version            | Supported |
|--------------------|-----------|
| latest 0.x release | ✅         |
| older releases     | ❌         |

## Reporting a vulnerability

Please **do not** open a public issue for security problems.

Report vulnerabilities privately via
[GitHub Security Advisories](https://github.com/GalushkoArt/openapi-testgen-monorepo/security/advisories/new).

You can expect an initial response within a week. Once a fix is available, it is released as a
patch version and the advisory is published with credit to the reporter (unless you prefer to
stay anonymous).

## Scope notes

- The generator parses untrusted OpenAPI/Swagger documents (YAML/JSON). Parser hardening issues
  (resource exhaustion, YAML bombs, etc.) are in scope; the `parserSettings` options exist to
  bound parsing.
- Generated test code is meant to be reviewed before execution; still, generated-code injection
  through crafted spec values is in scope.
