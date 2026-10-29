package org.dsqrwym.langora.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * It exists to get the request and response id even without tracing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceFilter extends OncePerRequestFilter {
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * An upstream trace id is echoed into the response header and written to every log line of the
     * request, so it is only trusted when it cannot corrupt either: no CR/LF (log injection and
     * response splitting), no whitespace, brackets or other separators (they break log grepping and
     * the {@code [requestId]} column), and capped at 64 characters.
     */
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Prefer a well-formed upstream X-Trace-Id, otherwise generate one.
        String id = resolveTraceId(request);

        MDC.put(ApiResponse.REQUEST_ID_KEY, id);
        response.setHeader(TRACE_ID_HEADER, id);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Must clear in finally, otherwise thread reuse will leak it.
            MDC.remove(ApiResponse.REQUEST_ID_KEY);
        }

    }

    private static String resolveTraceId(HttpServletRequest request) {
        String upstream = request.getHeader(TRACE_ID_HEADER);
        if (upstream != null && TRACE_ID_PATTERN.matcher(upstream).matches()) {
            return upstream;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
