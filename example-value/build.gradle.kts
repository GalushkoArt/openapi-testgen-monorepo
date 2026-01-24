plugins {
    id("testgen.library-with-allure")
}

description = "Schema-driven example value generation for OpenAPI Test Generator."

testgenQuality {
    koverMinCoverage = 95
}

dependencies {
    api(libs.testgen.model)
    api(libs.swagger.models)
    implementation(libs.swagger.parser.core)
    implementation(libs.swagger.parser)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.annotations)
    implementation(libs.slf4j.api)

    // Logging implementation for test (SLF4J backend)
    testRuntimeOnly(libs.logback.classic)
}
