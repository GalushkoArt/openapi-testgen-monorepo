package art.galushko.openapi.testgen.generator.template

import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Epic("Generator")
@Feature("TemplateArtifactGeneratorOptions")
@DisplayName("TemplateArtifactGeneratorOptions Tests")
class TemplateArtifactGeneratorOptionsTest {

    @Nested
    @Story("Data Class Validation")
    @DisplayName("Data Class Validation (init block)")
    inner class DataClassValidationTests {

        @Test
        @DisplayName("should throw when templateSet is blank")
        @Description("Verifies that blank templateSet throws IllegalArgumentException")
        fun shouldThrowWhenTemplateSetIsBlank() {
            assertThatThrownBy {
                TemplateArtifactGeneratorOptions(templateSet = "   ")
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("templateSet cannot be blank")
        }

        @Test
        @DisplayName("should throw when outputFileExtension is blank")
        @Description("Verifies that blank outputFileExtension throws IllegalArgumentException")
        fun shouldThrowWhenOutputFileExtensionIsBlank() {
            assertThatThrownBy {
                TemplateArtifactGeneratorOptions(outputFileExtension = "")
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("outputFileExtension cannot be blank")
        }

        @Test
        @DisplayName("should throw when outputFileNamePattern is blank")
        @Description("Verifies that blank outputFileNamePattern throws IllegalArgumentException")
        fun shouldThrowWhenOutputFileNamePatternIsBlank() {
            assertThatThrownBy {
                TemplateArtifactGeneratorOptions(outputFileNamePattern = "   ")
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("outputFileNamePattern cannot be blank")
        }

        @Test
        @DisplayName("should throw when classTemplatePath is blank")
        @Description("Verifies that blank classTemplatePath throws IllegalArgumentException")
        fun shouldThrowWhenClassTemplatePathIsBlank() {
            assertThatThrownBy {
                TemplateArtifactGeneratorOptions(classTemplatePath = "")
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("classTemplatePath cannot be blank")
        }

        @Test
        @DisplayName("should construct successfully with valid options")
        @Description("Verifies that valid options construct successfully")
        fun shouldConstructWithValidOptions() {
            val options = TemplateArtifactGeneratorOptions(
                templateSet = "custom-template",
                outputFileExtension = "java",
                outputFileNamePattern = "{{className}}.{{outputFileExtension}}",
                classTemplatePath = "templates/custom/class.mustache",
            )

            assertThat(options.templateSet).isEqualTo("custom-template")
            assertThat(options.outputFileExtension).isEqualTo("java")
            assertThat(options.outputFileNamePattern).isEqualTo("{{className}}.{{outputFileExtension}}")
            assertThat(options.classTemplatePath).isEqualTo("templates/custom/class.mustache")
        }

        @Test
        @DisplayName("should use default values when not specified")
        @Description("Verifies that default values are applied correctly")
        fun shouldUseDefaultValues() {
            val options = TemplateArtifactGeneratorOptions()

            assertThat(options.templateSet).isEqualTo("restassured-java")
            assertThat(options.classTemplatePath).isEqualTo("templates/{{templateSet}}/class.mustache")
            assertThat(options.customTemplateDir).isNull()
            assertThat(options.templateVariables).isEmpty()
            assertThat(options.outputFileExtension).isEqualTo("java")
            assertThat(options.outputFileNamePattern).isEqualTo("{{className}}.{{outputFileExtension}}")
            assertThat(options.writeMode).isEqualTo(WriteMode.OVERWRITE)
            assertThat(options.fileHeaderComment).isNull()
        }
    }

    @Nested
    @Story("Template Path Resolution")
    @DisplayName("resolveClassTemplatePath()")
    inner class ResolveClassTemplatePathTests {

        @Test
        @DisplayName("should resolve templateSet placeholder")
        @Description("Verifies that {{templateSet}} placeholder is replaced with actual value")
        fun shouldResolveTemplateSetPlaceholder() {
            val options = TemplateArtifactGeneratorOptions(
                templateSet = "restassured-kotlin",
                classTemplatePath = "templates/{{templateSet}}/class.mustache",
            )

            val result = options.resolveClassTemplatePath()

            assertThat(result).isEqualTo("templates/restassured-kotlin/class.mustache")
        }

        @Test
        @DisplayName("should return path as-is when no placeholder")
        @Description("Verifies that paths without placeholder are returned unchanged")
        fun shouldReturnPathAsIsWhenNoPlaceholder() {
            val options = TemplateArtifactGeneratorOptions(
                classTemplatePath = "custom/path/template.mustache",
            )

            val result = options.resolveClassTemplatePath()

            assertThat(result).isEqualTo("custom/path/template.mustache")
        }
    }

    @Nested
    @Story("Output File Name Resolution")
    @DisplayName("resolveOutputFileName()")
    inner class ResolveOutputFileNameTests {

        @Test
        @DisplayName("should resolve className and extension placeholders")
        @Description("Verifies that both placeholders are replaced correctly")
        fun shouldResolveClassNameAndExtensionPlaceholders() {
            val options = TemplateArtifactGeneratorOptions(
                outputFileExtension = "java",
                outputFileNamePattern = "{{className}}.{{outputFileExtension}}",
            )

            val result = options.resolveOutputFileName("UserApiTest")

            assertThat(result).isEqualTo("UserApiTest.java")
        }

        @Test
        @DisplayName("should handle custom pattern")
        @Description("Verifies that custom patterns work correctly")
        fun shouldHandleCustomPattern() {
            val options = TemplateArtifactGeneratorOptions(
                outputFileExtension = "kt",
                outputFileNamePattern = "Generated{{className}}.{{outputFileExtension}}",
            )

            val result = options.resolveOutputFileName("OrderTest")

            assertThat(result).isEqualTo("GeneratedOrderTest.kt")
        }
    }

    @Nested
    @Story("Options Transformation")
    @DisplayName("transformAndValidateTemplateOptions()")
    inner class TransformAndValidateOptionsTests {

        @Test
        @DisplayName("should use default templateSet when not provided")
        @Description("Verifies that 'restassured-java' is used as default templateSet")
        fun shouldUseDefaultTemplateSet() {
            val options = transformAndValidateTemplateOptions(emptyMap())

            assertThat(options.templateSet).isEqualTo("restassured-java")
        }

        @Test
        @DisplayName("should use 'custom' templateSet when classTemplatePath provided without templateSet")
        @Description("Verifies fallback to 'custom' when custom template path is provided")
        fun shouldUseFallbackTemplateSetWhenClassTemplatePathProvided() {
            val options = transformAndValidateTemplateOptions(
                mapOf(
                    "classTemplatePath" to "my/custom/template.mustache",
                    "outputFileExtension" to "java", // Required since 'custom' templateSet cannot infer extension
                )
            )

            assertThat(options.templateSet).isEqualTo("custom")
            assertThat(options.classTemplatePath).isEqualTo("my/custom/template.mustache")
        }

        @Test
        @DisplayName("should parse templateVariables from Map")
        @Description("Verifies that templateVariables map is parsed correctly")
        fun shouldParseTemplateVariablesFromMap() {
            val options = transformAndValidateTemplateOptions(
                mapOf(
                    "templateVariables" to mapOf(
                        "package" to "com.example.generated",
                        "baseUrl" to "http://localhost:8080",
                        "springBootTest" to true,
                    )
                )
            )

            assertThat(options.templateVariables).containsEntry("package", "com.example.generated")
            assertThat(options.templateVariables).containsEntry("baseUrl", "http://localhost:8080")
            assertThat(options.templateVariables).containsEntry("springBootTest", true)
        }

        @Test
        @DisplayName("should throw for invalid templateVariables type")
        @Description("Verifies that non-map templateVariables throws error")
        fun shouldThrowForInvalidTemplateVariablesType() {
            assertThatThrownBy {
                transformAndValidateTemplateOptions(
                    mapOf("templateVariables" to "invalid")
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Invalid 'templateVariables' option")
        }

        fun extensionInferenceProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("kotlin template", "restassured-kotlin", "kt"),
            Arguments.of("java template", "restassured-java", "java"),
            Arguments.of("kotlin in name (case insensitive)", "my-Kotlin-template", "kt"),
            Arguments.of("java in name (case insensitive)", "custom-Java-set", "java"),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("extensionInferenceProvider")
        @DisplayName("should infer outputFileExtension from templateSet")
        fun shouldInferOutputFileExtensionFromTemplateSet(
            scenario: String,
            templateSet: String,
            expectedExtension: String,
        ) {
            val options = transformAndValidateTemplateOptions(
                mapOf("templateSet" to templateSet)
            )

            assertThat(options.outputFileExtension).isEqualTo(expectedExtension)
        }

        @Test
        @DisplayName("should throw when extension cannot be inferred")
        @Description("Verifies that unknown templateSet without explicit extension throws error")
        fun shouldThrowWhenExtensionCannotBeInferred() {
            assertThatThrownBy {
                transformAndValidateTemplateOptions(
                    mapOf("templateSet" to "unknown-template")
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Cannot identify file extension")
        }

        @Test
        @DisplayName("should use explicit outputFileExtension over inference")
        @Description("Verifies that explicit extension takes precedence")
        fun shouldUseExplicitOutputFileExtension() {
            val options = transformAndValidateTemplateOptions(
                mapOf(
                    "templateSet" to "restassured-java",
                    "outputFileExtension" to "txt",
                )
            )

            assertThat(options.outputFileExtension).isEqualTo("txt")
        }

        @Test
        @DisplayName("should parse valid writeMode enum")
        @Description("Verifies that valid writeMode string is parsed correctly")
        fun shouldParseValidWriteMode() {
            val options = transformAndValidateTemplateOptions(
                mapOf("writeMode" to "SKIP_IF_EXISTS")
            )

            assertThat(options.writeMode).isEqualTo(WriteMode.SKIP_IF_EXISTS)
        }

        @Test
        @DisplayName("should throw for invalid writeMode")
        @Description("Verifies that invalid writeMode string throws error")
        fun shouldThrowForInvalidWriteMode() {
            assertThatThrownBy {
                transformAndValidateTemplateOptions(
                    mapOf("writeMode" to "INVALID_MODE")
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Invalid 'writeMode' option")
        }

        @Test
        @DisplayName("should use default writeMode when not provided")
        @Description("Verifies that OVERWRITE is used as default writeMode")
        fun shouldUseDefaultWriteMode() {
            val options = transformAndValidateTemplateOptions(emptyMap())

            assertThat(options.writeMode).isEqualTo(WriteMode.OVERWRITE)
        }

        @Test
        @DisplayName("should parse fileHeaderComment")
        @Description("Verifies that fileHeaderComment is parsed correctly")
        fun shouldParseFileHeaderComment() {
            val comment = "// Generated by OpenAPI Test Generator"
            val options = transformAndValidateTemplateOptions(
                mapOf("fileHeaderComment" to comment)
            )

            assertThat(options.fileHeaderComment).isEqualTo(comment)
        }

        @Test
        @DisplayName("should allow null fileHeaderComment")
        @Description("Verifies that fileHeaderComment can be null")
        fun shouldAllowNullFileHeaderComment() {
            val options = transformAndValidateTemplateOptions(emptyMap())

            assertThat(options.fileHeaderComment).isNull()
        }

        @Test
        @DisplayName("should parse customTemplateDir")
        @Description("Verifies that customTemplateDir is parsed correctly")
        fun shouldParseCustomTemplateDir() {
            val options = transformAndValidateTemplateOptions(
                mapOf("customTemplateDir" to "/path/to/templates")
            )

            assertThat(options.customTemplateDir).isEqualTo("/path/to/templates")
        }

        @Test
        @DisplayName("should parse outputFileNamePattern")
        @Description("Verifies that custom outputFileNamePattern is parsed correctly")
        fun shouldParseOutputFileNamePattern() {
            val options = transformAndValidateTemplateOptions(
                mapOf("outputFileNamePattern" to "Test{{className}}.{{outputFileExtension}}")
            )

            assertThat(options.outputFileNamePattern).isEqualTo("Test{{className}}.{{outputFileExtension}}")
        }
    }
}

