package art.galushko.kotlin.spring.rest.assured.api

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.web.context.request.NativeWebRequest
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Minimal utility shim expected by generated interfaces to emit example responses.
 * This is only used by default interface methods and not by our implementations.
 */
object ApiUtil {

    @JvmStatic
    fun setExampleResponse(request: NativeWebRequest?, contentType: String?, example: String?) {
        if (request == null || example == null) {
            return
        }
        val resp: HttpServletResponse = request.getNativeResponse(HttpServletResponse::class.java) ?: return
        resp.contentType = contentType ?: MediaType.TEXT_PLAIN_VALUE
        val bytes = example.toByteArray(StandardCharsets.UTF_8)
        resp.setContentLength(bytes.size)
        val os = resp.outputStream
        os.write(bytes)
        os.flush()
    }
}
