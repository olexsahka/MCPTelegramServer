package org.example.mcptelegram.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Simple in-memory rate limiter: max 30 requests per minute per IP.
 * Designed for single-user personal server — blocks brute-force and DDoS.
 */
@Component
class RateLimitFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RateLimitFilter::class.java)

    private data class Counter(val count: AtomicInteger = AtomicInteger(0), val windowStart: Long = System.currentTimeMillis())

    private val counters = ConcurrentHashMap<String, Counter>()

    private val maxRequests = 30
    private val windowMs = 60_000L

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val ip = getClientIp(request)
        val now = System.currentTimeMillis()

        val counter = counters.compute(ip) { _, existing ->
            if (existing == null || now - existing.windowStart > windowMs) {
                Counter(AtomicInteger(1), now)
            } else {
                existing.count.incrementAndGet()
                existing
            }
        }!!

        if (counter.count.get() > maxRequests) {
            log.warn("Rate limit exceeded for IP: {}", ip)
            response.status = 429
            response.setHeader("Retry-After", "60")
            response.writer.write("{\"error\":\"Too many requests. Try again in 60 seconds.\"}")
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun getClientIp(request: HttpServletRequest): String {
        return request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.remoteAddr
    }
}
