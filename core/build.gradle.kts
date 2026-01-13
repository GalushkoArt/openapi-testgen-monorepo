import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.allure)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.binary.compatibility)
    alias(libs.plugins.dependency.versions)
    alias(libs.plugins.dependency.analysis)
}

group = "art.galushko.openapi.testgen"
version = libs.versions.openapi.testgen.get()
description = "Core test generation engine for OpenAPI Test Generator."

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    explicitApiWarning()
}

dependencies {
    api(kotlin("stdlib"))
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

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.allure.junit5)
    testImplementation(libs.allure.java.commons)
    testImplementation(libs.allure.assertj)
    testImplementation(libs.allure.attachments)
    testImplementation(libs.allure.generator)

    // Detekt formatting rules (wraps ktlint) — must match detekt plugin version
    detektPlugins(libs.detekt.formatting)
}

tasks.test {
    useJUnitPlatform()
    systemProperty(
        "allure.results.directory",
        "build/allure-results",
    )
    finalizedBy("koverXmlReport", "koverHtmlReport")
}

val warningsAsErrors: Provider<Boolean> =
    providers.gradleProperty("warningsAsErrors")
        .map(String::toBoolean)
        .orElse(false)

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        apiVersion.set(KOTLIN_2_2)
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
        allWarningsAsErrors.set(warningsAsErrors)
    }
}

apply(from = "../gradle/publishing.gradle.kts")

// Detekt configuration
detekt {
    config.setFrom(files("$projectDir/config/detekt.yml"))
    buildUponDefaultConfig = true
    baseline = file("$projectDir/config/detekt-baseline.xml")
    parallel = true
    autoCorrect = false
}

// Binary compatibility validator
apiValidation {
    // single-module; keep defaults
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

// Reproducible archives
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

// Kover configuration
kover {
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
                        minValue = 95
                    }
                }
            }
        }
    }
}

// Wire checks into the standard lifecycle
tasks.named("check") {
    dependsOn("detekt", "apiCheck", "projectHealth", "koverVerify")
}

val dokkaModuleId: String = project.projectDir.name

dokka {
    moduleName.set(dokkaModuleId)
    moduleVersion.set(project.version.toString())


    pluginsConfiguration {
        html {
            customStyleSheets.from(layout.projectDirectory.file("../docs/dokka/hide-platform-filters.css"))
        }
    }

    dokkaPublications.named("html") {
        outputDirectory.set(layout.projectDirectory.dir("../docs/api/$dokkaModuleId"))
        failOnWarning.set(false)
    }

    dokkaSourceSets.configureEach {
        reportUndocumented.set(false)
        skipEmptyPackages.set(true)
        perPackageOption {
            matchingRegex.set(".*\\.internal.*")
            suppress.set(true)
        }

        val sourceBaseUrl = providers.gradleProperty("dokkaSourceBaseUrl").orNull
        if (!sourceBaseUrl.isNullOrBlank()) {
            sourceLink {
                localDirectory.set(projectDir.resolve("src/main/kotlin"))
                remoteUrl.set(uri("$sourceBaseUrl/tree/master/$dokkaModuleId/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}
