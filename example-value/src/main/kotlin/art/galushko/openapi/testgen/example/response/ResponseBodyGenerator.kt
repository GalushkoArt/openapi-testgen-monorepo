package art.galushko.openapi.testgen.example.response

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema

/**
 * Strategy for deriving a fallback response body from a schema.
 *
 * [ResponseExampleExtractor] invokes this generator only when the specification declares
 * no usable explicit example for the negotiated media type, so explicit-example precedence
 * stays in one place while the generated-body slot remains pluggable.
 *
 * This is a `fun interface`, so Kotlin callers can pass a lambda and Java callers can pass
 * a lambda via SAM conversion:
 *
 * ```java
 * new ResponseExampleExtractor((schema, api) -> myGenerator.generate(schema, api));
 * ```
 */
public fun interface ResponseBodyGenerator {

    /**
     * Generates a response body example for the given schema.
     *
     * @param schema response schema selected by media-type negotiation
     * @param openAPI OpenAPI document providing component definitions
     * @return generated body, or null when no body can be generated
     */
    public fun generate(schema: Schema<*>, openAPI: OpenAPI): Any?
}
