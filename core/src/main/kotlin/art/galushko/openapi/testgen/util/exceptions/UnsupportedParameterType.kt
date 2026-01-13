package art.galushko.openapi.testgen.util.exceptions

import io.swagger.v3.oas.models.parameters.Parameter

/**
 * Thrown when a parameter type is not supported by the generator.
 */
public class UnsupportedParameterType(parameter: Parameter) :
    RuntimeException("Unsupported parameter type: ${parameter.`in`} for parameter ${parameter.name}")


