package org.example.mcptelegram.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.io.PrintWriter
import java.io.StringWriter

class RateLimitFilterTest {

    private lateinit var filter: RateLimitFilter
    private val request: HttpServletRequest = mock()
    private val response: HttpServletResponse = mock()
    private val chain: FilterChain = mock()
    private val writer = StringWriter()

    @BeforeEach
    fun setup() {
        filter = RateLimitFilter()
        whenever(request.getHeader("X-Forwarded-For")).thenReturn(null)
        whenever(response.writer).thenReturn(PrintWriter(writer))
    }

    @Test
    fun `should allow requests within rate limit`() {
        whenever(request.remoteAddr).thenReturn("192.168.1.100")

        repeat(5) {
            filter.doFilter(request, response, chain)
        }

        verify(chain, times(5)).doFilter(request, response)
    }

    @Test
    fun `should block requests exceeding rate limit`() {
        whenever(request.remoteAddr).thenReturn("10.0.0.99")

        // Send 30 allowed requests
        repeat(30) {
            filter.doFilter(request, response, chain)
        }
        // 31st should be rate limited
        filter.doFilter(request, response, chain)

        verify(response, atLeastOnce()).status = 429
        verify(chain, times(30)).doFilter(request, response)
    }

    @Test
    fun `should use X-Forwarded-For header when present`() {
        whenever(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1")

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
        verify(request, never()).remoteAddr
    }
}
