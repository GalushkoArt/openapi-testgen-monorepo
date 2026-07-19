import org.gradle.kotlin.dsl.create

plugins {
    id("io.gitlab.arturbosch.detekt")
    id("com.github.ben-manes.versions")
    id("com.autonomousapps.dependency-analysis")
    id("org.jetbrains.kotlinx.kover")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

val catalog: VersionCatalog = the<VersionCatalogsExtension>().named("libs")
dependencies {
    "testImplementation"(platform(catalog.findLibrary("junit-bom").get()))
    "testRuntimeOnly"(catalog.findLibrary("junit-platform-launcher").get())
    "testImplementation"(catalog.findLibrary("junit-jupiter").get())
    "testImplementation"(catalog.findLibrary("junit-jupiter-api").get())
    "testImplementation"(catalog.findLibrary("junit-jupiter-params").get())
    "testImplementation"(catalog.findLibrary("assertj-core").get())
    "detektPlugins"(catalog.findLibrary("detekt-formatting").get())
}

// Create extension for configuration (shared by derived plugins)
val testgenQuality = extensions.create<TestgenQualityExtension>("testgenQuality")
testgenQuality.koverMinCoverage.convention(0)
testgenQuality.koverDisabledForTestTasks.convention(listOf<String>())

// Detekt and Kover configuration - deferred to allow extension configuration
afterEvaluate {
    detekt {
        // Use centralized config from build-logic (modules are siblings of build-logic)
        val buildLogicConfigDir = layout.projectDirectory.dir("../build-logic/config")
        config.setFrom(buildLogicConfigDir.file("detekt.yml"))
        buildUponDefaultConfig = true
        parallel = true
        autoCorrect = false
    }
    kover {
        currentProject {
            instrumentation {
                disabledForTestTasks.addAll(testgenQuality.koverDisabledForTestTasks.get())
            }
        }
        reports {
            filters {
                excludes {
                    classes("*.generated.*")
                }
            }
            total {
                verify {
                    rule {
                        bound {
                            minValue = testgenQuality.koverMinCoverage.get()
                        }
                    }
                }
            }
        }
    }
}

// Binary compatibility validator - single-module defaults
apiValidation {
    // Keep defaults
}

// Test task configuration
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    finalizedBy("koverXmlReport", "koverHtmlReport")
}

// Jackson must stay on the 2.x line (swagger modules do not fully support Jackson 3) and all
// Jackson modules on the runtime classpath must resolve to the version-catalog pins.
plugins.withId("java") {
    val jacksonCompatibilityCheck = tasks.register<JacksonCompatibilityCheckTask>("checkJacksonCompatibility") {
        group = "verification"
        description = "Verify resolved Jackson modules stay on the 2.x line and match the version catalog."
        rootComponent.set(
            configurations.named("runtimeClasspath").flatMap { it.incoming.resolutionResult.rootComponent },
        )
        expectedJacksonVersion.set(catalog.findVersion("jackson-lib").get().requiredVersion)
        expectedAnnotationsVersion.set(catalog.findVersion("jackson-annotations").get().requiredVersion)
    }
    tasks.named("check") {
        dependsOn(jacksonCompatibilityCheck)
    }
}

tasks.named("check") {
    dependsOn("detekt", "apiCheck", "projectHealth", "koverVerify")
}
