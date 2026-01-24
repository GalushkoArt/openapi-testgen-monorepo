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
        gradlePluginPortal()
        mavenCentral()
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
