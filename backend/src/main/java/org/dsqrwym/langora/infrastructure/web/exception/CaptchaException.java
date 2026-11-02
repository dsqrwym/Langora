package org.dsqrwym.langora.infrastructure.web.exception;

import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.springframework.http.HttpStatus;

public final class CaptchaException extends BusinessException {
    public CaptchaException(HttpStatus status, ErrorCode code) {
        super(status, code);
    }

    public static CaptchaException required() {
        return new CaptchaException(HttpStatus.BAD_REQUEST, ErrorCode.AUTH_CAPTCHA_REQUIRED);
    }

    public static CaptchaException invalid() {
        return new CaptchaException(HttpStatus.BAD_REQUEST, ErrorCode.AUTH_CAPTCHA_INVALID);
    }

    public static CaptchaException unavailable() {
        return new CaptchaException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.AUTH_CAPTCHA_UNAVAILABLE);
    }
}
