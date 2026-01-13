package art.galushko.kotlin.spring.rest.assured.config

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class ApiKeyFilter(
    @param:Value("\${app.security.apiKey}")
    private val expectedApiKey: String
) : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val apiKey = request.getHeader("X-API-Key")
        if (expectedApiKey.isNotBlank()) {
            if (expectedApiKey != apiKey) {
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                response.contentType = "application/json"
                response.writer.write("{\"code\":\"unauthorized\",\"message\":\"API key required\"}")
                return
            }
        }
        filterChain.doFilter(request, response)
    }
}
