plugins {
    id("testgen.library-with-allure")
}

description = "Regex pattern value generation for OpenAPI Test Generator."

testgenQuality {
    koverMinCoverage = 90
}

dependencies {
    api(libs.testgen.example.value)
    api(libs.swagger.models)

    implementation(libs.regexp.gen)
    implementation(libs.slf4j.api)

    // Logging implementation for test (SLF4J backend)
    testRuntimeOnly(libs.logback.classic)
}
