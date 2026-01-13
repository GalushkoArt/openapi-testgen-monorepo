import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

plugins.apply("maven-publish")
plugins.apply("signing")

val projectUrl = "https://docs.galushko.art/openapi-test-generator/"
val scmUrl = "https://github.com/GalushkoArt/openapi-testgen-monorepo"

plugins.withId("java") {
    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Javadoc jar."
}

plugins.withId("org.jetbrains.dokka") {
    val docTaskNames = listOf(
        "dokkaJavadoc",
        "dokkaGeneratePublicationJavadoc",
        "dokkaHtml",
        "dokkaGeneratePublicationHtml",
    )
    val docTask = docTaskNames.firstNotNullOfOrNull { name -> tasks.findByName(name) }
    if (docTask != null) {
        javadocJar.configure {
            dependsOn(docTask)
            from(docTask.outputs.files)
        }
    } else {
        logger.warn("No Dokka task found for ${project.path}; javadocJar will be empty.")
    }
}

extensions.configure<PublishingExtension> {
    publications {
        val isGradlePlugin = project.plugins.hasPlugin("java-gradle-plugin")
        if (!isGradlePlugin) {
            create<MavenPublication>("mavenJava") {
                from(project.components["java"])
            }
        }
        withType<MavenPublication>().configureEach {
            if (name == "mavenJava" || name == "pluginMaven") {
                artifact(javadocJar)
            }
            pom {
                name.set(project.name)
                description.set(project.description ?: "OpenAPI Test Generator module ${project.name}.")
                url.set(projectUrl)
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                scm {
                    url.set(scmUrl)
                    connection.set("scm:git:$scmUrl.git")
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
    }
    repositories {
        maven {
            name = "MavenCentral"
            // Maven Central Portal - https://central.sonatype.org/
            url = if (project.version.toString().endsWith("SNAPSHOT")) {
                uri("https://central.sonatype.com/repository/maven-snapshots/")
            } else {
                uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            }
            credentials {
                // Portal User Token - generate at https://central.sonatype.com/usertoken
                username = findProperty("centralPortalUsername") as String? ?: System.getenv("CENTRAL_PORTAL_USERNAME")
                password = findProperty("centralPortalPassword") as String? ?: System.getenv("CENTRAL_PORTAL_PASSWORD")
            }
        }
    }
}

val signingKey = providers.gradleProperty("signingKey")
    .orElse(providers.environmentVariable("SIGNING_KEY"))
val signingPassword = providers.gradleProperty("signingPassword")
    .orElse(providers.environmentVariable("SIGNING_PASSWORD"))

extensions.configure<SigningExtension> {
    if (signingKey.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassword.orNull)
        sign(extensions.getByType<PublishingExtension>().publications)
    }
}

tasks.withType<Sign>().configureEach {
    onlyIf { signingKey.isPresent }
}
