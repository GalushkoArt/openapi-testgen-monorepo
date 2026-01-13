rootProject.name = "distribution-bundle"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

buildCache {
    local {
        isEnabled = true
        // Keep cache data inside the project to ensure it's local-only to this repo
        directory = File(rootDir, ".gradle/build-cache")
    }
    remote(HttpBuildCache::class) {
        isEnabled = false
    }
}
