package org.dsqrwym.langora.auth.captcha;

import org.dsqrwym.langora.infrastructure.security.captcha.verifier.CaptchaVerifier;
import org.dsqrwym.langora.infrastructure.web.ApiResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class CaptchaConfigControllerTest {

    @Test
    void exposesOnlyThePublicSiteKeyAndAction() {
        CaptchaVerifier verifier = mock(CaptchaVerifier.class);
        given(verifier.enabled()).willReturn(true);
        given(verifier.provider()).willReturn("turnstile");
        given(verifier.siteKey()).willReturn("0x4AAAAAAA");
        given(verifier.action()).willReturn("langora-login");

        ApiResponse<CaptchaConfigResponse> response = new CaptchaConfigController(verifier).config();

        assertEquals(200, response.status());
        assertEquals("OK", response.code());
        assertTrue(response.data().enabled());
        assertEquals("turnstile", response.data().provider());
        assertEquals("0x4AAAAAAA", response.data().siteKey());
        assertEquals("langora-login", response.data().action());
    }

    @Test
    void disabledVerifierReturnsAnEmptySiteKey() {
        CaptchaVerifier verifier = mock(CaptchaVerifier.class);
        given(verifier.enabled()).willReturn(false);
        given(verifier.provider()).willReturn("none");
        given(verifier.siteKey()).willReturn("");
        given(verifier.action()).willReturn("");

        ApiResponse<CaptchaConfigResponse> response = new CaptchaConfigController(verifier).config();

        assertFalse(response.data().enabled());
        assertEquals("none", response.data().provider());
        assertEquals("", response.data().siteKey());
    }
}
