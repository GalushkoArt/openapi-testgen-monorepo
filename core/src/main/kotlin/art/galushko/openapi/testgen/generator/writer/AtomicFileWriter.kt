package art.galushko.openapi.testgen.generator.writer

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Writes files atomically: content is first written to a temporary sibling file which is then
 * moved into place with [StandardCopyOption.ATOMIC_MOVE], so readers never observe partially
 * written output. Write failures propagate to the caller; the temporary file is always cleaned up.
 */
public object AtomicFileWriter {

    /**
     * Writes to [outFile] by invoking [writeContent] against a temporary sibling file and
     * atomically moving the result into place. Missing parent directories are created.
     *
     * @param outFile target file; must have a parent directory
     * @param writeContent callback that writes the content to the provided temporary file
     * @throws IllegalArgumentException if [outFile] has no parent directory
     * @throws java.io.IOException if writing or moving fails
     */
    public fun write(outFile: File, writeContent: (File) -> Unit) {
        val parentDir = requireNotNull(outFile.parentFile) {
            "Output file must have a parent directory: $outFile"
        }
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }
        val tmpFile = File(parentDir, outFile.name + ".tmp")
        try {
            writeContent(tmpFile)
            Files.move(
                tmpFile.toPath(),
                outFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } finally {
            runCatching { tmpFile.delete() }
        }
    }
}
