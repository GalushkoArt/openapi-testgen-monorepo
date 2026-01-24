plugins {
    id("testgen.library")
    id("io.qameta.allure")
}

// Configure Allure results directory for tests
tasks.withType<Test>().configureEach {
    systemProperty(
        "allure.results.directory",
        "build/allure-results",
    )
}

val catalog: VersionCatalog = the<VersionCatalogsExtension>().named("libs")
dependencies {
    "testImplementation"(catalog.findLibrary("allure-junit5").get())
    "testImplementation"(catalog.findLibrary("allure-java-commons").get())
    "testImplementation"(catalog.findLibrary("allure-assertj").get())
    "testImplementation"(catalog.findLibrary("allure-attachments").get())
    "testImplementation"(catalog.findLibrary("allure-generator").get())
}
