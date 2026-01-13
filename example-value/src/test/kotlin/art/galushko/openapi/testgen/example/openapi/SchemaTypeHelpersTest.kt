package art.galushko.openapi.testgen.example.openapi

import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
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
    @DisplayName("tryGetResponseFromRef")
    inner class TryGetResponseFromRefTest {

        @Test
        @DisplayName("should return null when operation has no responses")
        fun shouldReturnNullWhenNoResponses() {
            val operation = Operation()
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.tryGetResponseFromRef(operation, openAPI, 200)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("should return null when status code not found")
        fun shouldReturnNullWhenStatusCodeNotFound() {
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("201", ApiResponse().description("Created"))
                }
            }
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.tryGetResponseFromRef(operation, openAPI, 200)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("should return direct response when no ref")
        fun shouldReturnDirectResponseWhenNoRef() {
            val response = ApiResponse().description("OK")
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("200", response)
                }
            }
            val openAPI = OpenAPI()

            val result = SchemaTypeHelpers.tryGetResponseFromRef(operation, openAPI, 200)

            assertThat(result).isSameAs(response)
        }

        @Test
        @DisplayName("should dereference response ref")
        fun shouldDereferenceResponseRef() {
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

            val result = SchemaTypeHelpers.tryGetResponseFromRef(operation, openAPI, 200)

            assertThat(result).isSameAs(referencedResponse)
        }

        @Test
        @DisplayName("should return original response when ref not found")
        fun shouldReturnOriginalResponseWhenRefNotFound() {
            val refResponse = ApiResponse().apply {
                `$ref` = "#/components/responses/NonExistent"
            }
            val operation = Operation().apply {
                responses = ApiResponses().apply {
                    addApiResponse("200", refResponse)
                }
            }
            val openAPI = OpenAPI().apply {
                components = Components().apply {
                    addResponses("SomeOtherResponse", ApiResponse().description("Other"))
                }
            }

            val result = SchemaTypeHelpers.tryGetResponseFromRef(operation, openAPI, 200)

            assertThat(result).isSameAs(refResponse)
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
