package org.dsqrwym.langora.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceFilterTest {

    private final TraceFilter filter = new TraceFilter();

    @Test
    void generatesResponseTraceIdAndClearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            String traceId = MDC.get(ApiResponse.REQUEST_ID_KEY);
            assertNotNull(traceId);
            assertEquals(traceId, ((MockHttpServletResponse) res).getHeader("X-Trace-Id"));
        });

        assertNotNull(response.getHeader("X-Trace-Id"));
        assertNull(MDC.get(ApiResponse.REQUEST_ID_KEY));
    }

    @Test
    void preservesProvidedTraceIdForTrustedUpstreamCompatibility() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "gateway-trace-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                assertEquals("gateway-trace-42", MDC.get(ApiResponse.REQUEST_ID_KEY)));

        assertEquals("gateway-trace-42", response.getHeader("X-Trace-Id"));
    }

    @Test
    void acceptedTraceIdIsExposedInTheResponseBodyAsWell() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "gateway-trace-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                assertEquals("gateway-trace-42", ApiResponse.ok(null).traceId()));
    }

    @Test
    void rejectsUpstreamTraceIdContainingLineBreaks() throws Exception {
        assertReplacedWithGeneratedId("abc\nforged log line");
        assertReplacedWithGeneratedId("abc\rforged log line");
    }

    @Test
    void rejectsUpstreamTraceIdWithSeparatorsOrWhitespace() throws Exception {
        assertReplacedWithGeneratedId("abc def");
        assertReplacedWithGeneratedId("[gateway-trace]");
        assertReplacedWithGeneratedId("trace:42");
    }

    @Test
    void rejectsOversizedUpstreamTraceId() throws Exception {
        assertReplacedWithGeneratedId("a".repeat(65));
    }

    @Test
    void acceptsUpstreamTraceIdAtTheLengthLimit() throws Exception {
        String maxLength = "a".repeat(64);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", maxLength);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                assertEquals(maxLength, MDC.get(ApiResponse.REQUEST_ID_KEY)));

        assertEquals(maxLength, response.getHeader("X-Trace-Id"));
    }

    /**
     * A rejected value must not reach the MDC, the response header or the response body; the request
     * must still be traceable through a freshly generated id.
     */
    private void assertReplacedWithGeneratedId(String upstream) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", upstream);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            String traceId = MDC.get(ApiResponse.REQUEST_ID_KEY);
            assertNotNull(traceId);
            assertNotEquals(upstream, traceId);
            assertTrue(traceId.matches("[0-9a-f]{32}"), () -> "unexpected generated trace id: " + traceId);
            assertEquals(traceId, ((MockHttpServletResponse) res).getHeader("X-Trace-Id"));
            assertEquals(traceId, ApiResponse.ok(null).traceId());
        });
    }
}
