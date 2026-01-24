import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar

plugins {
    id("testgen.library")
    id("java-gradle-plugin")
    alias(libs.plugins.plugin.publish)
}

description = "Gradle plugin for OpenAPI Test Generator."

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.kotlin.gradle.plugin)
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    api(libs.testgen.distribution.bundle)

    // Logback is the logging backend for the Gradle plugin
    implementation(libs.logback.classic)
}

testgenQuality {
    koverMinCoverage = 88
}

testgenLibrary {
    // Use None() to completely skip javadoc jar handling by vanniktech
    // java-gradle-plugin already adds its own javadoc jar
    platformPublishing = GradlePlugin(javadocJar = JavadocJar.None())
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
