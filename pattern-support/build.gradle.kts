plugins {
    id("testgen.library-with-allure")
}

description = "Pattern integration layer for OpenAPI Test Generator."

testgenQuality {
    koverMinCoverage = 90
}

dependencies {
    api(libs.testgen.core)
    api(libs.testgen.pattern.value)
    api(libs.swagger.models)

    implementation(libs.testgen.example.value)
    implementation(libs.slf4j.api)

    // Logging implementation for test (SLF4J backend)
    testRuntimeOnly(libs.logback.classic)
}
