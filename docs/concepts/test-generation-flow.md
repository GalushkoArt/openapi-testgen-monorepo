# Test generation flow

At a high level, generation follows a fixed pipeline per OpenAPI operation.

## High-level pipeline

1. **Parse OpenAPI**
   The engine parses and fully resolves the OpenAPI model.
2. **Filter operations**
   If `includeOperations` is set, the engine filters paths/methods before generation.
   `ignoreTestCases` is applied later, after suites are assembled.
3. **Build valid case**
   `ValidCaseBuilder` builds a baseline valid `TestCase`.
4. **Create context**
   `TestGenerationContext` is created with the OpenAPI model, operation, valid case, and helpers (example value generator, schema merger, budgets).
5. **Run providers**
   Providers execute in a fixed order:
   - auth
   - parameters
   - request body
6. **Aggregate outcomes**
   Provider outcomes are merged into `Outcome<TestSuite>`.
   If `includeValidCase` is enabled, the baseline valid case (with 2xx expected status)
   is prepended to the test list.
   Budgets and error handling determine whether the result is success, partial success, or failure.
7. **Write artifacts**
   An `ArtifactGenerator` emits outputs (template-based code or JSON/YAML suites).

## Outputs

- `TestSuite`: operation-level container (`operationName` is the stable identifier).
- `TestCase`: individual scenario (request inputs + expected status/body).
- `GenerationReport`: aggregated suites + errors + summary (success/partial/failure/not-tested).

See also:

- [Provider-rule model](provider-rule-model.md)
- [Error handling](error-handling.md)
- [Budget controls](budget-controls.md)

