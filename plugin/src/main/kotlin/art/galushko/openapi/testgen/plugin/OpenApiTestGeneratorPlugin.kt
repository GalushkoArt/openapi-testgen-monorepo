package art.galushko.openapi.testgen.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

public class OpenApiTestGeneratorPlugin : Plugin<Project> {
    @Suppress("LongMethod")
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "openApiTestGenerator",
            TestGeneratorExtension::class.java,
            project.objects,
        )

        val testGenerationTask = project.tasks.register(
            "generateOpenApiTests",
            OpenApiTestGeneratorTask::class.java,
        ) { task ->
            task.group = "verification"
            task.description = "Generates tests from an OpenAPI specification"

            task.configFile.set(extension.configFile)
            task.specFile.set(extension.specFile)
            task.outputDir.set(extension.outputDir)
            task.generator.set(extension.generator)
            task.generatorOptions.set(extension.generatorOptions)
            task.alwaysWriteTests.set(extension.alwaysWriteTests)
            task.logLevel.set(extension.logLevel)

            // Copy typed settings from extension to task
            copyTypedSettings(task.testGenerationSettings, extension.testGenerationSettings)
        }

        project.afterEvaluate {
            val generatorId = extension.generator.get()
            val manualOnly = extension.manualOnly.get()

            when (generatorId) {
                "template" -> {
                    project.plugins.withId("java") { _ ->
                        val sourceSets = project.extensions.getByName("sourceSets") as SourceSetContainer
                        sourceSets.named("test").configure { testSet ->
                            testSet.java.srcDir(extension.outputDir)
                        }

                        val compileTestJavaTask = project.tasks.named(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME)
                        compileTestJavaTask.configure {
                            it.mustRunAfter(testGenerationTask)
                            if (!manualOnly) {
                                it.dependsOn(testGenerationTask)
                            }
                        }
                    }
                    project.plugins.withId("org.jetbrains.kotlin.jvm") {
                        val kotlinExt = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
                        kotlinExt.sourceSets.getByName("test").kotlin.srcDir(
                            testGenerationTask.flatMap(OpenApiTestGeneratorTask::outputDir)
                        )

                        val compileTestKotlin = project.tasks.named("compileTestKotlin")
                        compileTestKotlin.configure {
                            it.mustRunAfter(testGenerationTask)
                            if (!manualOnly) {
                                it.dependsOn(testGenerationTask)
                            }
                        }
                    }
                }

                "test-suite-writer" -> {
                    project.plugins.withId("java") {
                        val proc = project.tasks.named(
                            JavaPlugin.PROCESS_TEST_RESOURCES_TASK_NAME,
                            ProcessResources::class.java
                        )
                        proc.configure {
                            it.mustRunAfter(testGenerationTask) // harmless if both run
                            if (!manualOnly) {
                                it.from(testGenerationTask.flatMap(OpenApiTestGeneratorTask::outputDir))
                                it.dependsOn(testGenerationTask)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Copies all typed settings from source to target extension using reflection.
     * Uses Gradle's convention mechanism to wire properties.
     *
     * This method automatically discovers and copies all Gradle Property/MapProperty/SetProperty
     * fields from [TestGenerationSettingsExtension]. When new properties are added to the extension,
     * they will be automatically included without manual updates to this method.
     *
     * @throws IllegalStateException if property copying fails due to reflection issues
     */
    @Suppress("UNCHECKED_CAST")
    private fun copyTypedSettings(
        target: TestGenerationSettingsExtension,
        source: TestGenerationSettingsExtension,
    ) {
        copyNestedProperties(target, source)
    }

    @Suppress("UNCHECKED_CAST")
    private fun copyNestedProperties(target: Any, source: Any) {
        source::class.memberProperties
            .sortedBy { it.name }
            .forEach { property ->
                property.isAccessible = true

                val typedProperty = property as kotlin.reflect.KProperty1<Any, *>
                val sourceValue = typedProperty.get(source)
                val targetValue = typedProperty.get(target)

                when (sourceValue) {
                    is Property<*> -> {
                        (targetValue as Property<Any?>).set(sourceValue as Property<Any?>)
                    }

                    is MapProperty<*, *> -> {
                        (targetValue as MapProperty<Any?, Any?>).set(sourceValue as MapProperty<Any?, Any?>)
                    }

                    is SetProperty<*> -> {
                        (targetValue as SetProperty<Any?>).set(sourceValue as SetProperty<Any?>)
                    }

                    is ListProperty<*> -> {
                        (targetValue as ListProperty<Any?>).set(sourceValue as ListProperty<Any?>)
                    }

                    else -> {
                        if (sourceValue != null && targetValue != null && sourceValue::class == targetValue::class) {
                            copyNestedProperties(targetValue, sourceValue)
                        } else {
                            throw IllegalStateException(
                                "Failed to copy property ${property.name} from source to target. " +
                                    "Source value: $sourceValue, Target value: $targetValue"
                            )
                        }
                    }
                }
            }
    }
}
