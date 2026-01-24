import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Base convention plugin providing common Kotlin/JVM configuration.
 *
 * This plugin provides:
 * - Kotlin JVM toolchain (Java 21)
 * - Kotlin compiler options (Kotlin 2.2, JSR305 strict)
 * - Java compiler options (UTF-8, parameters)
 * - Reproducible archives
 * - Binary compatibility validation
 * - Test task configuration with JUnit Platform
 * - Kover code coverage
 * - Dokka documentation
 *
 * This is the base for both testgen.library and testgen.gradle-plugin.
 */

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
}

// Group and version
group = "art.galushko.openapi.testgen"
version = extensions.findByType<VersionCatalogsExtension>()
    ?.named("libs")
    ?.findVersion("openapi-testgen")
    ?.orElse(null)
    ?.requiredVersion
    ?: providers.gradleProperty("openapi.testgen.version").orNull
        ?: error("Version not found. Ensure version catalog has 'openapi-testgen' version.")

// Kotlin JVM toolchain with explicit API
kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    explicitApiWarning()
}

dependencies {
    api(kotlin("stdlib"))
}

// warningsAsErrors support
val warningsAsErrors: Provider<Boolean> =
    providers.gradleProperty("warningsAsErrors")
        .map(String::toBoolean)
        .orElse(false)

// Kotlin compile options
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        apiVersion.set(KOTLIN_2_2)
        languageVersion.set(KOTLIN_2_2)
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
        allWarningsAsErrors.set(true)
        javaParameters.set(true)
    }
}

// Java compile options
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

// Reproducible archives
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

// Dokka configuration
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
