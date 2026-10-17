package org.dsqrwym.langora.infrastructure.web.exception;

import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.springframework.http.HttpStatus;

public final class UnauthorizedException extends BusinessException {
    public UnauthorizedException(ErrorCode code) {
        super(HttpStatus.UNAUTHORIZED, code);
    }

    public UnauthorizedException() {
        this(ErrorCode.AUTH_ACCESS_TOKEN_INVALID);
    }
}
