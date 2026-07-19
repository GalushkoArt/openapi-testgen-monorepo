import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask
import java.time.LocalDate.now
import javax.inject.Inject

plugins {
    base
    alias(libs.plugins.dokka)
    alias(libs.plugins.dependency.versions)
}

// Force a consistent Jackson version across the buildscript classpath to avoid clashes
// between Dokka (root build) and the OpenAPI Generator plugin applied in the samples.
buildscript {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-annotations") {
                // jackson-annotations dropped the patch component starting with 2.20 (e.g. "2.22")
                useVersion(libs.versions.jackson.annotations.get())
            } else if (requested.group == "com.fasterxml.jackson.core" ||
                requested.group == "com.fasterxml.jackson.dataformat" ||
                requested.group == "com.fasterxml.jackson.module"
            ) {
                useVersion(libs.versions.jackson.lib.get())
            }
        }
    }
}

val includedBuilds = listOf(
    "model",
    "example-value",
    "core",
    "generator-template",
    "pattern-value",
    "pattern-support",
    "distribution-bundle",
    "plugin",
    "cli",
)

val dokkaModuleCoordinates = includedBuilds.map { buildName ->
    "art.galushko.openapi.testgen:$buildName"
}

// Aggregate lifecycle: `./gradlew check` (or `build`) verifies every included build — enumerated
// dynamically so a new module can never be forgotten — plus the samples via task-name matching.
tasks.named("check") {
    dependsOn(gradle.includedBuilds.map { it.task(":check") })
}

// The sample apps all bind the fixed port 8080; serialize their test tasks so parallel
// scheduling cannot make two Spring contexts race for the same port.
abstract class SampleAppPortLock : BuildService<BuildServiceParameters.None>

val sampleAppPortLock = gradle.sharedServices.registerIfAbsent("sampleAppPortLock", SampleAppPortLock::class) {
    maxParallelUsages.set(1)
}

subprojects {
    tasks.withType<Test>().configureEach {
        usesService(sampleAppPortLock)
    }
}

tasks.register("publishAllToMavenCentral") {
    group = "publishing"
    description = "Upload every library module to the Maven Central Portal (manual release mode)."
    dependsOn(includedBuilds.map { buildName ->
        gradle.includedBuild(buildName).task(":publishAllPublicationsToMavenCentralRepository")
    })
}

tasks.register("publishAllToMavenLocal") {
    group = "publishing"
    description = "Publish every module to Maven Local (prerequisite for consumer compatibility checks)."
    dependsOn(includedBuilds.map { buildName ->
        gradle.includedBuild(buildName).task(":publishToMavenLocal")
    })
}

val docsRequirementsFile = layout.projectDirectory.file("requirements.txt")
val docsVenvDir = layout.projectDirectory.dir(".gradle/docs-python")
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val defaultPythonExecutable = if (isWindows) "python" else "python3"
val docsPythonExecutable = providers.gradleProperty("docsPython").orElse(defaultPythonExecutable)
val docsVenvPython = docsVenvDir.file(if (isWindows) "Scripts/python.exe" else "bin/python")
val docsVenvMkDocs = docsVenvDir.file(if (isWindows) "Scripts/mkdocs.exe" else "bin/mkdocs")
val docsRequirementsMarker = docsVenvDir.file("requirements-installed.txt")

abstract class DocsInstallPythonDeps : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val requirementsFile: RegularFileProperty

    @get:Input
    abstract val pythonExecutable: Property<String>

    @get:Internal
    abstract val venvPython: RegularFileProperty

    @get:OutputFile
    abstract val requirementsMarker: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun install() {
        execOperations.exec {
            commandLine(
                venvPython.get().asFile.absolutePath,
                "-m",
                "pip",
                "install",
                "--disable-pip-version-check",
                "-r",
                requirementsFile.get().asFile.absolutePath,
            )
        }

        requirementsMarker.get().asFile.writeText(
            "python=${pythonExecutable.get()}\n\n${requirementsFile.get().asFile.readText()}",
        )
    }
}

dependencies {
    dokkaModuleCoordinates.forEach { coordinate ->
        add("dokka", coordinate)
    }
}

dokka {
    moduleName.set("OpenAPI Test Generator API")
    pluginsConfiguration {
        html {
            customStyleSheets.from(layout.projectDirectory.file("docs/dokka/hide-platform-filters.css"))
        }
    }

    dokkaPublications.named("html") {
        outputDirectory.set(layout.projectDirectory.dir("docs/api"))
        failOnWarning.set(false)
    }
}

tasks.named<DokkaGeneratePublicationTask>("dokkaGeneratePublicationHtml") {
    dependsOn(includedBuilds.map { buildName ->
        gradle.includedBuild(buildName).task(":dokkaGenerateModuleHtml")
    })
}

tasks.register("dokkaHtmlAll") {
    group = "documentation"
    description = "Generate combined Dokka HTML API docs into docs/api/."
    dependsOn("dokkaGenerateHtml")
}

tasks.register<Exec>("docsCreatePythonVenv") {
    group = "documentation"
    description = "Create the documentation Python environment."

    inputs.property("pythonExecutable", docsPythonExecutable)
    outputs.file(docsVenvPython)

    commandLine(
        docsPythonExecutable.get(),
        "-m",
        "venv",
        "--clear",
        docsVenvDir.asFile.absolutePath,
    )
}

tasks.register<DocsInstallPythonDeps>("docsInstallPythonDeps") {
    group = "documentation"
    description = "Install MkDocs requirements into the documentation Python environment."
    dependsOn("docsCreatePythonVenv")

    requirementsFile.set(docsRequirementsFile)
    pythonExecutable.set(docsPythonExecutable)
    venvPython.set(docsVenvPython)
    requirementsMarker.set(docsRequirementsMarker)
}

tasks.register<Copy>("docsSyncChangelog") {
    group = "documentation"
    description = "Copy the root CHANGELOG.md into the docs tree for MkDocs."
    from(layout.projectDirectory.file("CHANGELOG.md"))
    into(layout.projectDirectory.dir("docs/changelog"))
}

tasks.register<Exec>("docsBuild") {
    group = "documentation"
    description = "Build documentation site via MkDocs (strict) into build/docs-site."
    dependsOn("dokkaHtmlAll", "docsInstallPythonDeps", "docsSyncChangelog")

    val outputDir = layout.buildDirectory.dir("docs-site")
    inputs.dir(layout.projectDirectory.dir("docs"))
    inputs.dir(layout.projectDirectory.dir("mkdocs"))
    inputs.file(layout.projectDirectory.file("mkdocs.yml"))
    outputs.dir(outputDir)

    commandLine(
        docsVenvMkDocs.asFile.absolutePath,
        "build",
        "--strict",
        "-d",
        outputDir.get().asFile.absolutePath,
    )
}

tasks.register<Exec>("docsServe") {
    group = "documentation"
    description = "Serve documentation site via MkDocs (live reload)."
    dependsOn("dokkaHtmlAll", "docsInstallPythonDeps", "docsSyncChangelog")

    commandLine(
        docsVenvMkDocs.asFile.absolutePath,
        "serve",
    )
}

tasks.register("generateDokkaSitemap") {
    group = "documentation"
    description = "Generate sitemap entries for Dokka API docs and merge with MkDocs sitemap."
    dependsOn("docsBuild")

    val siteUrl = "https://docs.galushko.art/openapi-test-generator"
    val docsOutputDir = layout.buildDirectory.dir("docs-site")

    doLast {
        val sitemapFile = docsOutputDir.get().file("sitemap.xml").asFile
        val originalContent = sitemapFile.readText()

        // Find all HTML files in api/ directory
        val apiDir = docsOutputDir.get().dir("api").asFile
        val htmlFiles = apiDir.walkTopDown()
            .filter { it.isFile && it.extension == "html" }
            .map {
                it.relativeTo(docsOutputDir.get().asFile).path.replace("\\", "/").removeSuffix("index.html").removeSuffix(".html")
            }.toList()

        // Generate sitemap entries for Dokka pages
        val dokkaEntries = htmlFiles.joinToString("\n") { path ->
            """  <url>
    <loc>$siteUrl/$path</loc>
    <lastmod>${now()}</lastmod>
  </url>"""
        }

        // Insert before closing </urlset>
        val mergedContent = originalContent.replace(
            "</urlset>",
            "$dokkaEntries\n</urlset>",
        )

        sitemapFile.writeText(mergedContent)
        println("Added ${htmlFiles.size} Dokka pages to sitemap.xml")
    }
}

tasks.register("docsFullBuild") {
    group = "documentation"
    description = "Build documentation site with complete sitemap including Dokka API docs."
    dependsOn("generateDokkaSitemap")
}
