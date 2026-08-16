import art.galushko.openapi.testgen.model.error.ErrorMode
import art.galushko.openapi.testgen.plugin.OpenApiTestGeneratorTask

plugins {
    id("java")
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.24.0"
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

    implementation("art.galushko.openapi.testgen:model")
    implementation("org.openapitools:jackson-databind-nullable:0.2.11")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.53")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.rest-assured:rest-assured:5.5.7")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.22.0")
}

tasks.test { useJUnitPlatform() }

// OpenAPI Generator configuration (server-side Spring interfaces)
openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(file("${rootDir}/samples/openapi.yaml").toURI().toString())
    outputDir.set("${project.layout.buildDirectory.get().asFile.absolutePath}/generated/openapi")
    apiPackage.set("art.galushko.java.spring.file.writer.api")
    modelPackage.set("art.galushko.java.spring.file.writer.model")
    invokerPackage.set("art.galushko.java.spring.file.writer.invoker")
    library.set("spring-boot")
    openapiGeneratorIgnoreList = listOf("**/ApiUtil.java")
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
    main.java.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/java"))
}

tasks.named("compileJava") { dependsOn(tasks.named("openApiGenerate")) }

// Configure custom OpenAPI Test Generator plugin
openApiTestGenerator {
    specFile.set(file("${rootDir}/samples/openapi.yaml").toURI().toString())
    outputDir.set(layout.projectDirectory.dir("src/test/resources"))
    generator.set("test-suite-writer")
    generatorOptions.putAll(
        mapOf(
            "outputFileName" to "openapi-test-suites.json",
            "writeMode" to "MERGE",
            "preventOverwriteSuites" to "false",
            "preventOverwriteCases" to "true",
            "protectedTestCaseFields" to "expectedStatusCode,expectedBody",
            "indent" to "    ",
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

// Add generated test suites to the test resources when set manualOnly to false and tests generated to test resources
tasks.named<Copy>("processTestResources") {
    dependsOn(tasks.named("generateOpenApiTests"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

// Additional generation in yaml format
tasks.register<OpenApiTestGeneratorTask>("generateOpenApiTestsYaml") {
    configFile.set(layout.projectDirectory.file("open-api-test-generation-config.yaml"))
    testGenerationSettings {
        validSecurityValues.putAll(mapOf("ApiKeyAuth" to "test-api-key-12")) // optional pass to override
    }
}

// Add test suites generation in a build chain
tasks.named("compileTestJava") { dependsOn(tasks.named("generateOpenApiTestsYaml")) }
