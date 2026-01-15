import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    application
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.shadow)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.binary.compatibility)
    alias(libs.plugins.dependency.versions)
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.maven.publish)
}

group = "art.galushko.openapi.testgen"
version = libs.versions.openapi.testgen.get()
description = "Command-line interface for OpenAPI Test Generator."

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    explicitApiWarning()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.testgen.distribution.bundle)

    implementation(libs.picocli)

    // Logback is the logging backend for the CLI application
    implementation(libs.logback.classic)
    implementation(libs.slf4j.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.jackson.kotlin)

    // Detekt formatting rules (wraps ktlint) — must match detekt plugin version
    detektPlugins(libs.detekt.formatting)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        apiVersion.set(KOTLIN_2_2)
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
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

application {
    applicationName = "openapi-testgen"
    mainClass = "art.galushko.openapi.testgen.cli.MainKt"
}

tasks.test {
    useJUnitPlatform {
        excludeTags("native-binary", "fat-jar")
    }
    finalizedBy("koverXmlReport", "koverHtmlReport")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("openapi-testgen")
            mainClass.set("art.galushko.openapi.testgen.cli.MainKt")
        }
    }
    toolchainDetection.set(true)
}

val testSourceSet: SourceSet = the<SourceSetContainer>()["test"]
val mainSourceSet: SourceSet = the<SourceSetContainer>()["main"]

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "OpenAPI Test Generator",
            "Implementation-Version" to project.version,
        )
    }
}

// Shadow JAR configuration for fat JAR distribution
tasks.shadowJar {
    archiveBaseName.set("openapi-testgen")
    archiveClassifier.set("all")
    archiveVersion.set(project.version.toString())

    manifest {
        attributes(
            "Main-Class" to "art.galushko.openapi.testgen.cli.MainKt",
            "Implementation-Title" to "OpenAPI Test Generator",
            "Implementation-Version" to project.version,
        )
    }

    // Merge service files for SLF4J, Jackson, etc.
    mergeServiceFiles()

    // Relocate dependencies to avoid classpath conflicts
    relocate("com.fasterxml.jackson", "art.galushko.openapi.testgen.shadow.jackson")
    relocate("com.github.mustachejava", "art.galushko.openapi.testgen.shadow.mustache")
    relocate("org.yaml.snakeyaml", "art.galushko.openapi.testgen.shadow.snakeyaml")

    // Exclude unnecessary files
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/NOTICE*")
    exclude("META-INF/LICENSE*")

    // Reproducible builds
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val nativeBinaryName = provider {
    if (System.getProperty("os.name").contains("win", ignoreCase = true)) {
        "openapi-testgen.exe"
    } else {
        "openapi-testgen"
    }
}

val nativeBinaryFile: Provider<RegularFile> = layout.buildDirectory.file(
    nativeBinaryName.map { "native/nativeCompile/$it" }
)

// Base configuration common to all smoke tests
fun Test.configureBaseSmokeTest(descriptionSuffix: String) {
    group = "verification"
    description = "Run smoke tests against $descriptionSuffix"

    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath

    filter {
        includeTestsMatching("*DistributionSmokeTest*")
    }
}

fun Test.dependsOnFatJar() {
    dependsOn(tasks.shadowJar)
    // Pass the path as a system property
    environment("TEST_FATJAR_PATH", tasks.shadowJar.flatMap { it.archiveFile }.get().asFile.absolutePath)
}

fun Test.dependsOnNative() {
    dependsOn(tasks.named("nativeCompile"))
    environment("TEST_NATIVE_PATH", nativeBinaryFile.get().asFile.absolutePath)
}

tasks.register<Test>("testFatJar") {
    configureBaseSmokeTest("fat JAR")
    useJUnitPlatform {
        includeTags("fat-jar")
        excludeTags("native-binary")
    }
    dependsOnFatJar()
}

tasks.register<Test>("testNative") {
    configureBaseSmokeTest("native binary")
    useJUnitPlatform {
        excludeTags("fat-jar")
        includeTags("native-binary")
    }
    dependsOnNative()
}

tasks.register<Test>("testDistributions") {
    configureBaseSmokeTest("fat JAR and native binary")
    description = "Verify all distribution formats work correctly"
    useJUnitPlatform {
        includeTags("fat-jar", "native-binary")
    }
    // Simply compose the behaviors
    dependsOnFatJar()
    dependsOnNative()
}

// Native image configuration regeneration
// Runs the tracing agent against representative workloads to capture reflection/resource metadata
val nativeImageConfigDir = "src/main/resources/META-INF/native-image/art.galushko.openapi.testgen"

tasks.register("regenerateNativeImageConfig") {
    group = "native"
    description = "Regenerate native-image reflection/resource configs using the tracing agent"
    val execOps = serviceOf<ExecOperations>()
    dependsOn(tasks.named("classes"))

    val agentOutputDir = layout.buildDirectory.dir("native-image-agent-output")
    val specFile = file("src/test/resources/openapi.yaml")
    val outputDir = layout.buildDirectory.dir("native-agent-test-output")
    val sourceConfigDir = file(nativeImageConfigDir)
    val configFiles = listOf(
        "reflect-config.json",
        "resource-config.json",
        "jni-config.json",
        "proxy-config.json",
        "serialization-config.json",
    )
    val os = org.gradle.internal.os.OperatingSystem.current()
    val agentLibName = when {
        os.isWindows -> "native-image-agent.dll"
        os.isMacOsX -> "libnative-image-agent.dylib"
        else -> "libnative-image-agent.so"
    }

    doFirst {
        agentOutputDir.get().asFile.mkdirs()
        outputDir.get().asFile.mkdirs()

        val javaHomePath = System.getenv("GRAALVM_HOME")
            ?: System.getenv("JAVA_HOME")
            ?: System.getProperty("java.home")
        val javaHome = file(javaHomePath)
        val agentCandidates = listOf(
            javaHome.resolve("lib/$agentLibName"),
            javaHome.resolve("lib/svm/$agentLibName"),
            javaHome.resolve("bin/$agentLibName"),
        )
        require(agentCandidates.any { it.exists() }) {
            "native-image-agent not found. Set JAVA_HOME or GRAALVM_HOME to GraalVM and run `gu install native-image`."
        }

        // Seed the merge directory with the current configs so manual entries are preserved.
        configFiles.forEach { configFile ->
            val existing = sourceConfigDir.resolve(configFile)
            if (existing.exists()) {
                existing.copyTo(agentOutputDir.get().file(configFile).asFile, overwrite = true)
            }
        }
    }

    doLast {
        val javaHomePath = System.getenv("GRAALVM_HOME")
            ?: System.getenv("JAVA_HOME")
            ?: System.getProperty("java.home")
        val javaBin = if (os.isWindows) "bin/java.exe" else "bin/java"
        val javaExecutable = file(javaHomePath).resolve(javaBin).absolutePath
        val classpath = mainSourceSet.runtimeClasspath.asPath
        val mainClass = "art.galushko.openapi.testgen.cli.MainKt"
        val agentArg = "-agentlib:native-image-agent=config-merge-dir=${agentOutputDir.get().asFile.absolutePath}"

        // Run test-suite-writer generator (JSON output)
        execOps.exec {
            commandLine(
                javaExecutable, agentArg, "-cp", classpath, mainClass,
                "--spec-file", specFile.absolutePath,
                "--output-dir", outputDir.get().asFile.absolutePath,
                "--generator", "test-suite-writer",
                "--setting", "validSecurityValues.ApiKeyAuth=test-api-key",
                "--generator-option", "outputFileName=generated.json",
            )
        }

        // Run template generator (Mustache templates - captures template reflection)
        execOps.exec {
            commandLine(
                javaExecutable, agentArg, "-cp", classpath, mainClass,
                "--spec-file", specFile.absolutePath,
                "--output-dir", outputDir.get().asFile.absolutePath,
                "--generator", "template",
                "--setting", "validSecurityValues.ApiKeyAuth=test-api-key",
                "--generator-option", "templateSet=restassured-java",
                "--generator-option", "package=com.example.test",
            )
        }

        // Copy generated configs to source directory (excluding native-image.properties which is manually maintained)
        configFiles.forEach { configFile ->
            val generated = agentOutputDir.get().file(configFile).asFile
            if (generated.exists() && generated.length() > 10) { // Skip empty/trivial files
                generated.copyTo(sourceConfigDir.resolve(configFile), overwrite = true)
                println("Updated: $nativeImageConfigDir/$configFile")
            }
        }
        println("\nReview the generated configs and commit the changes.")
        println("Note: native-image.properties is manually maintained and not overwritten.")
    }
}

// Kover configuration
kover {
    currentProject {
        instrumentation {
            disabledForTestTasks.addAll("testFatJar", "testNative", "testDistributions", "nativeTest")
        }
    }
    reports {
        total {
            verify {
                rule {
                    bound {
                        minValue = 80
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
