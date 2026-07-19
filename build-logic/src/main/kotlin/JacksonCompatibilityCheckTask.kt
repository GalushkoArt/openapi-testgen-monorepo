import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Guards the resolved runtime classpath against Jackson version drift.
 *
 * Enforced constraints:
 *  - No Jackson 3 artifacts (`tools.jackson*` groups): swagger-core/swagger-parser do not fully
 *    support Jackson 3, so the whole build must stay on the `com.fasterxml.jackson` 2.x line.
 *  - Every resolved `com.fasterxml.jackson*` module matches the version catalog pins, so a
 *    transitive bump (for example a newer swagger-parser requiring a newer Jackson) fails loudly
 *    with an actionable message instead of silently producing a mixed Jackson tree.
 *
 * The resolved graph is captured as a [ResolvedComponentResult] provider, which is safe to use
 * with the configuration cache.
 */
abstract class JacksonCompatibilityCheckTask : DefaultTask() {

    /** Root of the resolved runtime classpath graph. */
    @get:Input
    abstract val rootComponent: Property<ResolvedComponentResult>

    /** Expected version for all Jackson 2.x modules (catalog `jackson-lib`). */
    @get:Input
    abstract val expectedJacksonVersion: Property<String>

    /** Expected version for jackson-annotations (catalog `jackson-annotations`; no patch component since 2.20). */
    @get:Input
    abstract val expectedAnnotationsVersion: Property<String>

    @TaskAction
    fun verifyJacksonAlignment() {
        val modules = collectResolvedModules(rootComponent.get())

        val jackson3 = modules.filter { it.group.startsWith("tools.jackson") }
        if (jackson3.isNotEmpty()) {
            throw GradleException(
                "Jackson 3 artifacts found on the runtime classpath: ${jackson3.joinToString()}. " +
                    "The swagger-core/swagger-parser modules do not fully support Jackson 3; " +
                    "this project must stay on the com.fasterxml.jackson 2.x line.",
            )
        }

        val misaligned = modules
            .filter { it.group.startsWith("com.fasterxml.jackson") }
            .filterNot { it.version == expectedVersionFor(it.name) }
        if (misaligned.isNotEmpty()) {
            throw GradleException(
                "Jackson modules resolved to versions that differ from the version catalog pins: " +
                    "${misaligned.joinToString()}. Expected ${expectedJacksonVersion.get()} " +
                    "(jackson-annotations: ${expectedAnnotationsVersion.get()}). " +
                    "Align the `jackson-lib`/`jackson-annotations` versions in gradle/libs.versions.toml " +
                    "with what the swagger modules require (see their parent POMs), or investigate " +
                    "which dependency drags in the diverging version.",
            )
        }
    }

    private fun expectedVersionFor(moduleName: String): String =
        if (moduleName == "jackson-annotations") expectedAnnotationsVersion.get() else expectedJacksonVersion.get()

    private data class ResolvedModule(val group: String, val name: String, val version: String) {
        override fun toString(): String = "$group:$name:$version"
    }

    private fun collectResolvedModules(root: ResolvedComponentResult): List<ResolvedModule> {
        val visited = mutableSetOf(root.id)
        val queue = ArrayDeque(listOf(root))
        val modules = mutableSetOf<ResolvedModule>()
        while (queue.isNotEmpty()) {
            val component = queue.removeFirst()
            component.moduleVersion?.let { id ->
                modules.add(ResolvedModule(id.group, id.name, id.version))
            }
            component.dependencies
                .filterIsInstance<ResolvedDependencyResult>()
                .map { it.selected }
                .filter { visited.add(it.id) }
                .forEach(queue::add)
        }
        return modules.sortedBy(ResolvedModule::toString)
    }
}
