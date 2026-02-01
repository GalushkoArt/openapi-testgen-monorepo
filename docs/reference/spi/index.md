---
description: Overview of the core SPI (Service Provider Interface) for extending the OpenAPI Test Generator with custom validation rules, test providers, generators, and value providers.
---

# Core SPI (Extension Interfaces)

This document is non-normative and targets contributors building custom rules, providers, or
generators. For core entry points, see the [core module](../../modules/core.md). For built-in catalogs,
see [rules](../catalogs/rules-catalog.md) and [providers](../catalogs/providers-catalog.md).
For distribution defaults, see [distribution settings](../distribution-settings.md).
For contributor workflow, see [development setup](../../contributing/development-setup.md).

## Overview

The SPI lives in `core/src/main/kotlin/.../spi` and defines the stable extension surface for the
core module. Implementations should be deterministic, side-effect free, and compatible with the
generation pipeline.

## Pages

- [Validation rules](validation-rules.md)
- [Test providers](test-providers.md)
- [Generators](generators.md)
- [Value providers](value-providers.md)

## Rules

Rule interfaces and registration are canonical in [Validation rules SPI](validation-rules.md).

### SchemaValidationRule

See [SchemaValidationRule](validation-rules.md#schemavalidationrule).

### SimpleSchemaValidationRule

See [SimpleSchemaValidationRule](validation-rules.md#simpleschemavalidationrule).

### AuthValidationRule

See [AuthValidationRule](validation-rules.md#authvalidationrule).

## RuleValue and composed rules

See [RuleValue](validation-rules.md#rulevalue) for how rule-generated invalid values are represented.

## RuleRegistry

`RuleRegistry` assembles deterministically ordered rule lists and applies ignore filters (default: `ManualRuleRegistry`).
Most extensions should register additional rules via `TestGenerationModule` rather than implement a custom registry.

## TestCaseProvider

Test provider interfaces and wiring notes are canonical in [Test providers SPI](test-providers.md).

## ArtifactGenerator

Generator interfaces and wiring notes are canonical in [Generators SPI](generators.md).

## SecuritySchemeToScope

`SecuritySchemeToScope` pairs a resolved OpenAPI `SecurityScheme` with its name and scopes.
Auth rules and `SecurityValueProvider` use this model when deriving valid or invalid security values.

## Implementation checklist

- Deterministic iteration order; no non-deterministic maps/sets.
- No mutation of `TestGenerationContext` or `OpenAPI` models.
- Return empty sequences when a rule/provider is not applicable.
- Set `expectedStatusCode` explicitly for auth rules.
- Wrap provider logic with `runProviderSafely`.
