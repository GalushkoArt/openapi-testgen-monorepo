package art.galushko.openapi.testgen.plugin

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path

/**
 * Consumer-facing compatibility checks. Unlike [OpenApiTestGeneratorFunctionalTest], the plugin
 * and its whole dependency stack are consumed from Maven Local as real published artifacts (no
 * TestKit classpath injection), which exercises:
 *  - consumer projects running different Gradle versions, and
 *  - consumer buildscript classpaths that request or force different Jackson versions
 *    (for example another build plugin such as openapi-generator dragging in its own Jackson).
 *
 * Prerequisite: `./gradlew publishAllToMavenLocal` at the repository root. These tests only run
 * from the dedicated `compatibilityTest` task (tagged "compat", excluded from `functionalTest`);
 * `scripts/compat-check.sh` runs the whole sequence.
 */
@Tag("compat")
@DisplayName("Consumer compatibility checks against published artifacts")
class ConsumerCompatibilityFunctionalTest {

    @TempDir
    lateinit var projectDir: Path

    private val pluginVersion: String = System.getProperty("compat.plugin.version")
        ?: error("System property compat.plugin.version is not set; run via the compatibilityTest task")

    private fun writeConsumerProject(buildscriptBlock: String = "") {
        projectDir.resolve("settings.gradle.kts").toFile().writeText(
            """
            pluginManagement {
                repositories {
                    mavenLocal()
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            rootProject.name = "consumer-compat-test"
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").toFile().writeText(
            """
            $buildscriptBlock
            plugins {
                id("art.galushko.openapi-test-generator") version "$pluginVersion"
            }

            openApiTestGenerator {
                specFile.set("openapi.yaml")
                outputDir.set(layout.buildDirectory.dir("generated-tests"))
                generator.set("test-suite-writer")
                generatorOptions.put("format", "json")
                generatorOptions.put("outputMode", "MULTIPLE_FILES")
            }
            """.trimIndent(),
        )
        projectDir.resolve("openapi.yaml").toFile().writeText(MINIMAL_SPEC)
    }

    private fun runGeneration(gradleVersion: String? = null): org.gradle.testkit.runner.BuildResult {
        val runner = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments("generateOpenApiTests")
        if (gradleVersion != null) {
            runner.withGradleVersion(gradleVersion)
        }
        return runner.build()
    }

    private fun assertGenerationSucceeded(result: org.gradle.testkit.runner.BuildResult) {
        assertThat(result.task(":generateOpenApiTests")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/generated-tests").toFile().listFiles()).isNotEmpty()
    }

    @ParameterizedTest(name = "Gradle {0}")
    @MethodSource("gradleVersions")
    @DisplayName("generates test suites in consumer projects across Gradle versions")
    fun worksAcrossGradleVersions(gradleVersion: String) {
        writeConsumerProject()

        assertGenerationSucceeded(runGeneration(gradleVersion))
    }

    @Test
    @DisplayName("tolerates another buildscript dependency requesting an older Jackson")
    fun toleratesOlderJacksonRequestOnClasspath() {
        // Simulates a consumer whose other build plugins pull an older Jackson onto the same
        // classpath (like openapi-generator in the samples); Gradle resolves to the highest.
        writeConsumerProject(
            """
            buildscript {
                repositories {
                    mavenCentral()
                }
                dependencies {
                    classpath("com.fasterxml.jackson.core:jackson-databind:2.17.2")
                }
            }
            """.trimIndent(),
        )

        assertGenerationSucceeded(runGeneration())
    }

    @Test
    @DisplayName("works when the consumer forces Jackson down to the swagger-parser build version")
    fun worksWithJacksonForcedToSwaggerFloor() {
        // swagger-parser builds against this Jackson line (see its parent POM); a consumer pinning
        // it must not break the plugin. Guards the tolerated floor of our supported Jackson range.
        writeConsumerProject(
            """
            buildscript {
                configurations.all {
                    resolutionStrategy.eachDependency {
                        if (requested.group.startsWith("com.fasterxml.jackson") && requested.name != "jackson-annotations") {
                            useVersion("2.21.1")
                        }
                    }
                }
            }
            """.trimIndent(),
        )

        assertGenerationSucceeded(runGeneration())
    }

    private companion object {
        @JvmStatic
        fun gradleVersions(): List<String> =
            System.getProperty("compat.gradle.versions").orEmpty()
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
                .ifEmpty { error("System property compat.gradle.versions is not set or empty") }

        val MINIMAL_SPEC = """
            openapi: 3.1.0
            info:
              title: Minimal Test API
              version: 1.0.0
            paths:
              /users:
                get:
                  summary: List users
                  operationId: listUsers
                  parameters:
                    - name: page
                      in: query
                      required: true
                      schema:
                        type: integer
                        minimum: 1
                        maximum: 100
                  responses:
                    '200':
                      description: Users list
                    '400':
                      description: Bad request
        """.trimIndent()
    }
}
