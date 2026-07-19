plugins {
    id("testgen.library-with-allure")
}

description = "Mustache-based template generator for OpenAPI Test Generator."

testgenQuality {
    koverMinCoverage = 95
}

dependencies {
    api(libs.testgen.core)
    implementation(libs.testgen.model)

    implementation(libs.slf4j.api)
    implementation(libs.jackson.databind)
    implementation(libs.mustache)

    // Logging implementation for test (SLF4J backend)
    testRuntimeOnly(libs.logback.classic)
}
