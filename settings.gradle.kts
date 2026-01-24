apply(from = "./gradle/settings-base.gradle.kts")

rootProject.name = "openapi.testgen.monorepo"

includeBuild("build-logic")
includeBuild("model")
includeBuild("example-value")
includeBuild("core")
includeBuild("generator-template")
includeBuild("pattern-value")
includeBuild("pattern-support")
includeBuild("distribution-bundle")
includeBuild("plugin")
includeBuild("cli")

include(
    "samples:java-spring-rest-assured",
    "samples:java-spring-file-writer",
    "samples:kotlin-spring-rest-assured",
)
