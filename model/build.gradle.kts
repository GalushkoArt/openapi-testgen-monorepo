import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
    alias(libs.plugins.binary.compatibility)
    alias(libs.plugins.maven.publish)
}

group = "art.galushko.openapi.testgen"
version = libs.versions.openapi.testgen.get()
description = "Shared data models for OpenAPI Test Generator."

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    explicitApiWarning()
}

dependencies {
    // no external deps
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        apiVersion.set(KOTLIN_2_2)
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

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
                        minValue = 0
                    }
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy("koverXmlReport", "koverHtmlReport")
}

tasks.named("check") {
    dependsOn("koverVerify")
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

// Maven Central Publishing
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
    signAllPublications()
    configure(KotlinJvm(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml")))

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
