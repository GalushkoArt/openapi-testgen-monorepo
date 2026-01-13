package art.galushko.openapi.testgen.plugin

import art.galushko.openapi.testgen.distribution.TestGenerationReporter
import art.galushko.openapi.testgen.model.error.GenerationReport
import art.galushko.openapi.testgen.reporting.reporter.ConsoleReporter
import org.gradle.api.logging.Logger

/**
 * [TestGenerationReporter] implementation using Gradle's project logger.
 *
 * Uses `lifecycle` level for info messages (always shown unless quiet mode)
 * and `error` level for error messages.
 *
 * @param logger the Gradle project logger
 */
internal class GradleReporter(
    private val logger: Logger,
) : TestGenerationReporter {

    private val consoleReporter = ConsoleReporter()

    override fun logInfo(message: String) {
        logger.lifecycle(message)
    }

    override fun logError(message: String) {
        logger.error(message)
    }

    override fun formatReport(report: GenerationReport): String {
        return consoleReporter.format(report)
    }
}
