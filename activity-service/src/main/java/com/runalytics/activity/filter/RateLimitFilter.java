package com.runalytics.activity.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servlet filter that enforces per-IP rate limiting on FIT upload endpoints.
 * Allows a maximum of 5 POST requests per hour per IP address.
 * Uses Bucket4j token-bucket algorithm with an in-memory ConcurrentHashMap.
 *
 * When the limit is exceeded, writes a 429 JSON response directly —
 * exceptions thrown from filters are not handled by @RestControllerAdvice.
 *
 * Registered via {@code RateLimitConfig} — not directly annotated with @Component
 * so that @WebMvcTest slices can exclude it without extra configuration.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    static final int MAX_UPLOADS_PER_HOUR = 5;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        // Only apply to POST /activities and POST /activities/fit
        return !"POST".equalsIgnoreCase(method) ||
                (!path.equals("/activities") && !path.equals("/activities/fit"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);

        if (bucket.tryConsume(1)) {
            log.debug("action=rateLimit ip={} status=allowed remaining={}", ip, bucket.getAvailableTokens());
            filterChain.doFilter(request, response);
        } else {
            log.warn("action=rateLimit ip={} status=throttled", ip);
            writeRateLimitResponse(response, ip);
        }
    }

    private void writeRateLimitResponse(HttpServletResponse response, String ip) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", "Too Many Requests");
        body.put("message", "Rate limit exceeded for ip=" + ip + ". Max 5 uploads per hour.");

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(MAX_UPLOADS_PER_HOUR)
                .refillGreedy(MAX_UPLOADS_PER_HOUR, Duration.ofHours(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may contain a chain: "client, proxy1, proxy2"
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
