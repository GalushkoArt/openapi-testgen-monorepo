import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.process.CommandLineArgumentProvider

/**
 * Configuration-cache-safe `-javaagent` provider for the AspectJ weaver.
 *
 * Replaces allure-gradle's `JavaAgentArgumentProvider`, which holds a raw `Configuration` and
 * fails Test-task validation ("property 'jvmArgumentProviders.$1.agentJar' doesn't have a
 * configured value") when the build runs with the configuration cache.
 */
class AspectJWeaverAgentProvider(
    @get:Classpath
    val agentClasspath: FileCollection,
) : CommandLineArgumentProvider {

    override fun asArguments(): List<String> =
        listOf("-javaagent:${agentClasspath.singleFile.absolutePath}")
}
