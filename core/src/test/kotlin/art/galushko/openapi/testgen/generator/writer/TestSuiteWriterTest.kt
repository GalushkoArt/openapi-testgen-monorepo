package art.galushko.openapi.testgen.generator.writer
import art.galushko.openapi.testgen.generation.step

import art.galushko.openapi.testgen.model.KeyValuePair
import art.galushko.openapi.testgen.model.TestCase
import art.galushko.openapi.testgen.model.TestSuite
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
@Feature("TestSuiteWriter")
@DisplayName("TestSuiteWriter Integration Tests")
internal class TestSuiteWriterTest {

    private val jsonMapper: ObjectMapper = jacksonObjectMapper()

    private val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory())
        .registerModule(KotlinModule.Builder().build())

    private val createUserSuite = TestSuite(
        path = "/users",
        method = "POST",
        operationName = "createUser",
        testCases = listOf(
            TestCase(
                name = "Valid Case",
                method = "POST",
                path = "/users",
                expectedStatusCode = 201,
                body = mapOf("name" to "John Doe", "email" to "john@example.com"),
            ),
            TestCase(
                name = "Invalid Body: Missing Required Field email",
                method = "POST",
                path = "/users",
                expectedStatusCode = 400,
                body = mapOf("name" to "John Doe"),
                rule = "RequiredFieldRule",
            ),
        )
    )

    private val getUserSuite = TestSuite(
        path = "/users/{userId}",
        method = "GET",
        operationName = "getUser",
        testCases = listOf(
            TestCase(
                name = "Valid Case",
                method = "GET",
                path = "/users/{userId}",
                expectedStatusCode = 200,
                pathParams = mapOf("userId" to "123"),
            ),
            TestCase(
                name = "Invalid Path Param: userId Empty",
                method = "GET",
                path = "/users/{userId}",
                expectedStatusCode = 400,
                pathParams = mapOf("userId" to ""),
                rule = "EmptyValueRule",
            ),
        )
    )

    @Nested
    @Story("Single File Output")
    @DisplayName("SINGLE_FILE Mode")
    inner class SingleFileModeTests {
        fun outputFormatProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("JSON format", OutputFormat.JSON, "expected-single-file.json"),
            Arguments.of("YAML format", OutputFormat.YAML, "expected-single-file.yaml"),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("outputFormatProvider")
        @DisplayName("should write aggregated suites to single file")
        @Description("Verifies that SINGLE_FILE mode aggregates all suites into one file keyed by operationName")
        fun shouldWriteAggregatedSuitesToSingleFile(
            scenario: String,
            format: OutputFormat,
            expectedFileName: String,
            @TempDir tempDir: Path,
        ) {
            val outputFileName = "test-suites.${format.name.lowercase()}"
            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "OVERWRITE",
                "format" to format.name,
                "preventOverwriteSuites" to false,
                "preventOverwriteCases" to false,
            )

            step("Write test suites using TestSuiteWriter") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(createUserSuite)
                    writer.generateTests(getUserSuite)
                }
            }

            step("Verify output matches expected") {
                val producedFile = tempDir.resolve(outputFileName).toFile()
                assertThat(producedFile).exists()

                val produced: Map<String, TestSuite> = readAggregatedFile(producedFile, format)
                val expected: Map<String, TestSuite> = loadExpectedAggregatedResource(expectedFileName)

                assertThat(produced)
                    .usingRecursiveComparison()
                    .isEqualTo(expected)
            }
        }
    }

    @Nested
    @Story("Multiple Files Output")
    @DisplayName("MULTIPLE_FILES Mode")
    inner class MultipleFilesModeTests {
        fun multipleFilesFormatProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("JSON format", OutputFormat.JSON, "json"),
            Arguments.of("YAML format", OutputFormat.YAML, "yaml"),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("multipleFilesFormatProvider")
        @DisplayName("should write each suite to separate file")
        @Description("Verifies that MULTIPLE_FILES mode writes one file per suite with prefix + operationName")
        fun shouldWriteEachSuiteToSeparateFile(
            scenario: String,
            format: OutputFormat,
            extension: String,
            @TempDir tempDir: Path,
        ) {
            val fileNamePrefix = "tests-"
            val options = mapOf(
                "outputFileName" to "", // Not required for MULTIPLE_FILES
                "outputMode" to "MULTIPLE_FILES",
                "writeMode" to "OVERWRITE",
                "format" to format.name,
                "fileNamePrefix" to fileNamePrefix,
            )

            step("Write test suites using TestSuiteWriter") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(createUserSuite)
                    writer.generateTests(getUserSuite)
                }
            }

            step("Verify individual files exist") {
                val createUserFile = tempDir.resolve("${fileNamePrefix}createUser.$extension").toFile()
                val getUserFile = tempDir.resolve("${fileNamePrefix}getUser.$extension").toFile()

                assertThat(createUserFile).exists()
                assertThat(getUserFile).exists()

                val producedCreateUser: TestSuite = readSuiteFile(createUserFile, format)
                val producedGetUser: TestSuite = readSuiteFile(getUserFile, format)

                val expectedCreateUser: TestSuite = loadExpectedSuiteResource(
                    "expected-multiple-files/${fileNamePrefix}createUser.$extension"
                )
                val expectedGetUser: TestSuite = loadExpectedSuiteResource(
                    "expected-multiple-files/${fileNamePrefix}getUser.$extension"
                )

                assertThat(producedCreateUser)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedCreateUser)

                assertThat(producedGetUser)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedGetUser)
            }
        }

        @Test
        @DisplayName("should write immediately in MULTIPLE_FILES mode")
        @Description("Verifies that in MULTIPLE_FILES mode, files are written immediately per suite")
        fun shouldWriteImmediatelyInMultipleFilesMode(@TempDir tempDir: Path) {
            val fileNamePrefix = "tests-"
            val options = mapOf(
                "outputFileName" to "",
                "outputMode" to "MULTIPLE_FILES",
                "writeMode" to "OVERWRITE",
                "format" to "JSON",
                "fileNamePrefix" to fileNamePrefix,
            )

            val createUserFile = tempDir.resolve("${fileNamePrefix}createUser.json").toFile()

            val writer = TestSuiteWriter(tempDir.toFile(), options)

            step("Verify file does not exist before generation") {
                assertThat(createUserFile).doesNotExist()
            }

            step("Generate first suite") {
                writer.generateTests(createUserSuite)
            }

            step("Verify file exists immediately after generation (before close)") {
                assertThat(createUserFile).exists()
            }
        }
    }

    @Nested
    @Story("Merge Mode")
    @DisplayName("MERGE WriteMode - Suite and Case Level")
    inner class MergeModeTests {

        @Test
        @DisplayName("should merge new suites with existing file in SINGLE_FILE mode")
        @Description("Verifies that MERGE mode preserves existing suites and adds new ones")
        fun shouldMergeNewSuitesWithExistingFile(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val outputFile = tempDir.resolve(outputFileName).toFile()

            step("Copy existing file to temp directory") {
                val existingContent = loadResourceAsString("merge-existing-single-file.json")
                outputFile.writeText(existingContent)
            }

            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "MERGE",
                "format" to "JSON",
                "preventOverwriteSuites" to false,
                "preventOverwriteCases" to false,
            )

            step("Write new suite using TestSuiteWriter with MERGE mode") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(createUserSuite)
                }
            }

            step("Verify output contains both existing and new suites") {
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                val expected: Map<String, TestSuite> = loadExpectedAggregatedResource("expected-merged-single-file.json")

                assertThat(produced)
                    .usingRecursiveComparison()
                    .isEqualTo(expected)
            }
        }

        fun mergeFormatProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("JSON format", OutputFormat.JSON),
            Arguments.of("YAML format", OutputFormat.YAML),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("mergeFormatProvider")
        @DisplayName("should merge in MULTIPLE_FILES mode")
        @Description("Verifies that MERGE mode works correctly with MULTIPLE_FILES output")
        fun shouldMergeInMultipleFilesMode(
            scenario: String,
            format: OutputFormat,
            @TempDir tempDir: Path,
        ) {
            val extension = format.name.lowercase()
            val mapper = if (format == OutputFormat.YAML) yamlMapper else jsonMapper

            val originalCase = TestCase(
                name = "Original Case",
                method = "GET",
                path = "/users/{userId}",
                expectedStatusCode = 200,
                pathParams = mapOf("userId" to "original"),
            )
            val originalSuite = getUserSuite.copy(testCases = listOf(originalCase))

            step("Write original suite file") {
                val originalFile = tempDir.resolve("tests-getUser.$extension").toFile()
                mapper.writeValue(originalFile, originalSuite)
            }

            val options = mapOf(
                "outputFileName" to "",
                "outputMode" to "MULTIPLE_FILES",
                "writeMode" to "MERGE",
                "format" to format.name,
                "fileNamePrefix" to "tests-",
                "preventOverwriteSuites" to false,
                "preventOverwriteCases" to false,
            )

            step("Write new suite with same operationName") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(getUserSuite)
                }
            }

            step("Verify suite was updated") {
                val producedFile = tempDir.resolve("tests-getUser.$extension").toFile()
                assertThat(producedFile).exists()

                val produced: TestSuite = mapper.readValue(producedFile, TestSuite::class.java)
                assertThat(produced.testCases).hasSize(2)
                assertThat(produced.testCases.map { it.name }).containsExactlyInAnyOrder(
                    "Invalid Path Param: userId Empty",
                    "Valid Case",
                )
            }
        }

        @Test
        @DisplayName("should add new suites during merge")
        @Description("Verifies that new suites with different operationNames are added alongside existing ones")
        fun shouldAddNewSuitesDuringMerge(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val outputFile = tempDir.resolve(outputFileName).toFile()

            step("Write existing suite") {
                outputFile.writeText(jsonMapper.writeValueAsString(mapOf("createUser" to createUserSuite)))
            }

            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "MERGE",
                "format" to "JSON",
                "preventOverwriteSuites" to false,
                "preventOverwriteCases" to false,
            )

            step("Write new suite with different operationName") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(getUserSuite)
                }
            }

            step("Verify both suites exist") {
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                assertThat(produced).hasSize(2)
                assertThat(produced.keys).containsExactlyInAnyOrder("createUser", "getUser")
            }
        }
    }

    @Nested
    @Story("File Name Sanitization")
    @DisplayName("File Name Sanitization")
    inner class FileNameSanitizationTests {

        @Test
        @DisplayName("should sanitize operationName for safe filename")
        @Description("Verifies that unsafe characters in operationName are replaced with underscores")
        fun shouldSanitizeOperationNameForSafeFilename(@TempDir tempDir: Path) {
            val unsafeSuite = TestSuite(
                path = "/test",
                method = "GET",
                operationName = "get/user:by<id>with spaces",
                testCases = listOf(
                    TestCase(
                        name = "Test",
                        method = "GET",
                        path = "/test",
                        expectedStatusCode = 200,
                    )
                )
            )

            val options = mapOf(
                "outputFileName" to "",
                "outputMode" to "MULTIPLE_FILES",
                "writeMode" to "OVERWRITE",
                "format" to "JSON",
                "fileNamePrefix" to "",
            )

            step("Write suite with unsafe operationName") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(unsafeSuite)
                }
            }

            step("Verify sanitized filename exists") {
                val sanitizedFile = tempDir.resolve("get_user_by_id_with_spaces.json").toFile()
                assertThat(sanitizedFile).exists()
            }
        }

        @Test
        @DisplayName("should use sanitizeFileName correctly")
        fun shouldSanitizeFileNameCorrectly() {
            assertThat(TestSuiteWriter.sanitizeFileName("simple")).isEqualTo("simple")
            assertThat(TestSuiteWriter.sanitizeFileName("get/user")).isEqualTo("get_user")
            assertThat(TestSuiteWriter.sanitizeFileName("a:b*c?d")).isEqualTo("a_b_c_d")
            assertThat(TestSuiteWriter.sanitizeFileName("with spaces")).isEqualTo("with_spaces")
            assertThat(TestSuiteWriter.sanitizeFileName("  trimmed  ")).isEqualTo("trimmed")
        }
    }


    @Nested
    @Story("Case Level Merge")
    @DisplayName("Case Level Merge (preventOverwriteSuites=true)")
    inner class CaseLevelMergeTests {

        @Test
        @DisplayName("should merge new test cases into existing suite")
        @Description("When preventOverwriteSuites=true, new cases are added to existing suite")
        fun shouldMergeNewTestCasesIntoExistingSuite(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val outputFile = tempDir.resolve(outputFileName).toFile()

            val existingCase = TestCase(
                name = "Existing Case",
                method = "POST",
                path = "/users",
                expectedStatusCode = 200,
            )
            val existingSuite = createUserSuite.copy(testCases = listOf(existingCase))

            step("Write original suite") {
                outputFile.writeText(jsonMapper.writeValueAsString(mapOf("createUser" to existingSuite)))
            }

            val newCase = TestCase(
                name = "New Case",
                method = "POST",
                path = "/users",
                expectedStatusCode = 201,
            )
            val incomingSuite = createUserSuite.copy(testCases = listOf(newCase))

            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "MERGE",
                "format" to "JSON",
                "preventOverwriteSuites" to true,
                "preventOverwriteCases" to false,
            )

            step("Write suite with new case") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(incomingSuite)
                }
            }

            step("Verify both cases exist") {
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                assertThat(produced["createUser"]?.testCases).hasSize(2)
                assertThat(produced["createUser"]?.testCases?.map { it.name })
                    .containsExactly("Existing Case", "New Case")
            }
        }

        @Test
        @DisplayName("should replace existing case when preventOverwriteCases=false")
        @Description("When case with same name exists and preventOverwriteCases=false, case is replaced")
        fun shouldReplaceExistingCaseWhenPreventOverwriteCasesFalse(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val outputFile = tempDir.resolve(outputFileName).toFile()

            val existingCase = TestCase(
                name = "Same Name",
                method = "POST",
                path = "/original",
                expectedStatusCode = 200,
                body = mapOf("data" to "original"),
            )
            val existingSuite = createUserSuite.copy(testCases = listOf(existingCase))

            step("Write original suite") {
                outputFile.writeText(jsonMapper.writeValueAsString(mapOf("createUser" to existingSuite)))
            }

            val updatedCase = TestCase(
                name = "Same Name",
                method = "PUT",
                path = "/updated",
                expectedStatusCode = 201,
                body = mapOf("data" to "updated"),
            )
            val incomingSuite = createUserSuite.copy(testCases = listOf(updatedCase))

            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "MERGE",
                "format" to "JSON",
                "preventOverwriteSuites" to true,
                "preventOverwriteCases" to false,
            )

            step("Write suite with updated case") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(incomingSuite)
                }
            }

            step("Verify case was replaced") {
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                assertThat(produced["createUser"]?.testCases).hasSize(1)
                val resultCase = produced["createUser"]?.testCases?.first()
                assertThat(resultCase?.method).isEqualTo("PUT")
                assertThat(resultCase?.path).isEqualTo("/updated")
                assertThat(resultCase?.expectedStatusCode).isEqualTo(201)
                assertThat(resultCase?.body).isEqualTo(mapOf("data" to "updated"))
            }
        }

        @Test
        @DisplayName("should preserve existing case fields when preventOverwriteCases=true")
        @Description("When preventOverwriteCases=true, existing case is kept (with empty protectedFields)")
        fun shouldPreserveExistingCaseWhenPreventOverwriteCasesTrue(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val outputFile = tempDir.resolve(outputFileName).toFile()

            val existingCase = TestCase(
                name = "Same Name",
                method = "POST",
                path = "/original",
                expectedStatusCode = 200,
            )
            val existingSuite = createUserSuite.copy(testCases = listOf(existingCase))

            step("Write original suite") {
                outputFile.writeText(jsonMapper.writeValueAsString(mapOf("createUser" to existingSuite)))
            }

            val updatedCase = TestCase(
                name = "Same Name",
                method = "PUT",
                path = "/updated",
                expectedStatusCode = 201,
            )
            val incomingSuite = createUserSuite.copy(testCases = listOf(updatedCase))

            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "MERGE",
                "format" to "JSON",
                "preventOverwriteSuites" to true,
                "preventOverwriteCases" to true,
            )

            step("Write suite with updated case") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(incomingSuite)
                }
            }

            step("Verify existing case is preserved") {
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                val resultCase = produced["createUser"]?.testCases?.first()
                assertThat(resultCase?.method).isEqualTo("POST")
                assertThat(resultCase?.path).isEqualTo("/original")
                assertThat(resultCase?.expectedStatusCode).isEqualTo(200)
            }
        }

        @Test
        @DisplayName("should merge with protected fields preserving only specified fields")
        @Description("When protectedTestCaseFields is set, only those fields are preserved from existing case")
        fun shouldMergeWithProtectedFieldsPreservingOnlySpecified(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val outputFile = tempDir.resolve(outputFileName).toFile()

            val existingCase = TestCase(
                name = "Test Case",
                method = "POST",
                path = "/original",
                expectedStatusCode = 200,
                body = mapOf("data" to "original"),
                headers = listOf(KeyValuePair("X-Original", "value")),
            )
            val existingSuite = createUserSuite.copy(testCases = listOf(existingCase))

            step("Write original suite") {
                outputFile.writeText(jsonMapper.writeValueAsString(mapOf("createUser" to existingSuite)))
            }

            val incomingCase = TestCase(
                name = "Test Case",
                method = "PUT",
                path = "/updated",
                expectedStatusCode = 201,
                body = mapOf("data" to "updated"),
                headers = listOf(KeyValuePair("X-Updated", "value")),
            )
            val incomingSuite = createUserSuite.copy(testCases = listOf(incomingCase))

            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "MERGE",
                "format" to "JSON",
                "preventOverwriteSuites" to true,
                "preventOverwriteCases" to true,
                "protectedTestCaseFields" to "expectedStatusCode,body",
            )

            step("Write suite with protected fields") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(incomingSuite)
                }
            }

            step("Verify protected fields are preserved, others updated") {
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                val resultCase = produced["createUser"]?.testCases?.first()

                // Protected fields - preserved from existing
                assertThat(resultCase?.expectedStatusCode).isEqualTo(200)
                assertThat(resultCase?.body).isEqualTo(mapOf("data" to "original"))

                // Non-protected fields - updated from incoming
                assertThat(resultCase?.method).isEqualTo("PUT")
                assertThat(resultCase?.path).isEqualTo("/updated")
                assertThat(resultCase?.headers).usingRecursiveComparison()
                    .isEqualTo(listOf(KeyValuePair("X-Updated", "value")))
            }
        }

        @Test
        @DisplayName("should protect all TestCase fields when all are listed")
        @Description("All fields can be protected during merge")
        @Suppress("LongMethod")
        fun shouldProtectAllTestCaseFieldsWhenAllAreListed(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val outputFile = tempDir.resolve(outputFileName).toFile()

            val existingCase = TestCase(
                name = "Test Case",
                method = "POST",
                path = "/original",
                queryParams = mapOf("q" to "original"),
                pathParams = mapOf("id" to "original"),
                headers = listOf(KeyValuePair("X-Header", "original")),
                cookie = listOf(KeyValuePair("session", "original")),
                body = mapOf("field" to "original"),
                expectedBody = mapOf("result" to "original"),
                needToComplete = true,
                expectedStatusCode = 200,
                rule = "OriginalRule",
            )
            val existingSuite = createUserSuite.copy(testCases = listOf(existingCase))

            step("Write original suite") {
                outputFile.writeText(jsonMapper.writeValueAsString(mapOf("createUser" to existingSuite)))
            }

            val incomingCase = TestCase(
                name = "Test Case",
                method = "PUT",
                path = "/updated",
                queryParams = mapOf("q" to "updated"),
                pathParams = mapOf("id" to "updated"),
                headers = listOf(KeyValuePair("X-Header", "updated")),
                cookie = listOf(KeyValuePair("session", "updated")),
                body = mapOf("field" to "updated"),
                expectedBody = mapOf("result" to "updated"),
                needToComplete = false,
                expectedStatusCode = 500,
                rule = "UpdatedRule",
            )
            val incomingSuite = createUserSuite.copy(testCases = listOf(incomingCase))

            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "MERGE",
                "format" to "JSON",
                "preventOverwriteSuites" to true,
                "preventOverwriteCases" to true,
                "protectedTestCaseFields" to listOf(
                    "method", "path", "queryParams", "pathParams", "headers",
                    "cookie", "body", "expectedBody", "needToComplete", "expectedStatusCode", "rule"
                ),
            )

            step("Write suite with all protected fields") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(incomingSuite)
                }
            }

            step("Verify all fields are preserved from existing") {
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                val resultCase = produced["createUser"]?.testCases?.first()

                assertThat(resultCase?.method).isEqualTo("POST")
                assertThat(resultCase?.path).isEqualTo("/original")
                assertThat(resultCase?.queryParams).isEqualTo(mapOf("q" to "original"))
                assertThat(resultCase?.pathParams).isEqualTo(mapOf("id" to "original"))
                assertThat(resultCase?.headers).usingRecursiveComparison()
                    .isEqualTo(listOf(KeyValuePair("X-Header", "original")))
                assertThat(resultCase?.cookie).usingRecursiveComparison()
                    .isEqualTo(listOf(KeyValuePair("session", "original")))
                assertThat(resultCase?.body).isEqualTo(mapOf("field" to "original"))
                assertThat(resultCase?.expectedBody).isEqualTo(mapOf("result" to "original"))
                assertThat(resultCase?.needToComplete).isTrue()
                assertThat(resultCase?.expectedStatusCode).isEqualTo(200)
                assertThat(resultCase?.rule).isEqualTo("OriginalRule")
            }
        }

        @Test
        @DisplayName("should preserve order when preventOverwriteCases=true")
        @Description("Existing case order is preserved when merging with preventOverwriteCases=true")
        fun shouldPreserveOrderWhenPreventOverwriteCasesTrue(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val outputFile = tempDir.resolve(outputFileName).toFile()

            val existingCases = listOf(
                TestCase(name = "Z Case", method = "GET", path = "/z", expectedStatusCode = 200),
                TestCase(name = "A Case", method = "GET", path = "/a", expectedStatusCode = 200),
                TestCase(name = "M Case", method = "GET", path = "/m", expectedStatusCode = 200),
            )
            val existingSuite = getUserSuite.copy(operationName = "orderedOp", testCases = existingCases)

            step("Write original suite with specific order") {
                outputFile.writeText(jsonMapper.writeValueAsString(mapOf("orderedOp" to existingSuite)))
            }

            val incomingCase = TestCase(
                name = "New Case",
                method = "POST",
                path = "/new",
                expectedStatusCode = 201,
            )
            val incomingSuite = existingSuite.copy(testCases = listOf(incomingCase))

            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "MERGE",
                "format" to "JSON",
                "preventOverwriteSuites" to true,
                "preventOverwriteCases" to true,
            )

            step("Write suite with new case") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(incomingSuite)
                }
            }

            step("Verify order is preserved (not sorted) with new case appended") {
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                val caseNames = produced["orderedOp"]?.testCases?.map { it.name }
                assertThat(caseNames).containsExactly("Z Case", "A Case", "M Case", "New Case")
            }
        }

        @Test
        @DisplayName("should sort cases when preventOverwriteCases=false")
        @Description("Cases are sorted by name when preventOverwriteCases=false")
        fun shouldSortCasesWhenPreventOverwriteCasesFalse(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val outputFile = tempDir.resolve(outputFileName).toFile()

            val existingCases = listOf(
                TestCase(name = "Z Case", method = "GET", path = "/z", expectedStatusCode = 200),
                TestCase(name = "A Case", method = "GET", path = "/a", expectedStatusCode = 200),
            )
            val existingSuite = getUserSuite.copy(operationName = "orderedOp", testCases = existingCases)

            step("Write original suite") {
                outputFile.writeText(jsonMapper.writeValueAsString(mapOf("orderedOp" to existingSuite)))
            }

            val incomingCase = TestCase(
                name = "M Case",
                method = "POST",
                path = "/m",
                expectedStatusCode = 201,
            )
            val incomingSuite = existingSuite.copy(testCases = listOf(incomingCase))

            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "MERGE",
                "format" to "JSON",
                "preventOverwriteSuites" to true,
                "preventOverwriteCases" to false,
            )

            step("Write suite with new case") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(incomingSuite)
                }
            }

            step("Verify cases are sorted alphabetically") {
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                val caseNames = produced["orderedOp"]?.testCases?.map { it.name }
                assertThat(caseNames).containsExactly("A Case", "M Case", "Z Case")
            }
        }

        @Test
        @DisplayName("should handle empty incoming cases list")
        @Description("Existing cases are preserved when incoming list is empty")
        fun shouldHandleEmptyIncomingCasesList(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val outputFile = tempDir.resolve(outputFileName).toFile()

            val existingCase = TestCase(
                name = "Existing Case",
                method = "GET",
                path = "/test",
                expectedStatusCode = 200,
            )
            val existingSuite = getUserSuite.copy(operationName = "testOp", testCases = listOf(existingCase))

            step("Write original suite") {
                outputFile.writeText(jsonMapper.writeValueAsString(mapOf("testOp" to existingSuite)))
            }

            val emptySuite = existingSuite.copy(testCases = emptyList())

            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "MERGE",
                "format" to "JSON",
                "preventOverwriteSuites" to true,
                "preventOverwriteCases" to true,
            )

            step("Write suite with empty test cases") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(emptySuite)
                }
            }

            step("Verify existing cases are preserved") {
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                assertThat(produced["testOp"]?.testCases).hasSize(1)
                assertThat(produced["testOp"]?.testCases?.first()?.name).isEqualTo("Existing Case")
            }
        }

        @Test
        @DisplayName("should update suite metadata during merge")
        @Description("Suite path/method/operationName are updated from incoming suite during merge")
        fun shouldUpdateSuiteMetadataDuringMerge(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val outputFile = tempDir.resolve(outputFileName).toFile()

            val existingSuite = TestSuite(
                path = "/old-path",
                method = "GET",
                operationName = "testOp",
                testCases = listOf(
                    TestCase(name = "Case", method = "GET", path = "/test", expectedStatusCode = 200)
                )
            )

            step("Write original suite") {
                outputFile.writeText(jsonMapper.writeValueAsString(mapOf("testOp" to existingSuite)))
            }

            val incomingSuite = TestSuite(
                path = "/new-path",
                method = "POST",
                operationName = "testOp",
                testCases = listOf(
                    TestCase(name = "New Case", method = "POST", path = "/new", expectedStatusCode = 201)
                )
            )

            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "MERGE",
                "format" to "JSON",
                "preventOverwriteSuites" to true,
            )

            step("Write incoming suite") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(incomingSuite)
                }
            }

            step("Verify suite metadata is updated") {
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                assertThat(produced["testOp"]?.path).isEqualTo("/new-path")
                assertThat(produced["testOp"]?.method).isEqualTo("POST")
            }
        }
    }

    @Nested
    @Story("Edge Cases")
    @DisplayName("Edge Cases")
    inner class EdgeCaseTests {
        fun invalidOperationNameProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("null", null),
            Arguments.of("blank", "   "),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidOperationNameProvider")
        @DisplayName("should skip suite with invalid operationName")
        @Description("Verifies that suites with null or blank operationName are skipped")
        fun shouldSkipSuiteWithInvalidOperationName(
            scenario: String,
            operationName: String?,
            @TempDir tempDir: Path
        ) {
            val suite = TestSuite(
                path = "/test",
                method = "GET",
                operationName = operationName,
                testCases = listOf(
                    TestCase(
                        name = "Test",
                        method = "GET",
                        path = "/test",
                        expectedStatusCode = 200,
                    )
                )
            )

            val options = mapOf(
                "outputFileName" to "test.json",
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "OVERWRITE",
                "format" to "JSON",
            )

            step("Write suite with $scenario operationName") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(suite)
                }
            }

            step("Verify no file is written") {
                val outputFile = tempDir.resolve("test.json").toFile()
                assertThat(outputFile).doesNotExist()
            }
        }

        fun malformedFileProvider(): Stream<Arguments> {
            val jsonMapper = jacksonObjectMapper()
            return Stream.of(
                Arguments.of(
                    "SINGLE_FILE mode",
                    "SINGLE_FILE",
                    "",
                    { tempDir: Path ->
                        val outputFile = tempDir.resolve("test-suites.json").toFile()
                        outputFile.writeText("{ invalid json content }")
                    }
                ),
                Arguments.of(
                    "MULTIPLE_FILES mode",
                    "MULTIPLE_FILES",
                    "prefix-",
                    { tempDir: Path ->
                        val malformedFile = tempDir.resolve("prefix-malformed.json").toFile()
                        malformedFile.writeText("{ invalid json }")

                        val validSuite = TestSuite(
                            path = "/valid",
                            method = "GET",
                            operationName = "validOp",
                            testCases = listOf(
                                TestCase(name = "Valid Test", method = "GET", path = "/valid", expectedStatusCode = 200)
                            )
                        )
                        val validFile = tempDir.resolve("prefix-validOp.json").toFile()
                        jsonMapper.writeValue(validFile, validSuite)
                    }
                )
            )
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("malformedFileProvider")
        @DisplayName("should handle malformed existing files gracefully")
        @Description("Verifies that malformed existing files are handled and writer starts fresh or skips")
        fun shouldHandleMalformedExistingFiles(
            scenario: String,
            outputMode: String,
            fileNamePrefix: String,
            setupMalformedFile: (Path) -> Unit,
            @TempDir tempDir: Path
        ) {
            step("Setup malformed file(s)") {
                setupMalformedFile(tempDir)
            }

            val options = mapOf(
                "outputFileName" to "test-suites.json",
                "outputMode" to outputMode,
                "writeMode" to "MERGE",
                "format" to "JSON",
                "fileNamePrefix" to fileNamePrefix,
            )

            step("Write valid suite - should recover from malformed file") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(createUserSuite)
                }
            }

            step("Verify valid output was written") {
                if (outputMode == "SINGLE_FILE") {
                    val outputFile = tempDir.resolve("test-suites.json").toFile()
                    val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                    assertThat(produced).hasSize(1)
                    assertThat(produced).containsKey("createUser")
                } else {
                    val newFile = tempDir.resolve("${fileNamePrefix}createUser.json").toFile()
                    assertThat(newFile).exists()
                }
            }
        }

        @Test
        @DisplayName("should maintain deterministic ordering for suites and test cases")
        @Description("Verifies that suites and test cases are always written in alphabetical order")
        fun shouldMaintainDeterministicOrdering(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"

            // Create suites with names that would appear in different order if not sorted
            val zSuite = TestSuite(
                path = "/z",
                method = "GET",
                operationName = "zOperation",
                testCases = listOf(
                    TestCase(name = "Z Test", method = "GET", path = "/z", expectedStatusCode = 200),
                    TestCase(name = "A Test", method = "GET", path = "/z", expectedStatusCode = 200),
                )
            )
            val aSuite = TestSuite(
                path = "/a",
                method = "GET",
                operationName = "aOperation",
                testCases = listOf(
                    TestCase(name = "M Case", method = "GET", path = "/a", expectedStatusCode = 200),
                    TestCase(name = "B Case", method = "GET", path = "/a", expectedStatusCode = 200),
                )
            )
            val mSuite = TestSuite(
                path = "/m",
                method = "GET",
                operationName = "mOperation",
                testCases = listOf(
                    TestCase(name = "Y Test", method = "GET", path = "/m", expectedStatusCode = 200)
                )
            )

            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "OVERWRITE",
                "format" to "JSON",
            )

            step("Write suites in non-alphabetical order with unsorted test cases") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(zSuite)
                    writer.generateTests(aSuite)
                    writer.generateTests(mSuite)
                }
            }

            step("Verify suites and test cases are ordered alphabetically") {
                val outputFile = tempDir.resolve(outputFileName).toFile()
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)

                // Verify suite ordering
                assertThat(produced.keys.toList()).containsExactly(
                    "aOperation",
                    "mOperation",
                    "zOperation",
                )

                // Verify test case ordering within each suite
                assertThat(produced["aOperation"]?.testCases?.map { it.name })
                    .containsExactly("B Case", "M Case")
                assertThat(produced["zOperation"]?.testCases?.map { it.name })
                    .containsExactly("A Test", "Z Test")
            }
        }

        @Test
        @DisplayName("should create output directory if it does not exist")
        @Description("Verifies that missing output directory is created automatically")
        fun shouldCreateOutputDirectoryIfNotExists(@TempDir tempDir: Path) {
            val nestedDir = tempDir.resolve("nested/deep/directory").toFile()

            val options = mapOf(
                "outputFileName" to "test.json",
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "OVERWRITE",
                "format" to "JSON",
            )

            step("Write to nested directory that doesn't exist") {
                TestSuiteWriter(nestedDir, options).also { writer ->
                    writer.generateTests(createUserSuite)
                }
            }

            step("Verify directory was created and file written") {
                assertThat(nestedDir).exists()
                assertThat(nestedDir.resolve("test.json")).exists()
            }
        }

        @Test
        @DisplayName("should handle empty test cases list")
        @Description("Verifies that suites with empty test cases are written correctly")
        fun shouldHandleEmptyTestCasesList(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val emptySuite = TestSuite(
                path = "/empty",
                method = "GET",
                operationName = "emptyOp",
                testCases = emptyList()
            )

            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "OVERWRITE",
                "format" to "JSON",
            )

            step("Write suite with empty test cases") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(emptySuite)
                }
            }

            step("Verify suite was written with empty test cases") {
                val outputFile = tempDir.resolve(outputFileName).toFile()
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                assertThat(produced).hasSize(1)
                assertThat(produced["emptyOp"]?.testCases).isEmpty()
            }
        }


        @Test
        @DisplayName("should handle non-matching files during load")
        @Description("Verifies that files not matching prefix/extension are ignored")
        fun shouldIgnoreNonMatchingFiles(@TempDir tempDir: Path) {
            step("Create files with wrong prefix/extension") {
                tempDir.resolve("wrong-prefix.json").toFile().writeText("{}")
                tempDir.resolve("prefix-test.txt").toFile().writeText("{}")
                tempDir.resolve("other.yaml").toFile().writeText("{}")
            }

            val options = mapOf(
                "outputFileName" to "",
                "outputMode" to "MULTIPLE_FILES",
                "writeMode" to "MERGE",
                "format" to "JSON",
                "fileNamePrefix" to "prefix-",
            )

            val suite = TestSuite(
                path = "/test",
                method = "GET",
                operationName = "testOp",
                testCases = emptyList()
            )

            step("Write suite - should not fail on non-matching files") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(suite)
                }
            }

            step("Verify output file created") {
                val outputFile = tempDir.resolve("prefix-testOp.json").toFile()
                assertThat(outputFile).exists()
            }
        }

        @Test
        @DisplayName("should load existing multiple files on init in MERGE mode")
        @Description("Verifies that existing suite files are loaded during initialization in MULTIPLE_FILES MERGE mode")
        fun shouldLoadExistingMultipleFilesOnInit(@TempDir tempDir: Path) {
            step("Create existing suite file") {
                val existingSuite = TestSuite(
                    path = "/existing",
                    method = "GET",
                    operationName = "existingOp",
                    testCases = listOf(
                        TestCase(
                            name = "Existing Test",
                            method = "GET",
                            path = "/existing",
                            expectedStatusCode = 200,
                        )
                    )
                )
                val existingFile = tempDir.resolve("prefix-existingOp.json").toFile()
                jsonMapper.writeValue(existingFile, existingSuite)
            }

            val newSuite = TestSuite(
                path = "/new",
                method = "POST",
                operationName = "newOp",
                testCases = listOf(
                    TestCase(
                        name = "New Test",
                        method = "POST",
                        path = "/new",
                        expectedStatusCode = 201,
                    )
                )
            )

            val options = mapOf(
                "outputFileName" to "",
                "outputMode" to "MULTIPLE_FILES",
                "writeMode" to "MERGE",
                "format" to "JSON",
                "fileNamePrefix" to "prefix-",
            )

            step("Write new suite - existing files should be preserved") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(newSuite)
                }
            }

            step("Verify both files exist") {
                val existingFile = tempDir.resolve("prefix-existingOp.json").toFile()
                val newFile = tempDir.resolve("prefix-newOp.json").toFile()

                assertThat(existingFile).exists()
                assertThat(newFile).exists()

                val existingProduced: TestSuite = jsonMapper.readValue(existingFile, TestSuite::class.java)
                assertThat(existingProduced.operationName).isEqualTo("existingOp")

                val newProduced: TestSuite = jsonMapper.readValue(newFile, TestSuite::class.java)
                assertThat(newProduced.operationName).isEqualTo("newOp")
            }
        }
    }

    @Nested
    @Story("Lifecycle")
    @DisplayName("Instance Lifecycle and Isolation")
    inner class LifecycleTests {

        @Test
        @DisplayName("should have empty state when freshly created")
        @Description("Verifies that a new TestSuiteWriter instance has no accumulated state")
        fun shouldHaveEmptyStateWhenFreshlyCreated(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "OVERWRITE",
                "format" to "JSON",
            )

            step("Create writer but don't generate any tests") {
                TestSuiteWriter(tempDir.toFile(), options)
            }

            step("Verify no file is created (empty state)") {
                val outputFile = tempDir.resolve(outputFileName).toFile()
                assertThat(outputFile).doesNotExist()
            }
        }

        @Test
        @DisplayName("should not leak state between instances in OVERWRITE mode")
        @Description("Verifies that separate instances maintain independent state")
        fun shouldNotLeakStateBetweenInstancesInOverwriteMode(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val outputFile = tempDir.resolve(outputFileName).toFile()
            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "OVERWRITE",
                "format" to "JSON",
            )

            step("First generation with instance 1") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(createUserSuite)
                }
            }

            step("Verify first generation output") {
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                assertThat(produced).containsOnlyKeys("createUser")
            }

            step("Second generation with instance 2 (different suite)") {
                TestSuiteWriter(tempDir.toFile(), options).also { writer ->
                    writer.generateTests(getUserSuite)
                }
            }

            step("Verify second generation completely replaced first (no leakage)") {
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                assertThat(produced).containsOnlyKeys("getUser")
                assertThat(produced).doesNotContainKey("createUser")
            }
        }

        @Test
        @DisplayName("should accumulate state within single instance")
        @Description("Verifies that multiple generateTests calls on same instance accumulate state")
        fun shouldAccumulateStateWithinSingleInstance(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val outputFile = tempDir.resolve(outputFileName).toFile()
            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "OVERWRITE",
                "format" to "JSON",
            )

            step("Create single instance and call generateTests multiple times") {
                val writer = TestSuiteWriter(tempDir.toFile(), options)
                writer.generateTests(createUserSuite)
                writer.generateTests(getUserSuite)
            }

            step("Verify both suites are present (state accumulated)") {
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                assertThat(produced).containsOnlyKeys("createUser", "getUser")
            }
        }

        @Test
        @DisplayName("factory should create independent instances")
        @Description("Verifies that TestSuiteWriterGeneratorFactory creates independent instances")
        fun factoryShouldCreateIndependentInstances(@TempDir tempDir: Path) {
            val outputFileName = "test-suites.json"
            val outputFile = tempDir.resolve(outputFileName).toFile()
            val options = mapOf(
                "outputFileName" to outputFileName,
                "outputMode" to "SINGLE_FILE",
                "writeMode" to "OVERWRITE",
                "format" to "JSON",
            )

            step("Create two instances via factory") {
                val writer1 = TestSuiteWriterGeneratorFactory.create(tempDir.toFile(), options)
                val writer2 = TestSuiteWriterGeneratorFactory.create(tempDir.toFile(), options)

                assertThat(writer1).isNotSameAs(writer2)
            }

            step("Use first instance") {
                val writer1 = TestSuiteWriterGeneratorFactory.create(tempDir.toFile(), options)
                writer1.generateTests(createUserSuite)
            }

            step("Use second instance with different suite") {
                val writer2 = TestSuiteWriterGeneratorFactory.create(tempDir.toFile(), options)
                writer2.generateTests(getUserSuite)
            }

            step("Verify no state leakage between factory-created instances") {
                val produced: Map<String, TestSuite> = readAggregatedFile(outputFile, OutputFormat.JSON)
                assertThat(produced).containsOnlyKeys("getUser")
            }
        }
    }

    @Nested
    @Story("Options Validation")
    @DisplayName("transformAndValidateWriterOptions")
    inner class OptionsValidationTests {
        fun validEnumProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "OutputMode enum directly",
                mapOf("outputMode" to OutputMode.MULTIPLE_FILES, "outputFileName" to ""),
                { opts: TestSuiteWriterOptions -> assertThat(opts.outputMode).isEqualTo(OutputMode.MULTIPLE_FILES) }
            ),
            Arguments.of(
                "WriteMode enum directly",
                mapOf("outputFileName" to "test.json", "writeMode" to WriteMode.OVERWRITE),
                { opts: TestSuiteWriterOptions -> assertThat(opts.writeMode).isEqualTo(WriteMode.OVERWRITE) }
            ),
            Arguments.of(
                "OutputFormat enum directly",
                mapOf("outputFileName" to "test.yaml", "format" to OutputFormat.YAML),
                { opts: TestSuiteWriterOptions -> assertThat(opts.format).isEqualTo(OutputFormat.YAML) }
            ),
            Arguments.of(
                "OutputMode from string (case-insensitive)",
                mapOf("outputFileName" to "test.json", "outputMode" to "single_file"),
                { opts: TestSuiteWriterOptions -> assertThat(opts.outputMode).isEqualTo(OutputMode.SINGLE_FILE) }
            ),
            Arguments.of(
                "OutputFormat from string (case-insensitive)",
                mapOf("outputFileName" to "test.json", "format" to "yaml"),
                { opts: TestSuiteWriterOptions -> assertThat(opts.format).isEqualTo(OutputFormat.YAML) }
            )
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("validEnumProvider")
        @DisplayName("should accept and parse valid enum values")
        fun shouldAcceptValidEnumValues(
            scenario: String,
            options: Map<String, Any?>,
            validator: (TestSuiteWriterOptions) -> Unit
        ) {
            val result = transformAndValidateWriterOptions(options)
            validator(result)
        }

        fun invalidEnumProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "Invalid WriteMode string",
                mapOf("outputFileName" to "test.json", "writeMode" to "INVALID_MODE"),
                { opts: TestSuiteWriterOptions -> assertThat(opts.writeMode).isEqualTo(WriteMode.MERGE) }
            ),
            Arguments.of(
                "Invalid OutputMode string",
                mapOf("outputFileName" to "test.json", "outputMode" to "INVALID_MODE"),
                { opts: TestSuiteWriterOptions -> assertThat(opts.outputMode).isEqualTo(OutputMode.SINGLE_FILE) }
            ),
            Arguments.of(
                "Invalid OutputFormat string",
                mapOf("outputFileName" to "test.json", "format" to "INVALID_FORMAT"),
                { opts: TestSuiteWriterOptions -> assertThat(opts.format).isEqualTo(OutputFormat.JSON) }
            ),
            Arguments.of(
                "Non-string, non-enum OutputMode",
                mapOf("outputFileName" to "test.json", "outputMode" to 123),
                { opts: TestSuiteWriterOptions -> assertThat(opts.outputMode).isEqualTo(OutputMode.SINGLE_FILE) }
            ),
            Arguments.of(
                "Non-string, non-enum format",
                mapOf("outputFileName" to "test.json", "format" to listOf("json")),
                { opts: TestSuiteWriterOptions -> assertThat(opts.format).isEqualTo(OutputFormat.JSON) }
            ),
            Arguments.of(
                "Null WriteMode",
                mapOf("outputFileName" to "test.json", "writeMode" to null),
                { opts: TestSuiteWriterOptions -> assertThat(opts.writeMode).isEqualTo(WriteMode.MERGE) }
            ),
            Arguments.of(
                "Null OutputMode",
                mapOf("outputFileName" to "test.json", "outputMode" to null),
                { opts: TestSuiteWriterOptions -> assertThat(opts.outputMode).isEqualTo(OutputMode.SINGLE_FILE) }
            ),
            Arguments.of(
                "Null format",
                mapOf("outputFileName" to "test.json", "format" to null),
                { opts: TestSuiteWriterOptions -> assertThat(opts.format).isEqualTo(OutputFormat.JSON) }
            )
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidEnumProvider")
        @DisplayName("should use defaults for invalid enum values")
        fun shouldUseDefaultsForInvalidEnums(
            scenario: String,
            options: Map<String, Any?>,
            validator: (TestSuiteWriterOptions) -> Unit
        ) {
            val result = transformAndValidateWriterOptions(options)
            validator(result)
        }

        fun validBooleanProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "preventOverwriteSuites=true from string",
                mapOf("outputFileName" to "test.json", "preventOverwriteSuites" to "true"),
                { opts: TestSuiteWriterOptions -> assertThat(opts.preventOverwriteSuites).isTrue() }
            ),
            Arguments.of(
                "preventOverwriteSuites=false from string",
                mapOf("outputFileName" to "test.json", "preventOverwriteSuites" to "false"),
                { opts: TestSuiteWriterOptions -> assertThat(opts.preventOverwriteSuites).isFalse() }
            )
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("validBooleanProvider")
        @DisplayName("should parse valid boolean values")
        fun shouldParseValidBooleanValues(
            scenario: String,
            options: Map<String, Any?>,
            validator: (TestSuiteWriterOptions) -> Unit
        ) {
            val result = transformAndValidateWriterOptions(options)
            validator(result)
        }

        fun invalidBooleanProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "Invalid boolean string 'yes'",
                mapOf("outputFileName" to "test.json", "preventOverwriteSuites" to "yes"),
                { opts: TestSuiteWriterOptions -> assertThat(opts.preventOverwriteSuites).isTrue() }
            ),
            Arguments.of(
                "Invalid boolean string 'no'",
                mapOf("outputFileName" to "test.json", "preventOverwriteCases" to "no"),
                { opts: TestSuiteWriterOptions -> assertThat(opts.preventOverwriteCases).isTrue() }
            ),
            Arguments.of(
                "Null preventOverwriteSuites",
                mapOf("outputFileName" to "test.json", "preventOverwriteSuites" to null),
                { opts: TestSuiteWriterOptions -> assertThat(opts.preventOverwriteSuites).isTrue() }
            ),
            Arguments.of(
                "Null preventOverwriteCases",
                mapOf("outputFileName" to "test.json", "preventOverwriteCases" to null),
                { opts: TestSuiteWriterOptions -> assertThat(opts.preventOverwriteCases).isTrue() }
            )
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidBooleanProvider")
        @DisplayName("should use defaults for invalid boolean values")
        fun shouldUseDefaultsForInvalidBooleans(
            scenario: String,
            options: Map<String, Any?>,
            validator: (TestSuiteWriterOptions) -> Unit
        ) {
            val result = transformAndValidateWriterOptions(options)
            validator(result)
        }

        @Test
        @DisplayName("should require outputFileName for SINGLE_FILE mode")
        @Description("Verifies outputFileName validation for SINGLE_FILE mode")
        fun shouldRequireOutputFileNameForSingleFileMode() {
            assertThatThrownBy {
                transformAndValidateWriterOptions(mapOf("outputMode" to "SINGLE_FILE"))
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("generatorOptions['outputFileName'] is required for SINGLE_FILE mode but was not provided.")

            assertThatThrownBy {
                transformAndValidateWriterOptions(mapOf("outputMode" to "SINGLE_FILE", "outputFileName" to "   "))
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("generatorOptions['outputFileName'] is required for SINGLE_FILE mode but was not provided.")
        }

        @Test
        @DisplayName("should allow empty outputFileName for MULTIPLE_FILES mode")
        fun shouldAllowEmptyOutputFileNameForMultipleFilesMode() {
            val options = transformAndValidateWriterOptions(
                mapOf(
                    "outputMode" to "MULTIPLE_FILES",
                    "outputFileName" to "",
                )
            )

            assertThat(options.outputFileName).isEmpty()
            assertThat(options.outputMode).isEqualTo(OutputMode.MULTIPLE_FILES)
        }

        fun invalidTypeProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "Invalid writeMode type (integer)",
                mapOf("outputFileName" to "test.json", "writeMode" to 123),
                "Invalid 'writeMode' option: '123'. Expected enum name or string."
            ),
            Arguments.of(
                "Invalid preventOverwriteSuites type (list)",
                mapOf("outputFileName" to "test.json", "preventOverwriteSuites" to listOf("invalid")),
                "Invalid 'preventOverwriteSuites' option: '[invalid]'. Expected boolean or string."
            ),
            Arguments.of(
                "Invalid preventOverwriteCases type (map)",
                mapOf("outputFileName" to "test.json", "preventOverwriteCases" to mapOf("invalid" to true)),
                "Invalid 'preventOverwriteCases' option: '{invalid=true}'. Expected boolean or string."
            ),
            Arguments.of(
                "Invalid protectedTestCaseFields type (integer)",
                mapOf("outputFileName" to "test.json", "protectedTestCaseFields" to 12345),
                "Invalid 'protectedTestCaseFields' option: '12345'. Expected list or comma-separated string."
            )
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidTypeProvider")
        @DisplayName("should throw for invalid option types")
        fun shouldThrowForInvalidTypes(
            scenario: String,
            options: Map<String, Any?>,
            expectedMessage: String
        ) {
            assertThatThrownBy {
                transformAndValidateWriterOptions(options)
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage(expectedMessage)
        }

        fun protectedFieldsParsingProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "Collection with comma-separated values",
                listOf("field1", "field2,field3", "  field4  "),
                setOf("field1", "field2", "field3", "field4")
            ),
            Arguments.of(
                "Comma-separated string",
                "field1, field2 , field3",
                setOf("field1", "field2", "field3")
            ),
            Arguments.of(
                "String with empty values filtered",
                "field1,,  ,field2",
                setOf("field1", "field2")
            )
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("protectedFieldsParsingProvider")
        @DisplayName("should parse protectedTestCaseFields correctly")
        fun shouldParseProtectedTestCaseFields(
            scenario: String,
            input: Any?,
            expected: Set<String>
        ) {
            val options = transformAndValidateWriterOptions(
                mapOf(
                    "outputFileName" to "test.json",
                    "protectedTestCaseFields" to input,
                )
            )

            assertThat(options.protectedTestCaseFields).containsExactlyInAnyOrderElementsOf(expected)
        }

        @Test
        @DisplayName("should handle indent configuration")
        fun shouldHandleIndentConfiguration() {
            // Custom indent
            val customIndent = transformAndValidateWriterOptions(
                mapOf("outputFileName" to "test.json", "indent" to "\t")
            )
            assertThat(customIndent.indent).isEqualTo("\t")

            // Default indent when not provided
            val defaultIndent = transformAndValidateWriterOptions(
                mapOf("outputFileName" to "test.json")
            )
            assertThat(defaultIndent.indent).isEqualTo("    ")

            // Default indent when null
            val nullIndent = transformAndValidateWriterOptions(
                mapOf("outputFileName" to "test.json", "indent" to null)
            )
            assertThat(nullIndent.indent).isEqualTo("    ")
        }
    }

    // --- Helper methods ---

    private fun readAggregatedFile(file: File, format: OutputFormat): Map<String, TestSuite> {
        val mapper = if (format == OutputFormat.YAML) yamlMapper else jsonMapper
        return mapper.readValue(file)
    }

    private fun readSuiteFile(file: File, format: OutputFormat): TestSuite {
        val mapper = if (format == OutputFormat.YAML) yamlMapper else jsonMapper
        return mapper.readValue(file)
    }

    private fun loadExpectedAggregatedResource(resourceName: String): Map<String, TestSuite> {
        val resourcePath = "writer/$resourceName"
        val mapper = if (resourceName.endsWith(".yaml")) yamlMapper else jsonMapper
        return javaClass.classLoader.getResourceAsStream(resourcePath)?.use {
            mapper.readValue(it)
        } ?: throw IllegalArgumentException("Resource not found: $resourcePath")
    }

    private fun loadExpectedSuiteResource(resourceName: String): TestSuite {
        val resourcePath = "writer/$resourceName"
        val mapper = if (resourceName.endsWith(".yaml")) yamlMapper else jsonMapper
        return javaClass.classLoader.getResourceAsStream(resourcePath)?.use {
            mapper.readValue(it)
        } ?: throw IllegalArgumentException("Resource not found: $resourcePath")
    }

    private fun loadResourceAsString(resourceName: String): String {
        val resourcePath = "writer/$resourceName"
        val resource = javaClass.classLoader.getResource(resourcePath)
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")
        return resource.readText()
    }
}

