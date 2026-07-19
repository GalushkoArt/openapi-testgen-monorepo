package art.galushko.openapi.testgen.plugin

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * TestKit functional tests: the plugin is applied to a real Gradle build in an isolated
 * project directory, exercising task registration, execution, up-to-date checks, and
 * configuration-cache compatibility end-to-end.
 */
@DisplayName("OpenApiTestGenerator plugin functional tests")
class OpenApiTestGeneratorFunctionalTest {

    @TempDir
    lateinit var projectDir: Path

    @BeforeEach
    fun setUpProject() {
        projectDir.resolve("settings.gradle.kts").toFile()
            .writeText("rootProject.name = \"plugin-functional-test\"\n")
        projectDir.resolve("build.gradle.kts").toFile().writeText(
            """
            plugins {
                id("art.galushko.openapi-test-generator")
            }

            openApiTestGenerator {
                specFile.set("openapi.yaml")
                outputDir.set(layout.buildDirectory.dir("generated-tests"))
                generator.set("test-suite-writer")
                generatorOptions.put("format", "json")
                generatorOptions.put("outputMode", "MULTIPLE_FILES")
            }
            """.trimIndent()
        )
        projectDir.resolve("openapi.yaml").toFile().writeText(MINIMAL_SPEC)
    }

    private fun runner(vararg arguments: String): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir.toFile())
        .withPluginClasspath()
        .withArguments(*arguments)

    private fun generatedTestsDir() = projectDir.resolve("build/generated-tests").toFile()

    @Test
    @DisplayName("applies the plugin and generates test suites")
    fun appliesAndGenerates() {
        val result = runner("generateOpenApiTests").build()

        assertThat(result.task(":generateOpenApiTests")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(generatedTestsDir().listFiles()).isNotEmpty()
    }

    @Test
    @DisplayName("stores and reuses the configuration cache")
    fun worksWithConfigurationCache() {
        val first = runner("--configuration-cache", "generateOpenApiTests").build()
        assertThat(first.output).contains("Configuration cache entry stored")

        // Delete the output so the second run must execute from the serialized task graph
        // instead of short-circuiting as UP-TO-DATE.
        generatedTestsDir().deleteRecursively()

        val second = runner("--configuration-cache", "generateOpenApiTests").build()
        assertThat(second.output).contains("Reusing configuration cache")
        assertThat(second.task(":generateOpenApiTests")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(generatedTestsDir().listFiles()).isNotEmpty()
    }

    @Test
    @DisplayName("is up-to-date on unchanged inputs and re-runs when the spec content changes")
    fun tracksSpecContent() {
        runner("generateOpenApiTests").build()

        val second = runner("generateOpenApiTests").build()
        assertThat(second.task(":generateOpenApiTests")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)

        val spec = projectDir.resolve("openapi.yaml").toFile()
        spec.writeText(spec.readText().replace("maximum: 100", "maximum: 90"))

        val third = runner("generateOpenApiTests").build()
        assertThat(third.task(":generateOpenApiTests")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    @DisplayName("generates when every setting is declared only in the config file")
    fun generatesFromConfigFileOnly() {
        projectDir.resolve("build.gradle.kts").toFile().writeText(
            """
            plugins {
                id("art.galushko.openapi-test-generator")
            }

            openApiTestGenerator {
                configFile.set("config.yaml")
            }
            """.trimIndent()
        )
        projectDir.resolve("config.yaml").toFile().writeText(
            """
            specFile: openapi.yaml
            outputDir: build/generated-tests
            generator: test-suite-writer
            generatorOptions:
              format: json
              outputMode: MULTIPLE_FILES
            """.trimIndent()
        )

        val result = runner("--configuration-cache", "generateOpenApiTests").build()

        assertThat(result.task(":generateOpenApiTests")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("Configuration cache entry stored")
        assertThat(generatedTestsDir().listFiles()).isNotEmpty()

        generatedTestsDir().deleteRecursively()
        val second = runner("--configuration-cache", "generateOpenApiTests").build()

        assertThat(second.output).contains("Reusing configuration cache")
        assertThat(second.task(":generateOpenApiTests")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(generatedTestsDir().listFiles()).isNotEmpty()
    }

    @Test
    @DisplayName("tracks spec content when the spec path comes from the config file")
    fun tracksConfigFileSpecContent() {
        projectDir.resolve("build.gradle.kts").toFile().writeText(
            """
            plugins {
                id("art.galushko.openapi-test-generator")
            }

            openApiTestGenerator {
                configFile.set("config.yaml")
                outputDir.set(layout.buildDirectory.dir("generated-tests"))
                generator.set("test-suite-writer")
                generatorOptions.put("format", "json")
                generatorOptions.put("outputMode", "MULTIPLE_FILES")
            }
            """.trimIndent()
        )
        projectDir.resolve("config.yaml").toFile().writeText("specFile: openapi.yaml\n")

        runner("generateOpenApiTests").build()

        val second = runner("generateOpenApiTests").build()
        assertThat(second.task(":generateOpenApiTests")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)

        val spec = projectDir.resolve("openapi.yaml").toFile()
        spec.writeText(spec.readText().replace("maximum: 100", "maximum: 90"))

        val third = runner("generateOpenApiTests").build()
        assertThat(third.task(":generateOpenApiTests")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    private companion object {
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
