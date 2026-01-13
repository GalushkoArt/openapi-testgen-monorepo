package art.galushko.openapi.testgen.distribution

import art.galushko.openapi.testgen.model.error.GenerationReport
import art.galushko.openapi.testgen.reporting.reporter.ConsoleReporter
import org.slf4j.Logger

/**
 * Default [TestGenerationReporter] implementation using SLF4J.
 *
 * Uses the provided SLF4J logger for info/error messages and delegates
 * report formatting to the core [ConsoleReporter].
 *
 * @param logger the SLF4J logger to use for output
 */
public class Slf4jReporter(
    private val logger: Logger,
) : TestGenerationReporter {

    private val consoleReporter = ConsoleReporter()

    override fun logInfo(message: String) {
        logger.info(message)
    }

    override fun logError(message: String) {
        logger.error(message)
    }

    override fun formatReport(report: GenerationReport): String {
        return consoleReporter.format(report)
    }
}
