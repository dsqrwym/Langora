package org.dsqrwym.langora.infrastructure.security.captcha.verifier;

public interface CaptchaVerifier {
    boolean enabled();
    // "turnstile" / "none"，给前端排查用
    String provider();
    String siteKey();
    String action();

    /**
     * throw CaptchaException if verify failed
     */
    void verify(String token, String remoteIp);
}
