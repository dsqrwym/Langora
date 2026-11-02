package org.dsqrwym.langora.auth.captcha;

import lombok.RequiredArgsConstructor;
import org.dsqrwym.langora.infrastructure.security.captcha.verifier.CaptchaVerifier;
import org.dsqrwym.langora.infrastructure.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/captcha")
@RequiredArgsConstructor
public class CaptchaConfigController {
    private final CaptchaVerifier captchaVerifier;

    @GetMapping("/config")
    public ApiResponse<CaptchaConfigResponse> config() {
        return ApiResponse.ok(new CaptchaConfigResponse(
                captchaVerifier.enabled(),
                captchaVerifier.provider(),
                captchaVerifier.siteKey(),
                captchaVerifier.action()
        ));
    }
}
