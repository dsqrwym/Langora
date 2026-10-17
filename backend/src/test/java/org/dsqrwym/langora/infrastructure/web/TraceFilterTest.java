package org.dsqrwym.langora.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TraceFilterTest {

    private final TraceFilter filter = new TraceFilter();

    @Test
    void generatesResponseTraceIdAndClearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            String traceId = MDC.get(ApiResponse.TRACE_ID_KEY);
            assertNotNull(traceId);
            assertEquals(traceId, ((MockHttpServletResponse) res).getHeader("X-Trace-Id"));
        });

        assertNotNull(response.getHeader("X-Trace-Id"));
        assertNull(MDC.get(ApiResponse.TRACE_ID_KEY));
    }

    @Test
    void preservesProvidedTraceIdForTrustedUpstreamCompatibility() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "gateway-trace-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                assertEquals("gateway-trace-42", MDC.get(ApiResponse.TRACE_ID_KEY)));

        assertEquals("gateway-trace-42", response.getHeader("X-Trace-Id"));
    }
}
