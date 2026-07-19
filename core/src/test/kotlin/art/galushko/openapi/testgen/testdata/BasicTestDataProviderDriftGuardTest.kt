package art.galushko.openapi.testgen.testdata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("BasicTestDataProvider drift guard")
class BasicTestDataProviderDriftGuardTest {

    /**
     * Every public zero-arg String-returning method is an override key handled via
     * `valueFor(methodName, default)`. This guard catches a method whose body passes the
     * wrong key (or a new method whose key was forgotten): the override would silently
     * stop working.
     */
    @Test
    @DisplayName("every value method must honor an override under its own name")
    fun everyValueMethodHonorsItsOverrideKey() {
        val valueMethods = BasicTestDataProvider::class.java.methods
            .filter { it.declaringClass == BasicTestDataProvider::class.java }
            .filter { it.parameterCount == 0 && it.returnType == String::class.java }
            .filter { !it.isSynthetic && it.name != "toString" }
            .sortedBy { it.name }

        assertThat(valueMethods).isNotEmpty()

        for (method in valueMethods) {
            val overridden = "overridden-${method.name}"
            val provider = BasicTestDataProvider(mapOf(method.name to overridden))

            assertThat(method.invoke(provider))
                .withFailMessage(
                    "BasicTestDataProvider.%s() does not honor the override key '%s'. " +
                        "Check the valueFor(...) key in the method body and the knownKeys set.",
                    method.name,
                    method.name,
                )
                .isEqualTo(overridden)
        }
    }
}
