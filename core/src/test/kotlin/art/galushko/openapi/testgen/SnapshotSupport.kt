package art.galushko.openapi.testgen

import java.io.File

/**
 * Opt-in golden-file regeneration for snapshot tests.
 *
 * Run with `UPDATE_SNAPSHOTS=true ./gradlew :core:test` to copy the produced output over the
 * committed expectation instead of comparing; rerun without the variable to verify. Review the
 * resulting git diff deliberately — the snapshots are the contract.
 */
object SnapshotSupport {

    val updateSnapshots: Boolean = System.getenv("UPDATE_SNAPSHOTS") == "true"

    /**
     * When regeneration is enabled, copies [producedFile] over `src/test/resources/[resourcePath]`
     * and returns true so the caller can skip the comparison for this run.
     */
    fun maybeUpdateSnapshot(producedFile: File, resourcePath: String): Boolean {
        if (!updateSnapshots) return false
        val target = File("src/test/resources", resourcePath)
        target.parentFile.mkdirs()
        producedFile.copyTo(target, overwrite = true)
        return true
    }
}
