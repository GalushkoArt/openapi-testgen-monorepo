package art.galushko.openapi.testgen.example.config

import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.format.DateTimeParseException

@Epic("Configuration")
@Feature("Example Value Generation")
@DisplayName("ExampleValueSettings")
class ExampleValueSettingsTest {

    @Test
    @DisplayName("should accept valid default settings")
    fun shouldAcceptDefaults() {
        assertThatCode { ExampleValueSettings() }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("should use default provider order")
    fun shouldUseDefaultProviderOrder() {
        val settings = ExampleValueSettings()
        assertThat(settings.providers).isEqualTo(ExampleValueSettings.DEFAULT_PROVIDER_ORDER)
    }

    @Test
    @DisplayName("should accept unknown provider id (open-world)")
    fun shouldAcceptUnknownProvider() {
        assertThatCode { ExampleValueSettings(providers = listOf("unknown")) }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("should reject duplicate providers")
    fun shouldRejectDuplicateProviders() {
        assertThatThrownBy { ExampleValueSettings(providers = listOf("enum", "enum")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("providers list must not contain duplicates: [enum]")
    }

    @Test
    @DisplayName("should reject invalid date format")
    fun shouldRejectInvalidDateFormat() {
        assertThatThrownBy {
            ExampleValueSettings(date = DateProviderSettings(startDate = "not-a-date"))
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("date.startDate must be ISO-8601 format (YYYY-MM-DD): 'not-a-date'")
            .hasCauseInstanceOf(DateTimeParseException::class.java)
    }

    @Test
    @DisplayName("should reject uuid template without placeholder")
    fun shouldRejectUuidTemplateWithoutPlaceholder() {
        assertThatThrownBy {
            ExampleValueSettings(uuid = UuidProviderSettings(template = "no-placeholder"))
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("uuid.template must contain '%s'")
    }

    @Test
    @DisplayName("should reject email template without placeholder")
    fun shouldRejectEmailTemplateWithoutPlaceholder() {
        assertThatThrownBy {
            ExampleValueSettings(email = EmailProviderSettings(template = "no-placeholder@test.com"))
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("email.template must contain '%s'")
    }

    @Test
    @DisplayName("fromMap should parse nested settings")
    fun fromMapShouldParseNestedSettings() {
        val settings = ExampleValueSettings.fromMap(
            mapOf(
                "providers" to listOf("enum", "const", "pattern", "plain-string"),
                "maxExampleDepth" to 30,
                "email" to mapOf("template" to "user%s@mycompany.com"),
                "date" to mapOf("startDate" to "2025-01-01"),
                "dateTime" to mapOf(
                    "startDate" to "2025-01-01",
                    "timeSuffixTemplate" to "%sT00:00:00Z",
                ),
                "plainString" to mapOf("validChars" to "abc123"),
            )
        )

        assertThat(settings.providers).containsExactly("enum", "const", "pattern", "plain-string")
        assertThat(settings.maxExampleDepth).isEqualTo(30)
        assertThat(settings.email.template).isEqualTo("user%s@mycompany.com")
        assertThat(settings.date.startDate).isEqualTo("2025-01-01")
        assertThat(settings.dateTime.startDate).isEqualTo("2025-01-01")
        assertThat(settings.dateTime.timeSuffixTemplate).isEqualTo("%sT00:00:00Z")
        assertThat(settings.plainString.validChars).isEqualTo("abc123")
    }

    @Test
    @DisplayName("fromMap should throw ConfigurationException for invalid providers type")
    fun fromMapShouldThrowForInvalidProvidersType() {
        assertThatThrownBy {
            ExampleValueSettings.fromMap(mapOf("providers" to "not-a-list"))
        }
            .isInstanceOf(ConfigurationException::class.java)
            .hasMessage("Configuration error for 'providers': expected Collection<String>, got kotlin.String")
    }
}
