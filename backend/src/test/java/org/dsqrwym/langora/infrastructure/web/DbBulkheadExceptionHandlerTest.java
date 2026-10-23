package org.dsqrwym.langora.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The 429 contract has to survive two paths: the transaction boundary (exception propagates as-is)
 * and the jOOQ connection provider (exception may be translated into a {@code DataAccessException}).
 */
@WebMvcTest(controllers = BulkheadRejectionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class DbBulkheadExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mapsTheRejectionTo429WithRetryAfter() throws Exception {
        mockMvc.perform(get("/test/rate-limited"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "2"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void unwrapsARejectionTranslatedByJooq() throws Exception {
        mockMvc.perform(get("/test/translated"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "2"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }

    @Test
    void keepsOtherDataAccessFailuresAt500() throws Exception {
        mockMvc.perform(get("/test/data-access"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }
}
