package art.galushko.openapi.testgen.plugin

import art.galushko.openapi.testgen.config.TestGenerationSettings
import art.galushko.openapi.testgen.model.error.ErrorMode
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.internal.project.DefaultProject
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TestGenerationSettingsExtension")
class TestGenerationSettingsExtensionTest {

    private lateinit var project: DefaultProject
    private lateinit var extension: TestGenerationSettingsExtension

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build() as DefaultProject
        extension = project.objects.newInstance(TestGenerationSettingsExtension::class.java)
    }

    @Test
    @DisplayName("Extension should have null or empty default values for all properties")
    fun testDefaultValues() {
        assertThat(extension.includeOperations.get()).isEmpty()
        assertThat(extension.ignoreTestCases.get()).isEmpty()
        assertThat(extension.ignoreSchemaValidationRules.get()).isEmpty()
        assertThat(extension.ignoreAuthValidationRules.get()).isEmpty()
        assertThat(extension.maxSchemaDepth.orNull).isNull()
        assertThat(extension.overrideBasicTestData.get()).isEmpty()
        assertThat(extension.maxSchemaCombinations.orNull).isNull()
        assertThat(extension.maxMergedSchemaDepth.orNull).isNull()
        assertThat(extension.maxTestCasesPerOperation.orNull).isNull()
        assertThat(extension.validSecurityValues.get()).isEmpty()
        assertThat(extension.errorMode.orNull).isNull()
        assertThat(extension.includeValidCase.orNull).isNull()
        assertThat(extension.maxErrors.orNull).isNull()
        assertThat(extension.exampleValues.get()).isEmpty()
    }


    @Test
    @DisplayName("buildTestGenerationSettingsMap produces map compatible with TestGenerationSettings.fromMap")
    @Suppress("LongMethod")
    fun testRoundTripCompatibility() {
        // Arrange: Configure extension with all possible settings
        extension.includeOperations.put("/api/users", listOf("GET", "POST"))
        extension.includeOperations.put("/api/orders", listOf("*"))

        extension.ignoreTestCases.put("/users", mapOf("GET" to listOf("Missing required param: id")))
        extension.ignoreTestCases.put("/posts", "*")

        extension.ignoreSchemaValidationRules.addAll(listOf("MinLengthRule", "MaxLengthRule"))

        extension.ignoreAuthValidationRules.addAll(listOf("ApiKeyRule", "BearerTokenRule"))

        extension.maxSchemaDepth.set(20)

        extension.overrideBasicTestData.put("string", "custom-string-value")
        extension.overrideBasicTestData.put("integer", "42")

        extension.maxSchemaCombinations.set(200)
        extension.maxMergedSchemaDepth.set(300)

        extension.maxTestCasesPerOperation.set(500)

        extension.validSecurityValues.put("X-API-Key", "test-api-key-123")
        extension.validSecurityValues.put("Authorization", "Bearer test-token")

        extension.errorMode.set(ErrorMode.FAIL_FAST)

        extension.includeValidCase.set(true)

        extension.maxErrors.set(50)

        extension.exampleValues.putAll(
            mapOf(
                "providers" to listOf("enum", "const", "pattern"),
                "maxExampleDepth" to 30,
                "includeOptionalExampleProperties" to true,
                "includeWriteOnly" to false,
                "useSchemaExampleFallback" to true,
                "email" to mapOf("template" to "user%s@mycompany.com"),
                "date" to mapOf("startDate" to "2025-01-01"),
                "dateTime" to mapOf(
                    "startDate" to "2025-01-01",
                    "timeSuffixTemplate" to "%sT00:00:00Z",
                ),
                "plainString" to mapOf("validChars" to "abc123"),
            )
        )

        // Act: Build map and parse back to TestGenerationSettings
        val settingsMap = extension.buildTestGenerationSettingsMap()
        val parsedSettings = TestGenerationSettings.Companion.fromMap(settingsMap)

        // Assert: Verify all properties match expected values
        // If test fails you should either modify extension adding new field or removing deleted field
        // or modify expected string when change should not propagate to the extension
        assertThat(
            "TestGenerationSettings(" +
                "includeOperations={/api/users=[GET, POST], /api/orders=[*]}, " +
                "ignoreTestCases={/users={GET=[Missing required param: id]}, /posts=*}, " +
                "ignoreSchemaValidationRules=[MinLengthRule, MaxLengthRule], " +
                "ignoreAuthValidationRules=[ApiKeyRule, BearerTokenRule], " +
                "maxSchemaDepth=20, " +
                "overrideBasicTestData={string=custom-string-value, integer=42}, " +
                "maxMergedSchemaDepth=300, " +
                "maxSchemaCombinations=200, " +
                "maxTestCasesPerOperation=500, " +
                "validSecurityValues={X-API-Key=test-api-key-123, Authorization=Bearer test-token}, " +
                "errorMode=FAIL_FAST, " +
                "includeValidCase=true, " +
                "maxErrors=50, " +
                "exampleValues=ExampleValueSettings(providers=[enum, const, pattern], maxExampleDepth=30, " +
                "includeOptionalExampleProperties=true, includeWriteOnly=false, useSchemaExampleFallback=true, " +
                "uuid=UuidProviderSettings(template=d5a5495b-cbdc-4237-a66e-%s), " +
                "email=EmailProviderSettings(template=user%s@mycompany.com), " +
                "date=DateProviderSettings(startDate=2025-01-01), " +
                "dateTime=DateTimeProviderSettings(startDate=2025-01-01, timeSuffixTemplate=%sT00:00:00Z), " +
                "plainString=PlainStringProviderSettings(validChars=abc123)))"
        ).isEqualTo(
            parsedSettings.toString()
        )
    }
}
