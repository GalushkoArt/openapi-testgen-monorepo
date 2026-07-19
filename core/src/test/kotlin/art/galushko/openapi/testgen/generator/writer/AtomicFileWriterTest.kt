package art.galushko.openapi.testgen.generator.writer

import art.galushko.openapi.testgen.generation.step
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Path

@Epic("Generator")
@Feature("AtomicFileWriter")
@DisplayName("AtomicFileWriter Tests")
class AtomicFileWriterTest {

    @Test
    @DisplayName("should write content and remove the temporary file")
    fun shouldWriteContentAndRemoveTemporaryFile(@TempDir tempDir: Path) {
        val outFile = tempDir.resolve("out.txt").toFile()

        AtomicFileWriter.write(outFile) { tmpFile -> tmpFile.writeText("content") }

        assertThat(outFile.readText()).isEqualTo("content")
        assertThat(File(tempDir.toFile(), "out.txt.tmp")).doesNotExist()
    }

    @Test
    @DisplayName("should create missing parent directories")
    fun shouldCreateMissingParentDirectories(@TempDir tempDir: Path) {
        val outFile = tempDir.resolve("nested/dir/out.txt").toFile()

        AtomicFileWriter.write(outFile) { tmpFile -> tmpFile.writeText("content") }

        assertThat(outFile.readText()).isEqualTo("content")
    }

    @Test
    @DisplayName("should replace existing file content")
    fun shouldReplaceExistingFileContent(@TempDir tempDir: Path) {
        val outFile = tempDir.resolve("out.txt").toFile()
        outFile.writeText("old")

        AtomicFileWriter.write(outFile) { tmpFile -> tmpFile.writeText("new") }

        assertThat(outFile.readText()).isEqualTo("new")
    }

    @Test
    @DisplayName("should propagate write failures and clean up the temporary file")
    @Description("A parent path that is a regular file makes the temporary-file write fail with an IOException")
    fun shouldPropagateWriteFailuresAndCleanUp(@TempDir tempDir: Path) {
        val fileAsDir = tempDir.resolve("not-a-directory").toFile()
        fileAsDir.writeText("blocker")

        step("Write into an output path whose parent is a regular file") {
            assertThatThrownBy {
                AtomicFileWriter.write(File(fileAsDir, "out.txt")) { tmpFile -> tmpFile.writeText("content") }
            }.isInstanceOf(IOException::class.java)
        }

        step("Verify the blocking file is untouched") {
            assertThat(fileAsDir.readText()).isEqualTo("blocker")
        }
    }

    @Test
    @DisplayName("should reject an output file without a parent directory")
    fun shouldRejectOutputFileWithoutParentDirectory() {
        assertThatThrownBy {
            AtomicFileWriter.write(File("no-parent.txt")) { tmpFile -> tmpFile.writeText("content") }
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Output file must have a parent directory: no-parent.txt")
    }
}
