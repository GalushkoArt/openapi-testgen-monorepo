import org.gradle.api.tasks.Exec
import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask
import java.time.LocalDate.now

plugins {
    alias(libs.plugins.dokka)
}

// Force consistent Jackson version across all buildscript dependencies
// to avoid conflicts between Dokka (2.15.x) and OpenAPI Generator plugin (2.19.x)
buildscript {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.fasterxml.jackson.core" ||
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

tasks.register<Exec>("docsBuild") {
    group = "documentation"
    description = "Build documentation site via MkDocs (strict) into build/docs-site."
    dependsOn("dokkaHtmlAll")

    val outputDir = layout.buildDirectory.dir("docs-site")
    outputs.dir(outputDir)

    commandLine(
        "mkdocs",
        "build",
        "--strict",
        "-d",
        outputDir.get().asFile.absolutePath,
    )
}

tasks.register<Exec>("docsServe") {
    group = "documentation"
    description = "Serve documentation site via MkDocs (live reload)."
    dependsOn("dokkaHtmlAll")

    commandLine(
        "mkdocs",
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
            .map { it.relativeTo(docsOutputDir.get().asFile).path.replace("\\", "/") }
            .toList()

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
