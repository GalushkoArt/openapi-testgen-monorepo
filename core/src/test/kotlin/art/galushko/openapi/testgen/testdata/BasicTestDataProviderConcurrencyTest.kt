package art.galushko.openapi.testgen.testdata

import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Tests verifying that [BasicTestDataProvider] is thread-safe and isolated per instance.
 */
@Epic("Test Data Generation")
@Feature("BasicTestDataProvider")
@DisplayName("BasicTestDataProvider Concurrency Tests")
internal class BasicTestDataProviderConcurrencyTest {

    @Test
    @DisplayName("Multiple providers with different overrides should not interfere with each other")
    @Description("Verifies that each BasicTestDataProvider instance maintains its own isolated overrides")
    fun multipleProvidersWithDifferentOverridesShouldNotInterfere() {
        // Arrange
        val provider1 = BasicTestDataProvider(mapOf("invalidEnumValue" to "custom_enum_1"))
        val provider2 = BasicTestDataProvider(mapOf("invalidEnumValue" to "custom_enum_2"))
        val provider3 = BasicTestDataProvider(emptyMap())

        // Act
        val value1 = provider1.invalidEnumValue()
        val value2 = provider2.invalidEnumValue()
        val value3 = provider3.invalidEnumValue()

        // Assert
        assertThat(value1).isEqualTo("custom_enum_1")
        assertThat(value2).isEqualTo("custom_enum_2")
        assertThat(value3).isEqualTo("invalid_enum1") // default value
    }

    @Test
    @DisplayName("Concurrent access to different provider instances should be thread-safe")
    @Description("Verifies that multiple threads accessing different provider instances do not interfere")
    fun concurrentAccessToDifferentProviderInstancesShouldBeThreadSafe() {
        // Arrange
        val provider1 = BasicTestDataProvider(mapOf("invalidEnumValue" to "thread_1_value"))
        val provider2 = BasicTestDataProvider(mapOf("invalidEnumValue" to "thread_2_value"))
        val executor = Executors.newFixedThreadPool(2)

        // Act
        val results = Collections.synchronizedList(mutableListOf<String>())
        val latch = java.util.concurrent.CountDownLatch(2)

        executor.submit {
            repeat(100) {
                results.add(provider1.invalidEnumValue())
            }
            latch.countDown()
        }

        executor.submit {
            repeat(100) {
                results.add(provider2.invalidEnumValue())
            }
            latch.countDown()
        }

        latch.await(5, TimeUnit.SECONDS)
        executor.shutdown()

        // Assert - each provider should return its configured value consistently
        assertThat(results.filter { it == "thread_1_value" }).hasSize(100)
        assertThat(results.filter { it == "thread_2_value" }).hasSize(100)
    }

    @Test
    @DisplayName("Same provider instance accessed concurrently should return consistent values")
    @Description("Verifies that a single provider instance is thread-safe for reads")
    fun sameProviderInstanceAccessedConcurrentlyShouldReturnConsistentValues() {
        // Arrange
        val provider = BasicTestDataProvider(mapOf("invalidEnumValue" to "concurrent_value"))
        val executor = Executors.newFixedThreadPool(4)

        // Act
        val results = Collections.synchronizedList(mutableListOf<String>())
        val latch = java.util.concurrent.CountDownLatch(4)

        repeat(4) {
            executor.submit {
                repeat(50) {
                    results.add(provider.invalidEnumValue())
                }
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        executor.shutdown()

        // Assert - all threads should see the same value
        assertThat(results).hasSize(200)
        assertThat(results).allMatch { it == "concurrent_value" }
    }

    @Test
    @DisplayName("Multiple overrides should be applied independently per provider")
    @Description("Verifies that multiple override keys work independently across provider instances")
    fun multipleOverridesShouldBeAppliedIndependentlyPerProvider() {
        // Arrange
        val provider1 = BasicTestDataProvider(
            mapOf(
                "invalidEnumValue" to "provider1_enum",
                "invalidEmailValue" to "provider1@test.com"
            )
        )
        val provider2 = BasicTestDataProvider(
            mapOf(
                "invalidEnumValue" to "provider2_enum",
                "invalidEmailValue" to "provider2@test.com"
            )
        )

        // Act & Assert
        assertThat(provider1.invalidEnumValue()).isEqualTo("provider1_enum")
        assertThat(provider1.invalidEmailValue()).isEqualTo("provider1@test.com")

        assertThat(provider2.invalidEnumValue()).isEqualTo("provider2_enum")
        assertThat(provider2.invalidEmailValue()).isEqualTo("provider2@test.com")
    }

    @Test
    @DisplayName("Provider with no overrides should return default values")
    @Description("Verifies that provider without overrides returns default values for all methods")
    fun providerWithNoOverridesShouldReturnDefaultValues() {
        // Arrange
        val provider = BasicTestDataProvider()

        // Act & Assert - sampling a few defaults
        assertThat(provider.invalidEnumValue()).isEqualTo("invalid_enum1")
        assertThat(provider.invalidUuidValue()).isEqualTo("8e258b27-c787-49ef-9539-11461b251ffg")
        assertThat(provider.invalidEmailValue()).isEqualTo("invalid.email@example")
        assertThat(provider.invalidApiKey()).isEqualTo("some_really_invalid_api_key")
        assertThat(provider.nonIntegerValue()).isEqualTo("1.5")
        assertThat(provider.outOfInt32RangeValue()).isEqualTo("2147483648")
        assertThat(provider.threeDigitYearDate()).isEqualTo("917-07-21")
        assertThat(provider.threeDigitYearDateTime()).isEqualTo("917-07-21T17:32:28Z")
    }

    @Test
    @DisplayName("Partial overrides should use defaults for non-overridden values")
    @Description("Verifies that only specified overrides are applied while others use defaults")
    fun partialOverridesShouldUseDefaultsForNonOverriddenValues() {
        // Arrange
        val provider = BasicTestDataProvider(
            mapOf("invalidEnumValue" to "custom_enum_only")
        )

        // Act & Assert
        assertThat(provider.invalidEnumValue()).isEqualTo("custom_enum_only") // overridden
        assertThat(provider.invalidEmailValue()).isEqualTo("invalid.email@example") // default
        assertThat(provider.invalidApiKey()).isEqualTo("some_really_invalid_api_key") // default
    }

    @Test
    @DisplayName("Unknown override keys should be ignored with warning")
    @Description("Verifies that unknown keys in overrides are ignored and logged as warnings")
    fun unknownOverrideKeysShouldBeIgnoredWithWarning() {
        // Arrange & Act - unknown key should not cause failure
        val provider = BasicTestDataProvider(
            mapOf(
                "invalidEnumValue" to "valid_override",
                "nonExistentMethod" to "invalid_override" // unknown key
            )
        )

        // Assert - valid override should work, unknown key should be ignored
        assertThat(provider.invalidEnumValue()).isEqualTo("valid_override")
    }
}
