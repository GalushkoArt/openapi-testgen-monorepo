plugins {
    id("testgen.library")
}

description = "Shared data models for OpenAPI Test Generator."

testgenQuality {
    koverMinCoverage = 70
}

dependencies {
    // no external deps
}
