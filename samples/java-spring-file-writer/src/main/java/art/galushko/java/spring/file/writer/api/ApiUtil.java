package art.galushko.java.spring.file.writer.api;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.NativeWebRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Minimal utility shim expected by generated interfaces to emit example responses.
 * This is only used by default interface methods and not by our implementations.
 */
public final class ApiUtil {
    private ApiUtil() {}

    public static void setExampleResponse(NativeWebRequest request, String contentType, String example) {
        if (request == null || example == null) {
            return;
        }
        try {
            HttpServletResponse resp = request.getNativeResponse(HttpServletResponse.class);
            if (resp == null) {
                return;
            }
            resp.setContentType(contentType != null ? contentType : MediaType.TEXT_PLAIN_VALUE);
            byte[] bytes = example.getBytes(StandardCharsets.UTF_8);
            resp.setContentLength(bytes.length);
            OutputStream os = resp.getOutputStream();
            os.write(bytes);
            os.flush();
        } catch (IOException ignored) {
            // Best-effort only; safe to ignore in tests
        }
    }
}


