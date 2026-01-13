# TestCase

`TestCase` represents a single generated scenario.

## Key fields

- Request identity: `path`, `method`, `name`
- Inputs: `queryParams`, `pathParams`, `headers`, `cookie`, `body`
- Security: `securityValues` (kept separate from request fields)
- Expectations: `expectedStatusCode`, `expectedBody`, `needToComplete`
- Provenance: `rule` (fully qualified rule class name)

`expectedStatusCode = 0` means “unspecified”.

## Related documentation

- [TestSuite](test-suite.md)
- [Errors](errors.md)

