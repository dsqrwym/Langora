package org.dsqrwym.langora.infrastructure.security.captcha.verifier;

/**
 * 不开启 CAPTCHA 就会提供这个
 */
public final class NoopCaptchaVerifier implements CaptchaVerifier {
    /**
     * 单例
     */
    public static final NoopCaptchaVerifier INSTANCE = new NoopCaptchaVerifier();
    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public String provider() {
        return "none";
    }

    @Override
    public String siteKey() {
        return "";
    }

    @Override
    public String action() {
        return "";
    }

    @Override
    public void verify(String token, String remoteIp) {

    }
}
