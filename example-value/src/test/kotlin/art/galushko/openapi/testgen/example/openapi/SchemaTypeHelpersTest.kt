package art.galushko.openapi.testgen.example.openapi

import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.QueryParameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Epic("OpenAPI Utilities")
@Feature("Schema Type Helpers")
@DisplayName("SchemaTypeHelpers")
class SchemaTypeHelpersTest {

    @Nested
    @DisplayName("tryGetSchemaFromRef")
    inner class TryGetSchemaFromRefTest {

        @Test
        @DisplayName("should return same schema when no ref present")
        fun shouldReturnSameSchemaWhenNoRefPresent() {
            val schema = StringSchema()
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.tryGetSchemaFromRef(schema, openAPI)

            assertThat(result).isSameAs(schema)
        }

        @Test
        @DisplayName("should dereference schema ref")
        fun shouldDereferenceSchemaRef() {
            val referencedSchema = StringSchema().apply { minLength = 5 }
            val openAPI = OpenAPI().apply {
                components = Components().apply {
                    addSchemas("MySchema", referencedSchema)
                }
            }
            val refSchema = Schema<String>().apply {
                `$ref` = "#/components/schemas/MySchema"
            }

            val result = SchemaTypeHelpers.tryGetSchemaFromRef(refSchema, openAPI)

            assertThat(result).isSameAs(referencedSchema)
        }

        @Test
        @DisplayName("should return original schema when ref not found")
        fun shouldReturnOriginalSchemaWhenRefNotFound() {
            val refSchema = Schema<String>().apply {
                `$ref` = "#/components/schemas/NonExistent"
            }
            val openAPI = OpenAPI().apply {
                components = Components().apply {
                    addSchemas("SomeOtherSchema", StringSchema())
                }
            }

            val result = SchemaTypeHelpers.tryGetSchemaFromRef(refSchema, openAPI)

            assertThat(result).isSameAs(refSchema)
        }

        @Test
        @DisplayName("should return original schema when components is null")
        fun shouldReturnOriginalSchemaWhenComponentsNull() {
            val refSchema = Schema<String>().apply {
                `$ref` = "#/components/schemas/MySchema"
            }
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.tryGetSchemaFromRef(refSchema, openAPI)

            assertThat(result).isSameAs(refSchema)
        }
    }

    @Nested
    @DisplayName("resolveResponseByStatus")
    inner class ResolveResponseByStatusTest {

        @Test
        @DisplayName("should return null when operation has no responses")
        fun shouldReturnNullWhenNoResponses() {
            val operation = Operation()
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.resolveResponseByStatus(operation, openAPI, 200)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("should return exact match when present")
        fun shouldReturnExactMatchWhenPresent() {
            val exactResponse = ApiResponse().description("OK")
            val rangeResponse = ApiResponse().description("2XX Range")
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("200", exactResponse)
                    addApiResponse("2XX", rangeResponse)
                }
            }
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.resolveResponseByStatus(operation, openAPI, 200)

            assertThat(result).isSameAs(exactResponse)
        }

        @Test
        @DisplayName("should return range match when exact not found")
        fun shouldReturnRangeMatchWhenExactNotFound() {
            val rangeResponse = ApiResponse().description("2XX Range")
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("201", ApiResponse().description("Created"))
                    addApiResponse("2XX", rangeResponse)
                }
            }
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.resolveResponseByStatus(operation, openAPI, 200)

            assertThat(result).isSameAs(rangeResponse)
        }

        @Test
        @DisplayName("should return default when no exact or range match")
        fun shouldReturnDefaultWhenNoExactOrRangeMatch() {
            val defaultResponse = ApiResponse().description("Default")
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("201", ApiResponse().description("Created"))
                    addApiResponse("default", defaultResponse)
                }
            }
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.resolveResponseByStatus(operation, openAPI, 200)

            assertThat(result).isSameAs(defaultResponse)
        }

        @Test
        @DisplayName("should prefer range over default")
        fun shouldPreferRangeOverDefault() {
            val rangeResponse = ApiResponse().description("2XX Range")
            val defaultResponse = ApiResponse().description("Default")
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("2XX", rangeResponse)
                    addApiResponse("default", defaultResponse)
                }
            }
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.resolveResponseByStatus(operation, openAPI, 200)

            assertThat(result).isSameAs(rangeResponse)
        }

        @Test
        @DisplayName("should resolve ref in matched response")
        fun shouldResolveRefInMatchedResponse() {
            val referencedResponse = ApiResponse().description("Referenced OK")
            val refResponse = ApiResponse().apply {
                `$ref` = "#/components/responses/SuccessResponse"
            }
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("200", refResponse)
                }
            }
            val openAPI = OpenAPI().apply {
                components = Components().apply {
                    addResponses("SuccessResponse", referencedResponse)
                }
            }

            val result = SchemaTypeHelpers.resolveResponseByStatus(operation, openAPI, 200)

            assertThat(result).isSameAs(referencedResponse)
        }

        @Test
        @DisplayName("should return null when no matching response")
        fun shouldReturnNullWhenNoMatchingResponse() {
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("400", ApiResponse().description("Bad Request"))
                }
            }
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.resolveResponseByStatus(operation, openAPI, 200)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("should handle case-insensitive range keys")
        fun shouldHandleCaseInsensitiveRangeKeys() {
            val rangeResponse = ApiResponse().description("2xx Range")
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("2xx", rangeResponse)
                }
            }
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.resolveResponseByStatus(operation, openAPI, 200)

            assertThat(result).isSameAs(rangeResponse)
        }
    }

    @Nested
    @DisplayName("resolveExampleRef")
    inner class ResolveExampleRefTest {

        @Test
        @DisplayName("should return original example when no ref present")
        fun shouldReturnOriginalExampleWhenNoRefPresent() {
            val example = Example().value(mapOf("id" to 1))
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.resolveExampleRef(example, openAPI)

            assertThat(result).isSameAs(example)
        }

        @Test
        @DisplayName("should resolve example ref from components")
        fun shouldResolveExampleRefFromComponents() {
            val referencedExample = Example().value(mapOf("status" to "ok"))
            val refExample = Example().apply {
                `$ref` = "#/components/examples/OkExample"
            }
            val openAPI = OpenAPI().apply {
                components = Components().apply {
                    addExamples("OkExample", referencedExample)
                }
            }

            val result = SchemaTypeHelpers.resolveExampleRef(refExample, openAPI)

            assertThat(result).isSameAs(referencedExample)
        }

        @Test
        @DisplayName("should return original example when ref not found in components")
        fun shouldReturnOriginalExampleWhenRefNotFoundInComponents() {
            val refExample = Example().apply {
                `$ref` = "#/components/examples/NonExistent"
            }
            val openAPI = OpenAPI().apply {
                components = Components().apply {
                    addExamples("SomeOtherExample", Example().value("other"))
                }
            }

            val result = SchemaTypeHelpers.resolveExampleRef(refExample, openAPI)

            assertThat(result).isSameAs(refExample)
        }

        @Test
        @DisplayName("should return original example when components is null")
        fun shouldReturnOriginalExampleWhenComponentsNull() {
            val refExample = Example().apply {
                `$ref` = "#/components/examples/SomeExample"
            }
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.resolveExampleRef(refExample, openAPI)

            assertThat(result).isSameAs(refExample)
        }

        @Test
        @DisplayName("should return original example when ref has invalid prefix")
        fun shouldReturnOriginalExampleWhenRefHasInvalidPrefix() {
            val refExample = Example().apply {
                `$ref` = "#/components/schemas/NotAnExample"
            }
            val openAPI = OpenAPI().apply {
                components = Components().apply {
                    addExamples("NotAnExample", Example().value("wrong path"))
                }
            }

            val result = SchemaTypeHelpers.resolveExampleRef(refExample, openAPI)

            assertThat(result).isSameAs(refExample)
        }
    }

    @Nested
    @DisplayName("findSuccessStatusCode")
    inner class FindSuccessStatusCodeTest {

        @Test
        @DisplayName("should throw when operation has no responses")
        fun shouldThrowWhenNoResponses() {
            val operation = Operation()

            org.junit.jupiter.api.assertThrows<IllegalStateException> {
                SchemaTypeHelpers.findSuccessStatusCode(operation)
            }
        }

        @Test
        @DisplayName("should return minimum 2xx status code when present")
        fun shouldReturnMinimum2xxStatusCode() {
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("201", ApiResponse().description("Created"))
                    addApiResponse("200", ApiResponse().description("OK"))
                    addApiResponse("204", ApiResponse().description("No Content"))
                }
            }

            val result = SchemaTypeHelpers.findSuccessStatusCode(operation)

            assertThat(result).isEqualTo(200)
        }

        @Test
        @DisplayName("should return 200 when only 2XX range key is present")
        fun shouldReturn200When2XXRangePresent() {
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("2XX", ApiResponse().description("Success"))
                    addApiResponse("400", ApiResponse().description("Bad Request"))
                }
            }

            val result = SchemaTypeHelpers.findSuccessStatusCode(operation)

            assertThat(result).isEqualTo(200)
        }

        @Test
        @DisplayName("should return 200 when only default key is present")
        fun shouldReturn200WhenDefaultPresent() {
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("default", ApiResponse().description("Default"))
                }
            }

            val result = SchemaTypeHelpers.findSuccessStatusCode(operation)

            assertThat(result).isEqualTo(200)
        }

        @Test
        @DisplayName("should prefer numeric 2xx over range key")
        fun shouldPreferNumeric2xxOverRange() {
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("201", ApiResponse().description("Created"))
                    addApiResponse("2XX", ApiResponse().description("Range"))
                }
            }

            val result = SchemaTypeHelpers.findSuccessStatusCode(operation)

            assertThat(result).isEqualTo(201)
        }

        @Test
        @DisplayName("should prefer range key over default")
        fun shouldPreferRangeOverDefault() {
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("2XX", ApiResponse().description("Range"))
                    addApiResponse("default", ApiResponse().description("Default"))
                }
            }

            val result = SchemaTypeHelpers.findSuccessStatusCode(operation)

            assertThat(result).isEqualTo(200)
        }

        @Test
        @DisplayName("should handle case-insensitive 2xx range key")
        fun shouldHandleCaseInsensitive2xxRange() {
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("2xx", ApiResponse().description("Range"))
                }
            }

            val result = SchemaTypeHelpers.findSuccessStatusCode(operation)

            assertThat(result).isEqualTo(200)
        }

        @Test
        @DisplayName("should throw when no success response found")
        fun shouldThrowWhenNoSuccessResponse() {
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("400", ApiResponse().description("Bad Request"))
                    addApiResponse("500", ApiResponse().description("Error"))
                }
            }

            val exception = org.junit.jupiter.api.assertThrows<IllegalStateException> {
                SchemaTypeHelpers.findSuccessStatusCode(operation)
            }

            assertThat(exception.message).contains("Success status code not found")
        }

        @Test
        @DisplayName("should ignore non-2xx numeric codes")
        fun shouldIgnoreNon2xxNumericCodes() {
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("300", ApiResponse().description("Redirect"))
                    addApiResponse("400", ApiResponse().description("Bad Request"))
                    addApiResponse("default", ApiResponse().description("Default"))
                }
            }

            val result = SchemaTypeHelpers.findSuccessStatusCode(operation)

            assertThat(result).isEqualTo(200)
        }
    }

    @Nested
    @DisplayName("tryGetRequestBodyFromRef")
    inner class TryGetRequestBodyFromRefTest {

        @Test
        @DisplayName("should return same request body when no ref present")
        fun shouldReturnSameRequestBodyWhenNoRefPresent() {
            val requestBody = RequestBody().description("Direct body")
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.tryGetRequestBodyFromRef(requestBody, openAPI)

            assertThat(result).isSameAs(requestBody)
        }

        @Test
        @DisplayName("should dereference request body ref")
        fun shouldDereferenceRequestBodyRef() {
            val referencedBody = RequestBody().description("Referenced body")
            val openAPI = OpenAPI().apply {
                components = Components().apply {
                    addRequestBodies("MyBody", referencedBody)
                }
            }
            val refBody = RequestBody().apply {
                `$ref` = "#/components/requestBodies/MyBody"
            }

            val result = SchemaTypeHelpers.tryGetRequestBodyFromRef(refBody, openAPI)

            assertThat(result).isSameAs(referencedBody)
        }

        @Test
        @DisplayName("should return original request body when ref not found")
        fun shouldReturnOriginalRequestBodyWhenRefNotFound() {
            val refBody = RequestBody().apply {
                `$ref` = "#/components/requestBodies/NonExistent"
            }
            val openAPI = OpenAPI().apply {
                components = Components().apply {
                    requestBodies = emptyMap()
                }
            }

            val result = SchemaTypeHelpers.tryGetRequestBodyFromRef(refBody, openAPI)

            assertThat(result).isSameAs(refBody)
        }
    }

    @Nested
    @DisplayName("tryGetParametersFromRef")
    inner class TryGetParametersFromRefTest {

        @Test
        @DisplayName("should return same parameter when no ref present")
        fun shouldReturnSameParameterWhenNoRefPresent() {
            val parameter = QueryParameter().name("limit")
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.tryGetParametersFromRef(parameter, openAPI)

            assertThat(result).isSameAs(parameter)
        }

        @Test
        @DisplayName("should dereference parameter ref")
        fun shouldDereferenceParameterRef() {
            val referencedParam = QueryParameter().name("limit").description("Ref param")
            val openAPI = OpenAPI().apply {
                components = Components().apply {
                    addParameters("LimitParam", referencedParam)
                }
            }
            val refParam = Parameter().apply {
                `$ref` = "#/components/parameters/LimitParam"
            }

            val result = SchemaTypeHelpers.tryGetParametersFromRef(refParam, openAPI)

            assertThat(result).isSameAs(referencedParam)
        }

        @Test
        @DisplayName("should return original parameter when ref not found")
        fun shouldReturnOriginalParameterWhenRefNotFound() {
            val refParam = Parameter().apply {
                `$ref` = "#/components/parameters/NonExistent"
            }
            val openAPI = OpenAPI().apply {
                components = Components().apply {
                    parameters = emptyMap()
                }
            }

            val result = SchemaTypeHelpers.tryGetParametersFromRef(refParam, openAPI)

            assertThat(result).isSameAs(refParam)
        }
    }

    @Nested
    @DisplayName("isNumber")
    inner class IsNumberTest {

        @Suppress("UNUSED_PARAMETER") // schemaDescription is used in test name via {0}
        @ParameterizedTest(name = "{0} should return {2}")
        @MethodSource("art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpersTest#isNumberProvider")
        @DisplayName("should correctly identify number schemas")
        fun shouldCorrectlyIdentifyNumberSchemas(schemaDescription: String, schema: Schema<*>, expected: Boolean) {
            assertThat(SchemaTypeHelpers.isNumber(schema)).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("isString")
    inner class IsStringTest {

        @Suppress("UNUSED_PARAMETER") // schemaDescription is used in test name via {0}
        @ParameterizedTest(name = "{0} should return {2}")
        @MethodSource("art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpersTest#isStringProvider")
        @DisplayName("should correctly identify string schemas")
        fun shouldCorrectlyIdentifyStringSchemas(schemaDescription: String, schema: Schema<*>, expected: Boolean) {
            assertThat(SchemaTypeHelpers.isString(schema)).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("isInteger")
    inner class IsIntegerTest {

        @Suppress("UNUSED_PARAMETER") // schemaDescription is used in test name via {0}
        @ParameterizedTest(name = "{0} should return {2}")
        @MethodSource("art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpersTest#isIntegerProvider")
        @DisplayName("should correctly identify integer schemas")
        fun shouldCorrectlyIdentifyIntegerSchemas(schemaDescription: String, schema: Schema<*>, expected: Boolean) {
            assertThat(SchemaTypeHelpers.isInteger(schema)).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("isArray")
    inner class IsArrayTest {

        @Suppress("UNUSED_PARAMETER") // schemaDescription is used in test name via {0}
        @ParameterizedTest(name = "{0} should return {2}")
        @MethodSource("art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpersTest#isArrayProvider")
        @DisplayName("should correctly identify array schemas")
        fun shouldCorrectlyIdentifyArraySchemas(schemaDescription: String, schema: Schema<*>, expected: Boolean) {
            assertThat(SchemaTypeHelpers.isArray(schema)).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("isBoolean")
    inner class IsBooleanTest {

        @Suppress("UNUSED_PARAMETER") // schemaDescription is used in test name via {0}
        @ParameterizedTest(name = "{0} should return {2}")
        @MethodSource("art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpersTest#isBooleanProvider")
        @DisplayName("should correctly identify boolean schemas")
        fun shouldCorrectlyIdentifyBooleanSchemas(schemaDescription: String, schema: Schema<*>, expected: Boolean) {
            assertThat(SchemaTypeHelpers.isBoolean(schema)).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("isObject")
    inner class IsObjectTest {

        @Suppress("UNUSED_PARAMETER") // schemaDescription is used in test name via {0}
        @ParameterizedTest(name = "{0} should return {2}")
        @MethodSource("art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpersTest#isObjectProvider")
        @DisplayName("should correctly identify object schemas")
        fun shouldCorrectlyIdentifyObjectSchemas(schemaDescription: String, schema: Schema<*>, expected: Boolean) {
            assertThat(SchemaTypeHelpers.isObject(schema)).isEqualTo(expected)
        }
    }

    companion object {
        @JvmStatic
        fun isNumberProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("NumberSchema", NumberSchema(), true),
            Arguments.of("IntegerSchema", IntegerSchema(), true),
            Arguments.of("Schema with type 'number'", Schema<Any>().apply { type = "number" }, true),
            Arguments.of("Schema with type 'integer'", Schema<Any>().apply { type = "integer" }, true),
            Arguments.of("Schema with types containing 'number'", Schema<Any>().apply { types = setOf("number") }, true),
            Arguments.of("Schema with types containing 'integer'", Schema<Any>().apply { types = setOf("integer") }, true),
            Arguments.of("StringSchema", StringSchema(), false),
            Arguments.of("BooleanSchema", BooleanSchema(), false),
            Arguments.of("ObjectSchema", ObjectSchema(), false)
        )

        @JvmStatic
        fun isStringProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("StringSchema", StringSchema(), true),
            Arguments.of("Schema with type 'string'", Schema<Any>().apply { type = "string" }, true),
            Arguments.of("Schema with types containing 'string'", Schema<Any>().apply { types = setOf("string") }, true),
            Arguments.of("NumberSchema", NumberSchema(), false),
            Arguments.of("BooleanSchema", BooleanSchema(), false),
            Arguments.of("ObjectSchema", ObjectSchema(), false)
        )

        @JvmStatic
        fun isIntegerProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("IntegerSchema", IntegerSchema(), true),
            Arguments.of("Schema with type 'integer'", Schema<Any>().apply { type = "integer" }, true),
            Arguments.of("Schema with types containing 'integer'", Schema<Any>().apply { types = setOf("integer") }, true),
            Arguments.of("NumberSchema", NumberSchema(), false),
            Arguments.of("StringSchema", StringSchema(), false)
        )

        @JvmStatic
        fun isArrayProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("ArraySchema", ArraySchema(), true),
            Arguments.of("Schema with type 'array'", Schema<Any>().apply { type = "array" }, true),
            Arguments.of("Schema with types containing 'array'", Schema<Any>().apply { types = setOf("array") }, true),
            Arguments.of("ObjectSchema", ObjectSchema(), false),
            Arguments.of("StringSchema", StringSchema(), false)
        )

        @JvmStatic
        fun isBooleanProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("BooleanSchema", BooleanSchema(), true),
            Arguments.of("Schema with type 'boolean'", Schema<Any>().apply { type = "boolean" }, true),
            Arguments.of("Schema with types containing 'boolean'", Schema<Any>().apply { types = setOf("boolean") }, true),
            Arguments.of("StringSchema", StringSchema(), false),
            Arguments.of("NumberSchema", NumberSchema(), false)
        )

        @JvmStatic
        fun isObjectProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("ObjectSchema", ObjectSchema(), true),
            Arguments.of("Schema with type 'object'", Schema<Any>().apply { type = "object" }, true),
            Arguments.of("Schema with types containing 'object'", Schema<Any>().apply { types = setOf("object") }, true),
            Arguments.of("StringSchema", StringSchema(), false),
            Arguments.of("ArraySchema", ArraySchema(), false)
        )
    }
}
