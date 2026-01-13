package art.galushko.openapi.testgen.reporting.reporter

import art.galushko.openapi.testgen.model.error.GenerationReport
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * Serializes a [GenerationReport] to pretty-printed JSON using Jackson.
 *
 * Inputs: a [GenerationReport] instance.
 * Output: a JSON string; formatting order follows Jackson defaults and collection order in the report.
 * Errors: throws a Jackson exception if serialization fails.
 */
public class JsonErrorReporter(
    private val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
) : ErrorReporter {
    override fun format(report: GenerationReport): String {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report)
    }
}
