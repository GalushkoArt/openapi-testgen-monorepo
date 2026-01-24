plugins {
    id("testgen.library-with-allure")
}

description = "Core test generation engine for OpenAPI Test Generator."

testgenQuality {
    koverMinCoverage = 95
}

dependencies {
    api(libs.testgen.model)
    api(libs.testgen.example.value)
    api(libs.swagger.models)
    implementation(libs.slf4j.api)
    api(libs.jackson.databind)
    implementation(libs.jackson.core)
    implementation(libs.jackson.yaml)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.annotations)
    implementation(libs.swagger.parser.core)
    implementation(libs.swagger.parser)
    implementation(libs.commons.lang3)

    // Logging implementation for test (SLF4J backend)
    testRuntimeOnly(libs.logback.classic)
}
