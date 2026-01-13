import art.galushko.openapi.testgen.model.error.ErrorMode
import art.galushko.openapi.testgen.plugin.OpenApiTestGeneratorTask

plugins {
    id("java")
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.openapi.generator") version "7.7.0"
    id("art.galushko.openapi-test-generator")
}

group = "art.galushko"
version = libs.versions.openapi.testgen.get()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.35")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.rest-assured:rest-assured:5.5.0")
}

tasks.test { useJUnitPlatform() }

// OpenAPI Generator configuration (server-side Spring interfaces)
openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(file("${rootDir}/samples/openapi.yaml").toURI().toString())
    outputDir.set("${project.layout.buildDirectory.get().asFile.absolutePath}/generated/openapi")
    apiPackage.set("art.galushko.java.spring.rest.assured.api")
    modelPackage.set("art.galushko.java.spring.rest.assured.model")
    invokerPackage.set("art.galushko.java.spring.rest.assured.invoker")
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

tasks.named("compileJava") { dependsOn(tasks.named("openApiGenerate")) }

// Configure custom OpenAPI Test Generator plugin
openApiTestGenerator {
    specFile.set(file("${rootDir}/samples/openapi.yaml").toURI().toString())
    // Tests will be generated into build dir and added to test sources automatically by the plugin
    outputDir.set(layout.buildDirectory.dir("generated/openapi-tests/art/galushko/java/spring/rest/assured"))
    generator.set("template")
    generatorOptions.putAll(
        mapOf(
            "templateSet" to "restassured-java",
            "templateVariables" to mapOf(
                "package" to "art.galushko.java.spring.rest.assured.generatedtests",
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

// Additional generation into src/test/java with a different base package
tasks.register<OpenApiTestGeneratorTask>("generateOpenApiTestsToSrc") {
    configFile.set("open-api-test-generation-config.yaml")
}

// Ensure compilation and tests depend on the additional generation
tasks.named("compileTestJava") { dependsOn(tasks.named("generateOpenApiTestsToSrc")) }

