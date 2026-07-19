import art.galushko.openapi.testgen.model.error.ErrorMode
import art.galushko.openapi.testgen.plugin.OpenApiTestGeneratorTask

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.23.0"
    id("art.galushko.openapi-test-generator")
}

group = "art.galushko"
version = libs.versions.openapi.testgen.get()

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.openapitools:jackson-databind-nullable:0.2.10")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.52")

    // Kotlin dependencies
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.rest-assured:rest-assured:5.5.7")
    testImplementation("io.rest-assured:kotlin-extensions:5.5.7")
}

tasks.test { useJUnitPlatform() }

// OpenAPI Generator configuration (server-side Spring interfaces)
openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(file("${rootDir}/samples/openapi.yaml").toURI().toString())
    outputDir.set("${project.layout.buildDirectory.get().asFile.absolutePath}/generated/openapi")
    apiPackage.set("art.galushko.kotlin.spring.rest.assured.api")
    modelPackage.set("art.galushko.kotlin.spring.rest.assured.model")
    invokerPackage.set("art.galushko.kotlin.spring.rest.assured.invoker")
    library.set("spring-boot")
    configOptions.set(
        mapOf(
            "useSpringBoot3" to "true",
            "interfaceOnly" to "false",
            "delegatePattern" to "true",
            "useTags" to "true",
            "useSpringBuiltInValidation" to "true",
            "dateLibrary" to "java8",
            "skipDefaultInterface" to "false",
        ),
    )
    globalProperties.set(
        mapOf(
            "modelDocs" to "false",
            "apis" to "",
            "models" to ""
        ),
    )
}

sourceSets {
    val main by getting
    main.java.srcDir("${buildDir}/generated/openapi/src/main/java")
}

tasks.named("compileKotlin") { dependsOn(tasks.named("openApiGenerate")) }

// Configure custom OpenAPI Test Generator plugin
openApiTestGenerator {
    specFile.set(file("${rootDir}/samples/openapi.yaml").toURI().toString())
    // Tests will be generated into build dir and added to test sources automatically by the plugin
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests/art/galushko/kotlin/spring/rest/assured"))
    generator.set("template")
    generatorOptions.putAll(
        mapOf(
            "templateSet" to "restassured-kotlin",
            "templateVariables" to mapOf(
                "package" to "art.galushko.kotlin.spring.rest.assured.generatedtests",
                "baseUrl" to "http://localhost:8080/v1",
                "springBootTest" to "true",
            ),
        )
    )
    testGenerationSettings {
        ignoreTestCases.putAll(mapOf("/orders" to mapOf("GET" to listOf("Invalid Query page parameter: Integer Breaking"))))
        validSecurityValues.putAll(mapOf("ApiKeyAuth" to "test-api-key-123"))
        overrideBasicTestData.putAll(mapOf("invalidApiKey" to "unrealistic_key"))
        maxSchemaDepth.set(15)
        maxSchemaCombinations.set(100)
        maxTestCasesPerOperation.set(1000)
        maxErrors.set(100)
        errorMode.set(ErrorMode.FAIL_FAST)
    }
    manualOnly.set(false) // set value to true to run the job manually
}

// Additional generation into src/test/kotlin with a different base package
tasks.register<OpenApiTestGeneratorTask>("generateOpenApiTestsToSrc") {
    configFile.set(layout.projectDirectory.file("open-api-test-generation-config.yaml"))
}

// Ensure compilation and tests depend on the additional generation
tasks.named("compileTestKotlin") { dependsOn(tasks.named("generateOpenApiTestsToSrc")) }

// Another generation task with custom templates
tasks.register<OpenApiTestGeneratorTask>("generateOpenApiTestsCustomTemplate") {
    specFile.set(file("${rootDir}/samples/openapi.yaml").toURI().toString())
    outputDir.set(layout.projectDirectory.dir("src/test/kotlin/art/galushko/kotlin/spring/rest/assured/custom"))
    generator.set("template")
    generatorOptions.putAll(
        mapOf(
            "templateSet" to "restassured-kotlin",
            "customTemplateDir" to "samples/kotlin-spring-rest-assured/templates",
            "classTemplatePath" to "class.mustache",
            "outputFileExtension" to "kt",
            "templateVariables" to mapOf(
                "package" to "art.galushko.kotlin.spring.rest.assured.custom",
                "baseUrl" to "http://localhost:8080/v1",
                "springBootTest" to "true",
            ),
        )
    )
    testGenerationSettings {
        validSecurityValues.putAll(mapOf("ApiKeyAuth" to "test-api-key-123"))
    }
}

tasks.named("compileTestKotlin") { dependsOn(tasks.named("generateOpenApiTestsCustomTemplate")) }
