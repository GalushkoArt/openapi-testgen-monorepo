import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import org.gradle.plugins.signing.Sign

plugins {
    id("testgen.library")
    id("java-gradle-plugin")
    alias(libs.plugins.plugin.publish)
}

description = "Gradle plugin for OpenAPI Test Generator."

val functionalTest: SourceSet = sourceSets.create("functionalTest")
configurations[functionalTest.implementationConfigurationName]
    .extendsFrom(configurations.getByName("testImplementation"))
configurations[functionalTest.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.getByName("testRuntimeOnly"))

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.kotlin.gradle.plugin)
    implementation(kotlin("stdlib"))
    api(libs.testgen.distribution.bundle)

    // Inside Gradle the daemon owns the SLF4J binding; a backend is only needed for tests
    testRuntimeOnly(libs.logback.classic)
    testImplementation(gradleApi())
    testImplementation(kotlin("reflect"))
    "functionalTestImplementation"(gradleTestKit())
}

testgenQuality {
    koverMinCoverage = 88
    koverDisabledForTestTasks = listOf("functionalTest", "compatibilityTest")
}

// TestKit tests run in separate Gradle processes, so they produce no coverage data;
// keep their classes out of the coverage model entirely.
kover {
    currentProject {
        sources {
            excludedSourceSets.add("functionalTest")
        }
    }
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
    group = "verification"
    description = "Runs Gradle TestKit functional tests."
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    filter {
        excludeTestsMatching("*ConsumerCompatibility*")
    }
    mustRunAfter(tasks.named("test"))
}

tasks.named("check") {
    dependsOn(functionalTestTask)
}

// Consumer compatibility checks run against artifacts published to Maven Local, so they are a
// dedicated opt-in task instead of part of `check`; `scripts/compat-check.sh` runs the sequence
// (publishAllToMavenLocal at the root, then this task).
tasks.register<Test>("compatibilityTest") {
    group = "verification"
    description = "Runs consumer compatibility checks (Gradle version and Jackson version matrix) " +
        "against artifacts published to Maven Local."
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    filter {
        includeTestsMatching("*ConsumerCompatibility*")
    }
    systemProperty("compat.plugin.version", version.toString())
    systemProperty(
        "compat.gradle.versions",
        providers.gradleProperty("compatGradleVersions").getOrElse("8.5,8.14.5,9.6.1"),
    )
    outputs.upToDateWhen { false }
    mustRunAfter(tasks.named("test"), functionalTestTask)
}

// Configure vanniktech maven-publish for Gradle plugin artifacts
// Use None() to skip javadoc jar - java-gradle-plugin already adds its own
mavenPublishing {
    configure(GradlePlugin(javadocJar = JavadocJar.None()))
}

gradlePlugin {
    website = "https://docs.galushko.art/openapi-test-generator/"
    vcsUrl = "https://github.com/GalushkoArt/openapi-testgen-monorepo"
    testSourceSets(functionalTest)
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

// java-gradle-plugin creates a pluginMaven publication that needs explicit signing task dependency
tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.withType<Sign>())
}
