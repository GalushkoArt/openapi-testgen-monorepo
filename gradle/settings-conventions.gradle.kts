import java.io.FileNotFoundException


apply(from = "./settings-base.gradle.kts")

pluginManagement {
    plugins {
        id("com.vanniktech.maven.publish") version "0.30.0"
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val relativePath = "gradle/libs.versions.toml"
            // Try finding it in the current root (Root build)
            val rootCatalog = file(relativePath)
            // Try finding it one level up (Included build like `plugin/`)
            val parentCatalog = file("../$relativePath")

            if (rootCatalog.exists()) {
                from(files(rootCatalog))
            } else if (parentCatalog.exists()) {
                from(files(parentCatalog))
            } else {
                throw FileNotFoundException("Could not locate $relativePath")
            }
        }
    }
}
