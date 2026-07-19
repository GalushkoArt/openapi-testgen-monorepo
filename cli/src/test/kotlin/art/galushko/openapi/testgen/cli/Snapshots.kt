package art.galushko.openapi.testgen.cli

import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Opt-in golden-file regeneration for CLI snapshot tests.
 *
 * Run with `UPDATE_SNAPSHOTS=true ./gradlew :cli:test` to copy produced output over the
 * committed expectations instead of comparing; rerun without the variable to verify.
 */
object Snapshots {

    private val updateSnapshots: Boolean = System.getenv("UPDATE_SNAPSHOTS") == "true"

    /**
     * Compares the produced [outputFile] with the committed test resource [resourceName],
     * or overwrites the resource when regeneration is enabled.
     */
    fun assertMatchesResource(outputFile: Path, resourceName: String) {
        if (updateSnapshots) {
            File("src/test/resources", resourceName).writeText(outputFile.readText())
            return
        }
        val expected = requireNotNull(Snapshots::class.java.classLoader.getResource(resourceName)).readText().trim()
        assertEquals(expected.lines(), outputFile.readText().trim().lines(), "Output file content does not match expected")
    }
}
