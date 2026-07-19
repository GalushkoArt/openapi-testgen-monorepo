plugins {
    id("testgen.library")
    id("io.qameta.allure")
}

allure {
    adapter {
        // The weaver agent is wired manually below: allure-gradle's own agent provider holds a
        // raw Configuration and fails Test-task validation under the configuration cache.
        aspectjWeaver.set(false)
    }
}

val catalog: VersionCatalog = the<VersionCatalogsExtension>().named("libs")

// AspectJ weaving powers @Step annotations and allure-assertj interception.
val aspectjWeaverAgent: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isVisible = false
}

// Configure Allure results directory and the weaver agent for tests
tasks.withType<Test>().configureEach {
    systemProperty(
        "allure.results.directory",
        "build/allure-results",
    )
    jvmArgumentProviders.add(AspectJWeaverAgentProvider(aspectjWeaverAgent))
}

dependencies {
    "aspectjWeaverAgent"(catalog.findLibrary("aspectjweaver").get())
    "testImplementation"(catalog.findLibrary("allure-junit5").get())
    "testImplementation"(catalog.findLibrary("allure-java-commons").get())
    "testImplementation"(catalog.findLibrary("allure-assertj").get())
    "testImplementation"(catalog.findLibrary("allure-attachments").get())
    "testImplementation"(catalog.findLibrary("allure-generator").get())
}
