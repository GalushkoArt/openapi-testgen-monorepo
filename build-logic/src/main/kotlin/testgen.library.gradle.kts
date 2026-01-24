import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("testgen.kotlin-base")
    id("testgen.quality")
    id("com.vanniktech.maven.publish")
}

val testgenLibrary = extensions.create<TestgenLibraryExtension>("testgenLibrary")
testgenLibrary.platformPublishing.convention(KotlinJvm(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml")))

// Maven Central Publishing for Kotlin JVM libraries
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
    signAllPublications()
    configure(testgenLibrary.platformPublishing.get())

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
