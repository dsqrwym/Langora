package org.dsqrwym.langora.auth.captcha;

public record CaptchaConfigResponse(boolean enabled, String provider, String siteKey, String action) {
}