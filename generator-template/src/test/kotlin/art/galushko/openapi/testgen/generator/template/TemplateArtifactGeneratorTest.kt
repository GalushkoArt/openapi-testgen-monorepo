package art.galushko.openapi.testgen.generator.template

import art.galushko.openapi.testgen.model.KeyValuePair
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.TestSuite
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Path
import java.util.stream.Stream

@Epic("Generator")
@Feature("TemplateArtifactGenerator")
@DisplayName("TemplateArtifactGenerator Tests")
class TemplateArtifactGeneratorTest {

    private val basicTestSuite = TestSuite(
        path = "/users",
        method = "POST",
        operationName = "createUser",
        testCases = listOf(
            TestCase(
                name = "Valid Case",
                method = "POST",
                path = "/users",
                expectedStatusCode = 201,
                body = mapOf("name" to "John"),
            ),
        )
    )

    @Nested
    @Story("Class Name Generation")
    @DisplayName("Class Name Generation")
    inner class ClassNameGenerationTests {

        fun classNameProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "From operationName",
                TestSuite(path = "/users", method = "POST", operationName = "createUser", testCases = emptyList()),
                emptyMap<String, Any?>(),
                "CreateUserTest"
            ),
            Arguments.of(
                "From fallback operationName (method + path)",
                TestSuite(
                    path = "/users/{userId}/orders",
                    method = "GET",
                    operationName = "get /users/{userId}/orders",
                    testCases = emptyList()
                ),
                emptyMap<String, Any?>(),
                "GetUsersUserIdOrdersTest"
            ),
            Arguments.of(
                "From path when no operationName",
                TestSuite(path = "/users/{userId}/orders", method = "GET", operationName = null, testCases = emptyList()),
                emptyMap<String, Any?>(),
                "UsersOrdersTest"
            ),
            Arguments.of(
                "Path with only parameters falls back to ApiTest",
                TestSuite(path = "/{id}", method = "GET", operationName = null, testCases = emptyList()),
                emptyMap<String, Any?>(),
                "ApiTestTest"
            ),
            Arguments.of(
                "Empty path falls back to ApiTest",
                TestSuite(path = "/", method = "GET", operationName = null, testCases = emptyList()),
                emptyMap<String, Any?>(),
                "ApiTestTest"
            ),
            Arguments.of(
                "Custom classSuffix from templateVariables",
                TestSuite(path = "/users", method = "GET", operationName = "getUser", testCases = emptyList()),
                mapOf("classSuffix" to "Api"),
                "GetUserApi"
            ),
            Arguments.of(
                "Path with multiple segments",
                TestSuite(path = "/api/v1/users/{id}/profile", method = "GET", operationName = null, testCases = emptyList()),
                emptyMap<String, Any?>(),
                "ApiV1UsersProfileTest"
            ),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("classNameProvider")
        @DisplayName("should generate correct class name")
        fun shouldGenerateCorrectClassName(
            scenario: String,
            testSuite: TestSuite,
            templateVariables: Map<String, Any?>,
            expectedClassName: String,
            @TempDir tempDir: Path,
        ) {
            val options = mapOf(
                "templateSet" to "restassured-java",
                "templateVariables" to templateVariables,
            )
            val generator = TemplateArtifactGenerator(tempDir.toFile(), options)

            step("Generate tests for suite") {
                generator.generateTests(testSuite.copy(testCases = listOf(createMinimalTestCase())))
            }

            step("Verify generated file name contains expected class name") {
                val generatedFile = tempDir.resolve("$expectedClassName.java").toFile()
                assertThat(generatedFile).exists()
            }
        }
    }

    @Nested
    @Story("Method Name Generation")
    @DisplayName("Method Name Generation")
    inner class MethodNameGenerationTests {

        @Test
        @DisplayName("should convert test case name to camelCase method name")
        @Description("Verifies that spaces and special characters are handled correctly")
        fun shouldConvertTestCaseNameToCamelCase(@TempDir tempDir: Path) {
            val testCase = TestCase(
                name = "Invalid Body: Missing Field",
                method = "POST",
                path = "/users",
                expectedStatusCode = 400,
            )
            val testSuite = basicTestSuite.copy(testCases = listOf(testCase))

            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf("templateSet" to "restassured-java")
            )

            step("Generate tests") {
                generator.generateTests(testSuite)
            }

            step("Verify method name in generated file") {
                val generatedFile = tempDir.resolve("CreateUserTest.java").toFile()
                val content = generatedFile.readText()
                assertThat(content).contains("invalidBodyMissingField")
            }
        }

        @Test
        @DisplayName("should apply methodPrefix from templateVariables")
        @Description("Verifies that methodPrefix is prepended to method names")
        fun shouldApplyMethodPrefix(@TempDir tempDir: Path) {
            val testCase = createMinimalTestCase()
            val testSuite = basicTestSuite.copy(testCases = listOf(testCase))

            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf(
                    "templateSet" to "restassured-java",
                    "templateVariables" to mapOf("methodPrefix" to "test_"),
                )
            )

            step("Generate tests") {
                generator.generateTests(testSuite)
            }

            step("Verify method name has prefix") {
                val generatedFile = tempDir.resolve("CreateUserTest.java").toFile()
                val content = generatedFile.readText()
                assertThat(content).contains("test_validCase")
            }
        }

        @Test
        @DisplayName("should apply methodSuffix from templateVariables")
        @Description("Verifies that methodSuffix is appended to method names")
        fun shouldApplyMethodSuffix(@TempDir tempDir: Path) {
            val testCase = createMinimalTestCase()
            val testSuite = basicTestSuite.copy(testCases = listOf(testCase))

            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf(
                    "templateSet" to "restassured-java",
                    "templateVariables" to mapOf("methodSuffix" to "_negative"),
                )
            )

            step("Generate tests") {
                generator.generateTests(testSuite)
            }

            step("Verify method name has suffix") {
                val generatedFile = tempDir.resolve("CreateUserTest.java").toFile()
                val content = generatedFile.readText()
                assertThat(content).contains("validCase_negative")
            }
        }
    }

    @Nested
    @Story("Context Creation")
    @DisplayName("Context Creation")
    inner class ContextCreationTests {

        @Test
        @DisplayName("should include all test case fields in generated output")
        @Description("Verifies that headers, params, cookies, body are rendered")
        fun shouldIncludeAllTestCaseFields(@TempDir tempDir: Path) {
            val testCase = TestCase(
                name = "Full Test Case",
                method = "POST",
                path = "/users/{userId}",
                expectedStatusCode = 200,
                headers = listOf(
                    KeyValuePair("Authorization", "Bearer token123"),
                    KeyValuePair("X-Request-Id", "req-001"),
                ),
                pathParams = mapOf("userId" to "42"),
                queryParams = mapOf("include" to "profile"),
                cookie = listOf(KeyValuePair("session", "abc123")),
                body = mapOf("name" to "John", "email" to "john@example.com"),
            )
            val testSuite = basicTestSuite.copy(testCases = listOf(testCase))

            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf("templateSet" to "restassured-java")
            )

            step("Generate tests") {
                generator.generateTests(testSuite)
            }

            step("Verify generated content includes all fields") {
                val generatedFile = tempDir.resolve("CreateUserTest.java").toFile()
                val content = generatedFile.readText()

                assertThat(content).contains("Authorization")
                assertThat(content).contains("Bearer token123")
                assertThat(content).contains("X-Request-Id")
                assertThat(content).contains("pathParam(\"userId\"")
                assertThat(content).contains("queryParam(\"include\"")
            }
        }

        fun shouldHaveBodyProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("POST should have body", "POST", true),
            Arguments.of("PUT should have body", "PUT", true),
            Arguments.of("PATCH should have body", "PATCH", true),
            Arguments.of("GET should not have body", "GET", false),
            Arguments.of("DELETE should not have body", "DELETE", false),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("shouldHaveBodyProvider")
        @DisplayName("should compute shouldHaveBody correctly")
        fun shouldComputeShouldHaveBodyCorrectly(
            scenario: String,
            httpMethod: String,
            shouldHaveContentTypeHeader: Boolean,
            @TempDir tempDir: Path,
        ) {
            val testCase = TestCase(
                name = "Test",
                method = httpMethod,
                path = "/test",
                expectedStatusCode = 200,
            )
            val testSuite = TestSuite(
                path = "/test",
                method = httpMethod,
                operationName = "testOp",
                testCases = listOf(testCase),
            )

            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf("templateSet" to "restassured-java")
            )

            step("Generate tests") {
                generator.generateTests(testSuite)
            }

            step("Verify Content-Type header presence") {
                val generatedFile = tempDir.resolve("TestOpTest.java").toFile()
                val content = generatedFile.readText()

                if (shouldHaveContentTypeHeader) {
                    assertThat(content).contains("Content-Type")
                } else {
                    assertThat(content).doesNotContain("Content-Type")
                }
            }
        }

        @Test
        @DisplayName("should flatten query params with list values")
        @Description("Verifies that list query param values are flattened to multiple params")
        fun shouldFlattenQueryParamsWithListValues(@TempDir tempDir: Path) {
            val testCase = TestCase(
                name = "Test with list param",
                method = "GET",
                path = "/search",
                expectedStatusCode = 200,
                queryParams = mapOf("tags" to listOf("kotlin", "java", "scala")),
            )
            val testSuite = TestSuite(
                path = "/search",
                method = "GET",
                operationName = "search",
                testCases = listOf(testCase),
            )

            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf("templateSet" to "restassured-java")
            )

            step("Generate tests") {
                generator.generateTests(testSuite)
            }

            step("Verify all list values are present") {
                val generatedFile = tempDir.resolve("SearchTest.java").toFile()
                val content = generatedFile.readText()

                assertThat(content).contains("queryParam(\"tags\", \"kotlin\")")
                assertThat(content).contains("queryParam(\"tags\", \"java\")")
                assertThat(content).contains("queryParam(\"tags\", \"scala\")")
            }
        }

        @Test
        @DisplayName("should serialize body to JSON")
        @Description("Verifies that request body is serialized to JSON correctly")
        fun shouldSerializeBodyToJson(@TempDir tempDir: Path) {
            val testCase = TestCase(
                name = "Test with body",
                method = "POST",
                path = "/users",
                expectedStatusCode = 201,
                body = mapOf(
                    "name" to "John",
                    "age" to 30,
                    "active" to true,
                ),
            )
            val testSuite = basicTestSuite.copy(testCases = listOf(testCase))

            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf("templateSet" to "restassured-java")
            )

            step("Generate tests") {
                generator.generateTests(testSuite)
            }

            step("Verify body contains JSON") {
                val generatedFile = tempDir.resolve("CreateUserTest.java").toFile()
                val content = generatedFile.readText()

                assertThat(content).contains("requestBody")
                assertThat(content).contains("name")
                assertThat(content).contains("John")
            }
        }
    }

    @Nested
    @Story("WriteMode Behavior")
    @DisplayName("WriteMode Behavior")
    inner class WriteModeTests {

        @Test
        @DisplayName("should overwrite existing file when writeMode is OVERWRITE")
        @Description("Verifies that OVERWRITE mode replaces existing files")
        fun shouldOverwriteExistingFile(@TempDir tempDir: Path) {
            val existingFile = tempDir.resolve("CreateUserTest.java").toFile()
            existingFile.writeText("// Original content")

            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf(
                    "templateSet" to "restassured-java",
                    "writeMode" to "OVERWRITE",
                )
            )

            step("Generate tests") {
                generator.generateTests(basicTestSuite.copy(testCases = listOf(createMinimalTestCase())))
            }

            step("Verify file was overwritten") {
                val content = existingFile.readText()
                assertThat(content).doesNotContain("// Original content")
                assertThat(content).contains("class CreateUserTest")
            }
        }

        @Test
        @DisplayName("should skip existing file when writeMode is SKIP_IF_EXISTS")
        @Description("Verifies that SKIP_IF_EXISTS mode preserves existing files")
        fun shouldSkipExistingFile(@TempDir tempDir: Path) {
            val existingFile = tempDir.resolve("CreateUserTest.java").toFile()
            existingFile.writeText("// Original content")

            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf(
                    "templateSet" to "restassured-java",
                    "writeMode" to "SKIP_IF_EXISTS",
                )
            )

            step("Generate tests") {
                generator.generateTests(basicTestSuite.copy(testCases = listOf(createMinimalTestCase())))
            }

            step("Verify file was not overwritten") {
                val content = existingFile.readText()
                assertThat(content).isEqualTo("// Original content")
            }
        }

        @Test
        @DisplayName("should create new file when writeMode is SKIP_IF_EXISTS and file does not exist")
        @Description("Verifies that SKIP_IF_EXISTS mode creates new files")
        fun shouldCreateNewFileWhenNotExists(@TempDir tempDir: Path) {
            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf(
                    "templateSet" to "restassured-java",
                    "writeMode" to "SKIP_IF_EXISTS",
                )
            )

            step("Generate tests") {
                generator.generateTests(basicTestSuite.copy(testCases = listOf(createMinimalTestCase())))
            }

            step("Verify file was created") {
                val generatedFile = tempDir.resolve("CreateUserTest.java").toFile()
                assertThat(generatedFile).exists()
                assertThat(generatedFile.readText()).contains("class CreateUserTest")
            }
        }
    }

    @Nested
    @Story("Template Loading")
    @DisplayName("Template Loading")
    inner class TemplateLoadingTests {

        @Test
        @DisplayName("should load template from classpath")
        @Description("Verifies that templates are loaded from resources")
        fun shouldLoadTemplateFromClasspath(@TempDir tempDir: Path) {
            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf("templateSet" to "restassured-java")
            )

            step("Generate tests") {
                generator.generateTests(basicTestSuite.copy(testCases = listOf(createMinimalTestCase())))
            }

            step("Verify file was generated using classpath template") {
                val generatedFile = tempDir.resolve("CreateUserTest.java").toFile()
                assertThat(generatedFile).exists()
                assertThat(generatedFile.readText()).contains("RestAssured")
            }
        }

        @Test
        @DisplayName("should load template from custom directory")
        @Description("Verifies that custom template directory is used when specified")
        fun shouldLoadTemplateFromCustomDirectory(@TempDir tempDir: Path) {
            val customTemplateDir = tempDir.resolve("custom-templates").toFile()
            customTemplateDir.mkdirs()

            val classTemplate = File(customTemplateDir, "class.mustache")
            classTemplate.writeText(
                """
                // Custom Template
                public class {{className}} {
                {{#methods}}
                    public void {{methodName}}() {}
                {{/methods}}
                }
                """.trimIndent()
            )

            val outputDir = tempDir.resolve("output").toFile()
            outputDir.mkdirs()

            val generator = TemplateArtifactGenerator(
                outputDir,
                mapOf(
                    "templateSet" to "custom",
                    "customTemplateDir" to customTemplateDir.absolutePath,
                    "classTemplatePath" to "class.mustache",
                    "outputFileExtension" to "java",
                )
            )

            step("Generate tests") {
                generator.generateTests(basicTestSuite.copy(testCases = listOf(createMinimalTestCase())))
            }

            step("Verify custom template was used") {
                val generatedFile = File(outputDir, "CreateUserTest.java")
                assertThat(generatedFile).exists()
                assertThat(generatedFile.readText()).contains("// Custom Template")
            }
        }

        @Test
        @DisplayName("should throw when template not found in classpath")
        @Description("Verifies that missing classpath template throws IllegalStateException")
        fun shouldThrowWhenTemplateNotFoundInClasspath(@TempDir tempDir: Path) {
            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf(
                    "templateSet" to "nonexistent-template",
                    "outputFileExtension" to "java",
                )
            )

            assertThatThrownBy {
                generator.generateTests(basicTestSuite.copy(testCases = listOf(createMinimalTestCase())))
            }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Failed to load template")
        }

        @Test
        @DisplayName("should throw when custom template not found")
        @Description("Verifies that missing custom template throws IllegalStateException")
        fun shouldThrowWhenCustomTemplateNotFound(@TempDir tempDir: Path) {
            val emptyTemplateDir = tempDir.resolve("empty-templates").toFile()
            emptyTemplateDir.mkdirs()

            val outputDir = tempDir.resolve("output").toFile()
            outputDir.mkdirs()

            val generator = TemplateArtifactGenerator(
                outputDir,
                mapOf(
                    "templateSet" to "custom",
                    "customTemplateDir" to emptyTemplateDir.absolutePath,
                    "classTemplatePath" to "missing.mustache",
                    "outputFileExtension" to "java",
                )
            )

            assertThatThrownBy {
                generator.generateTests(basicTestSuite.copy(testCases = listOf(createMinimalTestCase())))
            }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Custom template not found")
        }
    }

    @Nested
    @Story("camelCaseName Utility")
    @DisplayName("camelCaseName Utility")
    inner class CamelCaseNameTests {

        fun camelCaseProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("null input", null, true, "test"),
            Arguments.of("empty input", "", true, "test"),
            Arguments.of("underscore separator with capitalize", "hello_world", true, "HelloWorld"),
            Arguments.of("hyphen separator without capitalize", "hello-world", false, "helloWorld"),
            Arguments.of("space separator with capitalize", "hello world", true, "HelloWorld"),
            Arguments.of("colon separator without capitalize", "path:segment", false, "pathSegment"),
            Arguments.of("mixed separators", "hello_world-test:case", true, "HelloWorldTestCase"),
            Arguments.of("already camelCase", "alreadyCamel", false, "alreadyCamel"),
            Arguments.of("single word capitalize", "word", true, "Word"),
            Arguments.of("single word no capitalize", "Word", false, "word"),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("camelCaseProvider")
        @DisplayName("should convert to camelCase correctly")
        @Suppress("UnusedParameter") // capitalizeFirst documents the test scenario context
        fun shouldConvertToCamelCaseCorrectly(
            scenario: String,
            input: String?,
            capitalizeFirst: Boolean,
            expected: String,
            @TempDir tempDir: Path,
        ) {
            // Test via method name generation which uses camelCaseName internally
            val testCase = TestCase(
                name = input ?: "",
                method = "GET",
                path = "/test",
                expectedStatusCode = 200,
            )
            val testSuite = TestSuite(
                path = "/test",
                method = "GET",
                operationName = "testOp",
                testCases = listOf(testCase),
            )

            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf("templateSet" to "restassured-java")
            )

            step("Generate tests") {
                generator.generateTests(testSuite)
            }

            step("Verify method name follows camelCase rules") {
                val generatedFile = tempDir.resolve("TestOpTest.java").toFile()
                val content = generatedFile.readText()
                // Method names use capitalizeFirst=false, so we check the lowercase version
                if (input.isNullOrEmpty()) {
                    assertThat(content).contains("public void test()")
                } else {
                    assertThat(content).contains(expected.replaceFirstChar { it.lowercaseChar() })
                }
            }
        }
    }

    @Nested
    @Story("Template Variables")
    @DisplayName("Template Variables")
    inner class TemplateVariablesTests {

        @Test
        @DisplayName("should render package from templateVariables")
        @Description("Verifies that package template variable is rendered in output")
        fun shouldRenderPackage(@TempDir tempDir: Path) {
            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf(
                    "templateSet" to "restassured-java",
                    "templateVariables" to mapOf(
                        "package" to "com.example.generated.tests",
                    ),
                )
            )

            step("Generate tests") {
                generator.generateTests(basicTestSuite.copy(testCases = listOf(createMinimalTestCase())))
            }

            step("Verify package is in generated file") {
                val generatedFile = tempDir.resolve("CreateUserTest.java").toFile()
                val content = generatedFile.readText()
                assertThat(content).contains("package com.example.generated.tests;")
            }
        }

        @Test
        @DisplayName("should render baseUrl from templateVariables")
        @Description("Verifies that baseUrl template variable is rendered in output")
        fun shouldRenderBaseUrl(@TempDir tempDir: Path) {
            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf(
                    "templateSet" to "restassured-java",
                    "templateVariables" to mapOf(
                        "baseUrl" to "http://localhost:9090/api",
                    ),
                )
            )

            step("Generate tests") {
                generator.generateTests(basicTestSuite.copy(testCases = listOf(createMinimalTestCase())))
            }

            step("Verify baseUrl is in generated file") {
                val generatedFile = tempDir.resolve("CreateUserTest.java").toFile()
                val content = generatedFile.readText()
                assertThat(content).contains("http://localhost:9090/api")
            }
        }

        @Test
        @DisplayName("should render fileHeaderComment when provided")
        @Description("Verifies that fileHeaderComment is rendered at the top of the file")
        fun shouldRenderFileHeaderComment(@TempDir tempDir: Path) {
            val headerComment = "// Auto-generated by OpenAPI Test Generator"
            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf(
                    "templateSet" to "restassured-java",
                    "fileHeaderComment" to headerComment,
                )
            )

            step("Generate tests") {
                generator.generateTests(basicTestSuite.copy(testCases = listOf(createMinimalTestCase())))
            }

            step("Verify header comment is in generated file") {
                val generatedFile = tempDir.resolve("CreateUserTest.java").toFile()
                val content = generatedFile.readText()
                assertThat(content).startsWith(headerComment)
            }
        }
    }

    @Nested
    @Story("Kotlin Template")
    @DisplayName("Kotlin Template Generation")
    inner class KotlinTemplateTests {

        @Test
        @DisplayName("should generate Kotlin test file with .kt extension")
        @Description("Verifies that Kotlin template generates .kt files")
        fun shouldGenerateKotlinFile(@TempDir tempDir: Path) {
            val generator = TemplateArtifactGenerator(
                tempDir.toFile(),
                mapOf("templateSet" to "restassured-kotlin")
            )

            step("Generate tests") {
                generator.generateTests(basicTestSuite.copy(testCases = listOf(createMinimalTestCase())))
            }

            step("Verify .kt file was generated") {
                val generatedFile = tempDir.resolve("CreateUserTest.kt").toFile()
                assertThat(generatedFile).exists()
                assertThat(generatedFile.readText()).contains("class CreateUserTest")
            }
        }
    }

    private fun createMinimalTestCase(): TestCase = TestCase(
        name = "Valid Case",
        method = "POST",
        path = "/users",
        expectedStatusCode = 201,
    )
}

