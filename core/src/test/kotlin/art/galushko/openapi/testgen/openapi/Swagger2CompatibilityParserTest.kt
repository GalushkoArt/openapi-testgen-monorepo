package art.galushko.openapi.testgen.openapi

import art.galushko.openapi.testgen.config.ParserSettings
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.parser.util.DeserializationUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import io.swagger.parser.util.DeserializationUtils as V1DeserializationUtils

@DisplayName("Swagger 2.0 parser compatibility")
class Swagger2CompatibilityParserTest {
    private val originalOptions = snapshotOptions(DeserializationUtils.getOptions())
    private val originalV1Options = snapshotV1Options(V1DeserializationUtils.getOptions())

    @AfterEach
    fun restoreParserOptions() {
        restoreOptions(originalOptions)
        restoreV1Options(originalV1Options)
    }

    @Test
    @DisplayName("should document that OpenAPIV3Parser does not directly return a Swagger 2.0 model")
    fun directOpenApiV3ParserShouldNotReturnSwagger2Model() {
        val parsed = OpenAPIV3Parser().readLocation(resourcePath("minimal.swagger.yaml"), null, defaultParseOptions())

        assertThat(parsed.openAPI).isNull()
    }

    @Test
    @DisplayName("should parse minimal Swagger 2.0 YAML through the adapter")
    fun shouldParseMinimalSwaggerYaml() {
        val openAPI = OpenApiSpecParser.parseOpenApi(resourcePath("minimal.swagger.yaml"))

        assertSoftly { softly ->
            softly.assertThat(openAPI.openapi).isEqualTo("3.0.1")
            softly.assertThat(openAPI.info.title).isEqualTo("Swagger Minimal API")
            softly.assertThat(openAPI.servers.map { it.url }).containsExactly("https://api.example.test/v1")
            softly.assertThat(openAPI.paths.keys).containsExactly("/pets")
            softly.assertThat(openAPI.components).isNotNull
            softly.assertThat(openAPI.components.schemas).isEmpty()
            softly.assertThat(openAPI.components.parameters).isEmpty()
            softly.assertThat(openAPI.components.requestBodies).isEmpty()
            softly.assertThat(openAPI.components.securitySchemes).isEmpty()
            softly.assertThat(openAPI.extensions).containsEntry("x-root-note", "parser-fixture")
            softly.assertThat(openAPI.extensions).containsEntry("x-original-swagger-version", "2.0")
        }

        val operation = openAPI.path("/pets").operation(PathItem.HttpMethod.GET)
        assertSoftly { softly ->
            softly.assertThat(operation.operationId).isEqualTo("listPets")
            softly.assertThat(operation.extensions).containsEntry("x-operation-note", "parser-fixture")
            softly.assertThat(operation.parameters).extracting<String> { it.name }.containsExactly("limit")
            softly.assertThat(operation.response("200").content.keys).containsExactly("application/json")
        }
    }

    @Test
    @DisplayName("should parse minimal Swagger 2.0 JSON through the adapter")
    fun shouldParseMinimalSwaggerJson() {
        val openAPI = OpenApiSpecParser.parseOpenApi(resourcePath("minimal.swagger.json"))

        assertSoftly { softly ->
            softly.assertThat(openAPI.openapi).isEqualTo("3.0.1")
            softly.assertThat(openAPI.info.title).isEqualTo("Swagger Minimal JSON API")
            softly.assertThat(openAPI.servers.map { it.url }).containsExactly("https://json.example.test/api")
            softly.assertThat(openAPI.paths.keys).containsExactly("/status")
            softly.assertThat(openAPI.path("/status").operation(PathItem.HttpMethod.GET).operationId).isEqualTo("getStatus")
        }
    }

    @Test
    @DisplayName("should merge path-level parameters and map Swagger collectionFormat values")
    fun shouldNormalizeParametersAndCollectionFormats() {
        val openAPI = OpenApiSpecParser.parseOpenApi(resourcePath("parameters.swagger.yaml"))
        val operation = openAPI.path("/pets/{petId}").operation(PathItem.HttpMethod.GET)
        val parameters = checkNotNull(operation.parameters)

        assertThat(parameters).extracting<String> { it.name }
            .containsExactly("petId", "tags", "colors", "X-Correlation-Id")

        val petId = parameters.parameter("petId")
        val tags = parameters.parameter("tags")
        val colors = parameters.parameter("colors")
        val correlation = parameters.parameter("X-Correlation-Id")

        assertSoftly { softly ->
            softly.assertThat(petId.`in`).isEqualTo("path")
            softly.assertThat(petId.required).isTrue
            softly.assertThat(petId.schema.minLength).isEqualTo(3)

            softly.assertThat(tags.`in`).isEqualTo("query")
            softly.assertThat(tags.style).isEqualTo(Parameter.StyleEnum.FORM)
            softly.assertThat(tags.explode).isTrue
            softly.assertThat(tags.schema.items.enum).containsExactly("cat", "dog")

            softly.assertThat(colors.style).isEqualTo(Parameter.StyleEnum.PIPEDELIMITED)

            softly.assertThat(correlation.`in`).isEqualTo("header")
            softly.assertThat(correlation.schema.pattern).isEqualTo("^[A-Z]{3}$")
        }
    }

    @Test
    @DisplayName("should convert body parameters and operation-level consumes")
    fun shouldNormalizeBodyParameters() {
        val openAPI = OpenApiSpecParser.parseOpenApi(resourcePath("request-body.swagger.yaml"))
        val operation = openAPI.path("/pets").operation(PathItem.HttpMethod.POST)
        val requestBody = operation.requestBody

        assertSoftly { softly ->
            softly.assertThat(requestBody.required).isTrue
            softly.assertThat(requestBody.content.keys).containsExactly("application/vnd.pet+json")
            softly.assertThat(openAPI.components.schemas.keys).contains("PetCreate", "Pet")
            softly.assertThat(operation.response("201").content.keys).containsExactly("application/json")
            softly.assertThat(operation.response("201").content["application/json"]?.example).isEqualTo(mapOf("id" to "pet-1", "name" to "Milo"))
        }
    }

    @Test
    @DisplayName("should convert formData parameters to request bodies")
    fun shouldNormalizeFormDataParameters() {
        val openAPI = OpenApiSpecParser.parseOpenApi(resourcePath("form-data.swagger.yaml"))
        val sessionOperation = openAPI.path("/sessions").operation(PathItem.HttpMethod.POST)
        val uploadOperation = openAPI.path("/uploads").operation(PathItem.HttpMethod.POST)
        val formSchema = checkNotNull(sessionOperation.requestBody.content["application/x-www-form-urlencoded"]).schema

        assertSoftly { softly ->
            softly.assertThat(sessionOperation.requestBody.required).isTrue
            softly.assertThat(formSchema.properties.keys).containsExactly("username", "password", "remember")
            softly.assertThat(formSchema.required).containsExactlyInAnyOrder("username", "password")
            softly.assertThat(uploadOperation.requestBody.content.keys).containsExactly("multipart/form-data")
            softly.assertThat(uploadOperation.requestBody.required).isTrue
        }
    }

    @Test
    @DisplayName("should convert definitions, response schemas, and security definitions")
    fun shouldNormalizeDefinitionsResponsesAndSecurityDefinitions() {
        val definitionsOpenApi = OpenApiSpecParser.parseOpenApi(resourcePath("definitions-composition.swagger.yaml"))
        val secureOpenApi = OpenApiSpecParser.parseOpenApi(resourcePath("security.swagger.yaml"))

        assertSoftly { softly ->
            softly.assertThat(definitionsOpenApi.components.schemas.keys)
                .contains("BasePet", "Pet", "PetResponse", "Error")
            softly.assertThat(definitionsOpenApi.path("/owners/{ownerId}/pets").operation(PathItem.HttpMethod.POST).response("201").content.keys)
                .containsExactly("application/json")
        }

        val schemes = secureOpenApi.components.securitySchemes
        val operationSecurity = secureOpenApi.path("/secure").operation(PathItem.HttpMethod.GET).security
        assertSoftly { softly ->
            softly.assertThat(schemes["ApiKeyAuth"]?.type).isEqualTo(SecurityScheme.Type.APIKEY)
            softly.assertThat(schemes["ApiKeyAuth"]?.`in`).isEqualTo(SecurityScheme.In.HEADER)
            softly.assertThat(schemes["ApiKeyAuth"]?.name).isEqualTo("X-API-Key")
            softly.assertThat(schemes["BasicAuth"]?.type).isEqualTo(SecurityScheme.Type.HTTP)
            softly.assertThat(schemes["BasicAuth"]?.scheme).isEqualTo("basic")
            softly.assertThat(schemes["OAuthAuth"]?.type).isEqualTo(SecurityScheme.Type.OAUTH2)
            softly.assertThat(schemes["OAuthAuth"]?.flows?.authorizationCode?.scopes?.keys).containsExactly("read:pets", "write:pets")
            softly.assertThat(secureOpenApi.security.first().keys).containsExactly("ApiKeyAuth")
            softly.assertThat(operationSecurity.first()["OAuthAuth"]).containsExactly("read:pets")
        }
    }

    @Test
    @DisplayName("should restore parser options after Swagger 2.0 parse")
    fun shouldRestoreParserOptionsAfterSwaggerParse() {
        val customOptions = OptionsSnapshot(
            maxYamlDepth = 111,
            maxYamlReferences = 222L,
            validateYamlInput = false,
            yamlCycleCheck = false,
            maxYamlCodePoints = 3_333_333,
            maxYamlAliasesForCollections = 55,
            yamlAllowRecursiveKeys = true,
        )
        restoreOptions(customOptions)

        OpenApiSpecParser.parseOpenApi(
            inputSpec = resourcePath("minimal.swagger.yaml"),
            parserSettings = ParserSettings(
                yamlCodePointLimit = 7_777_777,
                yamlMaxAliasesForCollections = 123,
                yamlAllowRecursiveKeys = false,
                yamlNestingDepthLimit = 222,
            ),
        )

        assertThat(snapshotOptions(DeserializationUtils.getOptions())).isEqualTo(customOptions)
    }

    @Test
    @DisplayName("should apply configured SnakeYAML limits to the v1 Swagger 2.0 parser")
    fun shouldConfigureV1ParserOptionsWhileParsing() {
        val v1Options = V1DeserializationUtils.getOptions()

        OpenApiSpecParser.withConfiguredParserOptions(
            ParserSettings(
                yamlCodePointLimit = 7_777_777,
                yamlMaxAliasesForCollections = 123,
                yamlAllowRecursiveKeys = true,
                yamlNestingDepthLimit = 222,
            ),
        ) {
            assertSoftly { softly ->
                softly.assertThat(v1Options.maxYamlCodePoints).isEqualTo(7_777_777)
                softly.assertThat(v1Options.maxYamlAliasesForCollections).isEqualTo(123)
                softly.assertThat(v1Options.isYamlAllowRecursiveKeys).isTrue()
                softly.assertThat(v1Options.maxYamlDepth).isEqualTo(222)
            }
        }
    }

    @Test
    @DisplayName("should restore v1 parser options after Swagger 2.0 parsing")
    fun shouldRestoreV1ParserOptionsAfterSwaggerParse() {
        val customOptions = OptionsSnapshot(
            maxYamlDepth = 111,
            maxYamlReferences = 222L,
            validateYamlInput = false,
            yamlCycleCheck = false,
            maxYamlCodePoints = 3_333_333,
            maxYamlAliasesForCollections = 55,
            yamlAllowRecursiveKeys = true,
        )
        restoreV1Options(customOptions)

        OpenApiSpecParser.parseOpenApi(
            inputSpec = resourcePath("minimal.swagger.yaml"),
            parserSettings = ParserSettings(
                yamlCodePointLimit = 7_777_777,
                yamlMaxAliasesForCollections = 123,
                yamlAllowRecursiveKeys = false,
                yamlNestingDepthLimit = 222,
            ),
        )

        assertThat(snapshotV1Options(V1DeserializationUtils.getOptions())).isEqualTo(customOptions)
    }

    @Test
    @DisplayName("should parse a Swagger 2.0 spec above the default YAML size limit when the configured limit is raised")
    fun shouldParseLargeSwagger2SpecWithRaisedCodePointLimit(@TempDir tmp: Path) {
        val spec = writeLargeSwagger2Spec(tmp)

        val openAPI = OpenApiSpecParser.parseOpenApi(
            inputSpec = spec.toString(),
            parserSettings = ParserSettings(yamlCodePointLimit = 16_000_000),
        )

        assertThat(openAPI.paths).containsOnlyKeys("/ping")
    }

    @Test
    @DisplayName("should fail to parse a Swagger 2.0 spec above the default YAML size limit")
    fun shouldFailOnLargeSwagger2SpecWithDefaultLimits(@TempDir tmp: Path) {
        val spec = writeLargeSwagger2Spec(tmp)

        assertThatThrownBy { OpenApiSpecParser.parseOpenApi(spec.toString()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageStartingWith("Parsed unknown OpenAPI/Swagger version model is null")
    }

    @Test
    @DisplayName("should detect Swagger 2.0 when the version scalar is unquoted")
    fun shouldDetectSwagger2WhenVersionScalarIsUnquoted(@TempDir tmp: Path) {
        val unquoted = tmp.resolve("unquoted-version.yaml")
        Files.writeString(
            unquoted,
            """
                swagger: 2.0
                info:
                  title: Unquoted Version API
                  version: "1.0"
                host: api.example.test
                basePath: /v1
                schemes:
                  - https
                paths:
                  /pets:
                    get:
                      operationId: listPets
                      responses:
                        "200":
                          description: OK
            """.trimIndent(),
        )

        val openAPI = OpenApiSpecParser.parseOpenApi(unquoted.toString())

        assertSoftly { softly ->
            softly.assertThat(openAPI.openapi).isEqualTo("3.0.1")
            softly.assertThat(openAPI.info.title).isEqualTo("Unquoted Version API")
            softly.assertThat(openAPI.paths.keys).containsExactly("/pets")
            softly.assertThat(openAPI.extensions).containsEntry("x-original-swagger-version", "2.0")
        }
    }

    @Test
    @DisplayName("should fail clearly for unsupported Swagger versions")
    fun shouldFailClearlyForUnsupportedSwaggerVersions(@TempDir tmp: Path) {
        val unsupported = tmp.resolve("swagger-12.yaml")
        Files.writeString(
            unsupported,
            """
                swagger: "1.2"
                info:
                  title: Old Swagger
                  version: "1.0"
                paths: {}
            """.trimIndent(),
        )

        assertThatThrownBy { OpenApiSpecParser.parseOpenApi(unsupported.toString()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unsupported Swagger version '1.2'")
            .hasMessageContaining("Only Swagger 2.0 is supported")
    }

    private fun resourcePath(name: String): String =
        Path.of(requireNotNull(this::class.java.getResource("/oas/swagger2/$name")).toURI()).toString()

    private fun defaultParseOptions(): ParseOptions = ParseOptions().apply {
        isResolveFully = true
        isResolveCombinators = false
        isResolveRequestBody = true
        isResolveResponses = true
    }

    private fun PathItem.operation(method: PathItem.HttpMethod) =
        checkNotNull(readOperationsMap()[method]) { "Operation $method not found" }

    private fun OpenAPI.path(path: String): PathItem =
        checkNotNull(paths[path]) { "Path $path not found" }

    private fun Operation.response(statusCode: String) =
        checkNotNull(responses[statusCode]) { "Response $statusCode not found" }

    private fun Iterable<Parameter>.parameter(name: String): Parameter =
        first { it.name == name }

    private data class OptionsSnapshot(
        val maxYamlDepth: Int?,
        val maxYamlReferences: Long?,
        val validateYamlInput: Boolean,
        val yamlCycleCheck: Boolean,
        val maxYamlCodePoints: Int?,
        val maxYamlAliasesForCollections: Int?,
        val yamlAllowRecursiveKeys: Boolean,
    )

    private fun snapshotOptions(options: DeserializationUtils.Options): OptionsSnapshot =
        OptionsSnapshot(
            maxYamlDepth = options.maxYamlDepth,
            maxYamlReferences = options.maxYamlReferences,
            validateYamlInput = options.isValidateYamlInput,
            yamlCycleCheck = options.isYamlCycleCheck,
            maxYamlCodePoints = options.maxYamlCodePoints,
            maxYamlAliasesForCollections = options.maxYamlAliasesForCollections,
            yamlAllowRecursiveKeys = options.isYamlAllowRecursiveKeys,
        )

    private fun restoreOptions(snapshot: OptionsSnapshot) {
        val options = DeserializationUtils.getOptions()
        options.maxYamlDepth = snapshot.maxYamlDepth
        options.maxYamlReferences = snapshot.maxYamlReferences
        options.isValidateYamlInput = snapshot.validateYamlInput
        options.isYamlCycleCheck = snapshot.yamlCycleCheck
        options.maxYamlCodePoints = snapshot.maxYamlCodePoints
        options.maxYamlAliasesForCollections = snapshot.maxYamlAliasesForCollections
        options.isYamlAllowRecursiveKeys = snapshot.yamlAllowRecursiveKeys
    }

    private fun snapshotV1Options(options: V1DeserializationUtils.Options): OptionsSnapshot =
        OptionsSnapshot(
            maxYamlDepth = options.maxYamlDepth,
            maxYamlReferences = options.maxYamlReferences,
            validateYamlInput = options.isValidateYamlInput,
            yamlCycleCheck = options.isYamlCycleCheck,
            maxYamlCodePoints = options.maxYamlCodePoints,
            maxYamlAliasesForCollections = options.maxYamlAliasesForCollections,
            yamlAllowRecursiveKeys = options.isYamlAllowRecursiveKeys,
        )

    private fun restoreV1Options(snapshot: OptionsSnapshot) {
        val options = V1DeserializationUtils.getOptions()
        options.maxYamlDepth = snapshot.maxYamlDepth
        options.maxYamlReferences = snapshot.maxYamlReferences
        options.isValidateYamlInput = snapshot.validateYamlInput
        options.isYamlCycleCheck = snapshot.yamlCycleCheck
        options.maxYamlCodePoints = snapshot.maxYamlCodePoints
        options.maxYamlAliasesForCollections = snapshot.maxYamlAliasesForCollections
        options.isYamlAllowRecursiveKeys = snapshot.yamlAllowRecursiveKeys
    }

    private fun writeLargeSwagger2Spec(tmp: Path): Path {
        val padding = "x".repeat(LARGE_SPEC_PADDING_CODE_POINTS)
        val spec = tmp.resolve("large.swagger.yaml")
        Files.writeString(
            spec,
            """
                swagger: "2.0"
                info:
                  title: Large API
                  version: "1.0"
                  description: $padding
                paths:
                  /ping:
                    get:
                      responses:
                        "200":
                          description: OK
            """.trimIndent(),
        )
        return spec
    }

    private companion object {
        /** Above SnakeYAML's default 3 MiB code-point limit so default-limit parsing fails. */
        private const val LARGE_SPEC_PADDING_CODE_POINTS = 4_000_000
    }
}
