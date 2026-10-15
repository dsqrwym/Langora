package org.dsqrwym.langora.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dsqrwym.langora.infrastructure.util.TextUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * It exists to get the request and response id even without tracing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceFilter extends OncePerRequestFilter {
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Prefer the upstream X-Trace-Id if present.
        String id = TextUtils.defaultIfBlank(request.getHeader(TRACE_ID_HEADER), UUID.randomUUID().toString().replace("-", ""));

        MDC.put(ApiResponse.TRACE_ID_KEY, id);
        response.setHeader(TRACE_ID_HEADER, id);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Must clear in finally, otherwise thread reuse will leak it.
            MDC.remove(ApiResponse.TRACE_ID_KEY);
        }

    }
}
