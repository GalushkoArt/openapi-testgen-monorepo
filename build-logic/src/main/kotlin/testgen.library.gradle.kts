import org.gradle.plugins.signing.Sign

plugins {
    id("testgen.kotlin-base")
    id("testgen.quality")
    id("com.vanniktech.maven.publish")
}

// Sign only when a key is configured (releases provide `signingInMemoryKey` via
// ORG_GRADLE_PROJECT_ env vars); local publishing to Maven Local — used by the consumer
// compatibility checks — runs unsigned.
tasks.withType<Sign>().configureEach {
    isRequired = providers.gradleProperty("signingInMemoryKey").isPresent
}

// Maven Central Publishing for Kotlin JVM libraries
mavenPublishing {
    publishToMavenCentral(automaticRelease = false)
    signAllPublications()

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
