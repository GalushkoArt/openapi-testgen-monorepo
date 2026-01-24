plugins {
    id("testgen.library-with-allure")
}

description = "Distribution-ready bundle for CLI and Gradle plugin."

testgenQuality {
    koverMinCoverage = 70
}

dependencies {
    // Transitive exposure for consumers needing core types
    api(libs.testgen.core)
    api(libs.testgen.model)
    api(libs.testgen.pattern.value)

    // Internal implementation details - bundled modules
    implementation(libs.testgen.generator.template)
    implementation(libs.testgen.pattern.support)

    // SLF4J API only (no backend - consumers bring their own)
    implementation(libs.slf4j.api)

    // Logging implementation for test (SLF4J backend)
    testRuntimeOnly(libs.logback.classic)
}
