import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    id("java-gradle-plugin")
    alias(libs.plugins.plugin.publish)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.binary.compatibility)
    alias(libs.plugins.dependency.versions)
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.maven.publish)
}

group = "art.galushko.openapi.testgen"
version = libs.versions.openapi.testgen.get()
description = "Gradle plugin for OpenAPI Test Generator."

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    explicitApiWarning()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.kotlin.gradle.plugin)
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    api(libs.testgen.distribution.bundle)

    // Logback is the logging backend for the Gradle plugin
    implementation(libs.logback.classic)

    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.api)

    // Detekt formatting rules (wraps ktlint) — must match detekt plugin version
    detektPlugins(libs.detekt.formatting)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy("koverXmlReport", "koverHtmlReport")
}

val warningsAsErrors: Provider<Boolean> =
    providers.gradleProperty("warningsAsErrors")
        .map(String::toBoolean)
        .orElse(false)

tasks.withType<KotlinCompile> {
    compilerOptions {
        apiVersion.set(KOTLIN_2_2)
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
        allWarningsAsErrors.set(warningsAsErrors)
    }
}

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

gradlePlugin {
    website = "https://docs.galushko.art/openapi-test-generator/"
    vcsUrl = "https://github.com/GalushkoArt/openapi-testgen-monorepo"
    plugins {
        create("openapiTestGenerator") {
            id = "art.galushko.openapi-test-generator"
            displayName = "OpenAPI Test Generator"
            description = "Generate API test cases from OpenAPI specifications."
            implementationClass = "art.galushko.openapi.testgen.plugin.OpenApiTestGeneratorPlugin"
            tags.set(listOf("openapi", "testing", "generator", "api"))
        }
    }
}

// Maven Central Publishing
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
    signAllPublications()
    // Use None() to completely skip javadoc jar handling by vanniktech
    // java-gradle-plugin already adds its own javadoc jar
    configure(GradlePlugin(javadocJar = JavadocJar.None()))

    coordinates(
        groupId = project.group.toString(),
        artifactId = project.name,
        version = project.version.toString()
    )

    pom {
        name.set(project.name)
        description.set(project.description)
        url.set("https://docs.galushko.art/openapi-test-generator/")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        scm {
            url.set("https://github.com/GalushkoArt/openapi-testgen-monorepo")
            connection.set("scm:git:https://github.com/GalushkoArt/openapi-testgen-monorepo.git")
            developerConnection.set("scm:git:ssh://git@github.com/GalushkoArt/openapi-testgen-monorepo.git")
        }

        developers {
            developer {
                id.set("GalushkoArt")
                name.set("Artem Galushko")
            }
        }
    }
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
        total {
            verify {
                rule {
                    bound {
                        minValue = 95
                    }
                }
            }
            filters {
                includes {
                    classes("art.galushko.openapi.testgen.TestGenerationSettingsExtension")
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

