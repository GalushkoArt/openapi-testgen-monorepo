package art.galushko.openapi.testgen.logging

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.OutputStreamAppender
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.util.*

internal object TestLogCapture {
    private const val LOG_PATTERN = "%-5level %logger{120} - %msg%n"

    fun <T> capture(block: () -> T): Pair<T, String> {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)

        val buffer = ByteArrayOutputStream()
        val appender = createAppender(loggerContext, buffer)
        rootLogger.addAppender(appender)

        return try {
            block() to buffer.toString(Charsets.UTF_8)
        } finally {
            rootLogger.detachAppender(appender)
            appender.stop()
        }
    }

    private fun createAppender(
        loggerContext: LoggerContext,
        buffer: ByteArrayOutputStream,
    ): OutputStreamAppender<ILoggingEvent> {
        val encoder = PatternLayoutEncoder().apply {
            context = loggerContext
            pattern = LOG_PATTERN
            start()
        }

        return OutputStreamAppender<ILoggingEvent>().apply {
            context = loggerContext
            name = "TEST_CAPTURE_${UUID.randomUUID()}"
            setEncoder(encoder)
            outputStream = buffer
            start()
        }
    }
}
